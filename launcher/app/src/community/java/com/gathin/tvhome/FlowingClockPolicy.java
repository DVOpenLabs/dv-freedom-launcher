package com.gathin.tvhome;

/** Pure timing and clock-format policy, shared by the drawing code and boundary tests. */
final class FlowingClockPolicy {
    static final long TRANSITION_MS = 360;
    private FlowingClockPolicy() { }
    static float progress(long elapsed) {
        if (elapsed <= 0) return 0f;
        if (elapsed >= TRANSITION_MS) return 1f;
        float t = (float) elapsed / TRANSITION_MS;
        return t * t * (3f - 2f * t);
    }
    static int hour(int hour24, boolean twentyFour) {
        if (twentyFour) return hour24;
        int hour = hour24 % 12;
        return hour == 0 ? 12 : hour;
    }
    static boolean motionAllowed(boolean animations, boolean reduced, boolean systemAnimations,
            boolean powerSaving, boolean lowRam) {
        return animations && !reduced && systemAnimations && !powerSaving && !lowRam;
    }
}
