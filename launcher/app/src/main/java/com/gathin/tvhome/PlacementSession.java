package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;

/** Process-only trial; leaving Home or letting the deadline expire restores the saved position. */
final class PlacementSession {
    static final HomeStyles.Preview PREVIEW=new HomeStyles.Preview(AppPlacement.IDS);
    static String preview(){return PREVIEW.active(SystemClock.elapsedRealtime());}
    static String selected(android.content.Context ctx){String value=preview();return value!=null?value:ProfileStore.preferences(ctx).getString(AppPlacement.KEY,null);}
    static boolean save(android.content.SharedPreferences p,String value){
        if(!AppPlacement.valid(value))return false;
        ProductFeatures.beforeChange("Change app position");
        String old=p.getString(AppPlacement.KEY,null);long revision=p.getLong(Config.SETTINGS_REVISION_KEY,0);
        if(p.edit().putString(AppPlacement.KEY,value).putLong(Config.SETTINGS_REVISION_KEY,revision+1).commit())return true;
        android.content.SharedPreferences.Editor repair=p.edit().putLong(Config.SETTINGS_REVISION_KEY,revision);
        if(old==null)repair.remove(AppPlacement.KEY);else repair.putString(AppPlacement.KEY,old);
        repair.commit();return false;
    }
    static void attach(Activity activity,Config config){
        TextView choice=activity.findViewById(R.id.positionChoice);
        String value=ProfileStore.preferences(activity).getString(AppPlacement.KEY,null);
        String[] names=activity.getResources().getStringArray(R.array.app_positions);
        int index=java.util.Arrays.asList(AppPlacement.IDS).indexOf(value);
        choice.setText(activity.getString(R.string.app_position_state,index<0?activity.getString(R.string.tv_button_follow):names[index]));
        choice.setOnClickListener(v->new AlertDialog.Builder(activity).setTitle(R.string.app_position)
            .setSingleChoiceItems(names,index,(d,which)->{PREVIEW.begin(AppPlacement.IDS[which],SystemClock.elapsedRealtime());d.dismiss();activity.finish();})
            .setNegativeButton(android.R.string.cancel,null).show());
    }
    static AlertDialog prompt(Activity activity){
        Handler handler=new Handler(android.os.Looper.getMainLooper());
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(R.string.app_position_keep)
            .setMessage(activity.getString(R.string.style_countdown,PREVIEW.seconds(SystemClock.elapsedRealtime())))
            .setNegativeButton(R.string.style_revert,(d,w)->{PREVIEW.cancel();activity.recreate();})
            .setPositiveButton(R.string.style_keep,(d,w)->{
                String value=preview();android.content.SharedPreferences p=ProfileStore.preferences(activity);
                if(!save(p,value))
                    Toast.makeText(activity,R.string.style_save_failed,Toast.LENGTH_LONG).show();
                PREVIEW.cancel();activity.recreate();
            }).create();
        Runnable timeout=()->{PREVIEW.cancel();dialog.dismiss();activity.recreate();};
        dialog.setOnCancelListener(d->{PREVIEW.cancel();activity.recreate();});
        dialog.setOnDismissListener(d->handler.removeCallbacks(timeout));
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();handler.postDelayed(timeout,PREVIEW.remaining(SystemClock.elapsedRealtime()));});
        dialog.show();return dialog;
    }
    private PlacementSession(){}
}
