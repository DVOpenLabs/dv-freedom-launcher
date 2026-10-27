package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeAppearanceTest {
    @Test public void absentChoiceKeepsAllOriginalSizes(){assertArrayEquals(new int[]{100,100,100},HomeAppearance.parse(""));}
    @Test public void eachTextRoleCanHaveItsOwnSize(){int[] value={85,110,120};assertArrayEquals(value,HomeAppearance.parse(HomeAppearance.encode(value)));}
    @Test public void malformedOrExcessiveSizesCannotEnterSavedSetups(){for(String bad:new String[]{"v2,100,100,100","v1,0,100,100","v1,100,1000000,100","v1,100,100","v1,100,100,100,120","../,100,100,100"})try{HomeAppearance.parse(bad);fail(bad);}catch(IllegalArgumentException expected){}}
    @Test public void productKeysAreScopedAndIncludedInRestorePoints(){for(String key:new String[]{HomeAppearance.KEY,HomeCategories.KEY,NativeTileEditor.KEY}){assertTrue(SetupValues.KEYS.contains(key));assertNotEquals(LocalProfiles.key("one",key),LocalProfiles.key("two",key));}assertFalse(SetupValues.KEYS.contains("home-history.json"));}
}
