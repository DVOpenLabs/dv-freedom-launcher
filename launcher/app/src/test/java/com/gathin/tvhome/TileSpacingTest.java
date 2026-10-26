package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class TileSpacingTest {
    @Test public void zeroKeepsLegacyDimensionsExactly() {
        for (float density : new float[]{1f, 1.5f, 2f})
            for (int width : new int[]{80, 100, 180, 270, 360})
                assertEquals(width, TileSpacing.width(width, 0, density));
        assertEquals(0, TileSpacing.pixels(0, 1.5f));
    }
    @Test public void extraGutterRespectsDensityAndMinimumReadableTile() {
        assertEquals(246, TileSpacing.width(270, 16, 1.5f));
        assertEquals(120, TileSpacing.width(130, 32, 1.5f));
        assertEquals(12, TileSpacing.pixels(8, 1.5f));
        assertTrue(TileSpacing.valid(32)); assertFalse(TileSpacing.valid(33)); assertFalse(TileSpacing.valid(-1));
    }
    @Test public void onlyRowsWithSuccessorsGetBottomSpace() {
        for (int position = 0; position < 8; position++)
            assertEquals(position < 4, TileSpacing.beforeAnotherRow(position, 8, 4, false));
        assertTrue(TileSpacing.beforeAnotherRow(3, 5, 4, false));
        assertFalse(TileSpacing.beforeAnotherRow(4, 5, 4, false));
        assertFalse(TileSpacing.beforeAnotherRow(0, 10, 4, true));
        assertFalse(TileSpacing.beforeAnotherRow(-1, 10, 4, false));
        assertFalse(TileSpacing.beforeAnotherRow(10, 10, 4, false));
        assertFalse(TileSpacing.beforeAnotherRow(0, 10, 0, false));
    }
}
