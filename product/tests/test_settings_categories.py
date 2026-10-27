"""Structural reachability checks: moving settings must not drop or duplicate controls."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
A='{http://schemas.android.com/apk/res/android}'

class SettingsCategoriesTest(unittest.TestCase):
    def setUp(self):
        self.root=ET.parse(ROOT/'launcher/app/src/main/res/layout/activity_launcher_settings.xml').getroot()
        self.ids={v.get(A+'id'):v for v in self.root.iter() if v.get(A+'id')}
    def contains(self,group,control):
        return any(v.get(A+'id')=='@+id/'+control for v in self.ids['@+id/'+group].iter())
    def test_every_control_is_reachable_in_one_group(self):
        expected={
            'groupLayout':['stylesChoice','styleUndoChoice','columns4','columns5','columns6','columns7','columns8','rows0','rows1','rows2','rows3','dock100','dock70','dock50','dock30','focusOutlineChoice','tileSpacingChoice','animationsChoice'],
            'groupApps':['reorderChoice','findAppsChoice','profilesChoice'],
            'groupWallpaper':['themesChoice','liveWallpaperChoice','wallpaperChoice'],
            'groupRemote':['bluetoothChoice','quickActionsChoice','audioIndicatorChoice','inputVisibilityChoice','tvButtonChoice','tvButtonHelper'],
            'groupAdvanced':['advancedChoice','openOnStartChoice','followThemeChoice','defaultHomeChoice','tvSettingsChoice']}
        for group,controls in expected.items():
            for control in controls:
                self.assertTrue(self.contains(group,control),(group,control))
                self.assertEqual(1,sum(v.get(A+'id')=='@+id/'+control for v in self.root.iter()),control)
    def test_category_and_content_have_separate_focus_targets(self):
        for category in ['Layout','Apps','Wallpaper','Remote','Advanced']:
            self.assertEqual('@style/SettingsCategory',self.ids['@+id/category'+category].get('style'))
        self.assertEqual('ScrollView',self.ids['@+id/settingsScroll'].tag)
    def test_only_initial_page_is_visible(self):
        self.assertEqual('visible',self.ids['@+id/groupLayout'].get(A+'visibility'))
        for name in ['Apps','Wallpaper','Remote','Advanced']:
            self.assertEqual('gone',self.ids['@+id/group'+name].get(A+'visibility'))

if __name__=='__main__':unittest.main()
