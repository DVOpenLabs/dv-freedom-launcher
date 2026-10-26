package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class TvButtonChoiceTest {
    @Test public void onlyRestrictedDestinationsAreAccepted() {
        for (String value : new String[]{"follow", "inputs", "live", "app:org.example.tv", "app:com.example_app.tv"})
            assertTrue(value, TvButtonChoice.valid(value));
        assertEquals("org.example.tv", TvButtonChoice.appPackage("app:org.example.tv"));
    }
    @Test public void arbitraryActionsComponentsAndBadPackagesCannotBecomeOverrides() {
        for (String value : new String[]{"", "settings", "home", "app:org.example/.Activity", "app:org.1bad", "app:org..tv",
                "app:org.example tv", "app:org.example:settings", "app:single", "app:https://example.com", "app:org." + "x".repeat(256)}) {
            assertFalse(value, TvButtonChoice.valid(value)); assertEquals(TvButtonChoice.FOLLOW, TvButtonChoice.normalize(value));
        }
        assertEquals(TvButtonChoice.FOLLOW, TvButtonChoice.normalize(null));
    }
}
