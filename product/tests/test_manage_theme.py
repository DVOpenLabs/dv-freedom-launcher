"""Windows theme management must use the native reviewed script, not a Bash path translation."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('dv_manage',ROOT/'tools/manage.py')
manage=importlib.util.module_from_spec(spec);spec.loader.exec_module(manage)

class ManageThemeTests(unittest.TestCase):
    def test_windows_uses_native_powershell_with_structured_arguments(self):
        folder=ROOT/'themes'/'a folder'
        with patch.object(manage.os,'name','nt'),patch.object(manage.shutil,'which',return_value='pwsh.exe'),patch.object(manage,'run',return_value=0) as run:
            self.assertEqual(0,manage.install_theme(folder))
        argv,working=run.call_args.args
        self.assertEqual(['pwsh.exe','-NoProfile','-NonInteractive','-File',str(manage.TOOLS/'theme.ps1'),str(folder)],argv)
        self.assertEqual(ROOT,working)
        self.assertNotIn('-ExecutionPolicy',argv)
    def test_windows_refuses_missing_shell_without_transfer(self):
        with patch.object(manage.os,'name','nt'),patch.object(manage.shutil,'which',return_value=None),patch.object(manage,'run') as run:
            self.assertEqual(2,manage.install_theme(ROOT/'themes'))
        run.assert_not_called()
    def test_unix_retains_bash_route(self):
        with patch.object(manage.os,'name','posix'),patch.object(manage,'run',return_value=0) as run:
            manage.install_theme(ROOT/'themes',('--reset',))
        self.assertEqual(['bash',str(manage.TOOLS/'theme.sh'),str(ROOT/'themes'),'--reset'],run.call_args.args[0])
