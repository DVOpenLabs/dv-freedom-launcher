package com.gathin.tvhome;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import org.json.JSONObject;

/** Allowlisted text from our visible Home only, read on the main thread by the ADB-protected editor. */
final class HomePreview {
    private static WeakReference<Activity> home=new WeakReference<>(null);
    private static WeakReference<TextView> weather=new WeakReference<>(null);
    static void attach(Activity activity,TextView weatherView){home=new WeakReference<>(activity);weather=new WeakReference<>(weatherView);}
    static void detach(Activity activity){if(home.get()==activity){home.clear();weather.clear();}}
    private static String text(Activity activity,int id){View view=activity.findViewById(id);return view instanceof TextView&&view.isShown()?((TextView)view).getText().toString():"";}
    static JSONObject snapshot(Config config)throws org.json.JSONException {
        Activity activity=home.get();
        JSONObject result=new JSONObject().put("available",activity!=null).put("inputs",config.showInputsPill)
            .put("settings",config.launcherSettingsButton?"Launcher settings":"Settings")
            .put("effectivePosition",config.dockRow?"bottom":"top");
        if(activity!=null){
            TextView weatherView=weather.get();
            result.put("capturedAt",System.currentTimeMillis()).put("clock",(text(activity,R.id.clock)+" "+text(activity,R.id.ampm)).trim())
                .put("date",text(activity,R.id.date)).put("greeting",text(activity,R.id.greeting))
                .put("weather",weatherView!=null&&weatherView.isShown()?weatherView.getText().toString():"")
                .put("inputs",activity.findViewById(R.id.btnInputs).isShown()).put("settings",text(activity,R.id.btnSettings));
        }
        return result;
    }
    private HomePreview(){}
}
