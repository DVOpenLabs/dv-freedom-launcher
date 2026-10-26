package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeStylesTest {
    @Test public void previewExpiresAtBoundaryWithoutPersistedState() {
        HomeStyles.Preview state = new HomeStyles.Preview();
        state.begin("cinematic", 100);
        assertEquals("cinematic", state.active(20_099));
        assertEquals(1, state.seconds(20_099));
        assertEquals(1, state.remaining(20_099));
        assertNull(state.active(20_100));
        assertEquals(0, state.seconds(20_100));
        assertEquals(0, state.remaining(20_100));
        assertEquals(2, state.revision());
    }
    @Test public void cancelIsIdempotentAndNewProcessHasNoPreview() {
        HomeStyles.Preview state = new HomeStyles.Preview();
        state.begin("minimal", 10);
        assertTrue(state.cancel()); assertFalse(state.cancel()); assertNull(state.active(20));
        assertNull(new HomeStyles.Preview().active(20));
    }
    @Test public void newPreviewGetsItsOwnDeadline() {
        HomeStyles.Preview state = new HomeStyles.Preview();
        state.begin("minimal", 0); state.begin("easy_view", 19_000);
        assertEquals("easy_view", state.active(20_000)); assertNull(state.active(39_000));
    }
    @Test(expected = IllegalArgumentException.class) public void unknownPreviewIsRejected() { new HomeStyles.Preview().begin("other", 0); }
    @Test public void undoRoundTripsMissingAndExplicitOverrides() {
        HomeStyles.Snapshot empty = new HomeStyles.Snapshot(null, null, null, null);
        assertEquals("-|-|-|-", empty.encode());
        assertEquals(empty.encode(), HomeStyles.Snapshot.decode(empty.encode()).encode());
        for (String style : HomeStyles.IDS) {
            HomeStyles.Snapshot before = new HomeStyles.Snapshot(style, 7, 3, 70);
            assertEquals(before.encode(), HomeStyles.Snapshot.decode(before.encode()).encode());
        }
    }
    @Test public void malformedUndoCannotResetOtherSettings() {
        for (String bad : new String[]{"", "-|-|-", "other|4|0|50", "minimal|2|0|50", "minimal|4|9|50", "minimal|4|0|200", "minimal|no|0|50"})
            assertNull(bad, HomeStyles.Snapshot.decode(bad));
    }
}
