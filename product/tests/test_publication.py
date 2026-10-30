"""The publication boundary excludes local work records, even when they exist in a checkout."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT=Path(__file__).resolve().parents[2]
INTERNAL={'AGENTS.md','CLAUDE.md','GEMINI.md','product/ROADMAP.md','product/VALIDATION.md',
 'product/LG-HELPER-REVIEW.md','product/SAMSUNG-REVIEW.md','product/UPSTREAM-3.1.0-REVIEW.md',
 'setup_app/VALIDATION.md','setup_app/DESIGN.md','product/ANDROID-APPEARANCE.md',
 'product/ANDROID-USABILITY.md','product/ANDROID-POLISH.md','product/LG-NEXT.md',
 'product/INSTALLER.md','product/PROJECT-SETUP.md','product/SHOWCASE-LIGHTWEIGHT.md',
 'product/PROJECTIVY-REVIEW.md','product/SHOWCASE-REVIEW-2026-10.md','docs/REPOSITORY-AUDIT.md'}

class PublicationTests(unittest.TestCase):
    def tracked(self):
        return set(subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().strip('\0').split('\0'))
    def test_internal_records_are_not_tracked(self):
        self.assertFalse(self.tracked() & INTERNAL)
        private_names=re.compile(r'^(?:AGENTS|CLAUDE|GEMINI|ROADMAP[^/]*|VALIDATION[^/]*|[^/]*-(?:REVIEW|AUDIT)[^/]*)\.md$',re.I)
        self.assertEqual([],sorted(p for p in self.tracked() if private_names.match(Path(p).name)
                                  or any(part in {'.agents','.codex'} for part in Path(p).parts)))
    def test_internal_records_are_ignored(self):
        for name in INTERNAL:
            self.assertEqual(subprocess.run(['git','check-ignore','-q','--',name],cwd=ROOT).returncode,0,name)
    def test_public_guides_do_not_link_internal_records(self):
        import posixpath
        for name in self.tracked():
            if not name.endswith(('.md','.json','.yml','.yaml','.html')):continue
            text=subprocess.check_output(['git','show',':'+name],cwd=ROOT).decode('utf-8')
            self.assertNotRegex(text,r'https://github\.com/DVOpenLabs/dv-freedom-launcher/blob/[^/]+/(?:product/(?:ROADMAP|VALIDATION|LG-NEXT|[^/]*REVIEW[^/]*)|setup_app/(?:DESIGN|VALIDATION))\.md',name)
            for link in re.findall(r'\]\(([^)]+)\)',text):
                if '://' in link:
                    if 'github.com/DVOpenLabs/dv-freedom-launcher/blob/' in link:
                        self.assertNotRegex(link,r'/(?:ROADMAP|VALIDATION|[^/]*REVIEW[^/]*|LG-NEXT)\.md',name)
                    continue
                target=posixpath.normpath(posixpath.join(posixpath.dirname(name),link.split('#')[0]))
                self.assertNotIn(target,INTERNAL,name+' -> '+link)
    def test_product_docs_have_an_explicit_public_purpose(self):
        guides={'AI-SETUP.md','ANDROID-COMMUNITY.md','ANDROID-COMPANION.md','ANDROID-PROFILES.md',
                'README.md','RELEASE-IDENTITY.md','STREAMER-HOME.md','THIRD-PARTY.md'}
        extras={p for p in self.tracked() if p.startswith('product/') and p.count('/')==1 and p.endswith('.md') and Path(p).name not in guides}
        self.assertEqual(set(),extras,'New public product documents require an explicit publication review')
    def test_no_generated_or_key_artifacts_in_publication(self):
        forbidden=[]
        for name in self.tracked():
            if name.endswith(('.apk','.aab','.exe','.ipk','.wgt','.p12','.pfx','.pem','.log')) or '/release_bundle/' in name or '/.venv/' in name or name.startswith(('product/private/','shots/','backup/')):forbidden.append(name)
        self.assertEqual([],forbidden)
