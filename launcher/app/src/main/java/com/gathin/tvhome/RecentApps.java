package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Opt-in local launcher history, separate from appearance revisions and scoped to the current profile. */
final class RecentApps {
    static final String ENABLED="remember_recent_apps";
    private static SharedPreferences history(Context ctx){return ctx.getSharedPreferences(RecentHistory.scope(ProfileStore.id()),Context.MODE_PRIVATE);}
    static List<String> stored(Context ctx){return RecentHistory.read(history(ctx).getString("packages",""));}
    static void record(Context ctx,String pkg){
        if(!ProfileStore.preferences(ctx).getBoolean(ENABLED,false)||pkg==null||pkg.equals(ctx.getPackageName()))return;
        try{SharedPreferences p=history(ctx);p.edit().putString("packages",RecentHistory.opened(p.getString("packages",""),pkg)).apply();}
        catch(RuntimeException unavailable){/* Optional history must never prevent a successful app launch. */}
    }
    static boolean clear(Context ctx){return clearPreferences(history(ctx));}
    static boolean clearPreferences(SharedPreferences p){
        String old=p.getString("packages",null);
        if(p.edit().remove("packages").commit())return true;
        if(old!=null)p.edit().putString("packages",old).commit();return false;
    }
    private static void confirm(AlertDialog.Builder builder){AlertDialog dialog=builder.create();dialog.show();dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();}
    static void attach(Activity activity){
        SharedPreferences p=ProfileStore.preferences(activity);
        TextView choice=activity.findViewById(R.id.recentAppsChoice);
        choice.setText(activity.getString(R.string.recent_apps_state,activity.getString(p.getBoolean(ENABLED,false)?R.string.live_on:R.string.live_off)));
        choice.setOnClickListener(v->{
            boolean next=!p.getBoolean(ENABLED,false);
            if(next)confirm(new AlertDialog.Builder(activity).setTitle(R.string.recent_apps)
                .setMessage(R.string.recent_apps_help).setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(R.string.recent_enable,(d,w)->toggle(activity,p,true)));
            else toggle(activity,p,false);
        });
        activity.findViewById(R.id.clearRecentChoice).setOnClickListener(v->confirm(new AlertDialog.Builder(activity)
            .setTitle(R.string.recent_clear).setMessage(R.string.recent_clear_help)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.recent_clear,(d,w)->{
                if(!clear(activity))Toast.makeText(activity,R.string.style_save_failed,Toast.LENGTH_SHORT).show();
            })));
    }
    private static void toggle(Activity activity,SharedPreferences p,boolean on){
        // Clear first when disabling; if clearing fails, keep the enabled choice rather than promise deletion.
        if((on||clear(activity))&&CompanionControls.saveBoolean(p,ENABLED,on?Boolean.TRUE:null)){activity.setResult(Activity.RESULT_OK);activity.finish();}
        else Toast.makeText(activity,R.string.style_save_failed,Toast.LENGTH_SHORT).show();
    }
    static void show(Activity activity,Config config){
        if(!ProfileStore.preferences(activity).getBoolean(ENABLED,false))return;
        List<String> packages=new ArrayList<>(),labels=new ArrayList<>();
        for(String pkg:stored(activity)){
            if(config.hiddenApps.contains(pkg)||pkg.equals(activity.getPackageName()))continue;
            Intent intent=launch(activity,pkg);if(intent==null)continue;
            try{
                android.content.pm.ResolveInfo info=activity.getPackageManager().resolveActivity(intent,0);
                if(info==null||info.activityInfo==null||!info.activityInfo.exported||!info.activityInfo.enabled||!info.activityInfo.applicationInfo.enabled)continue;
                packages.add(pkg);labels.add(config.appLabels.getOrDefault(pkg,info.loadLabel(activity.getPackageManager()).toString()));
            }catch(RuntimeException unavailable){/* Removed/disabled apps never become dead shortcuts. */}
        }
        if(packages.isEmpty()){new AlertDialog.Builder(activity).setTitle(R.string.recent_apps).setMessage(R.string.recent_empty).setPositiveButton(android.R.string.ok,null).show();return;}
        new AlertDialog.Builder(activity).setTitle(R.string.recent_apps).setItems(labels.toArray(new String[0]),(d,which)->{
            Intent intent=launch(activity,packages.get(which));
            if(intent!=null)try{activity.startActivity(intent);record(activity,packages.get(which));activity.finish();return;}
                catch(android.content.ActivityNotFoundException|SecurityException missing){/* App changed after the menu opened. */}
            Toast.makeText(activity,R.string.not_available,Toast.LENGTH_SHORT).show();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private static Intent launch(Context ctx,String pkg){
        Intent intent=ctx.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if(intent==null)intent=ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        return intent==null?null:intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
    private RecentApps(){}
}
