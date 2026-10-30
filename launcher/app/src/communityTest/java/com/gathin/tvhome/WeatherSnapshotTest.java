package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherSnapshotTest {
    private static final long NOW = 1_800_000_000_000L;
    @Test public void bothUnitsUseOneReadingIncludingBelowZero() {
        assertEquals("14°F / -10°C · Snow", new WeatherSnapshot(-10, "Example", "Snow", NOW).label("both", NOW));
    }
    @Test public void staleAndFutureReadingsDisappearRatherThanLookCurrent() {
        WeatherSnapshot s = new WeatherSnapshot(21, "Example", "Clear", NOW);
        assertEquals("", s.label("both", NOW + WeatherSnapshot.MAX_AGE + 1));
        assertEquals("", s.label("both", NOW - 60_001));
        assertEquals("", s.label("off", NOW));
    }
    @Test public void oldButUsableReadingsAreMarked() {
        assertEquals("21°C · Clear · Updated earlier", new WeatherSnapshot(21, "Example", "Clear", NOW).label("c", NOW + 3_600_000));
    }
    @Test public void invalidProviderDataCannotReachTheUi() {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -101, 71}) {
            assertThrows(IllegalArgumentException.class, () -> new WeatherSnapshot(value, "Example", "Clear", NOW));
        }
        assertThrows(IllegalArgumentException.class, () -> new WeatherSnapshot(20, "City\nInjected", "Clear", NOW));
        assertFalse(WeatherSnapshot.unit("kelvin"));
    }
}
