"""Run pinned upstream behavior tests, separately reporting upstream-only documentation fixtures."""
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))

def flatten(suite):
    for item in suite:
        if isinstance(item,unittest.TestSuite):yield from flatten(item)
        else:yield item

def main():
    import os
    os.chdir(ROOT)
    manifest=json.loads(Path(__file__).with_name('upstream-v3.1-tests.json').read_text(encoding='utf-8'))
    suite=unittest.TestSuite();excluded=[];loader=unittest.TestLoader()
    for module in manifest['modules']:
        for case in flatten(loader.discover(str(ROOT/'tests'),pattern=module)):
            reason=next((why for prefix,why in manifest['excludedFixtures'].items() if case.id().startswith(prefix)),None)
            if reason:excluded.append({'test':case.id(),'reason':reason})
            else:suite.addTest(case)
    print(f"Upstream {manifest['release']}: {suite.countTestCases()} behavior cases; {len(excluded)} separately reported repository-documentation fixtures.",flush=True)
    for entry in excluded:print('EXCLUDED DOCUMENT FIXTURE:',entry['test'],flush=True)
    result=unittest.TextTestRunner(verbosity=1).run(suite)
    return 0 if result.wasSuccessful() else 1
if __name__=='__main__':raise SystemExit(main())
