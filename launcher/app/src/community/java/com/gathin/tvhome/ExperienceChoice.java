package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.util.*;

/** Process-local trials; only Keep saves the presentation key, never geometry, apps or media. */
final class ExperienceChoice {
    static final String EXTRA="experience_preview";
    static final long DURATION=20000;
    private static Trial trial;
    static final class Trial {
        final String look,profile;final long deadline;final Map<String,?> before;
        Trial(Context c,String look){this.look=look;profile=ProfileStore.id();before=ProfileStore.preferences(c).getAll();deadline=SystemClock.elapsedRealtime()+DURATION;}
    }
    static String saved(Context c){String id=ProfileStore.preferences(c).getString(ExperiencePolicy.KEY,ExperiencePolicy.LEGACY);return ExperiencePolicy.valid(id)?id:ExperiencePolicy.LEGACY;}
    static boolean enabled(Context c){return !ExperiencePolicy.LEGACY.equals(saved(c));}
    static String preview(){return trial!=null&&SystemClock.elapsedRealtime()<trial.deadline&&trial.profile.equals(ProfileStore.id())?trial.look:null;}
    static boolean trialValid(String id){return id!=null&&id.equals(preview());}
    static void cancel(){trial=null;}
    static void show(Activity a){
        Config.load(a);String current=saved(a);String[] ids={ExperiencePolicy.GLASS,ExperiencePolicy.CLEAN,ExperiencePolicy.CINEMA,ExperiencePolicy.LEGACY};String[] labels=new String[ids.length];
        for(int i=0;i<ids.length;i++)labels[i]=ExperiencePolicy.name(ids[i])+(ids[i].equals(current)?" · Selected":"")+"\n"+ExperiencePolicy.description(ids[i]);
        new AlertDialog.Builder(a).setTitle("Home experience").setItems(labels,(d,n)->{
            if(ids[n].equals(current)){Toast.makeText(a,"This experience is already selected",Toast.LENGTH_SHORT).show();return;}
            if(ExperiencePolicy.LEGACY.equals(ids[n])){
                String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();
                new AlertDialog.Builder(a).setTitle("Use Classic?").setMessage("Return to your saved grid or category layout. Apps, wallpaper and other appearance choices stay saved.").setNegativeButton(android.R.string.cancel,null).setPositiveButton("Use Classic",(x,w)->{if(save(a,profile,before,ExperiencePolicy.LEGACY)){a.finish();home(a);}}).show();return;
            }
            trial=new Trial(a,ids[n]);a.startActivity(new Intent(a,ExperienceHomeActivity.class).putExtra(EXTRA,ids[n]));
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    static void prompt(Activity a,String id){
        Trial expected=trial;if(expected==null||!trialValid(id)){a.finish();return;}
        if(a instanceof ExperienceHomeActivity){
            ((ExperienceHomeActivity)a).trialControls(expected.deadline,()->{boolean kept=trial==expected&&trialValid(id)&&save(a,expected.profile,expected.before,id);cancel();a.finish();if(kept)home(a);},()->{if(trial==expected)cancel();a.finish();});return;
        }
        Handler handler=new Handler(Looper.getMainLooper());final boolean[] answered={false};
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Keep "+ExperiencePolicy.name(id)+"?")
            .setMessage("Try the new presentation. Keep saves only this profile’s Home experience. Your app order, wallpaper rotation and TV settings stay in place.")
            .setNegativeButton("Revert",(d,w)->{answered[0]=true;cancel();a.finish();})
            .setPositiveButton("Keep",(d,w)->{answered[0]=true;boolean kept=trial==expected&&save(a,expected.profile,expected.before,id);cancel();a.finish();if(kept)home(a);}).create();
        Runnable expire=()->{if(!answered[0]){answered[0]=true;cancel();dialog.dismiss();a.finish();}};
        dialog.setOnCancelListener(d->expire.run());dialog.setOnDismissListener(d->handler.removeCallbacks(expire));
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();handler.postDelayed(expire,Math.max(1,expected.deadline-SystemClock.elapsedRealtime()));});dialog.show();
    }
    private static boolean save(Activity a,String profile,Map<String,?> before,String id){
        if(!ExperiencePolicy.valid(id)||!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll())){Toast.makeText(a,"Home changed. Open Home experience again.",Toast.LENGTH_LONG).show();return false;}
        if(id.equals(saved(a)))return true;
        ProductFeatures.beforeChange(a,"Change Home experience");Map<String,Object> values=new HashMap<>();values.put(ExperiencePolicy.KEY,id);values.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(SetupValues.commit(ProfileStore.preferences(a),values))return true;
        Toast.makeText(a,"Could not save the Home experience",Toast.LENGTH_LONG).show();return false;
    }
    static void home(Activity a){a.startActivity(new Intent(a,MainActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
    static boolean route(MainActivity a){
        Config.load(a);if(!enabled(a)||!a.productRoutingAllowed())return false;
        a.getWindow().getDecorView().setAlpha(0);
        a.startActivity(new Intent(a,ExperienceHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));a.overridePendingTransition(0,0);return true;
    }
    private ExperienceChoice(){}
}
