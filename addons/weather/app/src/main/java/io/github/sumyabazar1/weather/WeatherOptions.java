package io.github.sumyabazar1.weather;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.List;

/** Runtime options retain the build defaults until the owner explicitly saves a choice. */
final class WeatherOptions {
    static void show(WeatherActivity a) {
        String shared = WeatherSharing.trust(a).getString("package", "");
        new AlertDialog.Builder(a).setTitle("Weather settings")
                .setItems(new String[]{"Temperature units", "Forecast and details", "Share with a launcher",
                        shared.isEmpty() ? "Sharing is off" : "Stop sharing with launcher"}, (d, which) -> {
                    if (which == 0) units(a);
                    else if (which == 1) details(a);
                    else if (which == 2) clients(a);
                    else if (!shared.isEmpty()) WeatherSharing.trust(a).edit().clear().apply();
                }).setNegativeButton("Close", null).show();
    }
    private static void units(WeatherActivity a) {
        SharedPreferences p = a.getSharedPreferences("options", 0);
        String fallback = a.getString(R.string.temperature_unit);
        int selected = p.getBoolean("both", a.getResources().getBoolean(R.bool.show_both_units)) ? 2
                : "f".equalsIgnoreCase(p.getString("unit", fallback)) ? 1 : 0;
        final int[] draft = {selected};
        new AlertDialog.Builder(a).setTitle("Temperature units")
                .setSingleChoiceItems(new String[]{"Celsius · °C", "Fahrenheit · °F", "Both · °F / °C"}, selected, (d, w) -> draft[0] = w)
                .setNegativeButton("Cancel", null).setPositiveButton("Apply", (d, w) -> {
                    p.edit().putBoolean("both", draft[0] == 2).putString("unit", draft[0] == 0 ? "c" : "f").apply();
                    a.reloadOptions();
                }).show();
    }
    private static void details(WeatherActivity a) {
        SharedPreferences p = a.getSharedPreferences("options", 0);
        boolean[] draft = {p.getInt("days", a.getResources().getInteger(R.integer.forecast_days)) == 7,
                p.getBoolean("humidity", a.getResources().getBoolean(R.bool.show_humidity)),
                p.getBoolean("wind", a.getResources().getBoolean(R.bool.show_wind))};
        new AlertDialog.Builder(a).setTitle("Forecast and details")
                .setMultiChoiceItems(new String[]{"Seven days (off: three)", "Humidity", "Wind speed"}, draft, (d, w, value) -> draft[w] = value)
                .setNegativeButton("Cancel", null).setPositiveButton("Apply", (d, w) -> {
                    p.edit().putInt("days", draft[0] ? 7 : 3).putBoolean("humidity", draft[1]).putBoolean("wind", draft[2]).apply();
                    a.reloadOptions();
                }).show();
    }
    private static void clients(WeatherActivity a) {
        List<String> packages = new ArrayList<>(), labels = new ArrayList<>();
        PackageManager pm = a.getPackageManager();
        for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.GET_META_DATA)) {
            if (r.activityInfo == null || r.activityInfo.applicationInfo.metaData == null
                    || !r.activityInfo.applicationInfo.metaData.getBoolean("com.dvopenlabs.weather.CLIENT", false)) continue;
            String pkg = r.activityInfo.packageName;
            if (!packages.contains(pkg) && !WeatherSharing.signer(a, pkg).isEmpty()) {
                packages.add(pkg); labels.add(r.loadLabel(pm) + "\n" + pkg);
            }
        }
        if (packages.isEmpty()) {
            new AlertDialog.Builder(a).setTitle("No compatible launcher found")
                    .setMessage("Install a DV Freedom build with weather display support first. Sharing remains off.")
                    .setPositiveButton("OK", null).show(); return;
        }
        new AlertDialog.Builder(a).setTitle("Choose the launcher you trust")
                .setItems(labels.toArray(new String[0]), (d, w) -> {
                    String pkg = packages.get(w), signer = WeatherSharing.signer(a, pkg);
                    new AlertDialog.Builder(a).setTitle("Share weather with this launcher?")
                            .setMessage(labels.get(w) + "\n\nShares city name, conditions, temperature and update time locally. Coordinates stay here. The launcher can request a refresh while visible.\n\nCertificate SHA-256:\n" + signer)
                            .setNegativeButton("Cancel", null).setPositiveButton("Allow sharing", (dialog, button) -> {
                                if (!signer.isEmpty()) WeatherSharing.trust(a).edit().putString("package", pkg).putString("signer", signer).apply();
                            }).show();
                }).setNegativeButton("Cancel", null).show();
    }
}
