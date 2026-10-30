"""Validate every distributed theme after retiring the upstream device-recipe catalogue."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tests'))
sys.path.insert(0, str(ROOT / 'tools'))
import test_shipped_config_keys_are_read as key_rules
import test_shipped_config_values as value_rules
import manage


class DistributedThemeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.folders = sorted(p for p in (ROOT / 'themes').iterdir() if p.is_dir())
        java = key_rules.CONFIG_JAVA.read_text(encoding='utf-8')
        cls.keys = key_rules.overlay_keys(java)
        cls.kinds = value_rules.readers(java)

    def test_every_theme_has_a_readable_object(self):
        self.assertTrue(self.folders, 'the distributed theme inputs must be checked')
        for folder in self.folders:
            with self.subTest(theme=folder.name):
                self.assertIsInstance(json.loads((folder / 'config.json').read_text(encoding='utf-8')), dict)

    def test_every_theme_key_and_value_is_supported(self):
        self.assertGreaterEqual(len(self.keys), 100, 'the actual Config.java readers must be inspected')
        for folder in self.folders:
            with self.subTest(theme=folder.name):
                data = json.loads((folder / 'config.json').read_text(encoding='utf-8'))
                self.assertEqual([], key_rules.unread(data, self.keys))
                self.assertEqual([], value_rules.problems(data, self.kinds))

    def test_xml_only_settings_are_not_shipped_as_theme_overrides(self):
        rows = key_rules.key_rows(key_rules.THEMES_README.read_text(encoding='utf-8'))
        xml_only = {key for key, kind in rows.items() if 'config.xml' in kind}
        self.assertIn('settings_fallback_components', xml_only)
        for folder in self.folders:
            with self.subTest(theme=folder.name):
                data = json.loads((folder / 'config.json').read_text(encoding='utf-8'))
                self.assertFalse(set(data) & xml_only)

    def test_all_shipped_themes_and_legacy_recipe_paths_remain_write_protected(self):
        self.assertIn('examples', manage.PROJECT_FOLDERS)
        for folder in [*self.folders, ROOT / 'examples' / 'legacy-recipe']:
            with self.subTest(folder=folder.name):
                self.assertIsNotNone(manage.own_folder_problem(folder))
