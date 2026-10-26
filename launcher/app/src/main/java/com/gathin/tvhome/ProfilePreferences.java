package com.gathin.tvhome;

import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Scoped appearance and global controls share one atomic preferences editor. */
final class ProfilePreferences implements SharedPreferences {
    private final SharedPreferences source;
    private final String id;
    private final Map<OnSharedPreferenceChangeListener, OnSharedPreferenceChangeListener> listeners = new HashMap<>();
    ProfilePreferences(SharedPreferences source, String id) {
        if (!id.isEmpty() && !LocalProfiles.validId(id)) throw new IllegalArgumentException("Invalid profile ID");
        this.source = source; this.id = id;
    }
    private String key(String key) { return LocalProfiles.key(id, key); }
    @Override public Map<String, ?> getAll() {
        Map<String, Object> values = new HashMap<>();
        Map<String, ?> all = source.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (!entry.getKey().startsWith("profile.") && (!LocalProfiles.SCOPED.contains(entry.getKey()) || id.isEmpty())) values.put(entry.getKey(), entry.getValue());
        }
        for (String field : LocalProfiles.SCOPED) if (all.containsKey(key(field))) values.put(field, all.get(key(field)));
        return values;
    }
    @Override public String getString(String k, String d) { return source.getString(key(k), d); }
    @Override public Set<String> getStringSet(String k, Set<String> d) { return source.getStringSet(key(k), d); }
    @Override public int getInt(String k, int d) { return source.getInt(key(k), d); }
    @Override public long getLong(String k, long d) { return source.getLong(key(k), d); }
    @Override public float getFloat(String k, float d) { return source.getFloat(key(k), d); }
    @Override public boolean getBoolean(String k, boolean d) { return source.getBoolean(key(k), d); }
    @Override public boolean contains(String k) { return source.contains(key(k)); }
    @Override public Editor edit() { return new ProfileEditor(source.edit()); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        unregisterOnSharedPreferenceChangeListener(listener);
        OnSharedPreferenceChangeListener wrapped = (prefs, changed) -> {
            if (changed == null) { listener.onSharedPreferenceChanged(this, null); return; }
            for (String field : LocalProfiles.SCOPED) if (key(field).equals(changed)) { listener.onSharedPreferenceChanged(this, field); return; }
            if (!changed.startsWith("profile.") && !LocalProfiles.SCOPED.contains(changed)) listener.onSharedPreferenceChanged(this, changed);
        };
        listeners.put(listener, wrapped); source.registerOnSharedPreferenceChangeListener(wrapped);
    }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        OnSharedPreferenceChangeListener wrapped = listeners.remove(listener);
        if (wrapped != null) source.unregisterOnSharedPreferenceChangeListener(wrapped);
    }
    private final class ProfileEditor implements Editor {
        private final Editor editor;
        ProfileEditor(Editor editor) { this.editor = editor; }
        @Override public Editor putString(String k, String v) { editor.putString(key(k), v); return this; }
        @Override public Editor putStringSet(String k, Set<String> v) { editor.putStringSet(key(k), v); return this; }
        @Override public Editor putInt(String k, int v) { editor.putInt(key(k), v); return this; }
        @Override public Editor putLong(String k, long v) { editor.putLong(key(k), v); return this; }
        @Override public Editor putFloat(String k, float v) { editor.putFloat(key(k), v); return this; }
        @Override public Editor putBoolean(String k, boolean v) { editor.putBoolean(key(k), v); return this; }
        @Override public Editor remove(String k) { editor.remove(key(k)); return this; }
        // Clearing an appearance profile must never erase global controls or another person.
        @Override public Editor clear() { for (String field : LocalProfiles.SCOPED) editor.remove(key(field)); return this; }
        @Override public boolean commit() { return editor.commit(); }
        @Override public void apply() { editor.apply(); }
    }
}
