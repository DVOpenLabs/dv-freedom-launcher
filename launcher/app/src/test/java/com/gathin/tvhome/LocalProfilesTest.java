package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.junit.Assert.*;

public class LocalProfilesTest {
    private Map<String, Object> profile(String id, Object columns, List<String> favorites, List<String> hidden) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id); row.put("name", id); row.put("columns", columns);
        row.put("favorites", favorites); row.put("hiddenApps", hidden); return row;
    }
    private Map<String, Object> document(List<Map<String, Object>> profiles) {
        Map<String, Object> apps = new HashMap<>();
        apps.put("video", Collections.singletonMap("android", "org.example.video"));
        apps.put("music", Collections.singletonMap("android", "org.example.music"));
        Map<String, Object> doc = new HashMap<>();
        doc.put("schemaVersion", 1); doc.put("apps", apps); doc.put("profiles", profiles); return doc;
    }
    private void rejected(Map<String, Object> document) {
        try { LocalProfiles.parse(document); fail("Invalid document accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    @Test public void profilesResolveIndependentOrderedFavoritesAndHiddenApps() {
        List<LocalProfiles.Profile> rows = LocalProfiles.parse(document(Arrays.asList(
                profile("adult", 6, Arrays.asList("music", "video"), Collections.emptyList()),
                profile("child", 4, Collections.singletonList("video"), Collections.singletonList("music")))));
        assertEquals(Arrays.asList("org.example.music", "org.example.video"), rows.get(0).favorites);
        assertEquals(Collections.singletonList("org.example.music"), rows.get(1).hidden);
        assertEquals(4, rows.get(1).columns);
    }
    @Test public void invalidIdsDuplicatesAndColumnsAreRejected() {
        for (String id : Arrays.asList("../other", "profile.one", "", "UPPER"))
            rejected(document(Collections.singletonList(profile(id, 4, Collections.singletonList("video"), Collections.emptyList()))));
        rejected(document(Arrays.asList(profile("same", 4, Collections.singletonList("video"), Collections.emptyList()),
                profile("same", 4, Collections.singletonList("music"), Collections.emptyList()))));
        for (Object columns : Arrays.asList(true, 4.0, 1, 9, "4"))
            rejected(document(Collections.singletonList(profile("adult", columns, Collections.singletonList("video"), Collections.emptyList()))));
    }
    @Test public void unknownEmptyDuplicateAndHiddenFavoritesAreRejected() {
        rejected(document(Collections.singletonList(profile("adult", 4, Collections.singletonList("missing"), Collections.emptyList()))));
        rejected(document(Collections.singletonList(profile("adult", 4, Collections.emptyList(), Collections.emptyList()))));
        rejected(document(Collections.singletonList(profile("adult", 4, Arrays.asList("video", "video"), Collections.emptyList()))));
        rejected(document(Collections.singletonList(profile("adult", 4, Collections.singletonList("video"), Collections.singletonList("video")))));
    }
    @Test public void missingAndroidMappingAndAliasesAreRejected() {
        Map<String, Object> doc = document(Collections.singletonList(profile("adult", 4, Arrays.asList("video", "music"), Collections.emptyList())));
        @SuppressWarnings("unchecked") Map<String, Object> apps = (Map<String, Object>) doc.get("apps");
        apps.put("music", Collections.singletonMap("webos", "example.music")); rejected(doc);
        apps.put("music", Collections.singletonMap("android", "org.example.video")); rejected(doc);
        doc.put("profiles", Collections.singletonList(profile("adult", 4, Collections.singletonList("video"), Collections.singletonList("music")))); rejected(doc);
    }
    @Test public void unknownFieldsAndVersionsAreNotSilentlyIgnored() {
        Map<String, Object> row = profile("adult", 4, Collections.singletonList("video"), Collections.emptyList());
        row.put("systemVolume", 50); rejected(document(Collections.singletonList(row)));
        Map<String, Object> doc = document(Collections.emptyList()); doc.put("schemaVersion", true); rejected(doc);
    }
    @Test public void profilePathsAndAppearanceKeysDoNotScopeDeviceControls() {
        assertEquals("columns", LocalProfiles.key("", "columns"));
        assertEquals("profile.adult.columns", LocalProfiles.key("adult", "columns"));
        assertEquals("settings_revision", LocalProfiles.key("adult", "settings_revision"));
        assertEquals(OpenOnStart.KEY, LocalProfiles.key("adult", OpenOnStart.KEY));
        assertEquals("order.txt", LocalProfiles.filename("", "order.txt"));
        assertNotEquals(LocalProfiles.filename("adult", "order.txt"), LocalProfiles.filename("child", "order.txt"));
        try { LocalProfiles.filename("../other", "order.txt"); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void boundedImportAcceptsExactLimitAndRejectsAnExtraByte() throws Exception {
        assertEquals(LocalProfiles.MAX_BYTES, ProfileStore.readBytes(new ByteArrayInputStream(new byte[LocalProfiles.MAX_BYTES])).length);
        try { ProfileStore.readBytes(new ByteArrayInputStream(new byte[LocalProfiles.MAX_BYTES + 1])); fail(); }
        catch (IOException expected) { }
    }
    @Test public void excessiveNestingIsRejectedBeforeAndroidJsonParsing() throws Exception {
        try { ProfileStore.parse("[[[[[[[[[0]]]]]]]]]".getBytes(java.nio.charset.StandardCharsets.UTF_8)); fail(); }
        catch (IOException expected) { }
    }
}
