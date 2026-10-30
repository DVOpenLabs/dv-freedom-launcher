"""The publication boundary excludes local work records, even when they exist in a checkout."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT=Path(__file__).resolve().parents[2]
INTERNAL={'AGENTS.md','CLAUDE.md','GEMINI.md','product/ROADMAP.md','product/VALIDATION.md',
 'product/LG-HELPER-REVIEW.md','product/SAMSUNG-REVIEW.md','product/UPSTREAM-3.1.0-REVIEW.md',
 'setup_app/VALIDATION.md','setup_app/DESIGN.md','product/ANDROID-APPEARANCE.md',
 'product/ANDROID-USABILITY.md','product/ANDROID-POLISH.md'}

class PublicationTests(unittest.TestCase):
    def tracked(self):
        return set(subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().strip('\0').split('\0'))
    def test_internal_records_are_not_tracked(self):
        self.assertFalse(self.tracked() & INTERNAL)
    def test_internal_records_are_ignored(self):
        for name in INTERNAL:
            self.assertEqual(subprocess.run(['git','check-ignore','-q','--',name],cwd=ROOT).returncode,0,name)
    def test_public_guides_do_not_link_internal_records(self):
        import posixpath
        for name in self.tracked():
            if not name.endswith('.md'):continue
            text=(ROOT/name).read_text(encoding='utf-8')
            for link in re.findall(r'\]\(([^)]+)\)',text):
                if '://' in link:
                    if 'github.com/DVOpenLabs/dv-freedom-launcher/blob/' in link:
                        self.assertNotRegex(link,r'/(?:ROADMAP|VALIDATION|[^/]*REVIEW[^/]*|LG-NEXT)\.md',name)
                    continue
                target=posixpath.normpath(posixpath.join(posixpath.dirname(name),link.split('#')[0]))
                self.assertNotIn(target,INTERNAL,name+' -> '+link)
    def test_no_generated_or_key_artifacts_in_publication(self):
        forbidden=[]
        for name in self.tracked():
            if name.endswith(('.apk','.aab','.exe','.ipk','.wgt','.p12','.pfx','.pem','.log')) or '/release_bundle/' in name or '/.venv/' in name or name.startswith(('product/private/','shots/','backup/')):forbidden.append(name)
        self.assertEqual([],forbidden)
