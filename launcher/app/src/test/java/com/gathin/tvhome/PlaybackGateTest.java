package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackGateTest {
    @Test public void hiddenAndOldCallbacksCannotRestartVideo() {
        PlaybackGate gate = new PlaybackGate(); int old = gate.begin();
        assertTrue(gate.accepts(old)); gate.stop(); assertFalse(gate.accepts(old));
        int fresh = gate.begin(); assertFalse(gate.accepts(old)); assertTrue(gate.accepts(fresh));
        assertFalse(gate.fail(old)); assertFalse(gate.failed());
    }
    @Test public void decoderFailureLatchesUntilActivityIsRebuilt() {
        PlaybackGate gate = new PlaybackGate(); int token = gate.begin();
        assertTrue(gate.fail(token)); assertFalse(gate.accepts(token));
        assertFalse(gate.fail(token)); assertTrue(gate.failed());
        assertFalse(gate.accepts(gate.begin()));
    }
    @Test public void conservativeMemoryAndMotionGate() {
        long threshold = 2L * 1024 * 1024 * 1024;
        assertTrue(PlaybackGate.supported(threshold, false, false));
        assertFalse(PlaybackGate.supported(threshold - 1, false, false));
        assertFalse(PlaybackGate.supported(threshold, true, false));
        assertFalse(PlaybackGate.supported(threshold, false, true));
    }
}
