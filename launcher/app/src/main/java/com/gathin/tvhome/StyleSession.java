package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

/** Preview lives only in this process. Process death implicitly restores the saved appearance. */
final class StyleSession {
    static final HomeStyles.Preview PREVIEW = new HomeStyles.Preview();
    private static SharedPreferences prefs(Context context) { return ProfileStore.preferences(context); }
    static String preview() { return PREVIEW.active(SystemClock.elapsedRealtime()); }
    private static HomeStyles.Snapshot snapshot(SharedPreferences p) {
        return new HomeStyles.Snapshot(p.getString(HomeStyles.CHOICE, null),
                p.contains("columns") ? p.getInt("columns", 4) : null,
                p.contains(Config.ROWS_BELOW_DOCK_KEY) ? p.getInt(Config.ROWS_BELOW_DOCK_KEY, 0) : null,
                p.contains(Config.DOCK_OPACITY_KEY) ? p.getInt(Config.DOCK_OPACITY_KEY, 100) : null);
    }
    private static void put(SharedPreferences.Editor editor, String key, Integer value) {
        if (value == null) editor.remove(key); else editor.putInt(key, value);
    }
    private static void restore(SharedPreferences.Editor editor, HomeStyles.Snapshot before) {
        if (before.style == null) editor.remove(HomeStyles.CHOICE); else editor.putString(HomeStyles.CHOICE, before.style);
        put(editor, "columns", before.columns); put(editor, Config.ROWS_BELOW_DOCK_KEY, before.rows); put(editor, Config.DOCK_OPACITY_KEY, before.opacity);
    }
    static boolean hasUndo(Context context) { return HomeStyles.Snapshot.decode(prefs(context).getString(HomeStyles.UNDO, null)) != null; }
    static boolean keep(Context context) {
        String style = preview();
        if (style == null) return false;
        SharedPreferences p = prefs(context);
        HomeStyles.Snapshot before = snapshot(p);
        String oldUndo = p.getString(HomeStyles.UNDO, null);
        boolean saved = p.edit().putString(HomeStyles.CHOICE, style).putString(HomeStyles.UNDO, before.encode())
                .remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove(Config.DOCK_OPACITY_KEY)
                .putLong(Config.SETTINGS_REVISION_KEY, p.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).commit();
        if (!saved) {
            SharedPreferences.Editor repair = p.edit(); restore(repair, before);
            if (oldUndo == null) repair.remove(HomeStyles.UNDO); else repair.putString(HomeStyles.UNDO, oldUndo);
            repair.commit();
        }
        PREVIEW.cancel();
        return saved;
    }
    static boolean undo(Context context) {
        SharedPreferences p = prefs(context);
        HomeStyles.Snapshot before = HomeStyles.Snapshot.decode(p.getString(HomeStyles.UNDO, null));
        if (before == null) return false;
        SharedPreferences.Editor editor = p.edit(); restore(editor, before);
        return editor.remove(HomeStyles.UNDO).putLong(Config.SETTINGS_REVISION_KEY,
                p.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).commit();
    }
    static void choose(Activity activity) {
        String[] names = {activity.getString(R.string.style_minimal), activity.getString(R.string.style_grid),
                activity.getString(R.string.style_cinematic), activity.getString(R.string.style_easy), activity.getString(R.string.style_carousel)};
        new AlertDialog.Builder(activity).setTitle(R.string.home_styles)
                .setItems(names, (dialog, which) -> {
                    PREVIEW.begin(HomeStyles.IDS[which], SystemClock.elapsedRealtime());
                    activity.finish();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
    static AlertDialog prompt(Activity activity) {
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.style_keep_title)
                .setMessage(activity.getString(R.string.style_countdown, PREVIEW.seconds(SystemClock.elapsedRealtime())))
                .setNegativeButton(R.string.style_revert, (d, w) -> { PREVIEW.cancel(); activity.recreate(); })
                .setPositiveButton(R.string.style_keep, (d, w) -> {
                    if (!keep(activity)) Toast.makeText(activity, R.string.style_save_failed, Toast.LENGTH_LONG).show();
                    activity.recreate();
                }).create();
        Runnable timeout = () -> { PREVIEW.cancel(); dialog.dismiss(); activity.recreate(); };
        dialog.setOnCancelListener(d -> { PREVIEW.cancel(); activity.recreate(); });
        dialog.setOnDismissListener(d -> handler.removeCallbacks(timeout));
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
            handler.postDelayed(timeout, PREVIEW.remaining(SystemClock.elapsedRealtime()));
        });
        dialog.show();
        return dialog;
    }
    private StyleSession() { }
}
