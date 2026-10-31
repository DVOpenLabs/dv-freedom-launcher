package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.*;
import android.content.Context;
import android.os.PowerManager;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import java.io.File;
import java.util.*;

/** One GPU view transform per visible still. No bitmap copies, key handling or permanent timer. */
final class StillMotion {
    private static final Map<ImageView,Session> ACTIVE = new WeakHashMap<>();
    static void stop(ImageView image) {
        Session session = ACTIVE.remove(image);
        if (session != null) session.close();
        image.setScaleX(1); image.setScaleY(1); image.setTranslationX(0); image.setTranslationY(0);
    }
    static void start(ImageView image, boolean reduced, boolean video) {
        stop(image);
        Context c = image.getContext();
        PowerManager power = c.getSystemService(PowerManager.class);
        boolean quiet = reduced || !ValueAnimator.areAnimatorsEnabled() || power != null && power.isPowerSaveMode();
        if (!PanZoomPolicy.allowed(ProfileStore.preferences(c).getBoolean(PanZoomPolicy.KEY,false), quiet,
                video, image.getDrawable()!=null)) return;
        Session session=new Session(image);ACTIVE.put(image,session);
        image.addOnAttachStateChangeListener(session);image.post(session);
    }
    private static final class Session implements android.view.View.OnAttachStateChangeListener,Runnable {
        final ImageView image;final float[] frame=new float[3];ValueAnimator animation;
        Session(ImageView image){this.image=image;}
        public void run(){
            if(ACTIVE.get(image)!=this||!image.isAttachedToWindow()||animation!=null)return;
            animation=ValueAnimator.ofFloat(0,1);animation.setDuration(PanZoomPolicy.DURATION_MS);
            animation.setRepeatCount(ValueAnimator.INFINITE);animation.setRepeatMode(ValueAnimator.REVERSE);
            animation.setInterpolator(new LinearInterpolator());
            animation.addUpdateListener(a->{PanZoomPolicy.frame((Float)a.getAnimatedValue(),frame);
                image.setScaleX(frame[0]);image.setScaleY(frame[0]);
                image.setTranslationX(frame[1]*image.getWidth());image.setTranslationY(frame[2]*image.getHeight());});
            animation.start();
        }
        public void onViewAttachedToWindow(android.view.View v){run();}
        public void onViewDetachedFromWindow(android.view.View v){stop(image);}
        void close(){image.removeCallbacks(this);image.removeOnAttachStateChangeListener(this);if(animation!=null){animation.removeAllUpdateListeners();animation.cancel();}}
    }
    static void background(ImageView image, boolean active, boolean reduced) {
        Context c=image.getContext();ThemeLibrary.Entry entry=ThemeLibrary.active();
        boolean video=WallpaperPlaylistChoice.enabled(c) || ThemeLibrary.liveAllowed()
                && ProfileStore.preferences(c).getBoolean(ThemeLibrary.LIVE,false)
                && entry!=null && new File(entry.directory,"video.mp4").isFile();
        if(active)start(image,reduced,video);else stop(image);
    }
    static void settings(Activity a) {
        Config.load(a);String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();
        boolean[] on={ProfileStore.preferences(a).getBoolean(PanZoomPolicy.KEY,false)};
        LinearLayout body=FreedomUi.body(a);
        body.addView(FreedomUi.text(a,"Slow pan and zoom over still pictures. This is whole-picture movement; it does not animate characters or auras. Live video, controls and app tiles stay untouched. Motion pauses away from Home and respects reduced motion and power saving.",17));
        TextView choice=FreedomUi.action(a,body,"Pan & zoom: "+(on[0]?"Gentle":"Off"),()->{});
        choice.setOnClickListener(v->{on[0]=!on[0];choice.setText("Pan & zoom: "+(on[0]?"Gentle":"Off"));});
        final AlertDialog[] dialog={null};
        FreedomUi.action(a,body,"Apply",()->{
            if(!ProductFeatures.allowEdit(a,()->settings(a)))return;
            if(!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll())){
                Toast.makeText(a,"Home changed. Review pan & zoom again.",Toast.LENGTH_LONG).show();return;
            }
            if(on[0]!=ProfileStore.preferences(a).getBoolean(PanZoomPolicy.KEY,false)){
                ProductFeatures.beforeChange(a,"Still picture motion");Map<String,Object> values=new HashMap<>();values.put(PanZoomPolicy.KEY,on[0]);
                values.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);
                if(!SetupValues.commit(ProfileStore.preferences(a),values)){Toast.makeText(a,"Could not save picture motion",Toast.LENGTH_LONG).show();return;}
            }
            dialog[0].dismiss();a.finish();
        });
        dialog[0]=FreedomUi.sheet(a,"Still pictures: Pan & zoom",body);
    }
    private StillMotion() {}
}
