package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicitly placed, optional Home surfaces. No screen capture or video frame access. */
final class HomeSurfaceStyle implements AutoCloseable {
    private final Activity activity;
    private final FrameLayout root;
    private final java.util.Map<View,Drawable> docks=new java.util.LinkedHashMap<>();
    private final Scene scene;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CancellationSignal cancel = new CancellationSignal();
    private final ExecutorService worker;
    private final boolean glass, palette, weather;
    private Bitmap thumbnail;
    private int accent=0xffb9d5e5;
    int accent(int fallback){return palette?accent:fallback;}
    private Runnable onPaletteChanged;
    void onPaletteChanged(Runnable callback){onPaletteChanged=callback;}
    private volatile boolean closed;
    private String stillKey = "";
    private long stillGeneration;
    private final Runnable refreshTick = new Runnable() {
        @Override public void run() {
            if (closed) return;
            if (glass || palette) refreshStill();
            if (weather) loadWeather();
            main.postDelayed(this, 60_000);
        }
    };

    static boolean coversWallpaper(Context c) {
        return !"current".equals(HomeSurfacePolicy.calm(HomeSurfacePolicy.string(ProfileStore.preferences(c).getAll().get(HomeSurfacePolicy.CALM),"current")));
    }
    static HomeSurfaceStyle attach(Activity a, FrameLayout root, int backgroundIndex, View dock) {
        SharedPreferences p=ProfileStore.preferences(a);
        String calm=HomeSurfacePolicy.calm(HomeSurfacePolicy.string(p.getAll().get(HomeSurfacePolicy.CALM),"current"));
        boolean glass=HomeSurfacePolicy.enabled(p.getAll().get(HomeSurfacePolicy.GLASS)), palette="wallpaper".equals(HomeSurfacePolicy.palette(HomeSurfacePolicy.string(p.getAll().get(HomeSurfacePolicy.PALETTE),"theme")));
        boolean weather=HomeSurfacePolicy.enabled(p.getAll().get(HomeSurfacePolicy.WEATHER));
        if("current".equals(calm)&&!glass&&!palette&&!weather)return null;
        return new HomeSurfaceStyle(a,root,backgroundIndex,dock,calm,glass,palette,weather);
    }
    /** Classic hook: insert immediately behind the first Home content, with grid as the dock target. */
    static HomeSurfaceStyle attach(Activity a) {
        View found=a.findViewById(R.id.root), grid=a.findViewById(R.id.grid);
        if(!(found instanceof FrameLayout))return null;
        FrameLayout root=(FrameLayout)found; View anchor=a.findViewById(R.id.racePanel);
        int index=anchor==null?0:root.indexOfChild(anchor);
        return attach(a,root,Math.max(0,index),grid);
    }
    private HomeSurfaceStyle(Activity a,FrameLayout host,int index,View dock,String calm,boolean glass,boolean palette,boolean weather) {
        activity=a; root=host; this.glass=glass; this.palette=palette; this.weather=weather;
        if(dock!=null)docks.put(dock,dock.getBackground());
        scene=new Scene(a,calm); scene.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(scene,Math.max(0,Math.min(index,root.getChildCount())),new FrameLayout.LayoutParams(-1,-1));
        worker=(glass||palette||weather)?Executors.newSingleThreadExecutor():null;
        if(worker!=null)refreshTick.run();
    }
    private File still() {
        ThemeLibrary.Entry entry=ThemeLibrary.active();
        List<ThemeLibrary.Entry> selected=WallpaperPlaylistChoice.entries(activity,WallpaperPlaylistChoice.read(activity));
        if(WallpaperPlaylistChoice.enabled(activity)&&!selected.isEmpty()) {
            String token=activity.getSharedPreferences(LocalProfiles.filename(ProfileStore.id(),WallpaperPlaylist.PLAYBACK),Context.MODE_PRIVATE).getString("current","");
            entry=selected.get(0); for(ThemeLibrary.Entry item:selected)if(item.token.equals(token))entry=item;
        }
        if (!WallpaperPlaylistChoice.enabled(activity)) {
            Config config=Config.load(activity);
            String current=activity.getSharedPreferences(LocalProfiles.filename(ProfileStore.id(),"wallpaper_rotation"),Context.MODE_PRIVATE).getString("image","");
            for (File candidate:config.wallpapers) if(candidate.getName().equals(current))return candidate;
        }
        File dir=entry==null?Config.dir(activity):entry.directory;
        return dir==null?null:new File(dir,Config.WALLPAPER_NAME);
    }
    private void refreshStill() {
        File file=still();
        String key=file==null?"none":file.getAbsolutePath()+"|"+file.length()+"|"+file.lastModified();
        if (key.equals(stillKey)) return;
        stillKey=key;
        final long generation=++stillGeneration;
        worker.execute(()->{
            Bitmap result=null; int color=0xff243b50;
            try {
                if(file!=null&&file.isFile()&&file.length()<=ThemeArchive.MAX_IMAGE) {
                    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);
                    if(WallpaperSize.accepted(o.outWidth,o.outHeight)) {
                        o.inSampleSize=WallpaperSize.sampleSize(o.outWidth,o.outHeight,64,36);o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;
                        Bitmap raw=BitmapFactory.decodeFile(file.getPath(),o);
                        if(raw!=null){result=Bitmap.createScaledBitmap(raw,64,36,true);if(raw!=result)raw.recycle();
                            int[] pixels=new int[64*36];result.getPixels(pixels,0,64,0,0,64,36);
                            long r=0,g=0,b=0;for(int pixel:pixels){r+=Color.red(pixel);g+=Color.green(pixel);b+=Color.blue(pixel);}
                            color=Color.rgb((int)(r/pixels.length),(int)(g/pixels.length),(int)(b/pixels.length));
                            Bitmap blurred=Bitmap.createBitmap(64,36,Bitmap.Config.RGB_565);int[] out=new int[pixels.length];
                            for(int y=0;y<36;y++)for(int x=0;x<64;x++){int rr=0,gg=0,bb=0,n=0;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){int xx=x+dx,yy=y+dy;if(xx>=0&&xx<64&&yy>=0&&yy<36){int c=pixels[yy*64+xx];rr+=Color.red(c);gg+=Color.green(c);bb+=Color.blue(c);n++;}}out[y*64+x]=Color.rgb(rr/n,gg/n,bb/n);}
                            blurred.setPixels(out,0,64,0,0,64,36);result.recycle();result=blurred;
                        }
                    }
                }
            } catch(RuntimeException|OutOfMemoryError ignored) { if(result!=null){result.recycle();result=null;} }
            final Bitmap image=result; final int sample=color;
            // Never remove this delivery callback: it owns an unattached bitmap until delivery.
            if(closed){if(image!=null)image.recycle();return;}
            boolean posted=main.post(()->{
                if(closed || generation!=stillGeneration){if(image!=null)image.recycle();return;}
                // A displayed bitmap may still be in RenderThread; release its reference, not recycle it.
                thumbnail=glass && !docks.isEmpty()?image:null;
                if(thumbnail==null && image!=null)image.recycle();
                applyDock(sample);
                if(onPaletteChanged!=null)onPaletteChanged.run();
            });
            if(!posted && image!=null)image.recycle();
        });
    }
    void addDock(View dock) {
        if(closed||dock==null||docks.containsKey(dock))return;
        docks.put(dock,dock.getBackground());
        if(glass||palette)applyDock(lastSample);
    }
    private int lastSample=0xff243b50;
    private void applyDock(int sample) {
        lastSample=sample;
        int tint=HomeSurfacePolicy.dock(palette?sample:0xff243b50);
        accent=palette?HomeSurfacePolicy.accent(sample):0xffb9d5e5;
        for(View dock:docks.keySet()) {
            GradientDrawable material=new GradientDrawable();material.setColor(tint);material.setCornerRadius(18*activity.getResources().getDisplayMetrics().density);material.setStroke(1,accent);
            if(glass&&thumbnail!=null){BitmapDrawable picture=new BitmapDrawable(activity.getResources(),thumbnail);picture.setGravity(android.view.Gravity.FILL);dock.setBackground(new RoundedGlass(new LayerDrawable(new Drawable[]{picture,material}),18*activity.getResources().getDisplayMetrics().density));}
            else if(glass||palette)dock.setBackground(material);
        }
    }
    /** Clips the still and tint as one drawable without changing the owner view's clip state. */
    private static final class RoundedGlass extends Drawable {
        private final Drawable content;
        private final float radius;
        private final Path clip=new Path();
        private final RectF bounds=new RectF();
        RoundedGlass(Drawable content,float radius){this.content=content;this.radius=radius;}
        @Override protected void onBoundsChange(Rect rect){
            content.setBounds(rect);bounds.set(rect);clip.reset();clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);
        }
        @Override public void draw(Canvas canvas){int saved=canvas.save();canvas.clipPath(clip);content.draw(canvas);canvas.restoreToCount(saved);}
        @Override public void setAlpha(int alpha){content.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){content.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private void loadWeather() {
        if(closed||!weather)return;
        worker.execute(()->{WeatherSnapshot snapshot=WeatherClient.trustedSnapshot(activity,cancel);String effect=HomeSurfacePolicy.weather(snapshot,System.currentTimeMillis());
            main.post(()->{if(closed)return;scene.effect=effect;scene.invalidate();});});
    }
    @Override public void close() {
        if(closed)return;closed=true;main.removeCallbacks(refreshTick);cancel.cancel();if(worker!=null)worker.shutdownNow();
        root.removeView(scene);for(java.util.Map.Entry<View,Drawable> item:docks.entrySet())item.getKey().setBackground(item.getValue());docks.clear();onPaletteChanged=null;thumbnail=null;
    }
    private static final class Scene extends View {
        final String calm;String effect="none";final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Scene(Context c,String calm){super(c);this.calm=calm;setFocusable(false);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();
            if("onyx".equals(calm)){c.drawColor(Color.BLACK);}
            else if(!"current".equals(calm)){
                int top="orba".equals(calm)?0xff172a32:0xff10192e;
                int bottom="orba".equals(calm)?0xff334848:0xff202b48;
                p.setShader(new LinearGradient(0,0,w,h,top,bottom,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
                p.setColor(0x183fc7c0);c.drawCircle(w*.76f,h*.25f,Math.min(w,h)*.25f,p);p.setColor(0x102a72ce);c.drawCircle(w*.17f,h*.8f,Math.min(w,h)*.38f,p);
            }
            if("rain".equals(effect)){p.setColor(0x397cc1e5);p.setStrokeWidth(2);for(int i=0;i<32;i++){float x=((i*127)%997)/997f*w,y=((i*223)%991)/991f*h;c.drawLine(x,y,x-5,y+22,p);}}
            else if("mist".equals(effect)){p.setColor(0x20d1e4eb);for(int i=0;i<3;i++)c.drawOval(-w*.1f,h*(.35f+i*.18f),w*1.1f,h*(.47f+i*.18f),p);}
            else if("sun".equals(effect)){p.setColor(0x28ffdf98);c.drawCircle(w*.82f,h*.17f,Math.min(w,h)*.14f,p);}
        }
    }
}
