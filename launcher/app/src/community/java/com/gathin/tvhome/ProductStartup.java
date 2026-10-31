package com.gathin.tvhome;

import android.app.Activity;
import android.content.*;
import android.os.Build;
import android.os.UserManager;
import android.view.Gravity;
import android.widget.TextView;
import java.util.Map;
import java.util.WeakHashMap;

/** Storage-free Home before unlock. Personal profiles, media and secrets remain credential-protected. */
final class ProductStartup {
    private static final Map<Activity, BroadcastReceiver> WAITING = new WeakHashMap<>();
    static boolean unlocked(Context c) {
        UserManager users = c.getSystemService(UserManager.class);
        return users == null || users.isUserUnlocked();
    }
    static boolean waiting(Activity a) { return WAITING.containsKey(a); }
    static boolean await(Activity a) {
        if (unlocked(a)) return false;
        TextView text = new TextView(a);
        text.setText(R.string.starting_home); text.setTextColor(0xfff3f6fc);
        text.setBackgroundColor(0xff101923); text.setTextSize(24); text.setGravity(Gravity.CENTER);
        a.setContentView(text);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            boolean requested;
            @Override public void onReceive(Context c, Intent i) {
                if (!requested && unlocked(a) && !a.isFinishing() && !a.isDestroyed()) { requested = true; a.recreate(); }
            }
        };
        WAITING.put(a, receiver);
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_UNLOCKED);
        if (Build.VERSION.SDK_INT >= 33) a.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else a.registerReceiver(receiver, filter);
        // Unlock may race registration. The original Activity stays marked until destruction.
        if (unlocked(a)) receiver.onReceive(a, new Intent(Intent.ACTION_USER_UNLOCKED));
        return true;
    }
    static void close(Activity a) { BroadcastReceiver receiver = WAITING.remove(a); if (receiver != null) a.unregisterReceiver(receiver); }
    private ProductStartup() {}
}
