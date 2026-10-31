package com.gathin.tvhome;

/** A bounded early-storage fallback is not an intentional theme reset. */
final class OverlayReadPolicy {
    static final long EARLY_MS = 5 * 60_000L, RETRY_MS = 5_000L, WATCH_MS = 3 * 60_000L;
    static boolean keepCopy(boolean validExternal, boolean copyExists, long uptime) {
        return !validExternal && copyExists && uptime >= 0 && uptime < EARLY_MS;
    }
    static boolean retry(boolean visible, boolean closed, long now, long deadline) {
        return visible && !closed && now >= 0 && now < deadline && now < EARLY_MS;
    }
    private OverlayReadPolicy() {}
}
