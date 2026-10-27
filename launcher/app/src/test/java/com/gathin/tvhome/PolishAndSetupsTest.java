package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PolishAndSetupsTest {
    @Test public void idleHasAnExactDeadlineAndNoWorkWhenOff(){
        PolishPolicy.Idle idle=new PolishPolicy.Idle();idle.arm(100,60);assertFalse(idle.due(60099));assertTrue(idle.due(60100));assertTrue(idle.quiet);
        idle.stop();idle.arm(0,0);assertFalse(idle.due(Long.MAX_VALUE));assertFalse(idle.quiet);
    }
    @Test public void interactionRestartsIdleAndStopCancelsIt(){
        PolishPolicy.Idle idle=new PolishPolicy.Idle();idle.arm(0,30);idle.arm(29999,30);assertFalse(idle.due(30000));idle.stop();assertFalse(idle.due(60000));
    }
    @Test public void wakingNavigationConsumesItsMatchingUpAndRepeat(){
        PolishPolicy.Idle idle=new PolishPolicy.Idle();idle.arm(0,30);idle.due(30000);
        assertTrue(idle.key(23,true,true));assertFalse(idle.quiet);assertTrue(idle.key(23,true,true));assertTrue(idle.key(23,false,true));assertFalse(idle.key(23,true,true));
    }
    @Test public void volumeAndInputWakeWithoutBeingSwallowed(){
        for(int code:new int[]{24,25,178,3}){
            PolishPolicy.Idle idle=new PolishPolicy.Idle();idle.arm(0,30);idle.due(30000);
            assertFalse(idle.key(code,true,false));assertFalse(idle.quiet);assertFalse(idle.key(code,false,false));
        }
    }
    @Test public void stoppingWhileKeyHeldCannotLeaveAStuckKey(){
        PolishPolicy.Idle idle=new PolishPolicy.Idle();idle.arm(0,30);idle.due(30000);idle.key(23,true,true);idle.stop();assertFalse(idle.key(23,false,true));
    }
    @Test public void quietPolicyRejectsArbitraryTimers(){
        for(int value:new int[]{-1,1,29,31,Integer.MAX_VALUE})assertFalse(PolishPolicy.validSeconds(value));
        for(int value:new int[]{0,30,60,120})assertTrue(PolishPolicy.validSeconds(value));
    }
    @Test public void posterContrastHasNoShadeForBlackAndBoundedShadeForWhite(){
        assertEquals(0,PolishPolicy.shade(null));assertEquals(0,PolishPolicy.shade(new int[]{0xff000000}));
        assertEquals(170,PolishPolicy.shade(new int[]{0xffffffff}));assertTrue(PolishPolicy.shade(new int[]{0xffeeeeee})>PolishPolicy.shade(new int[]{0xff888888}));
    }
    @Test public void themeTextUsesSolidContrastingInkAndCanChangeWithTheTheme(){
        assertEquals(0xfff5f7fa,PolishPolicy.ink(new int[]{0xff001127,0xff14263a}));
        assertEquals(0xff182334,PolishPolicy.ink(new int[]{0xfff9f4e8,0xffeeeeee}));
        assertEquals(255,PolishPolicy.ink(new int[]{0xff888888})>>>24);
    }
    @Test public void namesAreTrimmedAndCannotContainControls(){
        assertEquals("My Home",SetupValues.name(" My Home "));
        for(String bad:new String[]{""," ","x".repeat(41),"hello\nworld"})try{SetupValues.name(bad);fail(bad);}catch(IllegalArgumentException expected){}
    }
    @Test public void snapshotsCannotCarryDeviceOrAccountSettings(){
        for(String key:new String[]{"adb_enabled","product_active_profile","enabled_accessibility_services","remember_recent_apps","saved_home_setups","system_brightness"}){
            try{SetupValues.validate(Collections.singletonMap(key,true));fail(key);}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void snapshotRangesAndTypesAreValidatedBeforeCommit(){
        for(Map<String,?> value:Arrays.asList(Collections.singletonMap("columns",0),Collections.singletonMap("columns",8.0),Collections.singletonMap("adaptive_header","true"),Collections.singletonMap("cinematic_idle_seconds",20),Collections.singletonMap("app_position","floating"),Collections.singletonMap("theme_pack_id","../../outside"))){
            try{SetupValues.validate(value);fail(value.toString());}catch(IllegalArgumentException expected){}
        }
        assertEquals(8,SetupValues.validate(Collections.singletonMap("columns",8L)).get("columns"));
        assertEquals(3,SetupValues.validate(Collections.singletonMap("columns",3)).get("columns"));
    }
    @Test public void atomicHomeTransactionPreservesUnrelatedStateAndRepairsFailure(){
        ProfilePreferencesTest.Memory p=new ProfilePreferencesTest.Memory();p.edit().putString("original_user","owner").putBoolean("remember_recent_apps",true).commit();
        Map<String,Object> changes=new HashMap<>();changes.put("columns",8);changes.put("app_position","middle");changes.put(Config.SETTINGS_REVISION_KEY,1L);
        Map<String,?> before=p.getAll();p.failNext=true;assertFalse(SetupValues.commit(p,changes));assertEquals(before,p.getAll());
        assertTrue(SetupValues.commit(p,changes));assertEquals("owner",p.getString("original_user",null));assertTrue(p.getBoolean("remember_recent_apps",false));
    }
    @Test public void snapshotsAndPolishAreIsolatedPerProfile(){
        ProfilePreferencesTest.Memory memory=new ProfilePreferencesTest.Memory();android.content.SharedPreferences one=new ProfilePreferences(memory,"one"),two=new ProfilePreferences(memory,"two");
        Map<String,Object> value=new HashMap<>();value.put("saved_home_setups","[]");value.put("adaptive_header",true);value.put("cinematic_idle_seconds",60);
        assertTrue(SetupValues.commit(one,value));assertTrue(one.contains("saved_home_setups"));assertFalse(two.contains("saved_home_setups"));assertFalse(memory.contains("adaptive_header"));assertFalse(two.getBoolean("adaptive_header",false));
    }
}
