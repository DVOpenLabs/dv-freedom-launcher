"""Reject unsafe local theme edits and ambiguous recovery without contacting a TV."""
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import socket
import struct
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import theme_validation as validation


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, ROOT / 'tools' / filename)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


manage = module('hardened_manage', 'manage.py')
recovery = module('hardened_recovery', 'recovery.py')
adb = module('hardened_adb', 'adb-closed.py')


class ThemeLimitsTests(unittest.TestCase):
    def test_failed_atomic_replace_keeps_original_and_cleans_temporary(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            original = b'{"greeting_name":"original"}'
            (folder / 'config.json').write_bytes(original)
            item = {'kind': 'switch', 'type': 'string'}
            with patch.object(manage, 'find', return_value=item), patch.object(manage.os, 'replace', side_effect=OSError('disk failure')), patch.object(manage, 'install_theme') as push:
                with self.assertRaises(SystemExit):
                    manage.cmd_set('greeting_name', 'new', folder)
            self.assertEqual(original, (folder / 'config.json').read_bytes())
            self.assertEqual(['config.json'], [p.name for p in folder.iterdir()])
            push.assert_not_called()

    def test_bash_refuses_conflicting_or_extra_arguments_before_host_lookup(self):
        shell = (str(Path(os.environ.get('ProgramFiles', 'C:/Program Files')) / 'Git/bin/bash.exe')
                 if os.name == 'nt' else shutil.which('bash'))
        if not shell or not Path(shell).is_file():
            self.skipTest('Bash not installed')
        for args in (['--reset', 'theme'], ['theme', '--reset'], ['theme', 'other'], ['--unknown'], []):
            result = subprocess.run([shell, str(ROOT / 'tools/theme.sh'), *args], capture_output=True, text=True, timeout=20)
            self.assertEqual(2, result.returncode, result.stderr)
            self.assertIn('nothing was sent', result.stderr)

    def test_bash_oversized_config_never_reaches_transport(self):
        shell = (str(Path(os.environ.get('ProgramFiles', 'C:/Program Files')) / 'Git/bin/bash.exe')
                 if os.name == 'nt' else shutil.which('bash'))
        if not shell or not Path(shell).is_file():
            self.skipTest('Bash not installed')
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            scripts, theme = base / 'tools', base / 'theme'
            (scripts / 'venv/bin').mkdir(parents=True)
            theme.mkdir()
            shutil.copyfile(ROOT / 'tools/theme.sh', scripts / 'theme.sh')
            interpreter = scripts / 'venv/bin/python'
            interpreter.write_text('#!/usr/bin/env bash\nexec "' + sys.executable.replace('\\', '/') + '" "$@"\n', encoding='utf-8')
            interpreter.chmod(0o700)
            marker = base / 'transport-called'
            (scripts / 'tv.py').write_text('from pathlib import Path\nPath(' + repr(str(marker)) + ').touch()\n')
            (theme / 'config.json').write_text(json.dumps({'name': 'x' * 65536}))
            result = subprocess.run([shell, str(scripts / 'theme.sh'), str(theme)],
                                    env=dict(os.environ, TV_HOST='192.0.2.1'), capture_output=True, text=True, timeout=20)
            self.assertEqual(2, result.returncode, result.stderr)
            self.assertIn('65536', result.stderr)
            self.assertFalse(marker.exists(), 'Oversized config must not reach tv.py')

    def test_byte_limit_counts_utf8_and_accepts_exact_boundary(self):
        data = {'name': ''}
        overhead = len(validation.config_bytes(data))
        data['name'] = 'x' * (65536 - overhead)
        self.assertEqual(65536, len(validation.config_bytes(data)))
        data['name'] += 'é'
        with self.assertRaises(ValueError):
            validation.config_bytes(data)

    def test_oversized_edit_preserves_original_and_never_pushes(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            original = b'{"greeting_name":"original"}'
            (folder / 'config.json').write_bytes(original)
            item = {'kind': 'switch', 'type': 'string'}
            with patch.object(manage, 'find', return_value=item), patch.object(manage, 'install_theme') as push:
                with self.assertRaises(SystemExit):
                    manage.cmd_set('greeting_name', 'x' * 65536, folder)
            self.assertEqual(original, (folder / 'config.json').read_bytes())
            push.assert_not_called()

    def test_invalid_fields_do_not_write_or_push(self):
        cases = [('settings_key_keycodes', [4294967297]), ('settings_key_keycodes', [10**400]), ('settings_key_keycodes', [True]),
                 ('settings_key_keycodes', [1.5]), ('app_keys', ['83:action:android.intent.action.CALL']),
                 ('app_keys', ['scan:0:com.example.app']), ('app_labels', ['Netflix=Title']),
                 ('input_sources', ['HDMI1=2147483648']), ('input_sources', ['HDMI1=1', 'HDMI1=2']),
                 ('source_panel_extra', 'command=no'), ('festival_until', '2026-02-30'),
                 ('festival_until', '20261130'), ('wallpaper_moods', {'dusk': ['a.jpg']})]
        for key, value in cases:
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                validation.validate_value(key, value)

    def test_supported_settings_actions_and_dates(self):
        for action in validation.SETTINGS_ACTIONS:
            validation.validate_value('app_keys', ['83:action:' + action])
        validation.validate_value('festival_until', '2028-02-29')
        validation.validate_value('input_sources', ['HDMI1=1', 'HDMI2=-'])

    def test_mood_picture_must_exist_in_own_folder(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            with self.assertRaises(ValueError):
                validation.validate_value('wallpaper_moods', {'morning': ['sun.jpg']}, folder)
            (folder / 'wallpapers').mkdir()
            (folder / 'wallpapers/sun.jpg').write_bytes(b'fixture')
            validation.validate_value('wallpaper_moods', {'morning': ['sun.jpg']}, folder)

    def test_powershell_conflicting_arguments_rejected_before_host_or_device(self):
        shell = shutil.which('pwsh') or shutil.which('powershell')
        if not shell:
            self.skipTest('PowerShell not installed')
        result = subprocess.run([shell, '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
                                 '-File', str(ROOT / 'tools/theme.ps1'), 'nonexistent-theme', '-Reset'],
                                capture_output=True, text=True, timeout=20)
        self.assertEqual(2, result.returncode, result.stderr)
        self.assertIn('nothing was sent', result.stderr)

    def test_powershell_oversized_config_rejected_without_transport(self):
        shell = shutil.which('pwsh') or shutil.which('powershell')
        if not shell:
            self.skipTest('PowerShell not installed')
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            scripts = base / 'tools'
            (scripts / 'venv/Scripts').mkdir(parents=True)
            # Only existence is checked before preflight; this file must never execute.
            (scripts / 'venv/Scripts/python.exe').write_text('never execute')
            shutil.copyfile(ROOT / 'tools/theme.ps1', scripts / 'theme.ps1')
            theme = base / 'theme'
            theme.mkdir()
            (theme / 'config.json').write_text(json.dumps({'name': 'x' * 65536}))
            result = subprocess.run([shell, '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
                                     '-File', str(scripts / 'theme.ps1'), str(theme), '-TvHost', '192.0.2.1'],
                                    capture_output=True, text=True, timeout=20)
            self.assertEqual(2, result.returncode, result.stderr)
            self.assertIn('65536', result.stderr)


class RecoveryAliasTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = patch.object(recovery.Path, 'home', return_value=Path(self.temp.name))
        self.home.start()
        self.addCleanup(self.home.stop)
        (Path(self.temp.name) / '.cleanyourtv').mkdir()

    def test_alias_and_exact_records_are_ambiguous(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            original, alias = folder / 'list.txt', folder / 'alias.txt'
            original.write_text('fixture')
            try:
                os.link(original, alias)  # Real same-file alias; a copy must not match.
            except OSError:
                alias = original.parent / '.' / original.name
            with patch.object(recovery, 'list_digest', side_effect=lambda p: 'aaaaaaaa' if str(p) == str(original) else 'bbbbbbbb'):
                # On restricted Windows hosts simulate two spellings' historical path digests.
                # samefile still uses a real on-disk identity check.
                if alias == original:
                    alias = str(original.parent) + '/./' + original.name
                # All records are confined to the temporary home; inspect no device state.
                first = recovery.record_name(original, '192.0.2.1')
                second = recovery.record_name(alias, '192.0.2.1')
                first.parent.mkdir(parents=True, exist_ok=True)
                first.write_text(json.dumps({'list': str(original)}))
                self.assertEqual(first, recovery.find_record(alias, '192.0.2.1'))
                second.write_text(json.dumps({'list': str(alias)}))
                with self.assertRaises(SystemExit):
                    recovery.find_record(alias, '192.0.2.1')

    def test_copy_is_a_distinct_list(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            original, copy = folder / 'list.txt', folder / 'copy.txt'
            original.write_text('fixture')
            shutil.copyfile(original, copy)
            first = recovery.record_name(original, '192.0.2.1')
            first.write_text(json.dumps({'list': str(original)}))
            self.assertEqual(recovery.record_name(copy, '192.0.2.1'), recovery.find_record(copy, '192.0.2.1'))


class AdbProtocolTests(unittest.TestCase):
    def test_all_adb_headers_are_recognised_without_authenticating(self):
        for command, name in adb.ADB_ANSWERS.items():
            header = struct.pack('<6I', command, 0, 0, 0, 0, command ^ 0xffffffff)
            self.assertEqual(name, adb.adb_answer(header))
            self.assertEqual('unknown', adb.short_reply(header[:2])[0])
            self.assertEqual('unknown', adb.short_reply(header[:12])[0])
        self.assertIsNone(adb.adb_answer(b'HTTP/1.1 200 OK\r\n' + b' ' * 7))

    def test_fragmented_protocol_reply_and_silence(self):
        class Socket:
            def getpeername(self): return ('192.0.2.1', 5555)
            def settimeout(self, timeout): pass
            def sendall(self, raw): self.sent = raw
            def recv(self, size):
                return self.parts.pop(0) if self.parts else b''
        sock = Socket()
        header = struct.pack('<6I', adb.A_AUTH, 0, 0, 0, 0, adb.A_AUTH ^ 0xffffffff)
        sock.parts = [header[:4], header[4:12], header[12:]]
        self.assertEqual(('adb', 'AUTH'), adb.ask_adb(sock))
        self.assertEqual(adb.greeting(), sock.sent)  # No key or signed response ever sent.
        sock.parts = [header[:4]]
        self.assertEqual('unknown', adb.ask_adb(sock)[0])
        sock.parts = [b'HTTP/1.1 200 OK\r\n']
        self.assertEqual('unknown', adb.ask_adb(sock)[0])
        sock.parts = [b'HTTP/1.1 200 OK\r\n' + b' ' * 7]
        self.assertEqual('not adb', adb.ask_adb(sock)[0])
        sock.parts = []
        self.assertEqual('unknown', adb.ask_adb(sock)[0])


if __name__ == '__main__':
    unittest.main()
