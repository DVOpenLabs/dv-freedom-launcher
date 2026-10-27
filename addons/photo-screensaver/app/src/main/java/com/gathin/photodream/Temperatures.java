package com.gathin.photodream;

/** Convert the unrounded reading independently for each displayed unit. */
final class Temperatures {
    static String text(double celsius, boolean bothUnits) {
        if (!Double.isFinite(celsius)) throw new IllegalArgumentException("Invalid temperature");
        long c = Math.round(celsius);
        return bothUnits ? Math.round(celsius * 9 / 5 + 32) + "°F / " + c + "°C" : c + "°";
    }
    private Temperatures() { }
}
