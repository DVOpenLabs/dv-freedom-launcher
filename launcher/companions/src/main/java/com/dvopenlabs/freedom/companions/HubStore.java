package com.dvopenlabs.freedom.companions;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

final class HubStore {
    static final Uri URI=Uri.parse("content://com.dvopenlabs.freedom.companions.snapshot/current");
    static final ThreadPoolExecutor WORK=new ThreadPoolExecutor(1,1,20,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),new ThreadPoolExecutor.DiscardPolicy());
    static final Set<String> pending=new HashSet<>();
    static final PosterBackoff retry=new PosterBackoff();
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("private",0);}
    static synchronized void refresh(Context context){
        Context c=context.getApplicationContext();var p=prefs(c);
        if(!p.getBoolean("calendar",false)||System.currentTimeMillis()-p.getLong("attempt",0)<12*3600000L||pending.contains("calendar"))return;
        String family=p.getString("family",""),sports=p.getString("sports","");pending.add("calendar");p.edit().putLong("attempt",System.currentTimeMillis()).apply();
        WORK.execute(()->{try{JSONArray result=new JSONArray();for(String key:new String[]{"family","sports"}){String url=key.equals("family")?family:sports;if(url.isEmpty())continue;String body=new String(Network.request(url,null,null,false,1024*1024),StandardCharsets.UTF_8);JSONArray events=CalendarFeed.parse(body,System.currentTimeMillis(),key.equals("sports")?"Game Day":"Family");for(int i=0;i<events.length();i++)result.put(events.get(i));}
            if(p.getBoolean("calendar",false)&&family.equals(p.getString("family",""))&&sports.equals(p.getString("sports","")))p.edit().putString("events",EventDisplay.merge(result,System.currentTimeMillis()).toString()).putLong("updated",System.currentTimeMillis()).putBoolean("failed",false).apply();
        }catch(Exception unavailable){p.edit().putBoolean("failed",true).apply();}finally{synchronized(HubStore.class){pending.remove("calendar");}c.getContentResolver().notifyChange(URI,null);}});
    }
    static JSONArray events(Context c){try{return new JSONArray(prefs(c).getString("events","[]"));}catch(Exception bad){return new JSONArray();}}
    static String hash(String url)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static File folder(Context c){File f=new File(c.getCacheDir(),"posters");f.mkdirs();return f;}
    static synchronized String poster(Context context,String url){
        Context c=context.getApplicationContext();if(!prefs(c).getBoolean("posters",false))return "";
        try{Network.url(url,false);String key=hash(url);File file=new File(folder(c),key+".jpg");if(file.isFile()){file.setLastModified(System.currentTimeMillis());return key;}
            if(!retry.ready(key,System.currentTimeMillis())||pending.size()>=8||!pending.add(key))return "";
            WORK.execute(()->{Bitmap source=null,scaled=null;try{byte[] bytes=Network.request(url,null,null,false,2*1024*1024);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<1||o.outHeight<1||(long)o.outWidth*o.outHeight>12000000)throw new IOException();o.inJustDecodeBounds=false;o.inSampleSize=Math.max(1,Math.max(o.outWidth/768,o.outHeight/432));source=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(source==null)throw new IOException();double factor=Math.min(1,Math.min(384d/source.getWidth(),216d/source.getHeight()));scaled=Bitmap.createScaledBitmap(source,Math.max(1,(int)(source.getWidth()*factor)),Math.max(1,(int)(source.getHeight()*factor)),true);
                File temp=new File(folder(c),key+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){if(!scaled.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException();}
                if(prefs(c).getBoolean("posters",false))temp.renameTo(file);else temp.delete();trim(c);
            }catch(Exception unavailable){retry.failed(key,System.currentTimeMillis());/* No URLs or tokens in logs. App banner is the fallback. */}finally{if(scaled!=null&&scaled!=source)scaled.recycle();if(source!=null)source.recycle();synchronized(HubStore.class){pending.remove(key);}c.getContentResolver().notifyChange(URI,null);}});
        }catch(Exception bad){return "";}return "";
    }
    static synchronized void trim(Context c){File[] files=folder(c).listFiles();if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long bytes=0;for(File f:files)bytes+=f.length();for(File f:files)if(bytes>16*1024*1024){long size=f.length();if(f.delete())bytes-=size;}}
    static void clear(Context c){retry.clear();File[] files=folder(c).listFiles();if(files!=null)for(File f:files)f.delete();prefs(c).edit().remove("events").remove("updated").apply();}
}
