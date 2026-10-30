package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional local provider client. Workers and timers exist only while the owning surface is active. */
final class WeatherClient {
    static final String HOME = "private_weather_home", DREAM = "private_weather_dream";
    private static final String PACKAGE = "io.github.sumyabazar1.weather";
    private static final Uri CURRENT = Uri.parse("content://io.github.sumyabazar1.weather.snapshot/current");
    private static final Map<Activity, Attachment> ACTIVE = new WeakHashMap<>();
    static void attach(Activity a) {
        if (!(a instanceof MainActivity || a instanceof ExperienceHomeActivity || a instanceof CategoryHomeActivity)) return;
        Attachment previous = ACTIVE.remove(a);
        if (previous != null) previous.close(); // a profile/style can rebuild Home without pausing the Activity
        if ("off".equals(ProfileStore.preferences(a).getString(HOME, "off"))) return;
        View header = a.findViewById(R.id.leftBlock);
        if (!(header instanceof LinearLayout)) return;
        Attachment attachment = new Attachment(a, (LinearLayout) header, HOME, false);
        ACTIVE.put(a, attachment);
        Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity x, Bundle b) {} public void onActivityStarted(Activity x) {}
            public void onActivityStopped(Activity x) {} public void onActivitySaveInstanceState(Activity x, Bundle b) {}
            public void onActivityResumed(Activity x) {}
            public void onActivityPaused(Activity x) { if (x == a) detach(); }
            public void onActivityDestroyed(Activity x) { if (x == a) detach(); }
            private void detach() { attachment.close(); if (ACTIVE.get(a) == attachment) ACTIVE.remove(a); }
        };
        a.getApplication().registerActivityLifecycleCallbacks(lifecycle);
        attachment.afterClose = () -> a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);
        attachment.start();
    }
    static String status(Context c) {
        String units = ProfileStore.preferences(c).getString(HOME, "off");
        return "off".equals(units) ? "Off · optional Weather companion" : "On · requires companion sharing";
    }
    static void open(Context c) {
        try { c.startActivity(new Intent().setClassName(PACKAGE, PACKAGE + ".WeatherActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception missing) { Toast.makeText(c, "Install the optional Weather companion, then choose a city and approve launcher sharing in its settings.", Toast.LENGTH_LONG).show(); }
    }
    static void settings(Activity a) {
        new AlertDialog.Builder(a).setTitle("Weather display")
                .setItems(new String[]{"On Home", "On the screensaver", "Open Weather and sharing settings"}, (d, which) -> {
                    if (which == 2) { open(a); return; }
                    String key = which == 0 ? HOME : DREAM;
                    String[] values = {"off", "both", "c", "f"};
                    String saved = ProfileStore.preferences(a).getString(key, "off");
                    final int[] draft = {java.util.Arrays.asList(values).indexOf(saved)};
                    new AlertDialog.Builder(a).setTitle(which == 0 ? "Home weather" : "Screensaver weather")
                            .setSingleChoiceItems(new String[]{"Off", "Fahrenheit and Celsius", "Celsius", "Fahrenheit"}, draft[0], (dialog, item) -> draft[0] = item)
                            .setNegativeButton("Cancel", null).setPositiveButton("Apply", (dialog, button) -> {
                                if (!LauncherLock.require(a, () -> settings(a))) return;
                                ProductFeatures.beforeChange(a, "Change weather display");
                                java.util.Map<String,Object> changes = new java.util.HashMap<>();
                                changes.put(key, values[draft[0]]);
                                changes.put(Config.SETTINGS_REVISION_KEY, ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY, 0) + 1);
                                if (SetupValues.commit(ProfileStore.preferences(a), changes)) a.recreate();
                            }).show();
                }).setNegativeButton("Close", null).show();
    }
    static final class Attachment implements AutoCloseable {
        final Context context; final LinearLayout host; final String key; final boolean dream;
        final Handler main = new Handler(Looper.getMainLooper());
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        final CancellationSignal cancel = new CancellationSignal();
        TextView label; boolean closed; Runnable afterClose;
        final Runnable tick = this::load;
        Attachment(Context c, LinearLayout host, String key, boolean dream) { context = c; this.host = host; this.key = key; this.dream = dream; }
        void start() {
            if ("off".equals(ProfileStore.preferences(context).getString(key, "off"))) { close(); return; }
            label = new TextView(context); label.setTextSize(dream ? 20 : 14); label.setTypeface(SettingsTypography.regular(context));
            label.setTextColor(0xfff4f7fb); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setShadowLayer(3, 0, 1, 0xff081020); label.setVisibility(View.GONE);
            if (!dream) { label.setFocusable(true); label.setClickable(true); label.setBackgroundResource(R.drawable.settings_choice_bg); label.setOnClickListener(v -> open(context)); }
            host.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            load();
        }
        void load() {
            if (closed) return;
            worker.execute(() -> {
                WeatherSnapshot snapshot = null;
                try (Cursor row = context.getContentResolver().query(CURRENT, null, null, null, null, cancel)) {
                    if (row != null && row.moveToFirst() && row.getInt(0) == 1)
                        snapshot = new WeatherSnapshot(row.getDouble(1), row.getString(2), row.getString(3), row.getLong(4));
                } catch (RuntimeException unavailable) { /* Missing, unapproved or expired: do not invent a reading. */ }
                WeatherSnapshot result = snapshot;
                main.post(() -> {
                    if (closed) return;
                    String text = result == null ? "" : result.label(ProfileStore.preferences(context).getString(key, "off"), System.currentTimeMillis());
                    label.setText(text); label.setContentDescription(result == null ? "" : result.city + ", " + text);
                    label.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
                    main.postDelayed(tick, 60_000);
                });
            });
        }
        @Override public void close() { if (closed) return; closed = true; main.removeCallbacksAndMessages(null); cancel.cancel(); worker.shutdownNow(); if (label != null) host.removeView(label); if (afterClose != null) { afterClose.run(); afterClose = null; } }
    }
}
