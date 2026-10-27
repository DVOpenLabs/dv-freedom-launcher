package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

/** Native optional appearance rows; choices belong to the active local profile. */
final class AppearanceSettings {
    static void attach(Activity activity, Config config) {
        if (!config.appearanceControls) return;
        TextView focus = activity.findViewById(R.id.focusOutlineChoice);
        TextView spacing = activity.findViewById(R.id.tileSpacingChoice);
        focus.setVisibility(View.VISIBLE); spacing.setVisibility(View.VISIBLE);
        SharedPreferences prefs = ProfileStore.preferences(activity);
        focus.setText(activity.getString(R.string.focus_outline_state,
                !prefs.contains(TileSpacing.FOCUS) ? activity.getString(R.string.appearance_follow)
                : activity.getString(Config.GLASS_DOUBLE.equals(prefs.getString(TileSpacing.FOCUS, ""))
                ? R.string.focus_outline_strong : R.string.focus_outline_standard)));
        spacing.setText(activity.getString(R.string.tile_spacing_state,
                !prefs.contains(TileSpacing.CHOICE) ? activity.getString(R.string.appearance_follow)
                : activity.getString(R.string.tile_spacing_dp, prefs.getInt(TileSpacing.CHOICE, 0))));
        focus.setOnClickListener(v -> {
            String[] labels = {activity.getString(R.string.appearance_follow), activity.getString(R.string.focus_outline_standard), activity.getString(R.string.focus_outline_strong)};
            String[] values = {"", Config.GLASS_RIM, Config.GLASS_DOUBLE};
            String current = prefs.getString(TileSpacing.FOCUS, "");
            int selected = Config.GLASS_DOUBLE.equals(current) ? 2 : Config.GLASS_RIM.equals(current) ? 1 : 0;
            choose(activity, R.string.focus_outline, labels, selected, which -> {
                SharedPreferences.Editor edit = prefs.edit();
                if (which == 0) edit.remove(TileSpacing.FOCUS); else edit.putString(TileSpacing.FOCUS, values[which]);
                save(activity, prefs, edit);
            });
        });
        spacing.setOnClickListener(v -> {
            int[] values = {-1, 0, 8, 16, 24}; String[] labels = new String[values.length]; int selected = 0;
            for (int i = 0; i < values.length; i++) {
                labels[i] = i == 0 ? activity.getString(R.string.appearance_follow) : activity.getString(R.string.tile_spacing_dp, values[i]);
                if (prefs.getInt(TileSpacing.CHOICE, -1) == values[i]) selected = i;
            }
            choose(activity, R.string.tile_spacing, labels, selected, which -> {
                SharedPreferences.Editor edit = prefs.edit();
                if (which == 0) edit.remove(TileSpacing.CHOICE); else edit.putInt(TileSpacing.CHOICE, values[which]);
                save(activity, prefs, edit);
            });
        });
    }
    private static void save(Activity activity, SharedPreferences prefs, SharedPreferences.Editor edit) {
        ProductFeatures.beforeChange(activity,"Change focus or spacing");
        edit.putLong(Config.SETTINGS_REVISION_KEY, prefs.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
        activity.setResult(Activity.RESULT_OK); activity.finish();
    }
    interface Selection { void choose(int which); }
    static void choose(Activity activity, int title, String[] labels, int selected, Selection selection) {
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(title)
                .setSingleChoiceItems(labels, selected, (d, which) -> { d.dismiss(); selection.choose(which); })
                .setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(d -> {
            dialog.getListView().setTextDirection(View.TEXT_DIRECTION_LOCALE);
            dialog.getListView().setLayoutDirection(android.text.TextUtils.getLayoutDirectionFromLocale(
                    activity.getResources().getConfiguration().getLocales().get(0)));
            dialog.getListView().requestFocus(); dialog.getListView().setSelection(Math.max(0, selected));
        });
        dialog.show();
    }
    private AppearanceSettings() { }
}
