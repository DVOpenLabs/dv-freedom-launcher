package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/** Optional Home-only helper for the Google TV Streamer. Inactive until selected in Accessibility. */
public final class StockHomeRedirectService extends AccessibilityService {
    private final StockHomeRedirectPolicy policy = new StockHomeRedirectPolicy();
    private BroadcastReceiver unlock;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        UserManager users = getSystemService(UserManager.class);
        if (users == null || users.isUserUnlocked()) {
            openHome();
            return;
        }
        unlock = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                stopWaiting();
                openHome();
            }
        };
        registerReceiver(unlock, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        if (users.isUserUnlocked()) { stopWaiting(); openHome(); }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && StockHomeRedirectPolicy.isHome(event.getPackageName(), event.getClassName())) openHome();
    }

    private void openHome() {
        UserManager users = getSystemService(UserManager.class);
        PowerManager power = getSystemService(PowerManager.class);
        if ((users != null && !users.isUserUnlocked()) || (power != null && !power.isInteractive())
                || !policy.allow(SystemClock.elapsedRealtime())) return;
        try {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (android.content.ActivityNotFoundException | SecurityException failure) {
            Log.w("CYTV.HomeRedirect", "Cannot open Home (" + failure.getClass().getSimpleName() + ")");
        }
    }

    private void stopWaiting() {
        if (unlock != null) { unregisterReceiver(unlock); unlock = null; }
    }
    @Override public void onDestroy() { stopWaiting(); super.onDestroy(); }
    @Override public void onInterrupt() { }
}
