"""Cross-platform mapping, profile isolation and preservation boundaries."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('product_profiles', ROOT / 'tools/product_profiles.py')
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ProfilesTest(unittest.TestCase):
    def setUp(self):
        self.document = json.loads((ROOT / 'product/profiles.example.json').read_text(encoding='utf-8'))

    def test_each_profile_has_independent_lists_and_platform_mappings(self):
        android = MODULE.export_profile(self.document, 'adult', 'android')
        webos = MODULE.export_profile(self.document, 'child', 'webos')
        self.assertEqual(android['dock_apps'], ['org.example.video', 'org.example.music'])
        self.assertEqual(webos['dockApps'], ['example.video'])
        self.assertEqual(webos['hiddenApps'], ['example.music'])

    def test_export_preserves_unrelated_fields_and_does_not_mutate_inputs(self):
        base = {'wallpaper': True, 'app_keys': ['kept'], 'dock_apps': ['old']}
        original_document, original_base = copy.deepcopy(self.document), copy.deepcopy(base)
        result = MODULE.export_profile(self.document, 'child', 'android', base)
        self.assertEqual(result['app_keys'], ['kept'])
        self.assertTrue(result['wallpaper'])
        self.assertEqual(base, original_base)
        self.assertEqual(self.document, original_document)

    def test_missing_platform_mapping_is_not_silently_dropped(self):
        del self.document['apps']['music']['webos']
        with self.assertRaisesRegex(ValueError, 'No webos mapping'):
            MODULE.export_profile(self.document, 'adult', 'webos')

    def test_cli_exports_and_refuses_to_overwrite_an_existing_config(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'profile.json'
            command = [sys.executable, str(ROOT / 'tools/product_profiles.py'),
                       str(ROOT / 'product/profiles.example.json'), '--profile', 'child',
                       '--platform', 'webos', '--output', str(output)]
            first = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(first.returncode, 0, first.stderr)
            self.assertEqual(json.loads(output.read_text())['dockApps'], ['example.video'])
            before = output.read_bytes()
            second = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(second.returncode, 2)
            self.assertEqual(output.read_bytes(), before)

    def test_cli_invalid_mapping_does_not_write_partial_output(self):
        with tempfile.TemporaryDirectory() as directory:
            source, output = Path(directory) / 'input.json', Path(directory) / 'output.json'
            del self.document['apps']['video']['webos']
            source.write_text(json.dumps(self.document), encoding='utf-8-sig')
            result = subprocess.run([sys.executable, str(ROOT / 'tools/product_profiles.py'),
                                     str(source), '--profile', 'child', '--platform', 'webos',
                                     '--output', str(output)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 2)
            self.assertIn('No webos mapping', result.stderr)
            self.assertFalse(output.exists())

    def test_aliases_cannot_create_duplicate_or_hidden_favorites(self):
        self.document['apps']['music']['android'] = 'org.example.video'
        for profile in ('adult', 'child'):
            with self.assertRaises(ValueError):
                MODULE.export_profile(self.document, profile, 'android')

    def test_ambiguous_or_unknown_profile_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Unknown profile'):
            MODULE.export_profile(self.document, 'missing', 'android')
        self.document['profiles'][1]['id'] = 'adult'
        with self.assertRaisesRegex(ValueError, 'Duplicate profile'):
            MODULE.validate(self.document)

    def test_invalid_documents_do_not_coerce_types_or_ignore_unknown_fields(self):
        mutations = [lambda d: d.update(schemaVersion=True),
                     lambda d: d['profiles'][0].update(columns=True),
                     lambda d: d['profiles'][0].update(favorites=['missing']),
                     lambda d: d['profiles'][0].update(favorites=[]),
                     lambda d: d['profiles'][0].update(favorites=['video', 'video']),
                     lambda d: d['profiles'][0].update(hiddenApps=['video']),
                     lambda d: d['profiles'][0].update(id='../other'),
                     lambda d: d['profiles'][0].update(systemVolume=10),
                     lambda d: d['apps']['video'].update(android='bad; command')]
        for mutate in mutations:
            with self.subTest(mutation=mutate):
                document = copy.deepcopy(self.document)
                mutate(document)
                with self.assertRaises(ValueError):
                    MODULE.validate(document)


if __name__ == '__main__':
    unittest.main()
