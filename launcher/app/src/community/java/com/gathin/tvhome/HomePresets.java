package com.gathin.tvhome;

import java.util.LinkedHashMap;
import java.util.Map;

/** Draft-only starting points. Never change media, apps, permissions or the active profile. */
final class HomePresets {
    static final String[] NAMES = {"Everyday", "Big & clear", "Movie night"};
    static Map<String,Object> settings(int index) {
        if (index < 0 || index >= NAMES.length) throw new IllegalArgumentException("Unknown preset");
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("experience", index == 2 ? "cinema" : index == 0 ? "glass" : "clean");
        values.put("position", index == 1 ? "middle" : "bottom");
        values.put("text", "auto"); values.put("textSize", index == 1 ? 120 : 100);
        values.put("iconSize", index == 1 ? "large" : "medium");
        values.put("spacing", index == 1 ? 16 : 8);
        values.put("names", index == 2 ? "selected" : "all");
        values.put("strongFocus", true); values.put("idleSeconds", index == 2 ? 30 : 0);
        return values;
    }
    private HomePresets() {}
}
