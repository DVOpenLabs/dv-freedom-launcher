import hashlib
import http.client
import io
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib
from setup_app.core import Session, SetupError
from setup_app.phone import PhoneSetup, media_kind, MAX_FILE, MAX_INBOX


def picture():
    def chunk(kind,data): return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0\0\0\0'))+chunk(b'IEND',b'')


class PhoneFixture(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.session=Session(Path(self.tmp.name));self.session.preview();self.now=1000
        self.phone=PhoneSetup(self.session,addresses=lambda:['127.0.0.1'],clock=lambda:self.now)
        self.addCleanup(self.phone.stop)
        self.plan=dict(style='clean',position='bottom',size='medium',spacing='comfortable',wallpaper='coast',apps=['example.video'])
    def start(self,share=False):return self.phone.start(dict(address='127.0.0.1',shareApps=share,choices=self.plan,consent=True))
    def pair(self):return self.phone.pair(self.phone.pin)


class PhoneTests(PhoneFixture):
    def test_sharing_never_starts_from_missing_consent_or_unowned_address(self):
        for change in [dict(consent=False),dict(address='0.0.0.0'),dict(shareApps='yes')]:
            with self.assertRaises(SetupError):self.phone.start(dict(address='127.0.0.1',shareApps=False,choices=self.plan,consent=True,**{})|change)
        self.assertIsNone(self.phone.server)
    def test_pairing_is_bounded_one_phone_and_revocable(self):
        self.start();pin=self.phone.pin
        for _ in range(5):
            with self.assertRaises(SetupError):self.phone.pair('xxxxxx')
        with self.assertRaises(SetupError):self.phone.pair(pin)
        self.now+=61;token=self.phone.pair(pin)
        self.assertNotEqual(token,self.phone.token_hash)
        self.assertTrue(self.phone.authenticated('dv_phone='+token))
        self.assertFalse(self.phone.authenticated('dv_phone=wrong'))
        self.assertFalse(self.phone.authenticated('bad['))
        with self.assertRaises(SetupError):self.phone.pair(pin)
        self.phone.stop();self.assertFalse(self.phone.authenticated('dv_phone='+token))
    def test_pair_and_session_expiry_are_separate(self):
        self.start();self.now+=301
        with self.assertRaises(SetupError):self.pair()
        self.phone.stop();self.start();token=self.pair();self.now+=901
        self.assertFalse(self.phone.authenticated('dv_phone='+token))
    def test_drafts_are_only_received_and_app_sharing_is_separate(self):
        self.start();self.pair();before=json.dumps(self.session.public(),sort_keys=True)
        self.phone.draft(dict(style='cinema',position='top'))
        self.assertEqual(json.dumps(self.session.public(),sort_keys=True),before)
        with self.assertRaises(SetupError):self.phone.draft(dict(apps=['example.music']))
        claimed=self.phone.claim({'id':self.phone.pending['id']})
        self.assertFalse(claimed['applied']);self.assertEqual(claimed['choices']['apps'],['example.video'])
        self.assertIsNone(self.phone.server)
    def test_changed_device_or_unknown_apps_invalidate_review(self):
        self.start(True);self.pair()
        with self.assertRaises(SetupError):self.phone.draft(dict(apps=['not.installed']))
        self.phone.draft(dict(size='large'));id=self.phone.pending['id']
        self.session.connection={'host':'example-changed'}
        with self.assertRaises(SetupError):self.phone.claim({'id':id})
    def test_received_file_is_private_checksummed_and_not_selected(self):
        self.start();self.pair();data=picture();before=self.session.public()
        result=self.phone.receive(io.BytesIO(data),len(data),'wallpaper.png')
        self.assertEqual(result['sha256'],hashlib.sha256(data).hexdigest())
        self.assertNotIn('binding',result);self.assertEqual(self.session.public(),before)
        metadata,path=self.phone.file(result['id']);self.assertEqual(path.read_bytes(),data)
        self.phone.stop();self.assertEqual(len(self.phone.files()),1)
        self.phone.remove({'id':result['id']});self.assertFalse(path.exists())
    def test_upload_types_names_truncation_and_image_dimensions_are_checked(self):
        self.start();self.pair()
        for name,data,length in [('../out.png',picture(),len(picture())),('bad.bin',b'MZ'+b'\0'*50,52),('cut.png',picture(),len(picture())+1),('huge.mp4',b'0000ftypisom',MAX_FILE+1)]:
            with self.subTest(name=name),self.assertRaises(SetupError):self.phone.receive(io.BytesIO(data),length,name)
        data=bytearray(picture());data[16:24]=struct.pack('>II',16000,16000)
        with self.assertRaises(SetupError):self.phone.receive(io.BytesIO(data),len(data),'huge.png')
        self.assertFalse(list(self.phone.folder.glob('*.part')))
    def test_paths_and_arbitrary_commands_are_not_a_file_api(self):
        for id in ['../keys','a'*31,'a'*32+'.png']:
            with self.assertRaises(SetupError):self.phone.file(id)
        with self.assertRaises(SetupError):self.phone.remove({'id':'a'*32,'path':'other'})
        self.start();self.pair()
        with self.assertRaises(SetupError):self.phone.draft({'command':'reboot'})
    def test_qr_contains_address_only_and_has_no_code_or_token(self):
        self.start();svg=self.phone.qr();self.assertIn(b'<svg',svg)
        from unittest.mock import patch
        with patch('qrcode.make', wraps=__import__('qrcode').make) as make:
            self.phone.qr();self.assertEqual(make.call_args.args, (self.phone.url,))
        self.assertNotIn(self.phone.pin,self.phone.url)
        self.assertNotIn(b'<script',svg)


class PhoneHTTPTests(PhoneFixture):
    # Reuse the same session fixture, not a public-network listener.
    def request(self,path='/',method='GET',value=None,cookie=None,headers=None):
        h={'Host':self.phone.url.removeprefix('http://'),'Origin':self.phone.url}
        if cookie:h['Cookie']=cookie
        if value is not None:h['Content-Type']='application/json'
        h.update(headers or {})
        c=http.client.HTTPConnection('127.0.0.1',self.phone.server.server_port,timeout=3)
        c.request(method,path,body=json.dumps(value) if value is not None else None,headers=h)
        r=c.getresponse();body=r.read();extra=dict(r.getheaders());c.close();return r.status,body,extra
    def test_http_pairing_origin_cookie_inventory_and_action_boundaries(self):
        self.start()
        self.assertEqual(self.request('/api/state')[0],401)
        self.assertEqual(self.request(headers={'Host':'attacker.invalid'})[0],403)
        self.assertEqual(self.request('/api/pair','POST',{'code':self.phone.pin},headers={'Origin':'https://attacker.invalid'})[0],403)
        status,body,headers=self.request('/api/pair','POST',{'code':self.phone.pin});self.assertEqual(status,200)
        cookie=headers['Set-Cookie'].split(';')[0];self.assertIn('HttpOnly',headers['Set-Cookie']);self.assertIn('SameSite=Strict',headers['Set-Cookie'])
        status,body,_=self.request('/api/state',cookie=cookie);self.assertEqual(status,200)
        data=json.loads(body);self.assertEqual(data['apps'],[]);self.assertNotIn('apps',data['choices'])
        for path in ['/api/install','/api/shell','/api/reboot','/api/stop']:
            self.assertEqual(self.request(path,'POST',{},cookie)[0],404)
        self.assertEqual(self.request('/api/draft','POST',{'position':'middle'},cookie)[0],200)
        self.assertIsNotNone(self.phone.pending)
