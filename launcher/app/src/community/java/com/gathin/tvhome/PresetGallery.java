package com.gathin.tvhome;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;

/** Still illustrations and a bounded confirmation; browsing never writes preferences. */
final class PresetGallery {
    private static final String[] DETAILS={"Glass · bottom row · medium icons · all names","Clean · middle row · large icons · larger text","Cinema · bottom row · selected name · quiet idle","Clean · bottom row · small icons · selected name"};
    private static final String[] REVIEW={"Glass; bottom row; medium icons; 8 dp spacing; all app names.","Clean; middle row; large icons; 16 dp spacing; all app names; 120% clock, header and app-name sizes.","Cinema; bottom row; medium icons; 8 dp spacing; selected app name; quiet idle after 30 seconds.","Clean; bottom row; small icons; 8 dp spacing; selected app name."};
    static void show(Activity a){
        Config.load(a);LinearLayout b=FreedomUi.body(a);
        b.addView(FreedomUi.text(a,"Choose a starting point, then review its illustration. Your apps, wallpaper, text color and TV settings stay saved. Nothing changes while browsing.",17));
        for(int i=0;i<HomePresets.NAMES.length;i++){final int index=i;FreedomUi.action(a,b,HomePresets.NAMES[i]+"\n"+DETAILS[i],()->preview(a,index));}
        FreedomUi.sheet(a,"Appearance presets",b,"Close");
    }
    static Map<String,Object> values(int index){Map<String,Object> settings=new LinkedHashMap<>(HomePresets.settings(index));settings.remove("text");settings.remove("strongFocus");if(index!=1)settings.remove("textSize");if(index!=2)settings.remove("idleSeconds");Map<String,Object> root=new HashMap<>();root.put("schema",1);root.put("name",HomePresets.NAMES[index]);root.put("settings",settings);return SetupValues.validate(new SetupProposal(root,Collections.emptySet()).values);}
    private static void preview(Activity a,int index){
        Config.load(a);String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();Map<String,Object> changes=new HashMap<>(values(index));
        LinearLayout b=FreedomUi.body(a);HomeMiniPreview picture=new HomeMiniPreview(a,SettingsTypography.regular(a));picture.preset(HomePresets.settings(index));
        int width=Math.min(FreedomUi.dp(a,360),a.getResources().getDisplayMetrics().widthPixels-FreedomUi.dp(a,150));LinearLayout.LayoutParams image=new LinearLayout.LayoutParams(width,Math.round(width*9/16f));image.gravity=android.view.Gravity.CENTER_HORIZONTAL;b.addView(picture,image);
        b.addView(FreedomUi.text(a,"Still illustration. Changes: "+REVIEW[index]+" Keep applies these choices to this profile. Other text sizes, focus, app order, live media, playlists and explicit text color stay saved.",17));
        LinearLayout root=FreedomUi.body(a);root.setBackgroundColor(a.getColor(R.color.freedom_canvas));root.addView(FreedomUi.text(a,HomePresets.NAMES[index],24));ScrollView scroll=new ScrollView(a);scroll.addView(b);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout footer=new LinearLayout(a);root.addView(footer);LinearLayout no=new LinearLayout(a),yes=new LinearLayout(a);footer.addView(no,new LinearLayout.LayoutParams(0,-2,1));footer.addView(yes,new LinearLayout.LayoutParams(0,-2,1));
        AlertDialog d=new AlertDialog.Builder(a).setView(root).create();Handler handler=new Handler(Looper.getMainLooper());long deadline=SystemClock.elapsedRealtime()+20_000;
        TextView revert=FreedomUi.action(a,no,"Revert",d::dismiss);
        TextView keep=FreedomUi.action(a,yes,"Keep preset",()->{
            if(SystemClock.elapsedRealtime()>=deadline||!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll())){Toast.makeText(a,"Home changed or the preview expired. Review the preset again.",Toast.LENGTH_LONG).show();d.dismiss();return;}
            if(!ProductFeatures.allowEdit(a,()->preview(a,index))){d.dismiss();return;}
            ProductFeatures.beforeChange(a,"Appearance preset");changes.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);
            if(SetupValues.commit(ProfileStore.preferences(a),changes)){d.dismiss();a.finish();ExperienceChoice.home(a);}else Toast.makeText(a,"Could not save this preset",Toast.LENGTH_LONG).show();
        });
        Runnable tick=new Runnable(){public void run(){long left=(deadline-SystemClock.elapsedRealtime()+999)/1000;if(left<=0||a.isFinishing()||a.isDestroyed()){d.dismiss();return;}revert.setText("Revert · "+left+"s");handler.postDelayed(this,1000);}};
        Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity x,Bundle s){}public void onActivityStarted(Activity x){}public void onActivityResumed(Activity x){}public void onActivityStopped(Activity x){}public void onActivitySaveInstanceState(Activity x,Bundle s){}public void onActivityPaused(Activity x){if(x==a)d.dismiss();}public void onActivityDestroyed(Activity x){if(x==a)d.dismiss();}};
        d.setOnShowListener(x->{FreedomUi.bound(a,d);a.getApplication().registerActivityLifecycleCallbacks(lifecycle);revert.requestFocusFromTouch();tick.run();});
        d.setOnDismissListener(x->{handler.removeCallbacksAndMessages(null);a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);});d.show();
    }
    private PresetGallery(){}
}
