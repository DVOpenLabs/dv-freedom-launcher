package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ContentShelfPolicyTest {
    final long now=2000000000000L;final Set<String> apps=Collections.singleton("com.example.player");
    boolean card(String p,String t,int type,long age,long pos,long duration){return ContentShelfPolicy.accepted(p,t,type,now-age,pos,duration,now,apps);}
    @Test public void visibleRecentMetadataOnly(){assertTrue(card("com.example.player","Episode",0,1000,100,1000));assertFalse(card("com.other.app","Episode",0,1000,100,1000));assertFalse(card("com.example.player"," ",0,1000,100,1000));assertFalse(card("com.example.player","Bad\nTitle",0,1000,100,1000));assertFalse(card("com.example.player","Old",0,ContentShelfPolicy.MAX_AGE+1,100,1000));assertFalse(card("com.example.player","Future",0,-300001,100,1000));}
    @Test public void noInventedProgressAndCompletedItemsAreFiltered(){assertTrue(card("com.example.player","Continue",0,1000,-1,-1));assertFalse(card("com.example.player","Complete",0,1000,960,1000));assertFalse(card("com.example.player","Not begun",0,1000,0,1000));assertTrue(card("com.example.player","Next",1,1000,0,1000));assertFalse(card("com.example.player","Unknown",4,1000,10,100));}
    @Test public void unsafeSchemesAreRejected(){for(String s:new String[]{"file","content","javascript","data","intent","android-app","tel","SETTINGS","","bad value"})assertFalse(s,ContentShelfPolicy.scheme(s));for(String s:new String[]{"https","http","netflix","nflx"})assertTrue(s,ContentShelfPolicy.scheme(s));assertFalse(ContentShelfPolicy.scheme(null));}
    @Test public void consentCannotBeExportedOrCopied(){assertFalse(SetupValues.KEYS.contains(ContentShelves.KEY));assertTrue(LocalProfiles.SCOPED.contains(ContentShelves.KEY));assertFalse(SetupValues.KEYS.contains(LauncherLock.KEY));assertTrue(LocalProfiles.SCOPED.contains(LauncherLock.KEY));assertNotEquals(LocalProfiles.key("one",LauncherLock.KEY),LocalProfiles.key("two",LauncherLock.KEY));}
}
