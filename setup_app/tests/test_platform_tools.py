import hashlib
import io
import json
from pathlib import Path
import tempfile
import time
import unittest
import zipfile
from setup_app.core import SetupError
from setup_app.platform_tools import PairingTools,metadata,find_adb

def archive(extra=None):
    out=io.BytesIO()
    with zipfile.ZipFile(out,'w') as z:
        for name in ['adb.exe','AdbWinApi.dll','AdbWinUsbApi.dll','NOTICE.txt']:z.writestr('platform-tools/'+name,b'fake-test-file-not-executable')
        if extra:z.writestr(*extra)
    return out.getvalue()
def xml(data):return f'''<repository><license id="test">Example licence for a fake archive; not Google terms.</license><remotePackage path="platform-tools"><revision><major>1</major><minor>2</minor><micro>3</micro></revision><uses-license ref="test"/><channelRef ref="channel-0"/><archives><archive><host-os>windows</host-os><complete><size>{len(data)}</size><checksum type="sha1">{hashlib.sha1(data).hexdigest()}</checksum><url>platform-tools_r1.2.3-win.zip</url></complete></archive></archives></remotePackage></repository>'''.encode()
class ToolsTest(unittest.TestCase):
    def setUp(self):self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.data=archive();self.calls=[];self.t=PairingTools(self.tmp.name,self.fetch)
    def fetch(self,url,limit):self.calls.append(url);return xml(self.data) if url.endswith('.xml') else self.data
    def wait(self):
        for _ in range(100):
            if not self.t.lock.locked():return
            time.sleep(.01)
        self.fail('Tools job did not finish')
    def test_requires_explicit_licence_acceptance_and_current_offer(self):
        info=self.t.info()
        for value in [{'token':info['token'],'acceptLicense':False},{'token':'invented','acceptLicense':True}]:
            with self.assertRaises(SetupError):self.t.install(value)
        self.assertEqual(len(self.calls),1)
    def test_download_extracts_only_allowlisted_files_and_preserves_notice(self):
        self.data=archive(('../../outside.exe',b'ignored'))
        info=self.t.info();self.t.install({'token':info['token'],'acceptLicense':True});self.wait();self.assertEqual(self.t.progress['status'],'done')
        record=json.loads((Path(self.tmp.name)/'platform-tools.json').read_text());self.assertIn('NOTICE.txt',record['files']);self.assertNotIn('outside.exe',record['files']);self.assertTrue(Path(find_adb(self.tmp.name)).is_file())
    def test_checksum_failure_never_activates_tools(self):
        info=self.t.info();self.data=b'corrupted';self.t.install({'token':info['token'],'acceptLicense':True});self.wait();self.assertEqual(self.t.progress['status'],'error');self.assertFalse((Path(self.tmp.name)/'platform-tools.json').exists())
    def test_metadata_rejects_external_archive_and_entities(self):
        for data in [xml(self.data).replace(b'platform-tools_r1.2.3-win.zip',b'https://external.invalid/tool.zip'),b'<!DOCTYPE x [<!ENTITY a "x">]><x/>']:
            with self.assertRaises(SetupError):metadata(data)
    def test_tampered_private_tool_is_not_selected(self):
        info=self.t.info();self.t.install({'token':info['token'],'acceptLicense':True});self.wait();p=Path(find_adb(self.tmp.name));p.write_bytes(b'changed');self.assertNotEqual(find_adb(self.tmp.name),str(p))

if __name__=='__main__':unittest.main()
