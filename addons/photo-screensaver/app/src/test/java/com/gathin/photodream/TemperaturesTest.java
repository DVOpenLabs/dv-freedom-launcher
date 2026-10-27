package com.gathin.photodream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TemperaturesTest {
    @Test public void freezingAndOrdinaryTemperature() {
        assertEquals("32°F / 0°C", Temperatures.text(0, true));
        assertEquals("68°F / 20°C", Temperatures.text(20, true));
    }
    @Test public void convertsBeforeRoundingCelsius() {
        assertEquals("69°F / 20°C", Temperatures.text(20.4, true));
        assertEquals("31°F / 0°C", Temperatures.text(-0.4, true));
    }
    @Test public void negativeAndHotReadingsKeepSigns() {
        assertEquals("-40°F / -40°C", Temperatures.text(-40, true));
        assertEquals("14°F / -10°C", Temperatures.text(-10, true));
        assertEquals("99°F / 37°C", Temperatures.text(37, true));
    }
    @Test public void legacyDisplayRemainsTheDefault() {
        assertEquals("20°", Temperatures.text(20.4, false));
        assertEquals("-10°", Temperatures.text(-10, false));
    }
    @Test(expected = IllegalArgumentException.class) public void missingReadingIsNotShownAsZero() {
        Temperatures.text(Double.NaN, true);
    }
    @Test(expected = IllegalArgumentException.class) public void infiniteReadingIsRejected() {
        Temperatures.text(Double.POSITIVE_INFINITY, true);
    }
}
