package com.dvopenlabs.freedom.panchang;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocationLanguageTest {
    @Test public void manualLocationPreservesCoordinatesAndZone() {
        City city = City.manual(" My place ", 40.71, -74.0, "America/New_York");
        assertEquals("My place", city.name); assertEquals(40.71, city.lat, 0.00001);
        assertEquals(-74.0, city.lon, 0.00001); assertEquals("America/New_York", city.zone.getId());
    }
    @Test public void manualLocationRejectsInvalidValues() {
        for (double lat : new double[]{91, -91, Double.NaN, Double.POSITIVE_INFINITY}) {
            try { City.manual("X", lat, 0, "UTC"); fail(); } catch (IllegalArgumentException expected) { }
        }
        try { City.manual("X", 0, 181, "UTC"); fail(); } catch (IllegalArgumentException expected) { }
        try { City.manual(" ", 0, 0, "UTC"); fail(); } catch (IllegalArgumentException expected) { }
        try { City.manual("X", 0, 0, "Bad/Zone"); fail(); } catch (java.time.DateTimeException expected) { }
    }
    @Test public void sunTimesStayOnCivilDateAroundDaylightSaving() {
        City city = City.cityNamed("New York");
        for (String date : new String[]{"2025-03-08", "2025-03-09", "2025-03-10", "2025-11-02"}) {
            java.time.LocalDate day = java.time.LocalDate.parse(date);
            long[] times = Astro.sunriseSunset(day, city);
            assertNotNull(times);
            assertEquals(day, java.time.Instant.ofEpochMilli(times[0]).atZone(city.zone).toLocalDate());
            assertEquals(day, java.time.Instant.ofEpochMilli(times[1]).atZone(city.zone).toLocalDate());
            assertTrue(times[1] > times[0]);
        }
    }
    @Test public void languageSelectionFallsBackToEnglish() {
        assertEquals("hi", Language.supported("hi")); assertEquals("en", Language.supported("en"));
        assertEquals("en", Language.supported(null)); assertEquals("en", Language.supported("fr"));
    }
}
