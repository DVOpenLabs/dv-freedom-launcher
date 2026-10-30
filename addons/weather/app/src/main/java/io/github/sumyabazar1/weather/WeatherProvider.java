package io.github.sumyabazar1.weather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;

/** Cached snapshot only, pinned to the package AND certificate approved on the weather settings screen. */
public final class WeatherProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        if (!"/current".equals(uri.getPath()) || projection != null || selection != null || args != null || order != null)
            throw new IllegalArgumentException("Only the current weather snapshot is available");
        boolean authorized = false;
        String[] packages = getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packages != null) for (String pkg : packages) authorized |= WeatherSharing.allowed(getContext(), pkg);
        if (!authorized) throw new SecurityException("Approve this launcher in Weather settings first");
        MatrixCursor result = new MatrixCursor(new String[]{"schema", "celsius", "city", "condition", "updated"});
        SharedPreferences cache = WeatherSharing.cache(getContext());
        if (cache.contains("updated")) result.addRow(new Object[]{1, cache.getFloat("celsius", 0),
                cache.getString("city", ""), cache.getString("condition", ""), cache.getLong("updated", 0)});
        WeatherSharing.refresh(getContext()); // one bounded worker, rate limited; no provider-thread network I/O
        return result;
    }
    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.dvfreedom.weather"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
