package com.gathin.tvhome;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.*;
import android.widget.TextClock;
/** Pre-Android-12 TextClock recovery when OEM standby stops TIME_TICK. Based on upstream #3044 (MIT). */
final class ClockRefresh implements Runnable, DisplayManager.DisplayListener {
    private final View root;private final DisplayManager displays;private boolean active;
    ClockRefresh(View root){this.root=root;displays=root.getContext().getApplicationContext().getSystemService(DisplayManager.class);}
    void start(){if(Build.VERSION.SDK_INT>=31)return;if(!active&&displays!=null)displays.registerDisplayListener(this,null);active=true;run();}
    void stop(){active=false;root.removeCallbacks(this);if(displays!=null)displays.unregisterDisplayListener(this);}
    public void run(){root.removeCallbacks(this);if(!active)return;redraw(root);root.postDelayed(this,60250-System.currentTimeMillis()%60000);}
    private static void redraw(View v){if(v instanceof TextClock){TextClock c=(TextClock)v;c.setFormat12Hour(c.getFormat12Hour());}else if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)redraw(g.getChildAt(i));}}
    public void onDisplayChanged(int id){Display d=root.getDisplay();if(active&&d!=null&&d.getDisplayId()==id&&d.getState()==Display.STATE_ON)run();}
    public void onDisplayAdded(int id){}public void onDisplayRemoved(int id){}
}
