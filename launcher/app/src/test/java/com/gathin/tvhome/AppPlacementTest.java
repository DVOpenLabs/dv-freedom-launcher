package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppPlacementTest {
    @Test public void positionsReserveHeaderAndBottomClearance() {
        assertEquals(24,AppPlacement.padding("top",24,600,99));
        assertEquals(312,AppPlacement.padding("middle",24,600,99));
        assertEquals(600,AppPlacement.padding("bottom",24,600,99));
    }
    @Test public void crowdedGridKeepsHeaderClearAndMissingChoicePreservesTheme() {
        for(String id:AppPlacement.IDS)assertEquals(80,AppPlacement.padding(id,80,20,99));
        assertEquals(99,AppPlacement.padding(null,24,600,99));
        assertEquals(99,AppPlacement.padding("unknown",24,600,99));
    }
    @Test public void positionPreviewExpiresAndRejectsStyles() {
        HomeStyles.Preview p=new HomeStyles.Preview(AppPlacement.IDS);
        p.begin("middle",100);assertEquals("middle",p.active(20_099));assertNull(p.active(20_100));
        try {p.begin("cinematic",1);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void failedPositionSaveRestoresAllManagedAndUnrelatedChoices() {
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        p.edit().putString("wallpaper_choice","owner.jpg").putInt("columns",8).commit();
        assertTrue(DesktopLayout.commit(p,"layout","guard",false,"middle"));
        assertEquals("middle",p.getString(AppPlacement.KEY,null));assertEquals("",p.getString(DesktopLayout.UNDO_POSITION,null));
        java.util.Map<String,?> before=p.getAll();p.failNext=true;
        assertFalse(DesktopLayout.commit(p,"bad","guard2",false,"bottom"));assertEquals(before,p.getAll());
        assertTrue(DesktopLayout.commit(p,"","",true,null));assertFalse(p.contains(AppPlacement.KEY));assertFalse(p.contains(DesktopLayout.UNDO_POSITION));
        assertEquals("owner.jpg",p.getString("wallpaper_choice",null));assertEquals(8,p.getInt("columns",0));
    }
    @Test public void eightColumnStyleUndoIsValid() {
        assertEquals(Integer.valueOf(8),HomeStyles.Snapshot.decode("minimal|8|0|100").columns);
    }
    @Test public void nativeKeepTouchesOnlyPositionAndRevisionAndRepairsFailure() {
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        p.edit().putInt("columns",8).putString("wallpaper_choice","live.jpg").commit();
        assertTrue(PlacementSession.save(p,"middle"));assertEquals("middle",p.getString(AppPlacement.KEY,null));
        java.util.Map<String,?> before=p.getAll();p.failNext=true;
        assertFalse(PlacementSession.save(p,"bottom"));assertEquals(before,p.getAll());
        assertFalse(PlacementSession.save(p,null));assertEquals(before,p.getAll());
    }
}
