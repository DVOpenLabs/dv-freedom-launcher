package com.gathin.tvhome;

import android.content.SharedPreferences;

/** Restricted, global remote choice. Layout/profile/theme resets cannot overwrite it. */
final class TvButtonChoice {
    static final String KEY = "tv_button_choice", FOLLOW = "follow", INPUTS = "inputs", LIVE = "live";
    private static final java.util.regex.Pattern PACKAGE = java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    static boolean valid(String value) {
        return FOLLOW.equals(value) || INPUTS.equals(value) || LIVE.equals(value) || appPackage(value) != null;
    }
    static String appPackage(String value) {
        if (value == null || !value.startsWith("app:")) return null;
        String pkg = value.substring(4);
        return pkg.length() <= 255 && PACKAGE.matcher(pkg).matches() ? pkg : null;
    }
    static String normalize(String value) { return valid(value) ? value : FOLLOW; }
    static boolean save(SharedPreferences prefs, String value) {
        if (!valid(value)) return false;
        String old = prefs.getString(KEY, FOLLOW); long revision = prefs.getLong(Config.SETTINGS_REVISION_KEY, 0);
        if (prefs.edit().putString(KEY, value).putLong(Config.SETTINGS_REVISION_KEY, revision + 1).commit()) return true;
        prefs.edit().putString(KEY, old).putLong(Config.SETTINGS_REVISION_KEY, revision).commit();
        return false;
    }
    private TvButtonChoice() { }
}
