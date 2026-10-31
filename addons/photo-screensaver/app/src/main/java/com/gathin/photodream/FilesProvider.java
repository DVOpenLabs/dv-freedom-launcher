package com.gathin.photodream;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lets sync.sh write the pictures into the app's own files dir, so the app owns them. Only the names
 * {@link Media} allows, and only for callers holding WRITE_SECURE_SETTINGS (the adb shell).
 *
 *   content write  --uri content://com.gathin.photodream.files/pictures/sea.jpg < /data/local/tmp/sea.jpg
 *   content write  --uri content://com.gathin.photodream.files/seconds.txt < /data/local/tmp/seconds.txt
 *   content delete --uri content://com.gathin.photodream.files/pictures/sea.jpg
 *   content delete --uri content://com.gathin.photodream.files/pictures      (every picture and clip)
 */
public class FilesProvider extends ContentProvider {

    private File dir() {
        return getContext() == null ? null : getContext().getExternalFilesDir(null);
    }

    private File file(Uri uri) throws FileNotFoundException {
        String path = Media.path(uri.getPathSegments());
        File dir = dir();
        if (dir == null || path == null) throw new FileNotFoundException(String.valueOf(uri));
        File f = new File(dir, path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = file(uri);
        if (mode.contains("w")) {
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File dir = dir();
        if (dir == null || !dir.isDirectory() || dir.list() == null)
            throw new IllegalStateException("Photo storage is unavailable");
        if (Media.isFolder(uri.getPathSegments())) {
            File folder = new File(dir, Media.PICTURES);
            if (!folder.exists()) return 0;
            File[] files = folder.listFiles((d, name) -> Media.file(name));
            if (files == null) throw new IllegalStateException("Photo folder is unreadable");
            int count = 0;
            for (File f : files) count += remove(f);
            return count;
        }
        String path = Media.path(uri.getPathSegments());
        if (path == null) throw new IllegalArgumentException("Unsupported photo path");
        return remove(new File(dir, path));
    }

    private static int remove(File file) {
        File parent = file.getParentFile();
        if (parent != null && parent.exists() && parent.list() == null)
            throw new IllegalStateException("Photo folder is unreadable");
        if (!file.exists()) return 0;
        if (!file.delete()) throw new IllegalStateException("Photo file could not be removed");
        return 1;
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
