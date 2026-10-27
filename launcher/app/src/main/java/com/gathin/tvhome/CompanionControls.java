package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Lightweight visible-Home audio status, desktop refresh and an ordinary system-pairing shortcut. */
final class CompanionControls {
    static final String AUDIO="show_audio_indicator";
    private final Activity activity;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final TextView status;
    private boolean shown,observing,rebuildQueued;
    private final ContentObserver observer=new ContentObserver(handler) {
        @Override public void onChange(boolean self) {
            if(shown&&!rebuildQueued&&!activity.isFinishing()){rebuildQueued=true;activity.recreate();}
        }
    };
    private final Runnable tick=new Runnable(){public void run(){
        if(!shown)return;
        AudioManager audio=activity.getSystemService(AudioManager.class);
        boolean active=false;
        try{active=audio!=null&&audio.isMusicActive();}catch(RuntimeException unavailable){/* Status must never affect Home. */}
        status.setVisibility(active?View.VISIBLE:View.GONE);
        if(active)position();
        handler.postDelayed(this,2000);
    }};
    CompanionControls(Activity activity) {
        this.activity=activity;status=new TextView(activity);
        status.setText(R.string.audio_active);status.setTextColor(Color.WHITE);status.setTextSize(13);
        status.setPadding(dp(10),dp(4),dp(10),dp(4));status.setVisibility(View.GONE);
        status.setFocusable(false);status.setClickable(false);
        GradientDrawable background=new GradientDrawable();background.setColor(0xF0182434);background.setCornerRadius(dp(12));
        status.setBackground(background);
        ((FrameLayout)activity.findViewById(android.R.id.content)).addView(status,
                new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END));
    }
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private void position() {
        View anchor=activity.findViewById(R.id.btnSettings),root=activity.findViewById(android.R.id.content);
        int[] a=new int[2],r=new int[2];anchor.getLocationOnScreen(a);root.getLocationOnScreen(r);
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)status.getLayoutParams();
        status.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
        int top=Math.max(dp(14),a[1]-r[1]-status.getMeasuredHeight()-dp(8));
        int end=root.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?a[0]-r[0]:root.getWidth()-(a[0]-r[0]+anchor.getWidth());
        if(p.topMargin!=top||p.getMarginEnd()!=end){p.topMargin=top;p.setMarginEnd(Math.max(0,end));status.setLayoutParams(p);}
    }
    void start() {
        if(shown)return;shown=true;
        activity.getContentResolver().registerContentObserver(DesktopLayout.uri(activity),false,observer);observing=true;
        if(ProfileStore.preferences(activity).getBoolean(AUDIO,false))handler.post(tick);
    }
    void stop() {
        shown=false;handler.removeCallbacks(tick);status.setVisibility(View.GONE);
        if(observing){activity.getContentResolver().unregisterContentObserver(observer);observing=false;}
    }
    void close(){stop();((FrameLayout)activity.findViewById(android.R.id.content)).removeView(status);}
    static void attachSettings(Activity activity) {
        activity.findViewById(R.id.findAppsChoice).setOnClickListener(v->activity.startActivity(new Intent(activity,MainActivity.class).setAction(MainActivity.ACTION_LIBRARY)));
        activity.findViewById(R.id.quickActionsChoice).setOnClickListener(v->activity.startActivity(new Intent(activity,QuickSettingsActivity.class)));
        TextView pairing=activity.findViewById(R.id.bluetoothChoice);
        pairing.setOnClickListener(v->openPairing(activity));
        TextView audio=activity.findViewById(R.id.audioIndicatorChoice);
        SharedPreferences p=ProfileStore.preferences(activity);
        audio.setText(activity.getString(R.string.audio_indicator_state,activity.getString(p.getBoolean(AUDIO,false)?R.string.live_on:R.string.live_off)));
        audio.setOnClickListener(v->{
            if(saveAudio(p,!p.getBoolean(AUDIO,false))){
                activity.setResult(Activity.RESULT_OK);activity.finish();
            }else Toast.makeText(activity,R.string.audio_save_failed,Toast.LENGTH_SHORT).show();
        });
    }
    static boolean saveAudio(SharedPreferences p,boolean on) {
        return saveBoolean(p,AUDIO,on);
    }
    static boolean saveBoolean(SharedPreferences p,String key,Boolean on) {
        boolean existed=p.contains(key),before=p.getBoolean(key,false);
        boolean hadRevision=p.contains(Config.SETTINGS_REVISION_KEY);
        long revision=p.getLong(Config.SETTINGS_REVISION_KEY,0);
        SharedPreferences.Editor edit=p.edit();if(on==null)edit.remove(key);else edit.putBoolean(key,on);
        if(edit.putLong(Config.SETTINGS_REVISION_KEY,revision+1).commit())return true;
        SharedPreferences.Editor repair=p.edit();
        if(existed)repair.putBoolean(key,before);else repair.remove(key);
        if(hadRevision)repair.putLong(Config.SETTINGS_REVISION_KEY,revision);else repair.remove(Config.SETTINGS_REVISION_KEY);
        repair.commit();return false;
    }
    static void openPairing(Activity activity) {
        for(Intent intent:new Intent[]{new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                new Intent("android.settings.SLICE_SETTINGS").setPackage("com.android.tv.settings")
                        .putExtra("slice_uri","content://com.android.tv.settings.accessories.sliceprovider/general"),
                new Intent("com.google.android.intent.action.CONNECT_INPUT").setPackage("com.android.tv.settings")}) {
            ResolveInfo resolved=activity.getPackageManager().resolveActivity(intent,0);
            if(resolved==null||resolved.activityInfo==null||!resolved.activityInfo.exported)continue;
            String permission=resolved.activityInfo.permission;
            if(permission!=null&&activity.checkSelfPermission(permission)!=android.content.pm.PackageManager.PERMISSION_GRANTED)continue;
            try{activity.startActivity(intent);return;}catch(android.content.ActivityNotFoundException|SecurityException unavailable){/* Next supported entry. */}
        }
        Toast.makeText(activity,R.string.bluetooth_fallback,Toast.LENGTH_LONG).show();
        try{activity.startActivity(SystemSettings.intent(activity,Settings.ACTION_SETTINGS));}
        catch(android.content.ActivityNotFoundException|SecurityException unavailable){Toast.makeText(activity,R.string.not_available,Toast.LENGTH_SHORT).show();}
    }
}
