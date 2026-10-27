package com.gathin.tvhome;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class HomeHistoryPolicyTest {
    @Test public void keepsOnlyFiveNewestEntriesWithoutChangingCaller(){
        List<String> input=new ArrayList<>(Arrays.asList("one","two","three","four","five","six"));
        assertEquals(Arrays.asList("two","three","four","five","six"),HomeHistoryPolicy.trim(input));
        assertEquals(6,input.size());
    }
    @Test public void byteBudgetUsesUtf8AndNeverExceedsItsEnvelope(){
        String wide="\u754c".repeat(50000);
        List<String> kept=HomeHistoryPolicy.trim(Arrays.asList(wide,wide,"latest"));
        assertEquals(Arrays.asList(wide,"latest"),kept);
        assertTrue(HomeHistoryPolicy.trim(Arrays.asList("x".repeat(HomeHistoryPolicy.MAX_BYTES))).isEmpty());
    }
    @Test public void profileFileNamesCannotContainPaths(){
        for(String profile:new String[]{"","adult","local_1234-abcd"})assertTrue(HomeHistoryPolicy.profile(profile));
        for(String profile:new String[]{null,"../adult","adult/child","UPPER","a\\b"})assertFalse(HomeHistoryPolicy.profile(profile));
    }
    @Test public void descriptionsAreBoundedAndCannotInjectNewLines(){
        assertEquals("Home change",HomeHistoryPolicy.reason(null));
        assertEquals("Home change",HomeHistoryPolicy.reason(" \n "));
        assertEquals("Wallpaper change",HomeHistoryPolicy.reason(" Wallpaper\nchange "));
        assertEquals(80,HomeHistoryPolicy.reason("x".repeat(100)).length());
    }
    @Test public void onlyExistingLauncherChoiceFilesAreInTheRecoveryContract(){
        assertArrayEquals(new String[]{"order.txt","library_apps.txt","library_home_removed.txt"},HomeHistoryPolicy.FILES);
        assertFalse(Arrays.asList(HomeHistoryPolicy.FILES).contains("config.json"));
        assertFalse(Arrays.asList(HomeHistoryPolicy.FILES).contains("wallpaper.jpg"));
    }
}
