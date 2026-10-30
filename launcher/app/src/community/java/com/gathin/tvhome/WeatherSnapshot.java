package com.gathin.tvhome;

import java.util.Arrays;

/** Bounded display contract shared by Home and the screensaver. No network or Android dependency. */
final class WeatherSnapshot {
    static final long MAX_AGE = 6 * 60 * 60_000L;
    final double celsius;
    final String city, condition;
    final long updated;
    WeatherSnapshot(double celsius, String city, String condition, long updated) {
        if (!Double.isFinite(celsius) || celsius < -100 || celsius > 70 || !text(city, 80) || !text(condition, 80) || updated <= 0)
            throw new IllegalArgumentException("Invalid weather snapshot");
        this.celsius = celsius; this.city = city; this.condition = condition; this.updated = updated;
    }
    private static boolean text(String value, int max) {
        if (value == null || value.trim().isEmpty() || value.length() > max) return false;
        for (int i = 0; i < value.length(); i++) if (Character.isISOControl(value.charAt(i))) return false;
        return true;
    }
    static boolean unit(String value) { return Arrays.asList("off", "both", "c", "f").contains(value); }
    boolean fresh(long now) { return updated <= now + 60_000 && now - updated <= MAX_AGE; }
    String label(String units, long now) {
        if (!unit(units) || "off".equals(units) || !fresh(now)) return "";
        String c = Math.round(celsius) + "°C", f = Math.round(celsius * 9 / 5 + 32) + "°F";
        return ("both".equals(units) ? f + " / " + c : "c".equals(units) ? c : f) + " · " + condition
                + (now - updated >= 60 * 60_000 ? " · Updated earlier" : "");
    }
}
