"""A recovery record must name an actual Home, not a boot placeholder."""
import unittest
from setup_app.core import SetupError
from setup_app.transport import InstallerDevice
from setup_app.tests import test_installer as fixtures

UNSAFE = (
    'com.google.android.tungsten.setupwraith/.RecoveryActivity',
    'com.android.tv.settings/.system.FallbackHome',
    'com.vendor.shell/.FallbackHome',
    'com.android.provision/.DefaultActivity',
    'android/com.android.internal.app.ResolverActivity',
    'com.vendor.factory/.Main',
)

class StableHomeTransportTest(unittest.TestCase):
    def test_placeholders_are_rejected_even_when_current_home_matches(self):
        for home in UNSAFE:
            with self.subTest(home=home):
                class ReadOnly:
                    def read(self, kind): return home
                    def _shell(self, command): raise AssertionError('Must reject before any write')
                with self.assertRaises(SetupError):
                    InstallerDevice.preserve_home(ReadOnly(), home)

    def test_real_launcher_is_preserved_without_a_command(self):
        class ReadOnly:
            def read(self, kind): return 'example.stock/.Home'
            def _shell(self, command): raise AssertionError('Already selected')
        InstallerDevice.preserve_home(ReadOnly(), 'example.stock/.Home')

class StableHomeRecordsTest(unittest.TestCase):
    def setUp(self):
        self.fixture = fixtures.InstallerTest()
        self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)
        self.i, self.plan, self.d = self.fixture.i, self.fixture.plan, self.fixture.d
    def test_old_recovery_point_with_placeholder_is_refused(self):
        review = self.i.review(self.plan)
        point = self.i._load(review['id'])
        for home in UNSAFE:
            with self.subTest(home=home):
                point['originalHome'] = home
                self.i._save(point)
                with self.assertRaises(SetupError): self.i._load(review['id'])
        self.assertNotIn('install', self.d['log'])
