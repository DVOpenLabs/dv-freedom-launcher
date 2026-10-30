package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LauncherExtrasTest {
    @Test public void installingUtilitiesNeverCreatesHomeShortcutsWithoutChoice(){for(String pkg:OptionalToolPolicy.PACKAGES){assertFalse(OptionalToolPolicy.home(pkg,false));assertTrue(OptionalToolPolicy.home(pkg,true));}assertTrue(OptionalToolPolicy.home("com.example.video",false));}
    @Test public void startupWaitIsOneShotEvenWhenCallbackAndDeadlineRace(){StartupWait wait=new StartupWait();wait.begin(1000,30);assertFalse(wait.take(true,false,1001));assertTrue(wait.take(true,true,2000));assertFalse(wait.take(true,true,31000));assertFalse(wait.pending());}
    @Test public void leavingHomeCancelsFutureStartup(){StartupWait wait=new StartupWait();wait.begin(0,30);wait.cancel();assertFalse(wait.take(true,true,100));assertFalse(wait.take(true,false,31000));}
    @Test public void inactiveHomeCannotLaunchAtTheDeadline(){StartupWait wait=new StartupWait();wait.begin(0,15);assertFalse(wait.take(false,true,16000));wait.cancel();assertFalse(wait.take(true,true,17000));}
    @Test public void timeoutWorksWithoutAnyConnectivityCallbacks(){StartupWait wait=new StartupWait();wait.begin(1000,30);assertFalse(wait.take(true,false,30999));assertTrue(wait.take(true,false,31000));}
    @Test public void zeroWaitPreservesImmediateStartup(){StartupWait wait=new StartupWait();wait.begin(50,0);assertTrue(wait.take(true,false,50));}
    @Test public void startupWaitBounds(){for(int n:new int[]{-1,601,Integer.MAX_VALUE})try{new StartupWait().begin(0,n);fail();}catch(IllegalArgumentException expected){}}
    @Test public void exclusionsRoundTripWithoutRequiringAppsInstalled(){Set<String> saved=new HashSet<>(Arrays.asList("com.example.video","com.example.missing"));assertEquals(saved,LibraryVisibility.parse(LibraryVisibility.encode(saved)));assertTrue(LibraryVisibility.parse("").isEmpty());}
    @Test public void exclusionListsRejectDuplicatesMalformedAndOverflow(){for(String raw:new String[]{"com.example.a\ncom.example.a","com/example","com.example.a\n"})try{LibraryVisibility.parse(raw);fail(raw);}catch(IllegalArgumentException expected){}List<String> many=new ArrayList<>();for(int i=0;i<65;i++)many.add("com.example.app"+i);try{LibraryVisibility.encode(many);fail();}catch(IllegalArgumentException expected){}}
    @Test public void scopedLinksPreserveQueryAndUnicode(){Map<String,String> links=new LinkedHashMap<>();links.put("com.example.video","https://example.com/watch?id=42&lang=日本語");links.put("com.example.other","videoapp://item/42");assertEquals(links,ShortcutPolicy.parse(ShortcutPolicy.encode(links)));}
    @Test public void linksRejectIntentInjectionFilesCredentialsAndControls(){for(String link:new String[]{"intent://app/#Intent;end","file:///secret","content://provider/a","javascript:alert(1)","data:text/plain,x","https://user:pass@example.com/x","https:///bad","https://example.com/a\nb","not-a-link"})try{ShortcutPolicy.uri(link);fail(link);}catch(IllegalArgumentException expected){}}
    @Test public void shortcutListsAreBoundedAndRejectDuplicatePackages(){Map<String,String> tooMany=new HashMap<>();for(int i=0;i<33;i++)tooMany.put("com.example.app"+i,"video://item/"+i);try{ShortcutPolicy.encode(tooMany);fail();}catch(IllegalArgumentException expected){}String one=ShortcutPolicy.encode(Collections.singletonMap("com.example.a","video://one"));try{ShortcutPolicy.parse(one+"\n"+one);fail();}catch(IllegalArgumentException expected){}}
    @Test public void ignoredAppKeysMatchPrecisely(){Set<String> rules=RemoteIgnore.parse("key:190\nscan:490");assertTrue(RemoteIgnore.matches(rules,190,1));assertTrue(RemoteIgnore.matches(rules,196,490));assertFalse(RemoteIgnore.matches(rules,196,491));assertFalse(RemoteIgnore.matches(rules,189,0));}
    @Test public void scannedNavigationAndPowerCannotBeSwallowed(){Set<String> rules=RemoteIgnore.parse("scan:490");for(int code:new int[]{3,4,19,20,21,22,23,24,25,26,66,82,164,176,178,223,224})assertFalse("code "+code,RemoteIgnore.matches(rules,code,490));}
    @Test public void ignoredButtonRulesRejectUnsafeAndAmbiguousCodes(){for(String raw:new String[]{"key:3","key:4","key:26","key:0","scan:0","key:0190","key:190\nkey:190","scan:9999999","view:http://example.com"})try{RemoteIgnore.parse(raw);fail(raw);}catch(IllegalArgumentException expected){}}
}
