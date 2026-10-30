package com.majnioui.tv.prayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class PrayerSourceTest {
    @Test public void cityCannotReplaceTheProviderOrSmuggleAQuery(){String s=PrayerSource.url("01-01-2026","Example &method=99","Example",3);assertTrue(s.startsWith("https://api.aladhan.com/v1/timingsByCity/"));assertTrue(s.contains("%26method%3D99"));assertTrue(s.endsWith("&method=3"));}
    @Test public void configurationHasNoImplicitLocation(){for(String city:new String[]{""," ","A\r\nB","x".repeat(81)})try{PrayerSource.url("01-01-2026",city,"Example",3);fail();}catch(IllegalArgumentException expected){}try{PrayerSource.url("01-01-2026","Example","Example",999);fail();}catch(IllegalArgumentException expected){}}
}
