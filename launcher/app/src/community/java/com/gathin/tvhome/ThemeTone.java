package com.gathin.tvhome;
import android.content.Context;
import android.graphics.Typeface;
/** Existing world palette follows new surfaces; never selects a theme or adds a motion timer. */
final class ThemeTone {
    static int accent(Context c){CinemaWorld world=CinemaWorld.showcase(CinemaStore.read(c),WallpaperPlaylistChoice.read(c));return world==null?c.getColor(R.color.freedom_accent):world.accent();}
    static Typeface heading(Context c){CinemaWorld world=CinemaWorld.showcase(CinemaStore.read(c),WallpaperPlaylistChoice.read(c));return world!=null&&world.serif()?Typeface.create("serif",Typeface.NORMAL):SettingsTypography.heading(c);}
    private ThemeTone(){}
}
