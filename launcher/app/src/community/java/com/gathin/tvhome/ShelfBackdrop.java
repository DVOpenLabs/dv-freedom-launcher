package com.gathin.tvhome;
import android.app.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.*;
import java.util.concurrent.*;

/** Browser-only, opt-in soft artwork. The companion remains responsible for approved HTTPS retrieval. */
final class ShelfBackdrop {
    static final String KEY="shelf_backdrop_v1";
    private static final java.util.Map<Activity,Session> active=new java.util.WeakHashMap<>();
    static void focus(Activity a,String uri){if(!ProfileStore.preferences(a).getBoolean(KEY,false))return;Session s=active.get(a);if(s==null){s=new Session(a);active.put(a,s);}s.load(uri);}
    static void close(Activity a){Session s=active.remove(a);if(s!=null)s.close();}
    static final class Session {
        final Activity a;final String profile;final ExecutorService worker=Executors.newSingleThreadExecutor();final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());volatile int generation;volatile boolean closed;
        Session(Activity a){this.a=a;profile=ProfileStore.id();}
        void load(String uri){final int token=++generation;worker.execute(()->{if(closed||generation!=token)return;Bitmap raw=OptionalPanels.poster(a,uri);if(raw==null)return;if(closed||generation!=token){raw.recycle();return;}Bitmap small=Bitmap.createScaledBitmap(raw,16,9,true);Bitmap baked=Bitmap.createBitmap(64,36,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(baked);canvas.drawBitmap(small,null,new Rect(0,0,64,36),new Paint(Paint.FILTER_BITMAP_FLAG));canvas.drawColor(0xc0081020);if(raw!=small&&raw!=baked)raw.recycle();if(small!=baked)small.recycle();main.post(()->{if(closed||generation!=token||a.isDestroyed()||!profile.equals(ProfileStore.id())){baked.recycle();return;}View content=a.findViewById(android.R.id.content);BitmapDrawable bg=new BitmapDrawable(a.getResources(),baked);bg.setGravity(Gravity.FILL);content.setBackground(bg);});});}
        void close(){closed=true;generation++;worker.shutdownNow();main.removeCallbacksAndMessages(null);a.findViewById(android.R.id.content).setBackgroundColor(0xff111820);}
    }
}
