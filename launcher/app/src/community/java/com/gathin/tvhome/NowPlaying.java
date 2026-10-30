package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.media.session.MediaController;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Profile-opted-in media sessions. Listeners are scoped to a visible Home or the player panel. */
final class NowPlaying {
    static final String KEY="private_now_playing_enabled";
    private static final Map<Activity,Lease> ACTIVE=new WeakHashMap<>();
    static ComponentName component(Context c){return new ComponentName(c,MediaAccessService.class);}
    static boolean access(Context c){
        if(Build.VERSION.SDK_INT>=27)return ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationListenerAccessGranted(component(c));
        String enabled=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");
        return enabled!=null&&Arrays.asList(enabled.split(":")).contains(component(c).flattenToString());
    }
    static boolean enabled(Context c){return ProfileStore.preferences(c).getBoolean(KEY,false)&&access(c);}
    static String status(Context c){return ProfileStore.preferences(c).getBoolean(KEY,false)?access(c)?"On":"Access needed":"Off";}
    static void settings(Activity a){
        new AlertDialog.Builder(a).setTitle("Now Playing in this profile")
                .setMessage("Show media titles and supported player controls on Home. Only visible apps in this profile are included.\n\nAndroid Notification access is broader than media. DV Freedom ignores notification contents, saves or uploads nothing, and reads media sessions only while Home or the player panel is visible.\n\nEach profile starts Off. Turning this display off does not revoke Android access.")
                .setNegativeButton("Close",null)
                .setNeutralButton("Android access",(d,w)->{try{a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception unavailable){Toast.makeText(a,"This TV does not offer Notification access settings",Toast.LENGTH_LONG).show();}})
                .setPositiveButton(access(a)?enabled(a)?"Turn off":"Turn on":"Access help",(d,w)->{
                    if(!access(a)){Toast.makeText(a,"Enable Android Notification access manually first, then return here. App support varies.",Toast.LENGTH_LONG).show();return;}
                    if(!LauncherLock.require(a,()->settings(a)))return;ProductFeatures.beforeChange(a,"Change Now Playing display");
                    ProfileStore.preferences(a).edit().putBoolean(KEY,!enabled(a)).putLong(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();a.recreate();
                }).show();
    }
    static void attach(Activity a){
        if(!(a instanceof MainActivity||a instanceof ExperienceHomeActivity||a instanceof CategoryHomeActivity))return;
        Lease old=ACTIVE.remove(a);if(old!=null)old.close();
        if(!enabled(a))return;View root=a.findViewById(R.id.root);if(!(root instanceof FrameLayout))return;
        float scale=a.getResources().getDisplayMetrics().widthPixels/960f;TextView chip=new TextView(a);chip.setTextSize(14);chip.setTypeface(SettingsTypography.regular(a));chip.setTextColor(a.getColor(R.color.freedom_text));chip.setSingleLine(true);chip.setEllipsize(android.text.TextUtils.TruncateAt.END);chip.setPadding(Math.round(12*scale),0,Math.round(12*scale),0);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setBackgroundResource(R.drawable.settings_choice_bg);chip.setFocusable(true);chip.setClickable(true);chip.setVisibility(View.GONE);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(Math.round(455*scale),Math.round(40*scale));p.gravity=Gravity.TOP|Gravity.END;p.rightMargin=Math.round(40*scale);p.topMargin=Math.round(92*scale);((FrameLayout)root).addView(chip,p);
        Lease lease=new Lease(a,()->{});lease.changed=()->{String title=lease.title();chip.setText(title);chip.setContentDescription(title+". Open player controls");chip.setVisibility(title.isEmpty()?View.GONE:View.VISIBLE);};
        ACTIVE.put(a,lease);Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity x,Bundle b){}public void onActivityStarted(Activity x){}public void onActivityStopped(Activity x){}public void onActivitySaveInstanceState(Activity x,Bundle b){}public void onActivityResumed(Activity x){}public void onActivityPaused(Activity x){if(x==a)lease.close();}public void onActivityDestroyed(Activity x){if(x==a)lease.close();}};
        a.getApplication().registerActivityLifecycleCallbacks(lifecycle);lease.afterClose=()->{((FrameLayout)root).removeView(chip);if(ACTIVE.get(a)==lease)ACTIVE.remove(a);a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);};
        chip.setOnClickListener(v->a.startActivity(new Intent(a,NowPlayingActivity.class)));lease.start();
    }
    static final class Lease implements AutoCloseable {
        final Activity a;final MediaSessionManager manager;final Handler main=new Handler(Looper.getMainLooper());final Set<String> visible=new HashSet<>();
        final List<MediaController> watched=new ArrayList<>();MediaController selected;Runnable changed,afterClose;boolean closed,registered;
        final MediaSessionManager.OnActiveSessionsChangedListener sessions=value->select(value);
        final MediaController.Callback callback=new MediaController.Callback(){@Override public void onMetadataChanged(MediaMetadata m){paint();}@Override public void onPlaybackStateChanged(PlaybackState s){choose();}@Override public void onSessionDestroyed(){refresh();}};
        Lease(Activity a,Runnable changed){this.a=a;this.changed=changed;manager=(MediaSessionManager)a.getSystemService(Context.MEDIA_SESSION_SERVICE);Config config=Config.load(a);for(HomeCategories.App app:HomeCategories.catalog(a,config))visible.add(app.pkg);}
        void start(){if(!enabled(a)){paint();return;}try{manager.addOnActiveSessionsChangedListener(sessions,component(a),main);registered=true;refresh();}catch(RuntimeException denied){paint();}}
        void refresh(){if(closed)return;try{select(enabled(a)?manager.getActiveSessions(component(a)):Collections.emptyList());}catch(RuntimeException unavailable){select(Collections.emptyList());}}
        void select(List<MediaController> controllers){
            if(closed)return;for(MediaController controller:watched)controller.unregisterCallback(callback);watched.clear();int scanned=0;
            for(MediaController controller:controllers==null?Collections.<MediaController>emptyList():controllers){if(scanned++>=16)break;if(!visible.contains(controller.getPackageName()))continue;watched.add(controller);controller.registerCallback(callback,main);}choose();
        }
        void choose(){if(closed)return;selected=null;int rank=0;for(MediaController controller:watched){PlaybackState state=controller.getPlaybackState();int value=state==null?0:NowPlayingPolicy.rank(state.getState());if(value>rank){rank=value;selected=controller;}}paint();}
        String title(){if(selected==null)return "";try{MediaMetadata metadata=selected.getMetadata();String title=metadata==null?"":NowPlayingPolicy.text(metadata.getText(MediaMetadata.METADATA_KEY_TITLE),120);String artist=metadata==null?"":NowPlayingPolicy.text(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST),100);if(title.isEmpty())title=NowPlayingPolicy.text(a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(selected.getPackageName(),0)),80);PlaybackState state=selected.getPlaybackState();return (state!=null&&state.getState()==PlaybackState.STATE_PAUSED?"Paused \u00b7 ":"Now playing \u00b7 ")+title+(artist.isEmpty()?"":" \u00b7 "+artist);}catch(Exception unavailable){return "";}}
        boolean action(long action){PlaybackState state=selected==null?null:selected.getPlaybackState();return state!=null&&(state.getActions()&action)!=0;}
        void paint(){if(!closed&&changed!=null)changed.run();}
        @Override public void close(){if(closed)return;closed=true;main.removeCallbacksAndMessages(null);for(MediaController controller:watched)controller.unregisterCallback(callback);watched.clear();if(registered)try{manager.removeOnActiveSessionsChangedListener(sessions);}catch(Exception ignored){}selected=null;if(afterClose!=null){afterClose.run();afterClose=null;}}
    }
}
