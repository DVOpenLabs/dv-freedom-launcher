"""No hardware: review boundaries, pairing, source packages and real loopback HTTP protections."""
import hashlib
import http.client
import json
from pathlib import Path
import socket
import tempfile
import threading
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile
from setup_app.core import SetupError,Session
from setup_app.companion import CompanionSetup,HostedServer,app_entry,choices
from setup_app.tv_bridges import Vizio,Vidaa,PinnedHTTPS
from setup_app.capabilities import catalogue
from platforms.hosted.prepare_launcher import prepare
from platforms.roku.prepare_channel import prepare as roku

STYLE=dict(style='clean',position='bottom',size='medium',spacing='comfortable',wallpaper='coast')
ROW={'name':'Video','appId':'video','url':'video','urlType':37,'storeType':0}

class FakeBridge:
    def __init__(self,*args):self.pin='a'*64;self.calls=[];self.token='';self.closed=False
    def connect(self):self.calls.append('read-apps');return [ROW,dict(name='broken'),ROW]
    def begin(self):self.calls.append('pair-request');return {'pairingRequired':True}
    def pair(self,pin):
        if pin!='1234':raise SetupError('Wrong PIN.')
        self.token='private-token'
    def current(self):return {'APP_ID':'3','NAME_SPACE':4}
    def launch(self,value):self.calls.append(('launch',value))
    def close(self):self.closed=True;self.token=''

class FakeServer:
    def __init__(self,folder,host,port,tv,apps,settings,bridge):self.host=host;self.port=port;self.token='private-session';self.closed=False
    def start(self):return f'http://{self.host}:{self.port}/#'+self.token
    def close(self):self.closed=True

class CompanionTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.profile=self.root/'profile.json';self.profile.write_text('{}')
        self.s=CompanionSetup(self.root,{'vidaa':FakeBridge,'vizio':FakeBridge},FakeServer);self.addCleanup(self.s.disconnect)
    def connect(self,platform='vidaa'):
        if platform=='vizio':self.profile.write_text(json.dumps({'ca':'public.pem','fingerprint':'a'*64}))
        return self.s.connect(dict(platform=platform,host='192.168.50.20',profile=str(self.profile),consent=True))
    def review(self,**changes):
        data=dict(host='192.168.50.10',port=9507,apps=[self.s.apps[0]['id']],choices=STYLE,media='');data.update(changes)
        return self.s.review(data)
    def test_catalogue_describes_every_setup_and_does_not_claim_hardware_acceptance(self):
        c=catalogue();self.assertEqual(set(c),{'android','streamer','lg','tizen','vidaa','vizio','roku','other'})
        self.assertEqual(c['roku']['kind'],'ambient');self.assertTrue(all(not v['hardwareAccepted'] for v in c.values()))
    def test_catalogue_is_copy_not_mutable_global(self):
        a=catalogue();a['roku']['kind']='native';self.assertEqual(catalogue()['roku']['kind'],'ambient')
    def test_connect_requires_literal_private_target_and_explicit_consent(self):
        for extra in [dict(host='8.8.8.8'),dict(host='localhost'),dict(consent=False),dict(platform='roku')]:
            with self.assertRaises(SetupError):self.s.connect({**dict(platform='vidaa',host='192.168.50.20',profile=str(self.profile),consent=True),**extra})
    def test_inventory_keeps_valid_unique_records_only(self):
        result=self.connect();self.assertEqual(len(result['apps']),1);self.assertEqual(self.s.bridge.calls,['read-apps'])
        self.assertNotIn('payload',result['apps'][0]);self.assertNotIn('profile',result)
    def test_vizio_pin_then_actual_capture_before_review(self):
        self.assertTrue(self.connect('vizio')['pairingRequired'])
        with self.assertRaises(SetupError):self.s.pair({'pin':'bad'})
        self.s.pair({'pin':'1234'});self.s.capture({'label':'Netflix'})
        r=self.review();self.assertEqual(r['apps'],['Netflix']);self.assertNotIn('private-token',json.dumps(self.s.public()))
    def test_review_does_not_start_server_or_launch_app(self):
        self.connect();r=self.review();self.assertEqual(r['server'],'192.168.50.10:9507');self.assertIsNone(self.s.server)
        self.assertEqual(self.s.bridge.calls,['read-apps'])
    def test_review_rejects_wrong_host_app_selection_and_missing_media(self):
        self.connect()
        for change in [dict(host='192.168.50.20'),dict(apps=[]),dict(apps=['unknown']),dict(apps=[self.s.apps[0]['id']]*2),dict(choices={**STYLE,'wallpaper':'library'})]:
            with self.assertRaises(SetupError):self.review(**change)
    def test_review_single_use_expiry_and_confirmation(self):
        self.connect();r=self.review()
        for value in [dict(id=r['id'],confirmed=False),dict(id='wrong',confirmed=True)]:
            with self.assertRaises(SetupError):self.s.start(value)
        self.s.reviewed['expires']=0
        with self.assertRaises(SetupError):self.s.start(dict(id=r['id'],confirmed=True))
    def test_media_modified_after_review_cannot_start(self):
        from platforms.tizen.prepare_launcher import icon
        self.connect();media=self.root/'wallpapers';media.mkdir();still=media/'scene.png';still.write_bytes(icon())
        r=self.review(media=str(media),choices={**STYLE,'wallpaper':'library'});still.write_bytes(icon()+b'changed')
        with self.assertRaises(SetupError):self.s.start(dict(id=r['id'],confirmed=True))
        self.assertIsNone(self.s.server);self.assertIsNone(self.s.reviewed)
    def test_trust_helper_rejects_invalid_certificate_or_missing_consent(self):
        cert=self.root/'not-a-certificate.pem';cert.write_text('-----BEGIN CERTIFICATE-----\nZm9v\n-----END CERTIFICATE-----')
        with self.assertRaises(SetupError):self.s.trust_vizio(dict(certificate=str(cert),confirmed=False))
        with self.assertRaises(SetupError):self.s.trust_vizio(dict(certificate=str(cert),confirmed=True))
    def test_start_stop_revokes_link_without_install_or_home_action(self):
        self.connect();r=self.review();result=self.s.start(dict(id=r['id'],confirmed=True));self.assertFalse(result['installed']);self.assertTrue(result['running'])
        server=self.s.server
        self.assertNotIn('private-session',(next(self.s.root.glob('hosted-*'))/'setup-record.json').read_text())
        with self.assertRaises(SetupError):self.s.start(dict(id=r['id'],confirmed=True))
        with self.assertRaises(SetupError):self.connect()
        self.s.stop();self.assertTrue(server.closed);self.assertEqual(self.s.bridge.calls,['read-apps'])
    def test_open_vizio_is_separate_confirmed_normal_conjure_route(self):
        self.connect('vizio');self.s.pair({'pin':'1234'});self.s.capture({'label':'Video'});r=self.review();self.s.start(dict(id=r['id'],confirmed=True))
        with self.assertRaises(SetupError):self.s.open_vizio({'confirmed':False})
        self.s.open_vizio({'confirmed':True});call=self.s.bridge.calls[-1][1];self.assertEqual((call['APP_ID'],call['NAME_SPACE']),('17',4));self.assertIn('#private-session',call['MESSAGE'])
    def test_unknown_fields_never_become_commands(self):
        with self.assertRaises(SetupError):self.s.connect(dict(platform='vidaa',host='192.168.50.20',profile=str(self.profile),consent=True,command='reboot'))
    def test_vizio_system_surface_and_bad_vidaa_shapes_rejected(self):
        for row in [dict(APP_ID='3001',NAME_SPACE=4),dict(APP_ID='17',NAME_SPACE=4),dict(APP_ID='3',NAME_SPACE=True)]:
            with self.assertRaises(SetupError):app_entry('vizio',row,'Video')
        for row in [dict(name='Video',url=['bad']),dict(name='Video',url='video',urlType=True)]:
            with self.assertRaises(SetupError):app_entry('vidaa',row)
    def test_roku_build_download_has_no_device_or_ecp_code(self):
        r=self.s.roku(dict(theme='ocean',clock='24h',confirmed=True));p=self.s.package_file(self.s.package_id)
        with zipfile.ZipFile(p) as z:
            self.assertIn('manifest',z.namelist());source='\n'.join(z.read(n).decode() for n in z.namelist())
            self.assertNotIn('__CLOCK24__',source);self.assertNotIn('roUrlTransfer',source);self.assertNotIn('8060',source);self.assertIn('m.clock24 = true',source)
        with self.assertRaises(SetupError):self.s.package_file('../other')
        p.write_bytes(b'changed')
        with self.assertRaises(SetupError):self.s.package_file(self.s.package_id)
        self.assertFalse(r['installed']);self.assertIsNone(self.s.bridge)
    def test_roku_no_overwrite_bad_config_or_implicit_consent(self):
        p=self.root/'app.zip';roku(p)
        with self.assertRaises(ValueError):roku(p)
        with self.assertRaises(ValueError):roku(self.root/'other.zip',theme='bad')
        with self.assertRaises(SetupError):self.s.roku(dict(theme='ocean',clock='24h',confirmed=False))
    def test_hosted_staging_reuses_renderer_but_not_tizen_manifest(self):
        for platform in ('vidaa','vizio'):
            p=prepare(self.root/platform,platform);self.assertFalse((p/'config.xml').exists())
            html=(p/'index.html').read_text();self.assertIn("connect-src 'self'",html);self.assertIn('bridge.js',html)
            self.assertIn('native=true',(p/'app.js').read_text());self.assertNotIn('or network listener.',(p/'app.js').read_text())
            self.assertTrue((p/'licenses/Outfit-OFL.txt').is_file())
    def test_vizio_pairing_payload_and_pin_expiry(self):
        requests=[]
        def request(method,path,value,token):
            requests.append((method,path,value,token))
            return {'ITEM':{'PAIRING_REQ_TOKEN':45,'CHALLENGE_TYPE':1}} if path.endswith('start') else {'ITEM':{'AUTH_TOKEN':'secret'}}
        with patch('setup_app.tv_bridges.tls_profile',return_value=(None,'a'*64)):
            v=Vizio('192.168.50.20',{'identity':'unique-integration'},requester=request);v.begin();v.pair('1234')
            self.assertEqual(requests[-1][2]['PAIRING_REQ_TOKEN'],45);self.assertEqual(v.token,'secret');self.assertIsNone(v.challenge)
            v.begin();v.challenge=(45,1,0)
            with self.assertRaises(SetupError):v.pair('1234')
    def test_vizio_pin_is_checked_before_auth_request_and_forbidden_route(self):
        with patch('setup_app.tv_bridges.tls_profile',return_value=(None,'a'*64)):
            v=Vizio('192.168.50.20',{'identity':'unique-integration'},requester=lambda *x:self.fail('Network must not be called'))
            with self.assertRaises(SetupError):v.pair('1234')
            with self.assertRaises(SetupError):v.launch({'APP_ID':'3001','NAME_SPACE':4})
    def test_https_pin_failure_happens_before_http_request(self):
        class Sock:
            def getpeercert(self,binary_form):return b'unexpected'
            def close(self):pass
        conn=PinnedHTTPS('192.168.50.20',7345,None,'a'*64)
        def connect(this):this.sock=Sock()
        with patch.object(http.client.HTTPSConnection,'connect',connect):
            with self.assertRaises(SetupError):conn.connect()
    def test_vidaa_never_accepts_wildcards_or_missing_auth_material(self):
        with self.assertRaises(SetupError):Vidaa('192.168.50.20',{})
        p=dict(clientId='test',topicId='wild/#',username='user',password='test-only',certificate='x',key='x',ca='x',fingerprint='a'*64)
        with self.assertRaises(SetupError):Vidaa('192.168.50.20',p)
    def mqtt(self,pin):
        cert=self.root/'test-cert.pem';cert.write_text('synthetic transport fixture')
        class Context:
            def load_cert_chain(self,*args,**kwargs):pass
        class Sent:
            def wait_for_publish(self,timeout):pass
            def is_published(self):return True
        class Client:
            logged_in=False;stopped=False
            def username_pw_set(self,*args):pass
            def tls_set_context(self,*args):pass
            def subscribe(self,rows):self.subscriptions=rows
            def connect(self,*args,**kwargs):
                sock=SimpleNamespace(getpeercert=lambda binary_form:b'test-tv-certificate',close=lambda:None)
                self.on_socket_open(self,None,sock);self.logged_in=True;self.on_connect(self,None,None,0)
            def loop(self,timeout):pass
            def loop_start(self):pass
            def loop_stop(self):self.stopped=True
            def disconnect(self):pass
            def publish(self,topic,payload,qos):
                self.last=(topic,payload)
                if topic.endswith('/applist'):self.on_message(self,None,SimpleNamespace(topic='/remoteapp/mobile/test-device/ui_service/data/applist',payload=json.dumps([ROW]).encode()))
                return Sent()
        profile=dict(clientId='test-client',topicId='test-device',username='synthetic-user',password='synthetic-password',certificate=str(cert),key=str(cert),ca=str(cert),fingerprint=pin)
        client=Client()
        with patch('setup_app.tv_bridges.tls_profile',return_value=(Context(),pin)):
            return Vidaa('192.168.50.20',profile,factory=lambda:client),client
    def test_vidaa_pin_precedes_login_and_catalog_subscriptions_are_exact(self):
        bridge,client=self.mqtt(hashlib.sha256(b'test-tv-certificate').hexdigest())
        self.assertEqual(bridge.connect(),[ROW]);self.assertTrue(client.logged_in)
        self.assertEqual(len(client.subscriptions),2);self.assertTrue(all('#' not in topic and '+' not in topic for topic,_ in client.subscriptions))
        bridge.launch(ROW);self.assertTrue(client.last[0].endswith('/actions/launchapp'))
        bridge.close();self.assertTrue(client.stopped);self.assertFalse(bridge.ready)
    def test_vidaa_changed_pin_sends_no_login(self):
        bridge,client=self.mqtt('a'*64)
        with self.assertRaises(SetupError):bridge.connect()
        self.assertFalse(client.logged_in);bridge.close()

class HostedHTTPTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name);(self.root/'index.html').write_text('Home');(self.root/'sample.mp4').write_bytes(b'0123456789')
        self.bridge=FakeBridge();app=app_entry('vidaa',ROW)
        with patch('setup_app.companion.address',return_value=('127.0.0.1',0)):
            self.s=HostedServer(self.root,'127.0.0.1',0,'127.0.0.1',[app],choices(STYLE),self.bridge)
        # Reserve a real ephemeral loopback port, keeping production LAN validation unchanged.
        sock=socket.socket();sock.bind(('127.0.0.1',0));self.s.port=sock.getsockname()[1];sock.close()
        self.url=self.s.start();self.addCleanup(self.s.close);self.app=app
    def request(self,path='/',method='GET',value=None,headers=None):
        conn=http.client.HTTPConnection('127.0.0.1',self.s.port,timeout=3)
        h={'X-Freedom-Session':self.s.token,**(headers or {})};body=json.dumps(value) if value is not None else None
        if value is not None:h.setdefault('Content-Type','application/json')
        conn.request(method,path,body=body,headers=h);r=conn.getresponse();raw=r.read();status=r.status;extra=dict(r.getheaders());conn.close();return status,raw,extra
    def test_actual_http_rejects_wrong_token_host_origin_and_route(self):
        self.assertEqual(self.request('/api/catalog',headers={'X-Freedom-Session':'wrong'})[0],403)
        self.assertEqual(self.request(headers={'Host':'attacker.test'})[0],403)
        self.assertEqual(self.request(headers={'Origin':'https://attacker.test'})[0],403)
        self.assertEqual(self.request('/../private.json')[0],404)
        self.assertEqual(self.request('/api/reboot','POST',{})[0],404)
        self.assertEqual(self.bridge.calls,[])
    def test_actual_launch_requires_known_id_and_never_claims_foreground(self):
        self.assertEqual(self.request('/api/launch','POST',{'id':'unknown'})[0],400)
        status,raw,_=self.request('/api/launch','POST',{'id':self.app['id']})
        self.assertEqual(status,200);self.assertFalse(json.loads(raw)['foregroundVerified']);self.assertEqual(self.bridge.calls,[('launch',self.app['payload'])])
    def test_catalog_and_bootstrap_do_not_expose_transport_secrets(self):
        for path in ('/api/catalog','/bootstrap.js'):
            status,raw,_=self.request(path);self.assertEqual(status,200);self.assertNotIn(self.s.token.encode(),raw);self.assertNotIn(b'payload',raw);self.assertNotIn(b'password',raw)
    def test_video_single_range_streaming_and_bad_range(self):
        status,raw,headers=self.request('/sample.mp4',headers={'Range':'bytes=2-5'});self.assertEqual((status,raw),(206,b'2345'));self.assertEqual(headers['Content-Range'],'bytes 2-5/10')
        self.assertEqual(self.request('/sample.mp4',headers={'Range':'bytes=0-2,4-6'})[0],416)
        status,raw,headers=self.request('/sample.mp4',method='HEAD');self.assertEqual(status,200);self.assertEqual(raw,b'');self.assertEqual(headers['Content-Length'],'10')
    def test_shutdown_revokes_session_and_joins_server_thread(self):
        self.s.close();self.assertEqual(self.s.token,'');self.assertIsNone(self.s.thread)
