package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.File;

/** Adapted from csermet's MIT LiveVideo: local-only, permanently silent, still fallback, no timer/service. */
final class LiveVideo implements TextureView.SurfaceTextureListener {
    private final TextureView view;
    private final FrameLayout box;
    private final File file;
    private final Context context;
    private final PlaybackGate gate=new PlaybackGate();
    private MediaPlayer player;private Surface surface;
    private boolean visible;private int position,videoWidth,videoHeight;
    static boolean supported(Context ctx,boolean reducedMotion){
        ActivityManager manager=(ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager==null)return false;ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();manager.getMemoryInfo(info);
        return PlaybackGate.supported(info.totalMem,manager.isLowRamDevice(),reducedMotion);
    }
    LiveVideo(Context ctx,File file,int dim){
        context=ctx;this.file=file;box=new FrameLayout(ctx);box.setAlpha(0);
        box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view=new TextureView(ctx);view.setSurfaceTextureListener(this);
        box.addView(view,new FrameLayout.LayoutParams(-1,-1));View shade=new View(ctx);
        shade.setBackgroundColor(0xFF000000);shade.setAlpha(Math.max(0,Math.min(90,dim))/100f);
        box.addView(shade,new FrameLayout.LayoutParams(-1,-1));
    }
    View view(){return box;}
    void start(){visible=true;if(surface!=null&&player==null&&!gate.failed())open();}
    void stop(){visible=false;release();}
    void close(){stop();if(surface!=null){surface.release();surface=null;}}
    private void release(){
        gate.stop();box.setAlpha(0);
        if(player!=null){try{position=player.getCurrentPosition();}catch(IllegalStateException ignored){}player.release();player=null;}
    }
    private void failed(int token){
        if(!gate.fail(token))return;release();
        if(visible)Toast.makeText(context,R.string.live_wallpaper_failed,Toast.LENGTH_LONG).show();
    }
    private void open(){
        final int token=gate.begin();
        try{
            MediaPlayer next=new MediaPlayer();player=next;
            // Keep a muted soundtrack out of Android's music-active signal. Gain remains permanently zero.
            next.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build());
            next.setDataSource(file.getAbsolutePath());next.setSurface(surface);next.setLooping(true);
            next.setVolume(0f,0f); // No audio toggle, audio focus request or system-volume change.
            next.setOnPreparedListener(mp->{
                if(mp!=player||!gate.accepts(token))return;
                try{if(position>0)mp.seekTo(position);mp.start();}catch(RuntimeException e){failed(token);}
            });
            next.setOnVideoSizeChangedListener((mp,w,h)->{if(mp==player&&gate.accepts(token)){videoWidth=w;videoHeight=h;transform();}});
            next.setOnInfoListener((mp,what,extra)->{if(mp==player&&gate.accepts(token)&&what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)box.setAlpha(1);return false;});
            next.setOnErrorListener((mp,what,extra)->{if(mp==player)failed(token);return true;});
            next.prepareAsync();
        }catch(Exception e){failed(token);}
    }
    private void transform(){
        int width=view.getWidth(),height=view.getHeight();if(width<=0||height<=0||videoWidth<=0||videoHeight<=0)return;
        float aspect=(float)videoWidth/videoHeight,target=(float)width/height;
        Matrix matrix=new Matrix();matrix.setScale(aspect>target?aspect/target:1,aspect<target?target/aspect:1,width/2f,height/2f);view.setTransform(matrix);
    }
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int w,int h){surface=new Surface(texture);if(visible&&!gate.failed())open();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){release();if(surface!=null){surface.release();surface=null;}return true;}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int w,int h){transform();}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){}
}
