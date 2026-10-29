import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec=importlib.util.spec_from_file_location('tizen_prepare',Path(__file__).resolve().parents[1]/'prepare_launcher.py');p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)

class PackageTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name)
    def test_offline_package_has_only_staged_assets_and_no_keys_or_service(self):
        result=p.prepare(self.root/'stage',archive=self.root/'pilot.wgt');self.assertFalse(result['signed']);self.assertTrue(result['manualLaunchOnly'])
        with zipfile.ZipFile(self.root/'pilot.wgt') as z:
            self.assertIn('config.xml',z.namelist());self.assertIn('shared-core.js',z.namelist());self.assertIn('shared-media.js',z.namelist());self.assertNotIn('author-signature.xml',z.namelist());self.assertFalse(any(n.endswith(('.key','.p12','.pem')) for n in z.namelist()));self.assertTrue(all('..' not in n and '\\' not in n for n in z.namelist()))
            html=z.read('index.html').decode();self.assertIn("connect-src 'none'",html);self.assertNotIn('../../',html);self.assertNotIn('../../../',z.read('style.css').decode())
        p.validate_manifest(self.root/'stage/config.xml')
    def test_existing_outputs_are_never_overwritten(self):
        out=self.root/'stage';out.mkdir();(out/'mine.txt').write_text('keep')
        with self.assertRaises(ValueError):p.prepare(out)
        self.assertEqual((out/'mine.txt').read_text(),'keep');archive=self.root/'pilot.wgt';archive.write_bytes(b'keep')
        with self.assertRaises(ValueError):p.prepare(self.root/'new',archive=archive)
        self.assertFalse((self.root/'new').exists());self.assertEqual(archive.read_bytes(),b'keep')
    def test_paired_silent_video_manifest_uses_still_fallback(self):
        media=self.root/'media';media.mkdir();(media/'coast.png').write_bytes(p.icon());(media/'coast.mp4').write_bytes(b'\x00\x00\x00\x18ftypisom'+b'\0'*20)
        result=p.prepare(self.root/'stage',media);self.assertEqual(result['mediaScenes'],1);text=(self.root/'stage/media.js').read_text();self.assertIn('media/coast.png',text);self.assertIn('media/coast.mp4',text)
    def test_unpaired_video_and_extra_private_files_are_rejected_before_output(self):
        media=self.root/'media';media.mkdir();(media/'scene.mp4').write_bytes(b'x')
        with self.assertRaises(ValueError):p.prepare(self.root/'stage',media)
        self.assertFalse((self.root/'stage').exists());(media/'scene.png').write_bytes(p.icon());(media/'certificate.p12').write_bytes(b'private')
        with self.assertRaises(ValueError):p.prepare(self.root/'stage',media)
        self.assertFalse((self.root/'stage').exists())
    def test_bad_still_and_duplicate_scene_are_rejected(self):
        media=self.root/'media';media.mkdir();(media/'scene.png').write_bytes(b'not an image')
        with self.assertRaises(ValueError):p.media_files(media)
        (media/'scene.png').write_bytes(p.icon());(media/'scene.jpg').write_bytes(b'\xff\xd8\xffxx')
        with self.assertRaises(ValueError):p.media_files(media)
    def test_privilege_or_background_drift_fails_manifest_check(self):
        path=self.root/'config.xml';text=(p.SOURCE/'config.xml').read_text();path.write_text(text.replace('background-support="disable"','background-support="enable"'))
        with self.assertRaises(ValueError):p.validate_manifest(path)
        path.write_text(text.replace('</widget>','<access origin="*"/></widget>'))
        with self.assertRaises(ValueError):p.validate_manifest(path)
    def test_local_links_are_not_followed(self):
        media=self.root/'media';media.mkdir();source=self.root/'original.png';source.write_bytes(p.icon())
        try:(media/'scene.png').symlink_to(source)
        except OSError:self.skipTest('OS does not grant symbolic-link creation to this process')
        with self.assertRaises(ValueError):p.media_files(media)

if __name__=='__main__':unittest.main()
