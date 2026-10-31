package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.WeakHashMap;

/** Last-known-good overlay and foreground-only recovery; no polling after Home is hidden. */
final class OverlayRecovery {
    static boolean cached;
    private static final Map<Activity, Session> ACTIVE = new WeakHashMap<>();

    static boolean keep(File copy) {
        return OverlayReadPolicy.keepCopy(false, copy.isFile(), SystemClock.elapsedRealtime());
    }
    static void recover(File copy) {
        // openRead restores an interrupted AtomicFile write before the bounded Config parser reads it.
        try (java.io.InputStream ignored = new AtomicFile(copy).openRead()) { }
        catch (java.io.IOException unavailable) { /* First install has no copy. */ }
    }
    static void save(File copy, byte[] bytes) {
        AtomicFile target = new AtomicFile(copy);
        FileOutputStream out = null;
        try { out = target.startWrite(); out.write(bytes); target.finishWrite(out); }
        catch (java.io.IOException failure) { if (out != null) target.failWrite(out); }
    }
    static void reset(Context c) {
        File copy = new File(c.getFilesDir(), Config.FILE_NAME);
        for (File item : new File[]{copy, new File(copy + ".bak"), new File(copy + ".new")}) {
            if (item.exists() && !item.delete()) throw new IllegalStateException("Cannot reset saved theme copy");
        }
        cached = false;
    }
    static void resume(Activity a) {
        pause(a);
        if (!cached) return;
        Session s = new Session(a);
        ACTIVE.put(a, s);
        s.main.postDelayed(s, OverlayReadPolicy.RETRY_MS);
    }
    static void pause(Activity a) { Session s = ACTIVE.remove(a); if (s != null) s.close(); }
    private static final class Session implements Runnable {
        final Activity activity;
        final Handler main = new Handler(Looper.getMainLooper());
        final long deadline = SystemClock.elapsedRealtime() + OverlayReadPolicy.WATCH_MS;
        boolean closed;
        Session(Activity a) { activity = a; }
        public void run() {
            if (!OverlayReadPolicy.retry(!activity.isFinishing() && !activity.isDestroyed(), closed,
                    SystemClock.elapsedRealtime(), deadline)) { close(); return; }
            // Use the same bounded JSON parser as the next load; a partial file never recreates Home.
            if (Config.overlayReadable(activity)) { close(); activity.recreate(); }
            else main.postDelayed(this, OverlayReadPolicy.RETRY_MS);
        }
        void close() { closed = true; main.removeCallbacksAndMessages(null); if (ACTIVE.get(activity) == this) ACTIVE.remove(activity); }
    }
    private OverlayRecovery() {}
}
