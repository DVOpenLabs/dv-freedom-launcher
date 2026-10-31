package com.gathin.tvhome;
import android.content.Context;
import android.net.Uri;
import java.io.File;
/** A provider failure must not be indistinguishable from an already-absent theme file. */
final class ThemeDeletion {
    static int delete(Context c, Uri uri) {
        File directory = Config.dir(c);
        if (directory == null || !directory.isDirectory() || directory.list() == null)
            throw new IllegalStateException("Theme storage is unavailable");
        if (ThemeFiles.isCollection(uri.getPathSegments())) {
            File collection = new File(directory, ThemeFiles.WALLPAPERS);
            if (!collection.exists()) return 0;
            File[] files = collection.listFiles((d, name) -> ThemeFiles.collectionImage(name));
            if (files == null) throw new IllegalStateException("Theme collection is unreadable");
            int count = 0;
            for (File file : files) count += remove(file);
            return count;
        }
        String path = ThemeFiles.path(uri.getPathSegments());
        if (path == null) throw new IllegalArgumentException("Unsupported theme path");
        int count = remove(new File(directory, path));
        if (Config.FILE_NAME.equals(path)) OverlayRecovery.reset(c);
        return count;
    }
    static int remove(File file) {
        File parent = file.getParentFile();
        if (parent != null && parent.exists() && parent.list() == null)
            throw new IllegalStateException("Theme directory is unreadable");
        if (!file.exists()) return 0;
        if (!file.delete()) throw new IllegalStateException("Theme file could not be removed");
        return 1;
    }
    private ThemeDeletion() {}
}
