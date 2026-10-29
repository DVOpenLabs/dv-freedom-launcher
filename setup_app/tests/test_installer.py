import base64
import copy
import json
from pathlib import Path
import struct
import tempfile
import threading
import time
import unittest
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization
from setup_app.core import Session,SetupError
from setup_app.installer import Installer
from setup_app.release import Catalog,canonical,digest,apk_certificate
from setup_app.transport import PACKAGE

def lp(value):return struct.pack('<I',len(value))+value
def apk_fixture(certificate=b'unit-test-certificate',payload=b'unit-fixture'):
    # Structural parser fixture, not an Android-verifiable APK. Catalog signature tests are separate.
    signed=lp(b'')+lp(lp(certificate))+lp(b'');signer=lp(signed)+lp(b'')+lp(b'public-key')
    value=lp(lp(signer));pair=struct.pack('<Q',len(value)+4)+struct.pack('<I',0x7109871a)+value
    n=len(pair)+24;block=struct.pack('<Q',n)+pair+struct.pack('<Q',n)+b'APK Sig Block 42';central=len(payload)+len(block)
    footer=b'PK\x05\x06'+b'\x00'*12+struct.pack('<I',central)+b'\x00\x00'
    return payload+block+footer

class FakeInstallerDevice:
    def __init__(self,state,*args):self.s=state
    def connect(self):self.s['log'].append('connect')
    def close(self):pass
    def fingerprint(self):return self.s['device']
    def metadata(self):return None if self.s['apk'] is None else {'path':'/data/app/example/base.apk','versionCode':self.s['version']}
    def pull(self,remote,path):Path(path).write_bytes(self.s['apk'])
    def read(self,key):return 'example.stock/.Home'
    def launch(self):self.s['log'].append('launch')
    def install(self,apk,sha):
        self.s['log'].append('install');self.s['apk']=Path(apk).read_bytes();self.s['version']=14
        if self.s.get('disconnect'):raise OSError('socket closed')
        if self.s.get('barrier'):self.s['barrier'].wait(2)
    def provider(self,method,value=None):
        self.s['log'].append(method)
        if method=='preview':self.s['pending']={'id':'native-preview','requestId':value['requestId']};self.s['revision']='preview';self.s['profile']='new-profile'
        if method=='restore':self.s['revision']='restored';self.s['profile']=value['backup']['profile'];self.s['pending']=None
        if method=='revert':self.s['pending']=None;self.s['profile']='';self.s['revision']='original'
        return {'schema':1,'api':1,'package':PACKAGE,'profile':self.s['profile'],'revision':self.s['revision'],
         'pending':self.s['pending'],'result':self.s.get('result',''),'backup':{'schema':1,'profile':self.s['profile'],'values':{'app_position':'top'}}}
    def open_previous(self,component):self.s['log'].append('previous')

class InstallerTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);root=Path(self.tmp.name);self.bundle=root/'release';self.bundle.mkdir();self.apk=self.bundle/'launcher.apk';self.apk.write_bytes(apk_fixture())
        self.key=Ed25519PrivateKey.generate();self.trust=root/'trust.json';self.trust.write_text(json.dumps({'publicKey':base64.b64encode(self.key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw)).decode()}))
        self.body={'schema':1,'package':PACKAGE,'versionCode':14,'versionName':'test-alpha','minSdk':26,'file':'launcher.apk','bytes':self.apk.stat().st_size,'sha256':digest(self.apk),'certificate':apk_certificate(self.apk),'channel':'private-alpha'};self.sign()
        self.catalog=Catalog(self.bundle,self.trust);self.s=Session(root/'private');self.s.mode='device';self.s.connection={'host':'192.168.1.50','port':5555,'paired':False};self.s.inventory={'apps':[{'package':'example.video','name':'Video'}]}
        self.d={'device':'test-device-binding','apk':None,'version':0,'revision':'original','profile':'','pending':None,'log':[]}
        self.i=Installer(self.s,self.catalog,lambda *args:FakeInstallerDevice(self.d,*args));self.s.installer=self.i
        self.plan=dict(style='clean',position='bottom',size='medium',spacing='comfortable',wallpaper='keep',apps=['example.video'])
    def sign(self):
        (self.bundle/'catalog.json').write_text(json.dumps({'release':self.body,'signature':base64.b64encode(self.key.sign(canonical(self.body))).decode()}))
    def wait(self):
        for _ in range(300):
            if not self.i.lock.locked():return
            time.sleep(.01)
        self.fail('Installer did not finish')
    def start(self):
        review=self.i.review(self.plan);self.i.start({'id':review['id'],'reviewToken':review['reviewToken']});self.wait();return review
    def test_signed_catalog_and_byte_tampering(self):
        self.assertEqual(self.catalog.verify()['package'],PACKAGE);self.apk.write_bytes(self.apk.read_bytes()+b'x')
        with self.assertRaises(SetupError):self.catalog.verify()
    def test_catalog_signature_tampering(self):
        p=self.bundle/'catalog.json';v=json.loads(p.read_text());v['release']['versionCode']=15;p.write_text(json.dumps(v))
        with self.assertRaises(SetupError):self.catalog.verify()
        self.assertFalse(self.catalog.summary()['available'])
    def test_install_requires_exact_review_and_saves_recovery_before_write(self):
        with self.assertRaises(SetupError):self.i.start({'id':'a'*32,'reviewToken':'invented'})
        review=self.start();point=self.i._load(review['id']);self.assertEqual(self.i.progress['status'],'awaiting-tv');self.assertTrue(point['backup']);self.assertIn('install',self.d['log']);self.assertNotIn('home',self.d['log'])
    def test_newer_version_is_not_downgraded(self):
        self.d.update(apk=self.apk.read_bytes(),version=15)
        with self.assertRaises(SetupError):self.i.review(self.plan)
        self.assertNotIn('install',self.d['log'])
    def test_different_installed_certificate_is_blocked(self):
        self.d.update(apk=apk_fixture(b'other-certificate'),version=13)
        with self.assertRaises(SetupError):self.i.review(self.plan)
        self.assertNotIn('install',self.d['log'])
    def test_existing_home_change_invalidates_review(self):
        self.d.update(apk=apk_fixture(payload=b'old-version'),version=13)
        review=self.i.review(self.plan);self.d['revision']='edited'
        self.i.start({'id':review['id'],'reviewToken':review['reviewToken']});self.wait();self.assertEqual(self.i.progress['status'],'needs-attention');self.assertNotIn('install',self.d['log'])
    def test_wrong_device_is_blocked_after_review(self):
        r=self.i.review(self.plan);self.d['device']='another-tv';self.i.start({'id':r['id'],'reviewToken':r['reviewToken']});self.wait();self.assertNotIn('install',self.d['log'])
    def test_lost_install_reply_is_checked_without_reinstall(self):
        self.d['disconnect']=True;r=self.start();self.assertEqual(self.i.progress['status'],'needs-attention');checked=self.i.check({'id':r['id']});self.assertTrue(checked['canContinue']);self.assertTrue(self.i._load(r['id'])['backup']);self.d['disconnect']=False
        self.i.continue_preview({'id':r['id']});self.wait();self.assertEqual(self.d['log'].count('install'),1);self.assertEqual(self.i.progress['status'],'awaiting-tv')
    def test_stop_finishes_android_commit_before_pause(self):
        barrier=threading.Event();self.d['barrier']=barrier;r=self.i.review(self.plan);self.i.start({'id':r['id'],'reviewToken':r['reviewToken']})
        for _ in range(100):
            if 'install' in self.d['log']:break
            time.sleep(.01)
        self.i.cancelled.set();barrier.set();self.wait();self.assertEqual(self.i.progress['status'],'paused');self.assertNotIn('preview',self.d['log'])
    def test_keep_observed_and_restore_guarded(self):
        r=self.start();self.d['pending']=None;self.d['result']=json.dumps({'id':'native-preview','requestId':r['id'],'outcome':'kept'});checked=self.i.check({'id':r['id']});self.assertEqual(checked['state'],'kept')
        with self.assertRaises(SetupError):self.i.action('restore',{'id':r['id'],'revision':'stale'})
        self.i.action('restore',{'id':r['id'],'revision':checked['revision']});self.assertIn('restore',self.d['log'])
    def test_default_home_requires_kept_preview(self):
        r=self.start();checked=self.i.check({'id':r['id']})
        with self.assertRaises(SetupError):self.i.action('home',{'id':r['id'],'revision':checked['revision']})
        self.assertNotIn('home',self.d['log'])
    def test_recovery_point_cannot_be_a_path(self):
        for id in ['../../private','/absolute',None]:
            with self.assertRaises(SetupError):self.i._load(id)
    def test_corrupt_recovery_files_are_skipped_and_not_loaded(self):
        self.i.points.mkdir(parents=True,exist_ok=True);id='f'*32;(self.i.points/(id+'.json')).write_text('[]')
        self.assertEqual(self.i.state()['recoveryPoints'],[])
        with self.assertRaises(SetupError):self.i._load(id)
    def test_unknown_choice_cannot_invoke_transport(self):
        with self.assertRaises(SetupError):self.i.review({**self.plan,'command':'reboot'})
        self.assertEqual(self.d['log'],[])

if __name__=='__main__':unittest.main()
