"""tools/theme.sh and theme.ps1 check every collection name before anything changes on the TV.

The name check used to sit inside the loop that pushes the collection, after config.json had been
replaced and the old collection emptied. A theme with one name the script refuses ("bad name.jpg")
stopped there with exit 1 and left the new config.json and the pictures before it on the TV, though
nothing said the theme had half applied. A name the check let through but b64push.py refuses, a
path with ".." in it ("sunset..jpg"), stopped the same way with b64push's exit 2. Both are now
refused in one pass next to the JSON check, before the first command reaches the TV.

theme.ps1 now delegates collection deletion to ThemeProvider through provider_io; an unconfirmed
delete fails closed before new images are sent. The provider boundary has its own integration tests.

theme.sh runs against the fake TV of tests/test_theme_push_ownership.py. The Windows script's shared
provider route and the order of its checks are also pinned here.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_theme_push_ownership as push  # noqa: E402  (imported as a module so its tests do not run twice)

TOOLS = push.TOOLS
URI = "content://com.gathin.tvhome.theme"


class ThemeNamesFirstTest(unittest.TestCase):
    # Borrowed rather than inherited, so the tests of tests/test_theme_push_ownership.py do not run a
    # second time under this class.
    setUp = push.ThemePushTest.setUp
    run_theme = push.ThemePushTest.run_theme
    shell_commands = push.ThemePushTest.shell_commands

    def theme_on_the_tv(self):
        """The TV before the push: an earlier theme's config.json and one picture in its collection."""
        (self.device / "config.json").write_bytes(b'{"background": "#000000"}')
        (self.device / "wallpapers").mkdir()
        (self.device / "wallpapers" / "old.jpg").write_bytes(b"old collection")

    def assertTvUnchanged(self, result, name):
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn(f"unsupported wallpaper filename: {name}", result.stderr)
        self.assertEqual((self.device / "config.json").read_bytes(), b'{"background": "#000000"}',
                         "the refused theme's config.json must not replace the one on the TV")
        self.assertEqual(sorted(p.name for p in (self.device / "wallpapers").iterdir()), ["old.jpg"],
                         "the collection on the TV must be left as it was")
        self.assertEqual(self.shell_commands(), [], "nothing may reach the TV before every name is checked")

    def test_a_refused_name_leaves_the_tv_as_it_was(self):
        self.theme_on_the_tv()
        collection = self.theme / "wallpapers"
        collection.mkdir()
        (collection / "a-good.jpg").write_bytes(b"\xff\xd8good")
        (collection / "bad name.jpg").write_bytes(b"\xff\xd8bad")
        self.assertTvUnchanged(self.run_theme(str(self.theme)), "bad name.jpg")

    def test_a_name_b64push_refuses_is_refused_first(self):
        """b64push.py refuses a remote path with '..' in it, so sunset..jpg could never reach the TV."""
        self.theme_on_the_tv()
        collection = self.theme / "wallpapers"
        collection.mkdir()
        (collection / "dawn.jpg").write_bytes(b"\xff\xd8dawn")
        (collection / "sunset..jpg").write_bytes(b"\xff\xd8sunset")
        self.assertTvUnchanged(self.run_theme(str(self.theme)), "sunset..jpg")

    def test_names_with_single_dots_still_go_through(self):
        self.theme_on_the_tv()
        collection = self.theme / "wallpapers"
        collection.mkdir()
        names = ("01.dusk.jpg", "v1.2-lake.PNG")
        for name in names:
            (collection / name).write_bytes(b"\x00\xff" + name.encode())
        result = self.run_theme(str(self.theme))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(sorted(p.name for p in (self.device / "wallpapers").iterdir()), sorted(names))
        self.assertEqual((self.device / "config.json").read_bytes(), self.config)

    def test_windows_clear_requires_confirmed_provider_deletion(self):
        # The current transfer contract refuses to continue if collection deletion fails.
        script = (TOOLS / "theme.ps1").read_text(encoding="utf-8")
        self.assertIn('function Clear-Collection { Invoke-Tv provider_io.py delete --uri "$uri/wallpapers" }', script)
        self.assertIn("if ($LASTEXITCODE -ne 0) { throw", script)
        self.assertGreaterEqual(len(re.findall(r"^\s*Clear-Collection\s*$", script, re.M)), 2)
        sys.path.insert(0, str(TOOLS))
        import provider_io
        command, marker = provider_io.command("delete", URI + "/wallpapers", marker="DV_IO_TEST")
        self.assertIn("content delete", command)
        self.assertIn(marker, command)
        self.assertNotIn("rm -", command)

    def test_the_windows_script_checks_every_name_before_the_tv(self):
        """theme.ps1 cannot run here, so pin that its name check comes before its first call to tv.py."""
        script = (TOOLS / "theme.ps1").read_text(encoding="utf-8")
        check = script.index("unsupported wallpaper filename")
        json_at = script.index("is not valid JSON")
        # Invoke-Tv (#566) or a bare "& $py -I", whichever spelling the call has.
        first_call = json_at + re.search(r"^(?:Invoke-Tv|& \$py -I) tv\.py", script[json_at:], re.M).start()
        self.assertLess(check, first_call, "a refused name must stop the push before the TV changes")
        line = next(line for line in script.splitlines() if "unsupported wallpaper filename" in line)
        self.assertIn("'..'", line, "b64push.py refuses a path with '..' in it")


if __name__ == "__main__":
    unittest.main()
