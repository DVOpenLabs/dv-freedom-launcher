import hashlib
import io
from pathlib import Path
import tempfile
import shlex
import unittest
import zipfile
from setup_app.core import SetupError
from setup_app.release import verify_resource_paths
from setup_app.transport import InstallerDevice, PACKAGE


class FakeTransport(InstallerDevice):
    def __init__(self):
        self.paired=False
        self.device=self
        self.log=[]
        self.id='a'*32
        self.hash='b'*64
    def push(self,file,remote,**kwargs):self.log.append(('push',remote))
    def media_call(self,method,value):
        self.log.append((method,value))
        return {'schema':1,'id':self.id,'state':'ready'}
    def _shell(self,command,timeout=25):
        self.log.append(('shell',command))
        return self.hash+'  file' if command.startswith('sha256sum ') else ''


class MediaTransportTest(unittest.TestCase):
    def test_exact_transfer_checksum_review_and_scoped_cleanup(self):
        device=FakeTransport()
        item={'kind':'png','bytes':20,'sha256':device.hash,'name':'plain.png'}
        self.assertEqual(device.send_media(Path('private.png'),item,{'profile':'test','revision':'revision'}),device.id)
        self.assertEqual(device.log[0],('prepare',dict(schema=1,profile='test',revision='revision',**item)))
        commands=[value for kind,value in device.log if kind=='shell']
        remote='/data/local/tmp/dv-freedom-media-'+device.id+'.bin'
        self.assertIn('cat '+remote+' | content write --uri content://'+PACKAGE+'.inbox/upload/'+device.id,commands)
        self.assertEqual(commands[-1],'rm -f '+remote)
        self.assertIn(('review',device.id),device.log)
        self.assertFalse(any('install' in command or 'config.json' in command for command in commands))
    def test_checksum_failure_never_opens_review_and_still_cleans_exact_file(self):
        device=FakeTransport()
        with self.assertRaisesRegex(SetupError,'checksum'):
            device.send_media(Path('private.mp4'),{'kind':'mp4','bytes':20,'sha256':'c'*64,'name':'test.mp4'},{'profile':'test','revision':'revision'})
        self.assertFalse(any(kind=='review' for kind,value in device.log))
        self.assertEqual(device.log[-1],('shell','rm -f /data/local/tmp/dv-freedom-media-'+device.id+'.bin'))
    def test_unsafe_remote_slot_is_rejected_before_any_push(self):
        device=FakeTransport();device.id='../anything'
        with self.assertRaises(SetupError):
            device.send_media(Path('private.png'),{'kind':'png','bytes':20,'sha256':device.hash,'name':'test.png'},{'profile':'test','revision':'revision'})
        self.assertEqual(len(device.log),1)


class ResourcePackagingTest(unittest.TestCase):
    def test_xml_suffix_guard_catches_the_native_complex_color_crash(self):
        with tempfile.TemporaryDirectory() as folder:
            apk=Path(folder)/'test.apk'
            with zipfile.ZipFile(apk,'w') as archive:archive.writestr('res/SX',b'\x03\x00\x08\x00'+b'fixture')
            with self.assertRaisesRegex(SetupError,'suffix'):verify_resource_paths(apk)
            with zipfile.ZipFile(apk,'w') as archive:
                archive.writestr('res/color/focus.xml',b'\x03\x00\x08\x00'+b'fixture')
                archive.writestr('res/drawable/banner.webp',b'RIFFfixture')
            verify_resource_paths(apk)


class HomePreservationTest(unittest.TestCase):
    def device(self,current,candidate='example.stock/.Home'):
        device=FakeTransport();device.current=current;device.commands=[]
        device.read=lambda key:device.current
        def shell(command,timeout=25):
            device.commands.append(command)
            if command.startswith('cmd package query-activities'):return candidate
            if command.startswith('cmd package set-home-activity'):device.current=shlex.split(command)[-1]
            return ''
        device._shell=shell
        return device
    def test_existing_home_is_never_reselected(self):
        device=self.device('example.stock/.Home');device.preserve_home('example.stock/.Home')
        self.assertEqual(device.commands,[])
    def test_only_a_cleared_chooser_restores_exact_registered_original_home(self):
        device=self.device('android/com.android.internal.app.ResolverActivity')
        device.preserve_home('example.stock/.Home')
        self.assertEqual(device.current,'example.stock/.Home')
        self.assertEqual(device.commands[-1],'cmd package set-home-activity --user 0 example.stock/.Home')
    def test_a_different_home_choice_is_not_overwritten(self):
        device=self.device('other.home/.Home')
        with self.assertRaisesRegex(SetupError,'not overwritten'):device.preserve_home('example.stock/.Home')
        self.assertEqual(device.commands,[])
    def test_inner_class_component_is_quoted_without_shell_expansion(self):
        target='example.stock/.Home$Nested'
        device=self.device('android/com.android.internal.app.ResolverActivity',candidate=target)
        device.preserve_home(target)
        self.assertEqual(device.current,target)
        self.assertTrue(device.commands[-1].endswith("'"+target+"'"))
    def test_missing_or_unsafe_original_home_never_changes_routing(self):
        for target in ('bad;command','factory.home/.Home','android/com.android.internal.app.ResolverActivity','example.stock/.Home'):
            device=self.device('android/com.android.internal.app.ResolverActivity',candidate='other.home/.Home')
            with self.assertRaises(SetupError):device.preserve_home(target)
            self.assertFalse(any(command.startswith('cmd package set-home') for command in device.commands))
