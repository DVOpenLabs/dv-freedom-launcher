import unittest
from setup_app.core import SetupError
from setup_app.transport import InstallerDevice, PACKAGE
from setup_app.addons import AddonDevice


class MetadataTransport(InstallerDevice):
    def __init__(self, installed=False, path=None, error=False):
        self.installed=installed;self.path=path;self.error=error;self.commands=[]
    def read(self, key):
        if self.error:raise SetupError('Connection unavailable')
        if key!='packages':raise AssertionError(key)
        return 'package:example.other\n'+('package:'+PACKAGE+'\n' if self.installed else '')
    def _shell(self, command):
        self.commands.append(command)
        if command.startswith('pm path '):return self.path or ''
        return 'versionCode=23 minSdk=26'


class MetadataTest(unittest.TestCase):
    def test_fresh_paired_install_does_not_use_failing_pm_path_to_detect_absence(self):
        d=MetadataTransport()
        self.assertIsNone(d.metadata())
        self.assertEqual(d.commands,[])
    def test_missing_path_for_reported_installed_app_is_not_treated_as_fresh(self):
        with self.assertRaises(SetupError):MetadataTransport(installed=True).metadata()
    def test_connection_errors_are_not_treated_as_absence(self):
        with self.assertRaises(SetupError):MetadataTransport(error=True).metadata()
    def test_existing_valid_apk_keeps_version_check(self):
        d=MetadataTransport(installed=True,path='package:/data/app/example/base.apk')
        self.assertEqual(d.metadata(),{'path':'/data/app/example/base.apk','versionCode':23})
    def test_fresh_companion_also_avoids_missing_package_pm_path(self):
        d=MetadataTransport()
        self.assertIsNone(AddonDevice.metadata(d,'io.github.sumyabazar1.weather'))
        self.assertEqual(d.commands,[])
    def test_companion_read_errors_are_not_treated_as_absence(self):
        with self.assertRaises(SetupError):AddonDevice.metadata(MetadataTransport(error=True),'io.github.sumyabazar1.weather')
    def test_setup_chooser_has_only_fixed_intents_and_no_selection_command(self):
        d=MetadataTransport();d.open_setup_settings('home');d.open_setup_settings('accessibility')
        self.assertEqual(d.commands,['am start -a android.settings.HOME_SETTINGS','am start -a android.settings.ACCESSIBILITY_SETTINGS'])
        with self.assertRaises(SetupError):d.open_setup_settings('home;reboot')
