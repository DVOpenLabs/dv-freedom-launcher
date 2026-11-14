import unittest
from platforms.platform_health import assess

class PlatformHealthTest(unittest.TestCase):
    def test_capabilities_never_authorize_home_takeover(self):
        caps={k:True for k in ('inventory','launch','exit','inputs','manualAcceptance','recoveryShortcut')}
        for platform,version in [('webos','1.4.0'),('webos','23.1'),('tizen','6.0')]:
            report=assess(platform,version=version,capabilities=caps)
            self.assertTrue(report['manualReady']);self.assertFalse(report['homeTakeover']);self.assertFalse(report['helperEligible'])
    def test_unknown_expired_and_unverified_devices_fail_closed(self):
        report=assess('webos',developer_seconds=0)
        self.assertFalse(report['manualReady']);self.assertEqual(len(report['warnings']),4)
        with self.assertRaises(ValueError):assess('webos',version='1;reboot')
        with self.assertRaises(ValueError):assess('tizen',capabilities={'launch':'yes'})
