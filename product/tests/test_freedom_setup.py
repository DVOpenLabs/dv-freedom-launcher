from pathlib import Path
import importlib.util,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import freedom_setup as setup
class SetupTest(unittest.TestCase):
 def test_all_choices_and_inventory_grounded_order(self):
  d={'schema':1,'name':'Living room','settings':{'experience':'glass','text':'auto','columns':8,'strongFocus':True},'favorites':['org.example.b','org.example.a']};self.assertEqual(setup.validate(d,{'apps':[{'package':'org.example.a'},{'package':'org.example.b'}]}),d)
 def test_shell_permissions_and_api_keys_are_rejected(self):
  for k in ('shell','adb','permissions','apiKey','network','themeUrl'):
   with self.assertRaises(ValueError):setup.validate({'schema':1,'settings':{k:'anything'}})
 def test_schema_bool_integer_coercion_and_unknown_values_fail(self):
  for settings in ({'columns':True},{'columns':8.0},{'experience':'tvOS'},{'textSize':115},{'idleSeconds':20}):
   with self.assertRaises(ValueError):setup.validate({'schema':1,'settings':settings})
 def test_only_available_unique_apps_can_be_selected(self):
  for apps in (['org.example.a','org.example.a'],['org.example.missing'],[]):
   with self.assertRaises(ValueError):setup.validate({'schema':1,'settings':{},'favorites':apps},{'apps':[{'package':'org.example.a'}]})
 def test_profile_lock_and_account_data_cannot_be_imported(self):
  for key in ('private_layout_lock_v1','account','profile','token'):
   with self.assertRaises(ValueError):setup.validate({'schema':1,'settings':{'columns':8},key:'1234'})

 def test_bridge_has_no_apply_or_arbitrary_shell_operation(self):
  with self.assertRaisesRegex(ValueError,'Unsupported setup operation'):setup.call('192.168.1.50','org.example.launcher','apply')
