from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from setup_app.core import Device, SetupError


class AdbReplyTest(unittest.TestCase):
    def call(self, args, reply):
        device = Device('192.168.1.50', 5555, Path('unused'), paired=True)
        with patch('setup_app.platform_tools.find_adb', return_value='adb'), \
             patch('setup_app.core.subprocess.run', return_value=reply):
            return device._adb(args)

    def test_successful_shell_report_can_contain_error_words(self):
        report = 'Historical task: failed attempt; cannot restore old activity\n'
        reply = SimpleNamespace(returncode=0, stdout=report, stderr='')
        self.assertEqual(report, self.call(['-s', '192.168.1.50:5555', 'shell', 'dumpsys activity activities'], reply))

    def test_pair_and_connect_still_reject_reported_failure_with_zero_exit(self):
        for command in ('pair', 'connect'):
            for message in ('failed to connect', 'cannot connect'):
                with self.subTest(command=command, message=message), self.assertRaises(SetupError):
                    self.call([command, '192.168.1.50:5555'], SimpleNamespace(returncode=0, stdout=message, stderr=''))

    def test_nonzero_shell_exit_is_still_an_error(self):
        with self.assertRaises(SetupError):
            self.call(['-s', '192.168.1.50:5555', 'shell', 'dumpsys activity activities'],
                      SimpleNamespace(returncode=1, stdout='', stderr='device offline'))
