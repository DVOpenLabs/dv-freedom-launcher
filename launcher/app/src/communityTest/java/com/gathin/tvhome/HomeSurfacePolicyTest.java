package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeSurfacePolicyTest {
    @Test public void invalidValuesUseExistingAppearance() {
        assertEquals("current",HomeSurfacePolicy.calm(null));assertEquals("current",HomeSurfacePolicy.calm("unknown"));
        assertEquals("orba",HomeSurfacePolicy.calm("orba"));assertEquals("theme",HomeSurfacePolicy.palette(null));
        assertEquals("wallpaper",HomeSurfacePolicy.palette("wallpaper"));
        assertFalse(HomeSurfacePolicy.enabled("true"));assertFalse(HomeSurfacePolicy.enabled(null));
        assertTrue(HomeSurfacePolicy.enabled(Boolean.TRUE));assertEquals("current",HomeSurfacePolicy.string(42,"current"));
    }
    @Test public void weatherUsesOnlyFreshRecognizedConditions() {
        long now=1_000_000_000L;
        assertEquals("none",HomeSurfacePolicy.weather(null,now));
        assertEquals("rain",HomeSurfacePolicy.weather(new WeatherSnapshot(18,"City","Light rain",now),now));
        assertEquals("mist",HomeSurfacePolicy.weather(new WeatherSnapshot(18,"City","Fog",now),now));
        assertEquals("sun",HomeSurfacePolicy.weather(new WeatherSnapshot(18,"City","Clear",now),now));
        assertEquals("none",HomeSurfacePolicy.weather(new WeatherSnapshot(18,"City","Cloudy",now),now));
        assertEquals("none",HomeSurfacePolicy.weather(new WeatherSnapshot(18,"City","Rain",now-WeatherSnapshot.MAX_AGE-1),now));
        assertEquals("none",HomeSurfacePolicy.weather(new WeatherSnapshot(18,"City","Rain",now+60_001),now));
    }
    @Test public void wallpaperPaletteStaysDarkForLightText() {
        for(int color:new int[]{0xffffffff,0xffff0000,0xff00ff00,0xff0000ff,0xff000000}) {
            int dock=HomeSurfacePolicy.dock(color);
            assertEquals(204,(dock>>>24)&255);
            assertTrue(((dock>>>16)&255)<=45);assertTrue(((dock>>>8)&255)<=50);assertTrue((dock&255)<=55);
            // Worst case is a white still under the translucent material.
            double r=linear(composite((dock>>>16)&255));
            double g=linear(composite((dock>>>8)&255));
            double b=linear(composite(dock&255));
            double luminance=.2126*r+.7152*g+.0722*b;
            assertTrue("white text contrast",1.05/(luminance+.05)>=4.5);
        }
    }
    @Test public void importedSurfaceSettingsRejectUnknownTypesAndValues() {
        java.util.Map<String,Object> values=new java.util.HashMap<>();
        values.put(HomeSurfacePolicy.CALM,"onyx");values.put(HomeSurfacePolicy.GLASS,true);values.put(HomeSurfacePolicy.PALETTE,"wallpaper");values.put(HomeSurfacePolicy.WEATHER,false);values.put("freedom_clock_style","flow");assertEquals(values,SetupValues.validate(values));
        for(String key:values.keySet()) {java.util.Map<String,Object> bad=new java.util.HashMap<>(values);bad.put(key,42);try{SetupValues.validate(bad);fail(key);}catch(IllegalArgumentException expected){}}
        for(String key:new String[]{HomeSurfacePolicy.CALM,HomeSurfacePolicy.PALETTE,"freedom_clock_style"}){java.util.Map<String,Object> bad=new java.util.HashMap<>(values);bad.put(key,"unknown");try{SetupValues.validate(bad);fail(key);}catch(IllegalArgumentException expected){}}
    }
    private static double composite(int channel) { return (channel*.8+255*.2)/255.0; }
    private static double linear(double c) { return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4); }
}
