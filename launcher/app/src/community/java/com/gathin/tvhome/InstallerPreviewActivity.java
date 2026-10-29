package com.gathin.tvhome;
import android.app.*;
import android.os.*;
import android.content.*;
/** The person keeps a setup on the physical TV. Timeout, Back and leaving all revert. */
public final class InstallerPreviewActivity extends Activity {
    private boolean answered;private String id;private final Handler handler=new Handler(Looper.getMainLooper());private AlertDialog dialog;
    private void finishPreview(boolean keep){if(answered)return;answered=true;try{if(keep)InstallerTransfer.keep(this,id);else InstallerTransfer.revert(this,id);}catch(Exception e){try{InstallerTransfer.revert(this,id);}catch(Exception ignored){}android.widget.Toast.makeText(this,e.getMessage(),android.widget.Toast.LENGTH_LONG).show();}handler.removeCallbacksAndMessages(null);if(dialog!=null)dialog.dismiss();finish();ExperienceChoice.home(this);}
    @Override public void onCreate(Bundle state){super.onCreate(state);id=getIntent().getStringExtra("id");
        dialog=new AlertDialog.Builder(this).setTitle("Keep this Home?").setMessage("This is a new launcher profile. Your previous Home stays saved. Choose Keep to use it, or Revert to return. No TV settings or default Home are changed.").setNegativeButton("Revert",(d,w)->finishPreview(false)).setPositiveButton("Keep this Home",(d,w)->finishPreview(true)).create();
        dialog.setOnCancelListener(d->finishPreview(false));dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setFocusableInTouchMode(true);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();tick();});dialog.show();
    }
    private void tick(){if(answered)return;try{long seconds=(InstallerTransfer.deadline(this,id)-System.currentTimeMillis()+999)/1000;if(seconds<=0){finishPreview(false);return;}dialog.setTitle("Keep this Home?  "+seconds+"s");handler.postDelayed(this::tick,1000);}catch(Exception expired){finishPreview(false);}}
    @Override public void onBackPressed(){finishPreview(false);}
    @Override protected void onPause(){super.onPause();if(!answered)finishPreview(false);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
