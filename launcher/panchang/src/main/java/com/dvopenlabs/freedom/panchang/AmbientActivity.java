package com.dvopenlabs.freedom.panchang;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Optional foreground devotional display; Back returns to the calendar. */
public final class AmbientActivity extends Activity {
    private PanchangPanel panel;
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(PanchangActivity.localized(base)); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        panel = new PanchangPanel(this, true);
        SharedPreferences prefs = getSharedPreferences(PanchangActivity.PREFS, MODE_PRIVATE);
        panel.setCity(PanchangActivity.savedCity(prefs));
        panel.setRegion(Region.named(prefs.getString(PanchangActivity.KEY_REGION, Region.forLocale().name())));
        setContentView(panel);
    }
    @Override protected void onResume() { super.onResume(); panel.start(); }
    @Override protected void onPause() { panel.stop(); super.onPause(); }
}
