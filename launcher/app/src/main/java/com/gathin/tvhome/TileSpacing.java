package com.gathin.tvhome;

/** Extra gutters, leaving the legacy layout identical at zero. No changes to cell/order semantics. */
final class TileSpacing {
    static final String CHOICE = "tile_gap_choice";
    static final String FOCUS = "focus_outline_choice";
    static boolean valid(int dp) { return dp >= 0 && dp <= 32; }
    static int pixels(int dp, float density) { return valid(dp) ? Math.round(dp * density) : 0; }
    static int width(int legacyWidth, int gapDp, float density) {
        return gapDp == 0 ? legacyWidth : Math.max(Math.round(80 * density), legacyWidth - pixels(gapDp, density));
    }
    static boolean beforeAnotherRow(int position, int count, int columns, boolean singleRow) {
        return !singleRow && columns > 0 && position >= 0 && position < count
                && position / columns < (count - 1) / columns;
    }
    private TileSpacing() { }
}
