package com.gathin.tvhome;
import android.app.*;
import android.content.Intent;
import android.view.View;
/** One entry to existing controls. Opening it never grants access or saves settings. */
final class HomeControlCenter {
    static void attach(Activity a){View clock=a.findViewById(R.id.clock);if(clock==null)return;clock.setFocusable(true);clock.setClickable(true);clock.setContentDescription("Clock · open Control Center");clock.setOnClickListener(v->show(a));clock.setOnFocusChangeListener((v,on)->v.setBackgroundResource(on?R.drawable.freedom_settings_row:android.R.color.transparent));}
    static void show(Activity a){new AlertDialog.Builder(a).setTitle("Control Center").setItems(new String[]{"Quick settings","Inputs / HDMI","Now Playing","Watch tonight","Launcher settings","Home capabilities","Explain a problem"},(d,n)->{
        if(n==0)a.startActivity(new Intent(a,QuickSettingsActivity.class));
        else if(n==1)InputPicker.show(a,false);
        else if(n==2)a.startActivity(new Intent(a,NowPlayingActivity.class));
        else if(n==3)a.startActivity(new Intent(a,WatchTonightActivity.class));
        else if(n==4)a.startActivity(new Intent(a,LauncherSettingsActivity.class));
        else if(n==6)ProblemHelp.show(a);
        else new AlertDialog.Builder(a).setTitle(ExperiencePolicy.name(ExperienceChoice.saved(a))).setMessage(HomeCapabilities.summary(ExperienceChoice.saved(a))).setPositiveButton("Close",null).show();
    }).setNegativeButton("Back",null).show();}
    private HomeControlCenter(){}
}
