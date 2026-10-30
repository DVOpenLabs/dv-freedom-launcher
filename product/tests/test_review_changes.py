import unittest
from tools.review_changes import assess,private_path

class ReviewFactsTest(unittest.TestCase):
    def test_private_records_and_keys_block_but_public_tests_and_guides_do_not(self):
        for p in ['product/ROADMAP.md','outputs/device.json','product/private/settings.json','work/session.txt','AGENTS.md']:self.assertTrue(private_path(p),p)
        for p in ['product/tests/test_policy.py','product/ADDONS.md','mobile/README.md','tools/review_changes.py']:self.assertFalse(private_path(p),p)
        self.assertEqual(assess('keys/release.p12',None,b'not-a-real-key')[0]['level'],'block')
    def test_core_network_is_blocked_but_companion_permission_needs_review(self):
        data=b'<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.INTERNET"/></manifest>'
        self.assertTrue(any(x['kind']=='core-network' for x in assess('launcher/app/src/main/AndroidManifest.xml',None,data)))
        findings=assess('addons/example/app/src/main/AndroidManifest.xml',None,data)
        self.assertTrue(any(x['kind']=='permission' for x in findings));self.assertFalse(any(x['level']=='block' for x in findings))
    def test_accessibility_declaration_is_not_hidden_behind_empty_uses_permissions(self):
        data=b'<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><service android:name=".Guard" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"/></application></manifest>'
        self.assertTrue(any(x['kind']=='special-service' for x in assess('launcher/app/src/community/AndroidManifest.xml',None,data)))
    def test_bad_xml_or_source_encoding_cannot_pass_quietly(self):
        self.assertTrue(any(x['level']=='block' for x in assess('app/AndroidManifest.xml',None,b'<manifest>')))
        self.assertTrue(any(x['kind']=='encoding' for x in assess('app/source.java',None,b'\xffbad')))
    def test_checker_can_inspect_itself_without_flagging_its_patterns_as_private_data(self):
        from pathlib import Path
        findings=assess('tools/review_changes.py',None,(Path(__file__).resolve().parents[2]/'tools/review_changes.py').read_bytes())
        self.assertFalse(any(x['level']=='block' for x in findings))
    def test_real_home_directory_paths_still_block_publication(self):
        for value in ['C:'+chr(92)+'Users'+chr(92)+'PrivateUser'+chr(92)+'file.json', '/'+ 'home/person/file.json','/'+'Users/person/file.json']:
            self.assertTrue(any(x['kind']=='personal-path' for x in assess('example.txt',None,value.encode())),value)
    def test_normal_source_with_word_key_is_not_treated_as_a_secret(self):
        self.assertEqual(assess('tools/example.py',None,b'def key(): return "generated at runtime"\n'),[])

if __name__=='__main__':unittest.main()
