package com.dvopenlabs.freedom.companions;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
/** Manual preview only. Does not select a wallpaper or system screensaver. */
public final class AmbientActivity extends Activity {
    AmbientSurface surface;
    @Override public void onCreate(Bundle b){super.onCreate(b);Boolean art=getIntent().hasExtra("art-preview")?getIntent().getBooleanExtra("art-preview",false):null;surface=new AmbientSurface(this,art,getIntent().getStringExtra("seasonal-preview"));setContentView(surface);surface.requestFocus();getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    @Override public void onResume(){super.onResume();surface.start();}
    @Override public void onPause(){surface.stop();super.onPause();}
}
