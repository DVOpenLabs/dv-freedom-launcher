package com.gathin.tvhome;
import android.content.*;
import android.os.*;
import android.view.accessibility.AccessibilityEvent;
import java.util.Map;
import java.util.WeakHashMap;

/** Full-product redirect: no key filtering, screen reads or foreground-app polling. */
final class HomeRedirect {
    private static final Map<StockHomeRedirectService, Session> ACTIVE = new WeakHashMap<>();
    static void connect(StockHomeRedirectService service) {
        close(service); Session s = new Session(service); ACTIVE.put(service, s); s.connect();
    }
    static void window(StockHomeRedirectService service, AccessibilityEvent event) {
        Session s = ACTIVE.get(service);
        if (s != null && event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) s.window(event);
    }
    static void close(StockHomeRedirectService service) { Session s = ACTIVE.remove(service); if (s != null) s.close(); }
    private static final class Session {
        final StockHomeRedirectService service;
        final Handler main = new Handler(Looper.getMainLooper());
        final DeferredHomePolicy policy = new DeferredHomePolicy();
        BroadcastReceiver unlock;
        boolean closed;
        Session(StockHomeRedirectService s) { service = s; }
        void connect() {
            if (ProductStartup.unlocked(service)) { post(policy.schedule(SystemClock.elapsedRealtime())); return; }
            unlock = new BroadcastReceiver() { public void onReceive(Context c, Intent i) {
                if (ProductStartup.unlocked(service)) { stopWaiting(); post(policy.schedule(SystemClock.elapsedRealtime())); }
            }};
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_UNLOCKED);
            if (Build.VERSION.SDK_INT >= 33) service.registerReceiver(unlock, filter, Context.RECEIVER_NOT_EXPORTED);
            else service.registerReceiver(unlock, filter);
            if (ProductStartup.unlocked(service)) { stopWaiting(); post(policy.schedule(SystemClock.elapsedRealtime())); }
        }
        void window(AccessibilityEvent event) {
            // The service's manifest only subscribes to launcherx. A later profile/results/settings event cancels Home.
            if (!StockHomeRedirectPolicy.PACKAGE.contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) return;
            if (event.getClassName() == null) return;
            main.removeCallbacksAndMessages(null);
            int token = policy.window(event.getPackageName(), event.getClassName(), SystemClock.elapsedRealtime());
            if (token >= 0) post(token);
        }
        void post(int token) {
            main.removeCallbacksAndMessages(null);
            main.postDelayed(() -> {
                PowerManager power = service.getSystemService(PowerManager.class);
                if (closed || !ProductStartup.unlocked(service) || power != null && !power.isInteractive()) { policy.cancel(); return; }
                if (!policy.take(token, SystemClock.elapsedRealtime())) return;
                try { service.startActivity(new Intent(service, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)); }
                catch (RuntimeException unavailable) { /* A later real Home window can retry. */ }
            }, DeferredHomePolicy.GRACE_MS);
        }
        void stopWaiting() { if (unlock != null) { service.unregisterReceiver(unlock); unlock = null; } }
        void close() { closed = true; policy.cancel(); main.removeCallbacksAndMessages(null); stopWaiting(); }
    }
    private HomeRedirect() {}
}
