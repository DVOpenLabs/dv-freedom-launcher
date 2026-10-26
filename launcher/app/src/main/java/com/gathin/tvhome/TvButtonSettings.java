package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Restricted adaptation of lionlev21's MIT picker; preferences are global and no helper is auto-enabled. */
final class TvButtonSettings {
    static void attach(Activity activity, Config config) {
        if (!config.tvButtonPicker) return;
        TextView row = activity.findViewById(R.id.tvButtonChoice);
        row.setVisibility(View.VISIBLE); activity.findViewById(R.id.tvButtonHelper).setVisibility(View.VISIBLE);
        row.setOnClickListener(v -> show(activity));
    }
    private static void show(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        List<String> labels = new ArrayList<>(), values = new ArrayList<>();
        labels.add(activity.getString(R.string.tv_button_follow)); values.add(TvButtonChoice.FOLLOW);
        labels.add(activity.getString(R.string.tv_button_inputs)); values.add(TvButtonChoice.INPUTS);
        labels.add(activity.getString(R.string.live_tv)); values.add(TvButtonChoice.LIVE);
        Map<String, String> apps = new HashMap<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo info : activity.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                if (info.activityInfo != null && info.activityInfo.enabled && info.activityInfo.applicationInfo.enabled
                        && !activity.getPackageName().equals(info.activityInfo.packageName)) {
                    String pkg = info.activityInfo.packageName;
                    if (TvButtonChoice.appPackage("app:" + pkg) != null) apps.put(pkg, info.loadLabel(activity.getPackageManager()).toString());
                }
            }
        }
        List<String> packages = new ArrayList<>(apps.keySet());
        Collator collator = Collator.getInstance(activity.getResources().getConfiguration().getLocales().get(0));
        packages.sort((a,b) -> {int order = collator.compare(apps.get(a), apps.get(b)); return order != 0 ? order : a.compareTo(b);});
        for (String pkg : packages) {labels.add(apps.get(pkg)); values.add("app:" + pkg);}
        String current = TvButtonChoice.normalize(prefs.getString(TvButtonChoice.KEY, TvButtonChoice.FOLLOW));
        if (!values.contains(current)) {labels.add(activity.getString(R.string.tv_button_missing)); values.add(current);}
        AppearanceSettings.choose(activity, R.string.tv_button, labels.toArray(new String[0]), values.indexOf(current), which -> {
            if (TvButtonChoice.save(prefs, values.get(which))) {activity.setResult(Activity.RESULT_OK); activity.finish();}
            else Toast.makeText(activity, R.string.tv_button_save_failed, Toast.LENGTH_LONG).show();
        });
    }
    static void refreshStatus(Activity activity) {
        TextView text = activity.findViewById(R.id.tvButtonHelper);
        if (text == null || text.getVisibility() != View.VISIBLE) return;
        AccessibilityManager manager = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        boolean active = false;
        if (manager != null) for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            ComponentName component = ComponentName.unflattenFromString(info.getId());
            if (component != null && component.getPackageName().equals(activity.getPackageName())
                    && (component.getClassName().equals(InputKeyService.class.getName()) || component.getClassName().equals(HomeKeyService.class.getName()))
                    && (info.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0) active = true;
        }
        text.setText(active ? R.string.tv_button_helper_active : R.string.tv_button_helper_needed);
        text.setOnClickListener(v -> {
            try {activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
            catch (android.content.ActivityNotFoundException | SecurityException unavailable) {
                Toast.makeText(activity, R.string.not_available, Toast.LENGTH_LONG).show();
            }
        });
    }
    private TvButtonSettings() { }
}
