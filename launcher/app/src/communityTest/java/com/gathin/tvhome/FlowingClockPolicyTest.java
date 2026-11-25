package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlowingClockPolicyTest {
    @Test public void transitionClampsAndMovesSmoothly() {
        assertEquals(0f, FlowingClockPolicy.progress(-1), 0f);
        assertEquals(0f, FlowingClockPolicy.progress(0), 0f);
        assertEquals(.5f, FlowingClockPolicy.progress(180), .0001f);
        assertTrue(FlowingClockPolicy.progress(100) < FlowingClockPolicy.progress(200));
        assertEquals(1f, FlowingClockPolicy.progress(360), 0f);
        assertEquals(1f, FlowingClockPolicy.progress(Long.MAX_VALUE), 0f);
    }
    @Test public void hourFormatHandlesMidnightNoonAndEndOfDay() {
        assertEquals(12, FlowingClockPolicy.hour(0, false));
        assertEquals(12, FlowingClockPolicy.hour(12, false));
        assertEquals(1, FlowingClockPolicy.hour(13, false));
        assertEquals(11, FlowingClockPolicy.hour(23, false));
        assertEquals(0, FlowingClockPolicy.hour(0, true));
        assertEquals(23, FlowingClockPolicy.hour(23, true));
    }
    @Test public void everyMotionVetoWins() {
        assertTrue(FlowingClockPolicy.motionAllowed(true, false, true, false, false));
        assertFalse(FlowingClockPolicy.motionAllowed(false, false, true, false, false));
        assertFalse(FlowingClockPolicy.motionAllowed(true, true, true, false, false));
        assertFalse(FlowingClockPolicy.motionAllowed(true, false, false, false, false));
        assertFalse(FlowingClockPolicy.motionAllowed(true, false, true, true, false));
        assertFalse(FlowingClockPolicy.motionAllowed(true, false, true, false, true));
    }
}
