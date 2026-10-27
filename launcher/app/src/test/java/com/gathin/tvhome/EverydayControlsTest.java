package com.gathin.tvhome;

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class EverydayControlsTest {
    @Test public void recentListIsBoundedAndNewestFirst(){
        String saved="";for(int i=0;i<20;i++)saved=RecentHistory.opened(saved,"org.example.app"+i);
        assertEquals(6,RecentHistory.read(saved).size());assertEquals("org.example.app19",RecentHistory.read(saved).get(0));
        assertEquals("org.example.app14",RecentHistory.read(saved).get(5));
    }
    @Test public void reopensDeduplicateRatherThanReorderFavorites(){
        String saved=RecentHistory.opened("org.example.one\norg.example.two","org.example.two");
        assertEquals(Arrays.asList("org.example.two","org.example.one"),RecentHistory.read(saved));
    }
    @Test public void malformedStorageIsBoundedAndCannotBecomeAnIntent(){
        assertTrue(RecentHistory.read("x".repeat(1301)).isEmpty());assertTrue(RecentHistory.read(null).isEmpty());
        assertEquals(Arrays.asList("org.example.one"),RecentHistory.read("org.example.one\norg.example.one\napp;reboot\ncom.x/Private"));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidLaunchIsRejected(){RecentHistory.opened("","app;reboot");}
    @Test public void historyStorageScopesCurrentAndNamedProfiles(){
        assertNotEquals(RecentHistory.scope(""),RecentHistory.scope("child"));
        assertNotEquals(RecentHistory.scope("adult"),RecentHistory.scope("child"));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidProfileCannotNameStorage(){RecentHistory.scope("../other");}
    @Test public void dimFallbackRetainsThemeUntilExplicitValidChoice(){
        assertEquals(23,WallpaperDim.chosen(-1,23));assertEquals(23,WallpaperDim.chosen(91,23));
        assertEquals(0,WallpaperDim.chosen(0,23));assertEquals(90,WallpaperDim.chosen(90,23));
    }
    @Test public void dimSaveAndFollowThemePreserveUnrelatedAppearance(){
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();p.edit().putInt("columns",8).putString("wallpaper_choice","owner.jpg").commit();
        assertTrue(WallpaperDim.save(p,30));assertEquals(30,p.getInt(WallpaperDim.KEY,-1));assertTrue(WallpaperDim.save(p,-1));assertFalse(p.contains(WallpaperDim.KEY));
        assertEquals(8,p.getInt("columns",0));assertEquals("owner.jpg",p.getString("wallpaper_choice",null));
        Map<String,?> before=p.getAll();assertFalse(WallpaperDim.save(p,200));assertEquals(before,p.getAll());
    }
    @Test public void failedDimWriteRepairsMissingAndExplicitValues(){
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();Map<String,?> before=p.getAll();p.failNext=true;
        assertFalse(WallpaperDim.save(p,30));assertEquals(before,p.getAll());
        assertTrue(WallpaperDim.save(p,45));before=p.getAll();p.failNext=true;
        assertFalse(WallpaperDim.save(p,-1));assertEquals(before,p.getAll());
    }
    @Test public void recentChoiceIsOptInAndFailedSaveRepairsMissingKey(){
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();assertFalse(p.getBoolean(RecentApps.ENABLED,false));
        Map<String,?> before=p.getAll();p.failNext=true;assertFalse(CompanionControls.saveBoolean(p,RecentApps.ENABLED,true));assertEquals(before,p.getAll());
    }
    @Test public void recentAndDimmingChoicesDoNotLeakAcrossProfiles(){
        ProfilePreferencesTest.Memory source=new ProfilePreferencesTest.Memory();
        android.content.SharedPreferences adult=new ProfilePreferences(source,"adult"),child=new ProfilePreferences(source,"child");
        assertTrue(CompanionControls.saveBoolean(adult,RecentApps.ENABLED,true));assertTrue(WallpaperDim.save(adult,45));
        assertFalse(child.getBoolean(RecentApps.ENABLED,false));assertFalse(source.contains(RecentApps.ENABLED));
        assertEquals(-1,child.getInt(WallpaperDim.KEY,-1));assertFalse(source.contains(WallpaperDim.KEY));
    }
    @Test public void disablingRemovesTheOptionalFlagAndFailedClearRepairsHistory(){
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        assertTrue(CompanionControls.saveBoolean(p,RecentApps.ENABLED,true));assertTrue(CompanionControls.saveBoolean(p,RecentApps.ENABLED,null));
        assertFalse(p.contains(RecentApps.ENABLED));p.edit().putString("packages","org.example.one").commit();Map<String,?> before=p.getAll();
        p.failNext=true;assertFalse(RecentApps.clearPreferences(p));assertEquals(before,p.getAll());assertTrue(RecentApps.clearPreferences(p));assertFalse(p.contains("packages"));
    }
}
