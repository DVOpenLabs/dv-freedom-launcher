package io.github.sumyabazar1.weather;

import org.junit.Test;
import static org.junit.Assert.*;

public class SharingPolicyTest {
    final String signer = "a".repeat(64), other = "b".repeat(64);
    @Test public void approvedPackageAndCurrentCertificateAreBothRequired() {
        assertTrue(SharingPolicy.allows("example.launcher", signer, "example.launcher", signer));
        assertFalse(SharingPolicy.allows("example.launcher", signer, "example.copy", signer));
        assertFalse(SharingPolicy.allows("example.launcher", signer, "example.launcher", other));
        assertFalse(SharingPolicy.allows("", "", "example.launcher", ""));
        assertFalse(SharingPolicy.allows("example.launcher", "*", "example.launcher", "*"));
    }
    @Test public void pollingCannotTurnIntoUnboundedRefreshes() {
        long now = 2_000_000_000_000L;
        assertFalse(SharingPolicy.refreshDue(now, now - 1000, 0, false));
        assertFalse(SharingPolicy.refreshDue(now, 0, now + 1, false));
        assertFalse(SharingPolicy.refreshDue(now, 0, 0, true));
        assertTrue(SharingPolicy.refreshDue(now, now - Forecast.REFRESH_MS, 0, false));
        assertTrue(SharingPolicy.refreshDue(now, now + 60_000, 0, false));
    }
}
