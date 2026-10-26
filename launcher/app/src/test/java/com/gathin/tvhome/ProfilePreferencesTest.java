package com.gathin.tvhome;

import android.content.SharedPreferences;
import org.junit.Test;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class ProfilePreferencesTest {
    private static final class Memory implements SharedPreferences {
        final Map<String, Object> data = new HashMap<>(); int commits; boolean failNext;
        public Map<String, ?> getAll(){return new HashMap<>(data);}
        public String getString(String k,String d){return (String)data.getOrDefault(k,d);}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return (Set<String>)data.getOrDefault(k,d);}
        public int getInt(String k,int d){return (Integer)data.getOrDefault(k,d);}
        public long getLong(String k,long d){return (Long)data.getOrDefault(k,d);}
        public float getFloat(String k,float d){return (Float)data.getOrDefault(k,d);}
        public boolean getBoolean(String k,boolean d){return (Boolean)data.getOrDefault(k,d);}
        public boolean contains(String k){return data.containsKey(k);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public Editor edit(){return new Editor(){
            final Map<String,Object> changes=new HashMap<>(); final Set<String> removed=new HashSet<>(); boolean clear;
            private Editor put(String k,Object v){changes.put(k,v);removed.remove(k);return this;}
            public Editor putString(String k,String v){return put(k,v);}
            public Editor putStringSet(String k,Set<String>v){return put(k,v);}
            public Editor putInt(String k,int v){return put(k,v);}
            public Editor putLong(String k,long v){return put(k,v);}
            public Editor putFloat(String k,float v){return put(k,v);}
            public Editor putBoolean(String k,boolean v){return put(k,v);}
            public Editor remove(String k){removed.add(k);changes.remove(k);return this;}
            public Editor clear(){clear=true;return this;}
            public boolean commit(){commits++;apply();boolean result=!failNext;failNext=false;return result;}
            public void apply(){if(clear)data.clear();for(String k:removed)data.remove(k);data.putAll(changes);}
        };}
    }
    @Test public void appearanceOrderAndGlobalRevisionSaveInOneEditorCommit() {
        Memory memory=new Memory(); SharedPreferences adult=new ProfilePreferences(memory,"adult");
        adult.edit().putInt("columns",6).putString("wallpaper_choice","adult.jpg").putLong("settings_revision",8).commit();
        assertEquals(1,memory.commits); assertEquals(6,adult.getInt("columns",4));
        assertEquals(4,memory.getInt("columns",4)); assertEquals(8,memory.getLong("settings_revision",0));
        assertEquals("adult.jpg",adult.getString("wallpaper_choice",null));
    }
    @Test public void switchingBackFindsTheSameProfileWithoutCopyingOrChangingDefault() {
        Memory memory=new Memory(); memory.edit().putInt("columns",5).putString(OpenOnStart.KEY,"org.example.global").commit();
        new ProfilePreferences(memory,"adult").edit().putInt("columns",6).apply();
        SharedPreferences child=new ProfilePreferences(memory,"child"); child.edit().putInt("columns",4).apply();
        assertEquals(6,new ProfilePreferences(memory,"adult").getInt("columns",0));
        assertEquals(4,child.getInt("columns",0)); assertEquals(5,memory.getInt("columns",0));
        assertEquals("org.example.global",child.getString(OpenOnStart.KEY,null));
    }
    @Test public void clearRemoveAndEnumerationCannotEraseOtherProfilesOrControls() {
        Memory memory=new Memory(); memory.edit().putInt("columns",5).putLong("settings_revision",10).commit();
        SharedPreferences adult=new ProfilePreferences(memory,"adult"),child=new ProfilePreferences(memory,"child");
        adult.edit().putInt("columns",6).putString("home_style_undo","old").apply(); child.edit().putInt("columns",4).apply();
        assertEquals(6,adult.getAll().get("columns")); assertFalse(adult.getAll().containsKey("profile.child.columns"));
        adult.edit().clear().commit(); assertFalse(adult.contains("columns"));
        assertEquals(4,child.getInt("columns",0)); assertEquals(5,memory.getInt("columns",0)); assertEquals(10,memory.getLong("settings_revision",0));
    }
    @Test public void failedSelectionRestoresInMemoryPointerAndRevision() {
        Memory memory=new Memory(); memory.edit().putString("product_active_profile","adult").putLong("settings_revision",5).commit();
        memory.failNext=true; assertFalse(ProfileStore.saveSelection(memory,"child"));
        assertEquals("adult",memory.getString("product_active_profile","")); assertEquals(5,memory.getLong("settings_revision",0));
    }
    @Test public void selectionPersistsAndInvalidSelectionDoesNotWrite() {
        Memory memory=new Memory(); assertTrue(ProfileStore.saveSelection(memory,"adult"));
        assertEquals("adult",memory.getString("product_active_profile","")); assertEquals(1,memory.getLong("settings_revision",0));
        int commits=memory.commits; assertFalse(ProfileStore.saveSelection(memory,"../other")); assertEquals(commits,memory.commits);
    }
    @Test public void themesAndLiveChoicesAreScopedAndFailedSaveRepairsSelection() {
        Memory memory=new Memory(); SharedPreferences adult=new ProfilePreferences(memory,"adult");
        String old="p-00000000-0000-0000-0000-000000000001", next="p-00000000-0000-0000-0000-000000000002";
        assertTrue(ThemeLibrary.saveChoice(adult,old));
        adult.edit().putBoolean(ThemeLibrary.LIVE,true).apply();
        assertEquals("",new ProfilePreferences(memory,"child").getString(ThemeLibrary.CHOICE,""));
        assertFalse(new ProfilePreferences(memory,"child").getBoolean(ThemeLibrary.LIVE,false));
        long revision=memory.getLong(Config.SETTINGS_REVISION_KEY,0);
        memory.failNext=true; assertFalse(ThemeLibrary.saveChoice(adult,next));
        assertEquals(old,adult.getString(ThemeLibrary.CHOICE,""));
        assertEquals(revision,memory.getLong(Config.SETTINGS_REVISION_KEY,0));
        assertFalse(ThemeLibrary.saveChoice(adult,"../bad"));
    }
    @Test public void focusAndSpacingAreScopedButRemoteAndInputControlsStayGlobal() {
        Memory memory=new Memory(); SharedPreferences adult=new ProfilePreferences(memory,"adult"), child=new ProfilePreferences(memory,"child");
        adult.edit().putString(TileSpacing.FOCUS,"double").putInt(TileSpacing.CHOICE,16)
                .putString("tv_button_choice","inputs").putBoolean("hide_inactive_hdmi_choice",true).apply();
        assertEquals("double",adult.getString(TileSpacing.FOCUS,"")); assertEquals(16,adult.getInt(TileSpacing.CHOICE,0));
        assertEquals("",child.getString(TileSpacing.FOCUS,"")); assertEquals(0,child.getInt(TileSpacing.CHOICE,0));
        assertEquals("inputs",child.getString("tv_button_choice","")); assertTrue(child.getBoolean("hide_inactive_hdmi_choice",false));
        adult.edit().clear().apply(); assertFalse(adult.contains(TileSpacing.FOCUS));
        assertEquals("inputs",memory.getString("tv_button_choice","")); assertTrue(memory.getBoolean("hide_inactive_hdmi_choice",false));
    }
    @Test public void failedRemoteSaveRepairsGlobalChoiceAndRevisionWithoutTouchingAppearance() {
        Memory memory=new Memory();new ProfilePreferences(memory,"adult").edit().putString(TileSpacing.FOCUS,"double").apply();
        assertTrue(TvButtonChoice.save(memory,"inputs"));long before=memory.getLong(Config.SETTINGS_REVISION_KEY,0);
        memory.failNext=true;assertFalse(TvButtonChoice.save(memory,"live"));
        assertEquals("inputs",memory.getString(TvButtonChoice.KEY,""));assertEquals(before,memory.getLong(Config.SETTINGS_REVISION_KEY,0));
        assertEquals("double",new ProfilePreferences(memory,"adult").getString(TileSpacing.FOCUS,""));
        int commits=memory.commits;assertFalse(TvButtonChoice.save(memory,"settings"));assertEquals(commits,memory.commits);
    }
}
