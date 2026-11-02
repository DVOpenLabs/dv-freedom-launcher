package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.os.*;
/** Process-local twenty-second preview. It never rewrites the selected wallpaper or playlist. */
final class CinemaWorldPreview {
    private static String token="",profile="";private static long until;private static final Handler timer=new Handler(Looper.getMainLooper());
    static String token(){return SystemClock.elapsedRealtime()<until&&profile.equals(ProfileStore.id())?token:"";}
    static void start(Activity a,String value){if(!ThemeLibrary.validToken(value))throw new IllegalArgumentException("Choose a local wallpaper");cancel();token=value;profile=ProfileStore.id();until=SystemClock.elapsedRealtime()+20000;timer.postDelayed(CinemaWorldPreview::cancel,20000);a.startActivity(new Intent(a,ExperienceHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));WallpaperPlaylist.refreshAll();}
    static boolean cancel(){boolean was=!token.isEmpty();token="";until=0;timer.removeCallbacksAndMessages(null);if(was)WallpaperPlaylist.refreshAll();return was;}
    private CinemaWorldPreview(){}
}
