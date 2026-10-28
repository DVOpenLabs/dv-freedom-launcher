package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Optional process-local routing, refreshed by Config.load; selection is committed atomically. */
final class ProfileStore {
    static final String ACTIVE = "product_active_profile";
    private static boolean enabled;
    private static LocalProfiles.Profile active;
    static LocalProfiles.Profile configure(Context ctx, boolean on) {
        enabled = on || global(ctx).getBoolean(ProfileSetup.ENABLED,false); active = null;
        if (enabled) {
            String id = global(ctx).getString(ACTIVE, "");
            if (!id.isEmpty()) for (LocalProfiles.Profile row : read(ctx)) if (row.id.equals(id)) { active = row; break; }
        }
        return active;
    }
    static boolean enabled() { return enabled; }
    static String id() { return active == null ? "" : active.id; }
    private static SharedPreferences global(Context ctx) { return ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE); }
    static SharedPreferences preferences(Context ctx) { return active == null ? global(ctx) : new ProfilePreferences(global(ctx), id()); }
    static File file(Context ctx, String name) { return new File(ctx.getFilesDir(), LocalProfiles.filename(id(), name)); }
    private static AtomicFile manifest(Context ctx) { return new AtomicFile(new File(ctx.getFilesDir(), "product-profiles.json")); }
    static List<LocalProfiles.Profile> read(Context ctx) {
        List<LocalProfiles.Profile> rows=new ArrayList<>();
        try (InputStream in = manifest(ctx).openRead()) { rows.addAll(parse(readBytes(in))); }
        catch (Exception e) { /* No imported library is normal for on-TV-only profiles. */ }
        java.util.Set<String> ids=new java.util.HashSet<>();for(LocalProfiles.Profile row:rows)ids.add(row.id);
        for(LocalProfiles.Profile row:ProfileSetup.read(ctx))if(ids.add(row.id)&&rows.size()<16)rows.add(row);
        return rows;
    }
    static boolean select(Context ctx, String id) {
        if (!enabled) return false;
        boolean found = id.isEmpty();
        for (LocalProfiles.Profile row : read(ctx)) if (row.id.equals(id)) found = true;
        if (!found) return false;
        boolean saved = saveSelection(global(ctx), id);
        if (saved) {StyleSession.PREVIEW.cancel();PlacementSession.PREVIEW.cancel();}
        return saved;
    }
    static boolean saveSelection(SharedPreferences p, String id) {
        if (!id.isEmpty() && !LocalProfiles.validId(id)) return false;
        String old = p.getString(ACTIVE, "");
        long revision = p.getLong(Config.SETTINGS_REVISION_KEY, 0);
        if (p.edit().putString(ACTIVE, id).putLong(Config.SETTINGS_REVISION_KEY, revision + 1).commit()) return true;
        // Android updates its in-memory map even when commit fails. Restore that view too.
        p.edit().putString(ACTIVE, old).putLong(Config.SETTINGS_REVISION_KEY, revision).commit();
        return false;
    }
    static byte[] readBytes(InputStream in) throws IOException {
        if (in == null) throw new IOException("No profile file");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[4096]; int n;
        while ((n = in.read(bytes)) != -1) {
            if (out.size() + n > LocalProfiles.MAX_BYTES) throw new IOException("Profile file is too large");
            out.write(bytes, 0, n);
        }
        return out.toByteArray();
    }
    static List<LocalProfiles.Profile> parse(byte[] bytes) throws Exception {
        if (bytes.length > LocalProfiles.MAX_BYTES) throw new IOException("Profile file is too large");
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.startsWith("\ufeff")) text = text.substring(1);
        // Bound nesting before asking Android's recursive JSON parser to read external input.
        int depth = 0; boolean quoted = false, escaped = false;
        for (char c : text.toCharArray()) {
            if (quoted) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; }
            else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') { if (++depth > 8) throw new IOException("Profile nesting is too deep"); }
            else if (c == '}' || c == ']') depth--;
        }
        return LocalProfiles.parse(object(new JSONObject(text)));
    }
    static Map<String, Object> object(JSONObject json) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) { String key = keys.next(); map.put(key, value(json.get(key))); }
        return map;
    }
    private static Object value(Object value) throws Exception {
        if (value instanceof JSONObject) return object((JSONObject) value);
        if (value instanceof JSONArray) {
            List<Object> list = new ArrayList<>(); JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) list.add(value(array.get(i)));
            return list;
        }
        return value;
    }
    static void install(Context ctx, byte[] bytes) throws Exception {
        if (!enabled || !id().isEmpty()) throw new IOException("Switch to Current layout before importing");
        List<LocalProfiles.Profile> incoming=parse(bytes);
        for(LocalProfiles.Profile local:ProfileSetup.read(ctx))for(LocalProfiles.Profile row:incoming)if(local.id.equals(row.id))throw new IOException("Imported ID conflicts with a created profile");
        if(incoming.size()+ProfileSetup.read(ctx).size()>16)throw new IOException("Maximum 16 profiles");
        AtomicFile file = manifest(ctx); FileOutputStream out = null;
        try { out = file.startWrite(); out.write(bytes); file.finishWrite(out); }
        catch (IOException e) { if (out != null) file.failWrite(out); throw e; }
    }
    private ProfileStore() { }
}
