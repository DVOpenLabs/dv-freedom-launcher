package com.dvopenlabs.freedom.companions;
import android.service.dreams.DreamService;
/** System-selected screensaver; no work before it is started or after it stops. */
public final class AmbientDream extends DreamService {
    AmbientSurface surface;
    @Override public void onAttachedToWindow(){super.onAttachedToWindow();setFullscreen(true);setScreenBright(false);setInteractive(false);surface=new AmbientSurface(this);setContentView(surface);}
    @Override public void onDreamingStarted(){super.onDreamingStarted();if(surface!=null)surface.start();}
    @Override public void onDreamingStopped(){if(surface!=null)surface.stop();super.onDreamingStopped();}
    @Override public void onDetachedFromWindow(){if(surface!=null)surface.stop();super.onDetachedFromWindow();}
}
