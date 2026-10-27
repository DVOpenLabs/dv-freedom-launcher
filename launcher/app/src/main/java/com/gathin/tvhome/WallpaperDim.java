package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;

/** Reuse the existing still/video dim renderer. Never changes panel brightness or the media file. */
final class WallpaperDim {
    static final String KEY="wallpaper_dim_choice";
    static int chosen(int value,int fallback){return value>=0&&value<=90?value:fallback;}
    static boolean save(SharedPreferences p,int value){
        if(value< -1||value>90)return false;
        int old=p.getInt(KEY,-1);long revision=p.getLong(Config.SETTINGS_REVISION_KEY,0);boolean hadRevision=p.contains(Config.SETTINGS_REVISION_KEY);
        SharedPreferences.Editor edit=p.edit();if(value<0)edit.remove(KEY);else edit.putInt(KEY,value);
        if(edit.putLong(Config.SETTINGS_REVISION_KEY,revision+1).commit())return true;
        SharedPreferences.Editor repair=p.edit();if(old<0)repair.remove(KEY);else repair.putInt(KEY,old);
        if(hadRevision)repair.putLong(Config.SETTINGS_REVISION_KEY,revision);else repair.remove(Config.SETTINGS_REVISION_KEY);
        repair.commit();return false;
    }
    static void attach(Activity activity,Config config){
        TextView choice=activity.findViewById(R.id.wallpaperDimChoice);
        choice.setText(activity.getString(R.string.wallpaper_dim_state,config.wallpaperDim));
        choice.setOnClickListener(v->{
            int[] values={-1,0,15,30,45,60};String[] names=new String[values.length];int selected=0;
            SharedPreferences p=ProfileStore.preferences(activity);int current=p.getInt(KEY,-1);
            for(int i=0;i<values.length;i++){names[i]=i==0?activity.getString(R.string.tv_button_follow):activity.getString(R.string.wallpaper_dim_percent,values[i]);if(current==values[i])selected=i;}
            new AlertDialog.Builder(activity).setTitle(R.string.wallpaper_dim).setSingleChoiceItems(names,selected,(d,which)->{
                if(save(p,values[which])){d.dismiss();activity.setResult(Activity.RESULT_OK);activity.finish();}
                else Toast.makeText(activity,R.string.style_save_failed,Toast.LENGTH_SHORT).show();
            }).setNegativeButton(android.R.string.cancel,null).show();
        });
    }
    private WallpaperDim(){}
}
