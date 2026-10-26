package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The portable v1 profile contract. No Android accounts, permissions or device controls. */
final class LocalProfiles {
    static final int MAX_BYTES = 64 * 1024;
    static final Set<String> SCOPED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "columns", "rows_below_dock", "dock_opacity", "animations", "advanced_effects",
            "wallpaper_choice", "home_style_choice", "home_style_undo", "theme_signature", "theme_pack_id", "live_wallpaper")));

    static final class Profile {
        final String id, name;
        final List<String> favorites, hidden;
        final int columns;
        Profile(String id, String name, List<String> favorites, List<String> hidden, int columns) {
            this.id = id; this.name = name;
            this.favorites = Collections.unmodifiableList(new ArrayList<>(favorites));
            this.hidden = Collections.unmodifiableList(new ArrayList<>(hidden));
            this.columns = columns;
        }
    }

    static boolean validId(String value) { return value != null && value.matches("[a-z][a-z0-9_-]{0,63}"); }
    static String key(String id, String key) {
        if (id.isEmpty() || !SCOPED.contains(key)) return key;
        if (!validId(id)) throw new IllegalArgumentException("Invalid profile ID");
        return "profile." + id + "." + key;
    }
    static String filename(String id, String file) {
        if (!id.isEmpty() && !validId(id)) throw new IllegalArgumentException("Invalid profile ID");
        return id.isEmpty() ? file : "profile-" + id + "-" + file;
    }

    static List<Profile> parse(Map<String, Object> document) {
        fields(document, "schemaVersion", "apps", "profiles");
        check(integer(document.get("schemaVersion")) == 1, "Unsupported profile version");
        Map<String, Object> apps = object(document.get("apps"));
        check(!apps.isEmpty(), "Empty app catalog");
        Map<String, String> android = new LinkedHashMap<>();
        for (Map.Entry<String, Object> app : apps.entrySet()) {
            check(validId(app.getKey()), "Invalid logical app ID");
            Map<String, Object> platforms = object(app.getValue());
            check(!platforms.isEmpty() && new HashSet<>(Arrays.asList("android", "webos")).containsAll(platforms.keySet()), "Invalid platform mapping");
            for (Object id : platforms.values()) check(id instanceof String && ((String) id).matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,254}"), "Invalid platform app ID");
            if (platforms.containsKey("android")) android.put(app.getKey(), (String) platforms.get("android"));
        }
        Object raw = document.get("profiles");
        check(raw instanceof List, "Invalid profile list");
        List<?> rows = (List<?>) raw;
        check(!rows.isEmpty() && rows.size() <= 16, "Expected 1 to 16 profiles");
        List<Profile> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Object row : rows) {
            Map<String, Object> profile = object(row);
            fields(profile, "id", "name", "favorites", "hiddenApps", "columns");
            String id = string(profile.get("id"));
            check(validId(id) && ids.add(id), "Invalid or duplicate profile ID");
            String name = string(profile.get("name")).trim();
            check(!name.isEmpty() && name.length() <= 64, "Invalid profile name");
            int columns = integer(profile.get("columns"));
            check(columns >= 2 && columns <= 8, "Invalid column count");
            List<String> favorites = mapped(profile.get("favorites"), apps, android);
            List<String> hidden = mapped(profile.get("hiddenApps"), apps, android);
            check(!favorites.isEmpty(), "A profile needs a favorite");
            check(Collections.disjoint(favorites, hidden), "A favorite is also hidden");
            result.add(new Profile(id, name, favorites, hidden, columns));
        }
        return Collections.unmodifiableList(result);
    }
    private static List<String> mapped(Object value, Map<String, Object> catalog, Map<String, String> android) {
        check(value instanceof List && ((List<?>) value).size() <= 100, "Invalid app list");
        List<String> result = new ArrayList<>();
        for (Object logical : (List<?>) value) {
            check(logical instanceof String && catalog.containsKey(logical), "Unknown logical app");
            String id = android.get(logical);
            check(id != null, "Missing Android app mapping");
            check(!result.contains(id), "Duplicate mapped app");
            result.add(id);
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        check(value instanceof Map, "Expected an object");
        return (Map<String, Object>) value;
    }
    private static String string(Object value) { check(value instanceof String, "Expected text"); return (String) value; }
    private static int integer(Object value) {
        check(value instanceof Integer || value instanceof Long, "Expected an integer");
        long number = ((Number) value).longValue();
        check(number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE, "Integer out of range");
        return (int) number;
    }
    private static void fields(Map<String, Object> value, String... keys) {
        check(value.keySet().equals(new HashSet<>(Arrays.asList(keys))), "Unknown or missing profile fields");
    }
    private static void check(boolean valid, String error) { if (!valid) throw new IllegalArgumentException(error); }
    private LocalProfiles() { }
}
