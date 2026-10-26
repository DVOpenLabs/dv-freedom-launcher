package com.gathin.tvhome;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adapted from lionlev21's press gate: per-device, shared-helper deduplication and changed-choice cancel. */
final class TvButtonPress {
    static final class Result {
        final boolean consume; final String launch;
        Result(boolean consume, String launch) { this.consume = consume; this.launch = launch; }
    }
    private static final class Press {
        long downAt = -1, upAt = -1; boolean armed, consumed, cancelled;
        String choice = TvButtonChoice.FOLLOW;
    }
    private final Map<String, Press> presses = new LinkedHashMap<String, Press>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Press> eldest) { return size() > 32; }
    };
    Result update(String button, boolean down, int repeats, boolean cancelled, long time, String value) {
        String choice = TvButtonChoice.normalize(value);
        boolean override = !TvButtonChoice.FOLLOW.equals(choice);
        Press press = presses.computeIfAbsent(button, key -> new Press());
        if (down) {
            if (repeats == 0 && time > press.downAt && time >= press.upAt) {
                press.downAt = time; press.armed = true; press.choice = choice;
                press.consumed = override; press.cancelled = cancelled;
            }
            return new Result(press.consumed || override, null);
        }
        boolean consume = press.consumed || override;
        if (time <= press.upAt || time < press.downAt) return new Result(consume, null);
        press.upAt = time;
        String launch = press.armed && time >= press.downAt && !cancelled && !press.cancelled
                && override && choice.equals(press.choice) ? choice : null;
        press.armed = false;
        return new Result(consume, launch);
    }
}
