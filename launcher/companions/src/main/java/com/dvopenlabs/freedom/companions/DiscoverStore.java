package com.dvopenlabs.freedom.companions;

import android.content.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import okhttp3.HttpUrl;

/** Explicit-user TMDB requests, no streaming account access. Fixed endpoint and bounded disk cache. */
final class DiscoverStore {
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("discover",0);}
    static boolean key(String key){return key!=null&&key.length()>=20&&key.length()<=2048&&!key.matches("(?s).*\\s.*");}
    static boolean region(String s){return s!=null&&s.matches("[A-Z]{2}");}
    static String token(JSONObject x){String type=x.optString("media_type",x.has("title")?"movie":"tv");int id=x.optInt("id");if(!(type.equals("movie")||type.equals("tv"))||id<1)throw new IllegalArgumentException("Invalid title");return type+"/"+id;}
    static String title(JSONObject x){return x.optString("title",x.optString("name","Untitled"));}
    static String endpoint(String path,Map<String,String> params,String apiKey){if(!path.matches("(discover/(movie|tv)|search/multi|(movie|tv)/[1-9][0-9]{0,9}(/watch/providers)?)"))throw new IllegalArgumentException("Unsupported catalogue route");HttpUrl.Builder b=HttpUrl.get("https://api.themoviedb.org/3/"+path).newBuilder();for(Map.Entry<String,String> p:params.entrySet())b.addQueryParameter(p.getKey(),p.getValue());if(!apiKey.startsWith("eyJ"))b.addQueryParameter("api_key",apiKey);return b.build().toString();}
    static JSONObject request(Context c,String path,Map<String,String> params)throws Exception{
        var p=prefs(c);String key=p.getString("key","");if(!p.getBoolean("enabled",false)||!key(key))throw new IllegalStateException("Connect TMDB first");String url=endpoint(path,params,key),id=hash(path+params.toString()+p.getString("region","US"));File dir=new File(c.getCacheDir(),"discover");dir.mkdirs();File file=new File(dir,id+".json");
        if(file.isFile()&&file.length()<=1024*1024&&System.currentTimeMillis()-file.lastModified()<6*3600000L)return new JSONObject(new String(read(file),StandardCharsets.UTF_8));
        try{byte[] data=Network.request(url,key.startsWith("eyJ")?key:null,null,false,1024*1024);JSONObject value=new JSONObject(new String(data,StandardCharsets.UTF_8));try(FileOutputStream out=new FileOutputStream(file)){out.write(data);}trim(dir);return value;}catch(Exception unavailable){if(file.isFile()&&file.length()<=1024*1024){JSONObject saved=new JSONObject(new String(read(file),StandardCharsets.UTF_8));saved.put("dv_saved",true);return saved;}throw unavailable;}
    }
    static byte[] read(File f)throws IOException{if(f.length()>1024*1024)throw new IOException();try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;){if(out.size()+n>1024*1024)throw new IOException();out.write(b,0,n);}return out.toByteArray();}}
    static String hash(String s)throws Exception{StringBuilder b=new StringBuilder();for(byte x:MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)))b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
    static void trim(File dir){File[] files=dir.listFiles();if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());long size=0;for(File f:files){size+=f.length();if(size>4L*1024*1024)f.delete();}}
    static void clear(Context c){File[] files=new File(c.getCacheDir(),"discover").listFiles();if(files!=null)for(File f:files)if(f.getName().matches("[a-f0-9]{64}\\.json"))f.delete();}
    private DiscoverStore(){}
}
