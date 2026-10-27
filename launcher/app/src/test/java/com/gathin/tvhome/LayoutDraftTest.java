package com.gathin.tvhome;

import android.content.SharedPreferences;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class LayoutDraftTest {
    private static final String A="org.example.one",B="org.example.two";
    @Test public void acceptsAppOnlyEditsAndDefendsCopies() {
        java.util.List<String> favorites=new java.util.ArrayList<>(Arrays.asList(A,B));
        LayoutDraft value=new LayoutDraft(favorites,Collections.emptyList(),Collections.singletonMap(A,"Anime"));
        favorites.clear();assertEquals(Arrays.asList(A,B),value.favorites);
    }
    @Test(expected=IllegalArgumentException.class) public void duplicateFavoriteRejected(){new LayoutDraft(Arrays.asList(A,A),Collections.emptyList(),Collections.emptyMap());}
    @Test(expected=IllegalArgumentException.class) public void favoriteHiddenOverlapRejected(){new LayoutDraft(Arrays.asList(A),Arrays.asList(A),Collections.emptyMap());}
    @Test(expected=IllegalArgumentException.class) public void shellSyntaxCannotBeAnApp(){new LayoutDraft(Arrays.asList("app;reboot"),Collections.emptyList(),Collections.emptyMap());}
    @Test(expected=IllegalArgumentException.class) public void emptyFavoritesRejected(){new LayoutDraft(Collections.emptyList(),Collections.emptyList(),Collections.emptyMap());}
    @Test(expected=IllegalArgumentException.class) public void controlCharactersInLabelsRejected(){new LayoutDraft(Arrays.asList(A),Collections.emptyList(),Collections.singletonMap(A,"A\nB"));}
    @Test(expected=IllegalArgumentException.class) public void staleDraftRejected(){LayoutDraft.requireCurrent("a".repeat(64),"b".repeat(64),"adult","adult");}
    @Test(expected=IllegalArgumentException.class) public void changedProfileRejected(){LayoutDraft.requireCurrent("a".repeat(64),"a".repeat(64),"adult","child");}
    @Test public void currentDraftAccepted(){LayoutDraft.requireCurrent("a".repeat(64),"a".repeat(64),"","");}
    @Test(expected=IllegalArgumentException.class) public void nativeReorderingBlocksDesktop(){LayoutDraft.requireEditable(true,false);}
    @Test(expected=IllegalArgumentException.class) public void stylePreviewBlocksDesktop(){LayoutDraft.requireEditable(false,true);}
    @Test(expected=java.io.IOException.class) public void duplicateJsonKeysRejected()throws Exception {ThemeArchive.validateJsonObject("{\"schema\":1,\"schema\":2}");}
    @Test public void applyAndUndoTouchOnlyManagedFields() {
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        p.edit().putString("wallpaper_choice","my.jpg").putInt("columns",7).putString("tv_button_choice","live").commit();
        assertTrue(DesktopLayout.commit(p,"draft","guard",false));
        assertEquals("",p.getString(DesktopLayout.UNDO,null));assertEquals("draft",p.getString(DesktopLayout.STATE,null));
        assertTrue(DesktopLayout.commit(p,"","",true));assertFalse(p.contains(DesktopLayout.STATE));assertFalse(p.contains(DesktopLayout.UNDO));
        assertEquals("my.jpg",p.getString("wallpaper_choice",null));assertEquals(7,p.getInt("columns",0));assertEquals("live",p.getString("tv_button_choice",null));
    }
    @Test public void failedCommitRestoresMemoryAndPriorUndo() {
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        assertTrue(DesktopLayout.commit(p,"old","first",false));Map<String,?> before=p.getAll();
        p.failNext=true;assertFalse(DesktopLayout.commit(p,"bad","second",false));assertEquals(before,p.getAll());
    }
    @Test public void desktopAndAudioAreScopedWithoutChangingOtherProfile() {
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        SharedPreferences adult=new ProfilePreferences(p,"adult"),child=new ProfilePreferences(p,"child");
        assertTrue(DesktopLayout.commit(adult,"adult layout","adult guard",false));
        assertTrue(CompanionControls.saveAudio(adult,true));
        assertFalse(child.contains(DesktopLayout.STATE));assertFalse(child.getBoolean(CompanionControls.AUDIO,false));
        assertEquals("adult layout",adult.getString(DesktopLayout.STATE,null));
    }
    @Test public void failedAudioChoiceDoesNotChangeExistingAppearance() {
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();
        p.edit().putString("wallpaper_choice","live").putLong(Config.SETTINGS_REVISION_KEY,4).commit();Map<String,?> before=p.getAll();
        p.failNext=true;assertFalse(CompanionControls.saveAudio(p,true));assertEquals(before,p.getAll());
    }
    @Test public void desktopMoveWinsOverCollidingNativeRanks() {
        Map<String,Integer> ranks=new HashMap<>();ranks.put(A+"/.Main",0);ranks.put(B+"/.Main",1);
        assertTrue(LayoutDraft.compareOrder(true,Arrays.asList(B,A),ranks,A,A+"/.Main",B,B+"/.Main")>0);
        assertTrue(LayoutDraft.compareOrder(false,Arrays.asList(B,A),ranks,A,A+"/.Main",B,B+"/.Main")<0);
    }
    @Test public void desktopFavoritesPrecedeSavedNonFavorites() {
        assertTrue(LayoutDraft.compareOrder(true,Arrays.asList(B),Collections.singletonMap(A+"/.Main",0),B,B+"/.Main",A,A+"/.Main")<0);
    }
    @Test public void nonFavoritesKeepTheirNativeOrder() {
        Map<String,Integer> ranks=new HashMap<>();ranks.put(A+"/.Main",0);ranks.put(B+"/.Main",1);
        assertTrue(LayoutDraft.compareOrder(true,Arrays.asList("org.example.other"),ranks,A,A+"/.Main",B,B+"/.Main")<0);
    }
    @Test public void oldOrderRanksCanBeRetainedAcrossDockChanges() {
        Order old=new Order(A,Arrays.asList(A+"/.Main",B+"/.Main"),1);
        assertTrue(Order.ranks(old,Arrays.asList(B)).isEmpty());
        assertEquals(Integer.valueOf(1),Order.allRanks(old).get(B+"/.Main"));
    }
}
