package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import java.io.File;
import java.util.*;

/** Only immutable, already bounded private artwork is cached. Originals remain Android's responsibility. */
final class ArtworkCache {
    static final int MAX_BYTES=2*1024*1024;
    private static final WeightedCache<String,Bitmap> CACHE=new WeightedCache<>(MAX_BYTES,12);
    private static String owner="";private static long hits,misses,decodes,nanos;
    static synchronized Bitmap read(Context c,File file){
        String scope=ProfileStore.id()+":"+ProfileStore.preferences(c).getLong(Config.SETTINGS_REVISION_KEY,0);
        if(!scope.equals(owner)){CACHE.clear();owner=scope;}
        if(!file.isFile()||file.length()<1||file.length()>1024*1024)return null;
        String key=file.getAbsolutePath()+":"+file.length()+":"+file.lastModified()+":"+c.getResources().getDisplayMetrics().densityDpi;
        Bitmap cached=CACHE.get(key);if(cached!=null&&!cached.isRecycled()){hits++;return cached;}misses++;
        long began=System.nanoTime();
        try{
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);
            if(o.outWidth<1||o.outHeight<1||o.outWidth>960||o.outHeight>540)return null;
            o.inJustDecodeBounds=false;o.inSampleSize=2;o.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap bitmap=BitmapFactory.decodeFile(file.getPath(),o);
            if(bitmap!=null){decodes++;CACHE.put(key,bitmap,bitmap.getAllocationByteCount());}return bitmap;
        }catch(RuntimeException|OutOfMemoryError unavailable){return null;}
        finally{nanos+=System.nanoTime()-began;}
    }
    static synchronized void clear(){CACHE.clear();owner="";}
    /** Aggregate diagnostics only; no package names, paths or user artwork leave this class. */
    static synchronized Map<String,Long> stats(){Map<String,Long> out=new LinkedHashMap<>();out.put("hits",hits);out.put("misses",misses);out.put("decodes",decodes);out.put("decodeNanos",nanos);out.put("bytes",(long)CACHE.bytes());out.put("entries",(long)CACHE.size());return out;}
    private ArtworkCache(){}
}
