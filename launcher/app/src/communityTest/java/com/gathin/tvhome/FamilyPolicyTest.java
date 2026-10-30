package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class FamilyPolicyTest {
    @Test public void normalProfileIsUnrestrictedAndChildNeedsApps(){assertFalse(new FamilyPolicy("").enabled);try{new FamilyPolicy("{\"enabled\":true,\"allowed\":[]}");fail();}catch(IllegalArgumentException expected){}FamilyPolicy p=new FamilyPolicy(FamilyPolicy.encode(true,Arrays.asList("org.example.video","org.example.music")));assertTrue(p.enabled);assertEquals(2,p.allowed.size());}
    @Test public void malformedOrDuplicateAppRulesAreRejected(){for(String raw:new String[]{"{\"enabled\":\"true\",\"allowed\":[]}","{\"enabled\":false,\"allowed\":[\"org.example.app\",\"org.example.app\"]}","{\"enabled\":false,\"allowed\":[\"shell:grant\"]}","{\"enabled\":false,\"allowed\":[],\"command\":\"grant\"}"})try{new FamilyPolicy(raw);fail(raw);}catch(IllegalArgumentException expected){}}
    @Test public void childCannotExitToAdultWithoutApprovalButCanChooseAnotherChild(){assertFalse(FamilyPolicy.canSelect(true,false,false,false));assertTrue(FamilyPolicy.canSelect(true,false,false,true));assertTrue(FamilyPolicy.canSelect(true,true,false,false));assertTrue(FamilyPolicy.canSelect(false,false,false,false));assertTrue(FamilyPolicy.canSelect(true,false,true,false));}
    @Test public void anAppNameDoesNotMakeItAProtectedSystemWindow(){assertTrue(FamilyGuardPolicy.system("com.android.tv.settings",Collections.emptySet()));assertTrue(FamilyGuardPolicy.system("org.example.input",Collections.singleton("org.example.input")));assertFalse(FamilyGuardPolicy.system("org.evil.settings",Collections.emptySet()));assertFalse(FamilyGuardPolicy.system("com.android.vending",Collections.emptySet()));}
}
