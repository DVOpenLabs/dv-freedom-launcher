package com.gathin.tvhome;
import android.media.tv.TvInputManager;
import org.junit.Test;
import static org.junit.Assert.*;
public class HdmiVisibilityTest {
    @Test public void disabledFilterPreservesEveryReportedState() {
        for (int state : new int[]{TvInputManager.INPUT_STATE_CONNECTED,
                TvInputManager.INPUT_STATE_CONNECTED_STANDBY, TvInputManager.INPUT_STATE_DISCONNECTED, -1, 99})
            assertTrue(HdmiVisibility.visible(false, state));
    }
    @Test public void explicitFilterHidesOnlyKnownInactiveStates() {
        assertTrue(HdmiVisibility.visible(true, TvInputManager.INPUT_STATE_CONNECTED));
        assertFalse(HdmiVisibility.visible(true, TvInputManager.INPUT_STATE_CONNECTED_STANDBY));
        assertFalse(HdmiVisibility.visible(true, TvInputManager.INPUT_STATE_DISCONNECTED));
    }
    @Test public void unknownAndUnreliableFirmwareStatesStayAccessible() {
        assertTrue(HdmiVisibility.visible(true, -1)); assertTrue(HdmiVisibility.visible(true, 99));
    }
}
