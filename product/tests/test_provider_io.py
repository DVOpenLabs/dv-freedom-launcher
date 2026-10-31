"""Run the actual remote acknowledgement branches with controlled shell operations, never a TV."""
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('provider_io', ROOT / 'tools/provider_io.py')
io = importlib.util.module_from_spec(spec); spec.loader.exec_module(io)

class ProviderIoTest(unittest.TestCase):
    def test_command_echo_prefix_suffix_and_missing_marker_are_not_success(self):
        for value in ['echo DV_IO_TEST', '$ command; echo DV_IO_TEST', 'prefixDV_IO_TEST', 'DV_IO_TESTsuffix', '']:
            self.assertFalse(io.acknowledged(value, 'DV_IO_TEST'))
        self.assertTrue(io.acknowledged('DV_IO_TEST\r\n', 'DV_IO_TEST'))

    def test_path_injection_and_parent_traversal_are_refused(self):
        for uri in ['content://example.provider/../config.json', 'content://example.provider/config;id']:
            with self.assertRaises(ValueError): io.command('delete', uri)
        with self.assertRaises(ValueError): io.command('put', 'content://example.provider/config.json', '/tmp/arbitrary')

    def run_boundary(self, operation, provider_status, diagnostics, copy_status=1):
        shell = shutil.which('bash') if os.name != 'nt' else str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'Git/bin/bash.exe')
        if not shell or not Path(shell).is_file(): self.skipTest('A local Bash is needed for the remote-shell fixture')
        command, marker = io.command(operation, 'content://example.provider/config.json',
            '/data/local/tmp/review-stage' if operation == 'put' else None,
            '/sdcard/Android/data/example.provider/files/config.json' if operation == 'put' else None, marker='DV_IO_TEST')
        command = command.replace('/data/local/tmp/review-stage', '/dev/null')
        fixture = f"content() {{ printf '%s' '{diagnostics}'; return {provider_status}; }}; cp() {{ return {copy_status}; }}; mkdir() {{ return 0; }}; rm() {{ return 0; }}; "
        result = subprocess.run([shell, '-c', fixture + command], capture_output=True, text=True, timeout=15)
        return io.acknowledged(result.stdout, marker)

    def test_zero_exit_provider_diagnostics_are_a_failure(self):
        self.assertFalse(self.run_boundary('put', 0, 'Exception while accessing provider'))
        self.assertFalse(self.run_boundary('delete', 0, 'Error deleting file'))

    def test_no_acknowledgement_when_both_write_paths_fail(self):
        self.assertFalse(self.run_boundary('put', 1, '', 1))

    def test_silent_provider_success_and_legacy_copy_fallback(self):
        self.assertTrue(self.run_boundary('put', 0, '', 1))
        self.assertTrue(self.run_boundary('put', 1, '', 0))
        self.assertTrue(self.run_boundary('delete', 0, ''))
