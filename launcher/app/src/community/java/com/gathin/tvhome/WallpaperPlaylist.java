package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** One visible decoder, a still fallback and a resumed-Home-only deadline. No theme selection writes. */
final class WallpaperPlaylist {
    static final String PLAYBACK="wallpaper_playlist_playback";
    private static final Map<Activity,WallpaperPlaylist> homes=new WeakHashMap<>();
    private final Activity activity;private final FrameLayout root,layer;private final ImageView poster;
    private final String profile,raw;private final PlaylistPolicy.Spec spec;private final Config config;private final SharedPreferences playback;
    private final List<ThemeLibrary.Entry> entries;private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService decoder=Executors.newSingleThreadExecutor();private final Random random=new Random();private final PlaylistPolicy.Clock clock;
    private LiveVideo video;private Bitmap bitmap;private int index,displayIndex,generation;private String scene="";private boolean active,closed,loaded;
    private final Runnable deadline=this::onDeadline;
    private final Runnable scenePulse=new Runnable(){public void run(){if(!active||closed)return;updateScene();schedule();handler.postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    private void updateScene(){String wanted=SceneSchedules.current(activity);int target=index;for(int i=0;i<entries.size();i++)if(entries.get(i).token.equals(wanted))target=i;if(!wanted.isEmpty()&&target==index&&!entries.get(target).token.equals(wanted))wanted="";boolean was=!scene.isEmpty();scene=wanted;if(scene.isEmpty()){if(was)clock.start(SystemClock.elapsedRealtime());target=index;}else clock.stop(SystemClock.elapsedRealtime());if(displayIndex!=target){displayIndex=target;loaded=false;}if(!loaded)load();}

    private void onDeadline(){if(active&&!closed&&clock.due(SystemClock.elapsedRealtime()))next();}
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
        public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}
        public void onActivityResumed(Activity a){} // attach() runs after the Home's own resume/recreate decision.
        public void onActivityPaused(Activity a){if(a==activity)stop();}
        public void onActivityDestroyed(Activity a){if(a==activity)close();}
    };
    static void attach(Activity a){
        View view=a.findViewById(R.id.root);if(!(view instanceof FrameLayout))return;
        String raw=ProfileStore.preferences(a).getString(WallpaperPlaylistChoice.KEY,"");WallpaperPlaylist prior=homes.get(a);
        if(prior!=null&&(!prior.raw.equals(raw)||!prior.profile.equals(ProfileStore.id())||prior.layer.getParent()!=view)){prior.close();prior=null;}
        if(!WallpaperPlaylistChoice.enabled(a)){if(prior!=null)prior.close();return;}
        if(prior==null){prior=new WallpaperPlaylist(a,(FrameLayout)view,Config.load(a));homes.put(a,prior);}prior.start();
    }
    private WallpaperPlaylist(Activity a,FrameLayout root,Config config){
        activity=a;this.root=root;this.config=config;profile=ProfileStore.id();raw=ProfileStore.preferences(a).getString(WallpaperPlaylistChoice.KEY,"");spec=PlaylistPolicy.parse(raw);entries=WallpaperPlaylistChoice.entries(a,spec);
        playback=a.getSharedPreferences(LocalProfiles.filename(profile,PLAYBACK),Context.MODE_PRIVATE);boolean same=raw.equals(playback.getString("spec",""));
        String last=same?playback.getString("current",""):"";for(int i=0;i<entries.size();i++)if(entries.get(i).token.equals(last))index=i;
        long period=spec.seconds*1000L;clock=new PlaylistPolicy.Clock(period,same?playback.getLong("remaining",period):period);
        layer=new FrameLayout(a);layer.setBackgroundColor(config.backgroundColor);layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);layer.setFocusable(false);layer.setClickable(false);
        poster=new ImageView(a);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);layer.addView(poster,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(a);shade.setBackgroundColor(0xff000000);shade.setAlpha(Math.max(0,Math.min(90,config.wallpaperDim))/100f);layer.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        int behindChrome=root.getChildCount()>0&&root.getChildAt(0) instanceof ImageView?1:0;
        root.addView(layer,behindChrome,new FrameLayout.LayoutParams(-1,-1));a.getApplication().registerActivityLifecycleCallbacks(lifecycle);
    }
    private void start(){
        if(active||closed)return;
        if(raw.equals(playback.getString("spec",""))){String current=playback.getString("current","");for(int i=0;i<entries.size();i++)if(entries.get(i).token.equals(current)&&i!=index){index=i;loaded=false;poster.setImageDrawable(null);bitmap=null;}clock.restore(playback.getLong("remaining",clock.period));}
        active=true;updateScene();if(scene.isEmpty())clock.start(SystemClock.elapsedRealtime());if(loaded&&video!=null)video.start();schedule();handler.removeCallbacks(scenePulse);handler.postDelayed(scenePulse,60000-System.currentTimeMillis()%60000);
    }
    private void stop(){if(!active)return;clock.stop(SystemClock.elapsedRealtime());active=false;generation++;handler.removeCallbacks(deadline);handler.removeCallbacks(scenePulse);if(video!=null)video.stop();save();}
    private void save(){playback.edit().putString("spec",raw).putString("current",entries.get(index).token).putLong("remaining",clock.remaining(SystemClock.elapsedRealtime())).apply();}
    private void schedule(){handler.removeCallbacks(deadline);if(active&&!closed&&scene.isEmpty()&&entries.size()>1)handler.postDelayed(deadline,Math.max(1,clock.remaining(SystemClock.elapsedRealtime())));}
    private void next(){index=PlaylistPolicy.next(entries.size(),index,spec.shuffle,random);clock.reset(SystemClock.elapsedRealtime());loaded=false;displayIndex=index;load();save();schedule();}
    private void load(){
        int token=++generation;ThemeLibrary.Entry entry=entries.get(displayIndex);if(video!=null){video.close();layer.removeView(video.view());video=null;}
        decoder.execute(()->{Bitmap decoded=decode(new File(entry.directory,Config.WALLPAPER_NAME));handler.post(()->{
            if(closed||!active||generation!=token){if(decoded!=null)decoded.recycle();return;}
            loaded=true;if(decoded!=null){Bitmap old=bitmap;bitmap=decoded;poster.setImageBitmap(decoded);ProductFeatures.wallpaperSample(activity,decoded);if(old!=null&&old!=decoded)poster.postDelayed(old::recycle,500);}
            File file=new File(entry.directory,"video.mp4");
            if(ThemeLibrary.liveAllowed()&&ProfileStore.preferences(activity).getBoolean(ThemeLibrary.LIVE,false)&&file.isFile()&&LiveVideo.supported(activity,config.reducedMotion)){
                video=new LiveVideo(activity,file,0);layer.addView(video.view(),1,new FrameLayout.LayoutParams(-1,-1));video.start();
            }
        });});
    }
    private Bitmap decode(File file){
        try{if(!file.isFile()||file.length()>ThemeArchive.MAX_IMAGE)return null;BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);if(!WallpaperSize.accepted(o.outWidth,o.outHeight))return null;
            int width=Math.max(1,root.getResources().getDisplayMetrics().widthPixels),height=Math.max(1,root.getResources().getDisplayMetrics().heightPixels);o.inSampleSize=WallpaperSize.sampleSize(o.outWidth,o.outHeight,width,height);o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(file.getPath(),o);
        }catch(RuntimeException|OutOfMemoryError unavailable){return null;}
    }
    private void close(){if(closed)return;stop();closed=true;generation++;handler.removeCallbacksAndMessages(null);decoder.shutdownNow();if(video!=null){video.close();video=null;}poster.setImageDrawable(null);bitmap=null;root.removeView(layer);activity.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);homes.remove(activity);}
    /** Internal diagnostics; no exported endpoint in the product. The private shell-only pilot reads it. */
    static Map<String,Object> status(){Map<String,Object> out=new HashMap<>();int allocated=0;for(WallpaperPlaylist p:homes.values())if(p.video!=null&&p.video.decoderAllocated())allocated++;out.put("allocatedDecoders",allocated);for(WallpaperPlaylist p:homes.values())if(p.active&&!p.closed){out.put("active",true);out.put("profile",p.profile);out.put("current",p.entries.get(p.displayIndex).token);out.put("name",p.entries.get(p.displayIndex).pack.name);out.put("remainingMs",p.clock.remaining(SystemClock.elapsedRealtime()));out.put("seconds",p.spec.seconds);out.put("items",p.entries.size());out.put("video",p.video!=null);out.put("scheduled",!p.scene.isEmpty());return out;}out.put("active",false);return out;}
    private WallpaperPlaylist(){throw new AssertionError();}
}
