package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.util.Log;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Small immutable-pack library. ZIPs and personal media stay outside the APK. */
final class ThemeLibrary {
    static final String CHOICE="theme_pack_id", LIVE="live_wallpaper";
    static final int MAX_PACKS=8;
    static final long MAX_LIBRARY_BYTES=128L*1024*1024;
    private static boolean enabled, liveAllowed;
    private static Entry active;
    static final class Entry {
        final String token; final File directory; final ThemeArchive.Pack pack;
        Entry(String token,File directory,ThemeArchive.Pack pack){this.token=token;this.directory=directory;this.pack=pack;}
    }
    interface Validator {void validate(File directory)throws IOException;}
    static boolean enabled(){return enabled;}
    static boolean liveAllowed(){return liveAllowed;}
    static Entry active(){return active;}
    static JSONObject configure(Context ctx,boolean on,boolean video) {
        enabled=on;liveAllowed=video;active=null;
        if(!on)return null;
        String token=ProfileStore.preferences(ctx).getString(CHOICE,"");
        if(validToken(token))try{
            File directory=new File(root(ctx),token);
            active=new Entry(token,directory,ThemeArchive.read(directory));
            return new JSONObject(active.pack.visual);
        }catch(Exception e){Log.w("TvHome.Themes","Selected pack unavailable; keeping original theme");}
        return null;
    }
    private static File root(Context ctx){return new File(ctx.getFilesDir(),"product-themes");}
    static JSONObject merge(JSONObject base,JSONObject visual){
        JSONObject result=base==null?new JSONObject():base;
        java.util.Iterator<String> keys=visual.keys();
        while(keys.hasNext()){String key=keys.next();try{result.put(key,visual.opt(key));}catch(org.json.JSONException impossible){throw new IllegalArgumentException(impossible);}}
        return result;
    }
    static List<Entry> list(Context ctx){return list(root(ctx));}
    static List<Entry> list(File root){
        List<Entry> rows=new ArrayList<>();File[] files=root.listFiles();
        if(files!=null)for(File file:files)if(validToken(file.getName())&&file.isDirectory()&&!Files.isSymbolicLink(file.toPath()))try{
            rows.add(new Entry(file.getName(),file,ThemeArchive.read(file)));
        }catch(IOException ignored){}
        rows.sort(Comparator.comparing(e->e.pack.name));return rows;
    }
    static boolean validToken(String token){return token!=null&&token.matches("p-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");}
    static Entry importPack(Context ctx,InputStream stream)throws IOException{
        return importPack(root(ctx),stream,ThemeLibrary::validateImages);
    }
    static Entry importPack(File root,InputStream stream,Validator validator)throws IOException{
        if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Cannot create library");
        // Only the single import worker calls this; a previous process's unpublished staging is disposable.
        File[] stale=root.listFiles();
        if(stale!=null)for(File file:stale)if(file.getName().matches("\\.import-[a-f0-9-]{36}"))removeTree(file);
        if(list(root).size()>=MAX_PACKS)throw new IOException("Theme library is full");
        File staging=new File(root,".import-"+UUID.randomUUID());
        if(!staging.mkdir())throw new IOException("Cannot stage theme");
        try{
            ThemeArchive.Pack pack=ThemeArchive.extract(stream,staging);validator.validate(staging);
            if(bytes(root)>MAX_LIBRARY_BYTES)throw new IOException("Theme library storage limit reached");
            String token="p-"+UUID.randomUUID();File target=new File(root,token);
            if(!staging.renameTo(target))throw new IOException("Cannot publish theme");
            return new Entry(token,target,pack);
        }finally{if(staging.exists())removeTree(staging);}
    }
    private static void validateImages(File directory)throws IOException{
        File[] entries=directory.listFiles();if(entries==null)throw new IOException("Unreadable theme");
        for(File file:entries){
            if(file.isDirectory()){validateImages(file);continue;}
            if(file.getName().matches("(?i).+\\.(jpg|jpeg|png|webp)")){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeFile(file.getAbsolutePath(),options);
                if(!WallpaperSize.accepted(options.outWidth,options.outHeight))throw new IOException("Invalid or oversized theme image");
            }
        }
    }
    static long bytes(File file)throws IOException{
        if(Files.isSymbolicLink(file.toPath()))throw new IOException("Unexpected symlink");
        if(!file.isDirectory())return file.length();long total=0;File[] files=file.listFiles();
        if(files==null)throw new IOException("Unreadable directory");
        for(File child:files){total+=bytes(child);if(total>MAX_LIBRARY_BYTES)return total;}return total;
    }
    static void removeTree(File file)throws IOException{
        if(file.isDirectory()&&!Files.isSymbolicLink(file.toPath())){
            File[] files=file.listFiles();if(files==null)throw new IOException("Unreadable staging");
            for(File child:files)removeTree(child);
        }
        if(!file.delete()&&file.exists())throw new IOException("Cannot remove unused theme data");
    }
    static boolean select(Context ctx,String token){
        if(!enabled)return false;
        if(!token.isEmpty()&&list(ctx).stream().noneMatch(e->e.token.equals(token)))return false;
        return saveChoice(ProfileStore.preferences(ctx),token);
    }
    static boolean saveChoice(SharedPreferences p,String token){
        if(!token.isEmpty()&&!validToken(token))return false;
        String old=p.getString(CHOICE,"");long revision=p.getLong(Config.SETTINGS_REVISION_KEY,0);
        if(p.edit().putString(CHOICE,token).putLong(Config.SETTINGS_REVISION_KEY,revision+1).commit())return true;
        p.edit().putString(CHOICE,old).putLong(Config.SETTINGS_REVISION_KEY,revision).commit();return false;
    }
    static void remove(Context ctx,Entry entry)throws IOException{
        SharedPreferences global=ctx.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);
        for(Map.Entry<String,?> value:global.getAll().entrySet())
            if((value.getKey().equals(CHOICE)||value.getKey().matches("profile\\.[a-z][a-z0-9_-]{0,63}\\.theme_pack_id"))&&entry.token.equals(value.getValue()))throw new IOException("Theme is used by a profile");
        File expected=new File(root(ctx),entry.token);
        if(!validToken(entry.token)||!expected.getCanonicalFile().equals(entry.directory.getCanonicalFile()))throw new IOException("Invalid theme");
        removeTree(expected);
    }
    private ThemeLibrary(){}
}
