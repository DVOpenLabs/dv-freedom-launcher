package com.gathin.tvhome;

/** Exact Google Home window matching; no content, app tracking or remote-key inspection. */
final class StockHomeRedirectPolicy {
    static final String PACKAGE = "com.google.android.apps.tv.launcherx";
    static final String HOME = PACKAGE + ".home.HomeActivity";
    private boolean attempted;
    private long lastAttempt;

    static boolean isHome(CharSequence packageName, CharSequence className) {
        return PACKAGE.contentEquals(packageName == null ? "" : packageName)
                && HOME.contentEquals(className == null ? "" : className);
    }

    boolean allow(long elapsedRealtime) {
        // Coalesce same-tick duplicates without discarding a distinct, rapid Home press.
        // A longer cooldown can strand the user on Google Home with no later window event.
        if (elapsedRealtime < 0 || (attempted && elapsedRealtime == lastAttempt)) return false;
        attempted = true;
        lastAttempt = elapsedRealtime;
        return true;
    }
}
