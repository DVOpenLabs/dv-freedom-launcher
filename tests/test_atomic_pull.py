import tempfile
import unittest
from pathlib import Path
from test_tv_target import load_tv

class AtomicPullTest(unittest.TestCase):
    def test_interrupted_transfer_retains_backup(self):
        tv=load_tv({"TV_HOST":"192.0.2.1"})
        class Device:
            def pull(self, remote, stage, **kw):
                Path(stage).write_bytes(b'partial')
                raise OSError('interrupted')
        with tempfile.TemporaryDirectory() as folder:
            target=Path(folder)/'backup.json';target.write_bytes(b'original')
            with self.assertRaises(OSError):tv.atomic_pull(Device(),'remote',str(target))
            self.assertEqual(target.read_bytes(),b'original')
            self.assertEqual([p.name for p in Path(folder).iterdir()],['backup.json'])
    def test_invalid_capture_does_not_replace_existing_image(self):
        tv=load_tv({"TV_HOST":"192.0.2.1"})
        class Device:
            def pull(self, remote, stage, **kw):Path(stage).write_bytes(b'not png')
        with tempfile.TemporaryDirectory() as folder:
            target=Path(folder)/'screen.png';target.write_bytes(b'original')
            with self.assertRaises(ValueError):tv.atomic_pull(Device(),'remote',str(target),png=True)
            self.assertEqual(target.read_bytes(),b'original')
