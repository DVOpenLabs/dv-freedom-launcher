package com.gathin.tvhome;

import java.util.Locale;

/** Decisions for optional Home surfaces, independent of Android and stored artwork. */
final class HomeSurfacePolicy {
    static final String CALM="freedom_calm_look", GLASS="freedom_glass_dock", PALETTE="freedom_palette", WEATHER="freedom_weather_scene";
    private HomeSurfacePolicy() { }
    static boolean enabled(Object value) { return Boolean.TRUE.equals(value); }
    static String string(Object value, String fallback) { return value instanceof String ? (String)value : fallback; }
    static String calm(String value) { return "onyx".equals(value)||"orba".equals(value)||"midnight".equals(value)?value:"current"; }
    static String palette(String value) { return "wallpaper".equals(value)?value:"theme"; }
    static String weather(WeatherSnapshot snapshot,long now) {
        if(snapshot==null||!snapshot.fresh(now))return "none";
        String c=snapshot.condition.toLowerCase(Locale.ROOT);
        if(c.contains("rain")||c.contains("drizzle")||c.contains("shower"))return "rain";
        if(c.contains("mist")||c.contains("fog")||c.contains("haze"))return "mist";
        if(c.contains("clear")||c.contains("sunny"))return "sun";
        return "none";
    }
    static int accent(int rgb) {
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        return 0xff000000 | ((160+r*80/255)<<16) | ((160+g*80/255)<<8) | (160+b*80/255);
    }
    // A dark translucent material keeps light labels legible even on bright pictures.
    static int dock(int rgb) {
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        return 0xcc000000 | (Math.min(45,r/3)<<16) | (Math.min(50,g/3)<<8) | Math.min(55,b/3);
    }
}
