package io.github.sumyabazar1.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicit, revocable local sharing. Coordinates never leave the weather app through this API. */
final class WeatherSharing {
    static final String AUTHORITY = "io.github.sumyabazar1.weather.snapshot";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static boolean busy;
    private static long retryAt;
    private WeatherSharing() {}

    static SharedPreferences cache(Context c) { return c.getSharedPreferences("snapshot", Context.MODE_PRIVATE); }
    static SharedPreferences trust(Context c) { return c.getSharedPreferences("sharing", Context.MODE_PRIVATE); }
    static String signer(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            Signature[] signatures = Build.VERSION.SDK_INT >= 28
                    ? pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.getApkContentsSigners()
                    : pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures;
            if (signatures == null || signatures.length != 1) return "";
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
            StringBuilder result = new StringBuilder();
            for (byte b : digest) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (Exception unavailable) { return ""; }
    }
    static boolean allowed(Context c, String pkg) {
        SharedPreferences p = trust(c);
        String expected = p.getString("signer", "");
        return pkg != null && SharingPolicy.allows(p.getString("package", ""), expected, pkg, signer(c, pkg));
    }
    static void save(Context c, Forecast.Report report, String city, long latitude, long longitude) {
        SharedPreferences place = c.getSharedPreferences("place", Context.MODE_PRIVATE);
        if (!city.equals(place.getString("name", "")) || latitude != place.getLong("lat", 0)
                || longitude != place.getLong("lon", 0)) return; // a city changed while the request was running
        cache(c).edit().putFloat("celsius", (float) report.tempC)
                .putString("city", city).putString("condition", c.getString(Forecast.sky(report.code, report.day)))
                .putLong("updated", System.currentTimeMillis()).apply();
    }
    static synchronized void refresh(Context context) {
        Context c = context.getApplicationContext();
        long now = System.currentTimeMillis();
        SharedPreferences place = c.getSharedPreferences("place", Context.MODE_PRIVATE);
        if (!place.contains("name") || !SharingPolicy.refreshDue(now, cache(c).getLong("updated", 0), retryAt, busy)) return;
        busy = true;
        retryAt = now + Forecast.RETRY_MS;
        String city = place.getString("name", "");
        long lat = place.getLong("lat", 0), lon = place.getLong("lon", 0);
        WORKER.execute(() -> {
            try { save(c, Forecast.fetch(Double.longBitsToDouble(lat), Double.longBitsToDouble(lon), 3), city, lat, lon); }
            catch (Exception unavailable) { /* Keep the dated cache. Never replace it with made-up weather. */ }
            finally { synchronized (WeatherSharing.class) { busy = false; } }
        });
    }
}
