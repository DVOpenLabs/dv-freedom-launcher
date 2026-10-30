import base64
import json
from pathlib import Path
import tempfile
import threading
import unittest
from types import SimpleNamespace
from unittest.mock import patch
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization
from setup_app.addons import AddonCatalog,AddonCenter,PACKAGES
from setup_app.core import SetupError
from setup_app.release import canonical,digest

class CatalogTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.folder=Path(self.temp.name);self.key=Ed25519PrivateKey.generate()
        self.trust=self.folder/'trust.json';self.trust.write_text(json.dumps({'publicKey':base64.b64encode(self.key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw)).decode()}))
        self.items=[]
        for id,pkg in PACKAGES.items():
            file=self.folder/(id+'.apk');file.write_bytes(b'fixed-test-artifact-'+id.encode())
            self.items.append(dict(id=id,package=pkg,file=file.name,versionCode=1,versionName='test',minSdk=26,bytes=file.stat().st_size,sha256=digest(file),certificate='a'*64,permissions=[]))
        self.save();self.catalog=AddonCatalog(self.folder,self.trust)
        mock=patch('setup_app.addons.apk_certificate',return_value='a'*64);mock.start();self.addCleanup(mock.stop)
    def save(self):
        body={'schema':1,'items':self.items};(self.folder/'catalog.json').write_text(json.dumps({'addons':body,'signature':base64.b64encode(self.key.sign(canonical(body))).decode()}))
    def test_six_reviewed_artifacts_verify(self):
        for id in PACKAGES:self.assertEqual(self.catalog.verify(id)['package'],PACKAGES[id])
    def test_tampered_apk_is_rejected(self):
        (self.folder/'weather.apk').write_bytes(b'changed')
        with self.assertRaises(SetupError):self.catalog.verify('weather')
    def test_unsigned_catalog_edit_is_rejected(self):
        doc=json.loads((self.folder/'catalog.json').read_text());doc['addons']['items'][0]['versionCode']=99;(self.folder/'catalog.json').write_text(json.dumps(doc))
        with self.assertRaises(SetupError):self.catalog.verify('weather')
    def test_signed_unknown_identity_is_still_rejected(self):
        self.items[0]['package']='org.unreviewed.app';self.save()
        with self.assertRaises(SetupError):self.catalog.verify('weather')
    def test_path_escape_is_rejected_even_when_signed(self):
        self.items[0]['file']='../weather.apk';self.save()
        with self.assertRaises(SetupError):self.catalog.verify('weather')
    def test_duplicate_catalog_ids_rejected(self):
        self.items[1]=dict(self.items[0]);self.save()
        with self.assertRaises(SetupError):self.catalog.verify('weather')
    def test_preview_lists_permissions_but_cannot_install(self):
        s=SimpleNamespace(mode='preview',inventory=None,connection=None,root=self.folder,installer=SimpleNamespace(lock=threading.Lock()))
        center=AddonCenter(s,self.catalog)
        self.assertFalse(center.catalogue()['connected'])
        with self.assertRaises(SetupError):center.review({'id':'weather'})
        with self.assertRaises(SetupError):center.install({'id':'weather','reviewToken':'made-up'})

class TransactionTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name);self.apk=self.root/'source.apk';self.apk.write_bytes(b'tested app')
        self.item=dict(id='companions',package=PACKAGES['companions'],path=self.apk,file='companions.apk',versionCode=1,versionName='test',minSdk=26,bytes=self.apk.stat().st_size,sha256=digest(self.apk),certificate='a'*64,permissions=['android.permission.INTERNET'])
        self.device=FakeDevice(self.apk);self.session=SimpleNamespace(mode='device',inventory={'api':34},connection={'host':'example','port':5555,'paired':False},root=self.root,installer=SimpleNamespace(lock=threading.Lock()))
        catalog=SimpleNamespace(verify=lambda id:dict(self.item),summary=lambda id:{'available':True})
        self.center=AddonCenter(self.session,catalog,lambda *args:self.device)
        mock=patch('setup_app.addons.apk_certificate',return_value='a'*64);mock.start();self.addCleanup(mock.stop)
    def test_review_never_installs_or_grants_and_commit_verifies(self):
        r=self.center.review({'id':'companions'});self.assertEqual(self.device.installs,0);self.assertFalse(r['accessEnabled'])
        result=self.center.install({'id':r['id'],'reviewToken':r['reviewToken']});self.assertTrue(result['installed']);self.assertEqual(self.device.installs,1);self.assertEqual(self.device.home,'org.home/.Main')
        with self.assertRaises(SetupError):self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
    def test_device_switch_after_review_is_blocked(self):
        r=self.center.review({'id':'companions'});self.device.fingerprint_value='another-device'
        with self.assertRaises(SetupError):self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
        self.assertEqual(self.device.installs,0)
    def test_changed_home_is_not_overwritten(self):
        r=self.center.review({'id':'companions'});self.device.home='other.home/.Main'
        with self.assertRaises(SetupError):self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
        self.assertEqual(self.device.installs,0)
    def test_connection_change_requires_new_review(self):
        r=self.center.review({'id':'companions'});self.session.connection['host']='other'
        with self.assertRaises(SetupError):self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
    def test_catalog_change_after_review_is_blocked(self):
        r=self.center.review({'id':'companions'});self.item['versionCode']=2
        with self.assertRaises(SetupError):self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
    def test_failure_leaves_recovery_record(self):
        r=self.center.review({'id':'companions'});self.device.fail=True
        with self.assertRaises(SetupError):self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
        self.assertEqual(json.loads((Path(r['recoveryPath'])/'record.json').read_text())['state'],'needs-attention')
    def test_open_requires_installed_matching_artifact(self):
        with self.assertRaises(SetupError):self.center.launch({'id':'companions'})
        r=self.center.review({'id':'companions'});self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
        self.assertTrue(self.center.launch({'id':'companions'})['opened']);self.assertEqual(self.device.opened,PACKAGES['companions'])
    def test_changed_installed_artifact_cannot_be_opened_as_reviewed(self):
        r=self.center.review({'id':'companions'});self.center.install({'id':r['id'],'reviewToken':r['reviewToken']})
        self.apk.write_bytes(b'another app')
        with self.assertRaises(SetupError):self.center.launch({'id':'companions'})

class FakeDevice:
    def __init__(self,apk):self.apk=apk;self.installs=0;self.home='org.home/.Main';self.fingerprint_value='fixed-device';self.fail=False
    def connect(self):pass
    def close(self):pass
    def fingerprint(self):return self.fingerprint_value
    def read(self,key):return '34' if key=='api' else self.home
    def metadata(self,pkg):return {'path':'/data/app/fixed/base.apk','versionCode':1} if self.installs else None
    def pull(self,remote,path):path.write_bytes(self.apk.read_bytes())
    def install(self,apk,sha):
        if self.fail:raise SetupError('Test interruption')
        self.installs+=1
    def preserve_home(self,home):assert self.home==home
    def open_addon(self,package):self.opened=package

if __name__=='__main__':unittest.main()
