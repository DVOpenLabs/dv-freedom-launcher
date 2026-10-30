"""Directory and read-only status boundaries, independent of real TVs or release keys."""
import tempfile
import threading
import unittest
from pathlib import Path
from types import SimpleNamespace
from setup_app.addon_directory import items
from setup_app.addons import AddonCenter, AddonDevice, PACKAGES
from setup_app.core import SetupError

class DirectoryTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.session=SimpleNamespace(mode='device',inventory={'api':34},connection={'host':'test','port':5555,'paired':False},root=Path(self.temp.name),installer=SimpleNamespace(lock=threading.Lock()))
        self.device=SimpleNamespace(connect=lambda:None,close=lambda:None,status=lambda p:{'installed':True,'versionCode':1,'access':'check-on-tv'})
        self.catalog=SimpleNamespace(summary=lambda id:{'available':True,'minSdk':26,'versionCode':2})
        self.center=AddonCenter(self.session,self.catalog,lambda *args:self.device)
    def test_directory_has_no_duplicate_or_unreviewed_install_ids(self):
        rows=items();self.assertEqual(len(rows),48);self.assertEqual(len({x['id'] for x in rows}),48)
        self.assertEqual({x['id'] for x in rows if x['distribution']=='signed'},set(PACKAGES))
        for x in rows:self.assertTrue(x['source'].startswith('https://github.com/'));self.assertTrue(x['access']);self.assertTrue(x['recovery'])
    def test_guide_cannot_be_installed_even_with_confirmation(self):
        with self.assertRaises(SetupError):self.center.review({'id':'castfix'})
        with self.assertRaises(SetupError):self.center.install({'id':'castfix','reviewToken':'yes'})
    def test_browsing_does_not_connect_or_report_unchecked_grants(self):
        self.center.factory=lambda *args:self.fail('Browsing attempted a TV connection')
        rows=self.center.catalogue()['items'];self.assertTrue(all(x['device']['installed'] is None for x in rows))
    def test_explicit_status_refresh_then_connection_change_discards_results(self):
        rows=self.center.refresh({})['items'];self.assertEqual(next(x for x in rows if x['id']=='weather')['status'],'update')
        self.session.connection['host']='different'
        self.assertTrue(all(x['device']['installed'] is None for x in self.center.catalogue()['items']))
    def test_refresh_is_read_only_and_serialized_with_installer(self):
        self.session.installer.lock.acquire()
        with self.assertRaises(SetupError):self.center.refresh({})
        self.session.installer.lock.release()
        self.assertTrue(self.center.refresh({})['checked'])
    def test_minimum_sdk_is_not_ignored_for_installed_app(self):
        self.session.inventory={'api':25};self.center.refresh({})
        self.assertEqual(next(x for x in self.center.catalogue()['items'] if x['id']=='weather')['status'],'unsupported')
    def test_navigation_cannot_interrupt_an_installation(self):
        self.session.installer.lock.acquire()
        try:
            with self.assertRaises(SetupError):self.center.settings({'id':'weather'})
            with self.assertRaises(SetupError):self.center.launch({'id':'weather'})
        finally:self.session.installer.lock.release()
    def test_no_device_in_preview_or_arbitrary_status_arguments(self):
        self.session.mode='preview'
        with self.assertRaises(SetupError):self.center.refresh({})
        with self.assertRaises(SetupError):self.center.refresh({'command':'grant'})
    def test_status_parser_reports_denied_access_without_granting(self):
        d=object.__new__(AddonDevice);commands=[]
        d.metadata=lambda package:{'versionCode':2}
        def shell(cmd):
            commands.append(cmd);return '  android.permission.BLUETOOTH_CONNECT: granted=false, flags=[]\n  android.permission.INTERNET: granted=true'
        d._shell=shell
        state=d.status(PACKAGES['headphones'])
        self.assertEqual(state['access'],'needs-access');self.assertEqual(state['missingPermissions'],['android.permission.BLUETOOTH_CONNECT'])
        self.assertEqual(commands,['dumpsys package '+PACKAGES['headphones']])
        with self.assertRaises(SetupError):d.status('unreviewed.package')

if __name__=='__main__':unittest.main()
