package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Community-only recovery history. No listeners, services, media copies or TV settings. */
final class HomeHistory {
    private static final String TAG="TvHome.History",HISTORY="home-history.json",PENDING="home-history-pending.json",JOURNAL="home-history-restore.json";
    private static final String[] UNDO={HomeStyles.UNDO,DesktopLayout.UNDO,DesktopLayout.GUARD,DesktopLayout.UNDO_POSITION};
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Set<String> scheduled=new HashSet<>();
    private static boolean busy;

    /** Repair a persisted interrupted restore before ordinary mutation hooks are admitted. */
    static boolean initialize(Context context){
        Context app=context.getApplicationContext();
        boolean repaired=false;
        try{
            AtomicFile journal=journal(app);
            if(exists(journal)){
                JSONObject value=object(read(journal,HomeHistoryPolicy.MAX_BYTES*4));
                if(!value.optBoolean("committed",false)){busy=true;rollback(app,value);repaired=true;}
                journal.delete();
                if(exists(journal))throw new IOException("Recovery journal could not be cleared");
            }
        }catch(Exception failure){Log.w(TAG,"Keeping unfinished restore journal for recovery",failure);}
        finally{busy=false;}
        MAIN.post(()->finishPending(app,ProfileStore.id()));
        // Main may have read Config before its product hook. Recreate only after genuine recovery.
        if(repaired&&context instanceof Activity){Activity activity=(Activity)context;MAIN.post(()->{if(!activity.isFinishing()&&!activity.isDestroyed())activity.recreate();});}
        else if(repaired)MAIN.post(()->app.getContentResolver().notifyChange(DesktopLayout.uri(app),null));
        return repaired;
    }

    /** Call only after validation and immediately before a real main-thread mutation. */
    static void before(Context context,String reason){
        if(busy||Looper.myLooper()!=Looper.getMainLooper())return;
        Context app=context.getApplicationContext();String profile=ProfileStore.id();
        if(!HomeHistoryPolicy.profile(profile)||scheduled.contains(profile))return;
        try{
            if(exists(journal(app)))return; // A failed recovery must not be overwritten by another edit.
            finishPending(app,profile);
            if(exists(file(app,profile,PENDING)))return;
            JSONObject snapshot=capture(app);
            JSONObject pending=new JSONObject().put("schema",1).put("profile",profile).put("time",System.currentTimeMillis())
                    .put("id",UUID.randomUUID().toString()).put("reason",HomeHistoryPolicy.reason(reason)).put("snapshot",snapshot);
            byte[] bytes=utf(pending.toString());if(bytes.length>HomeHistoryPolicy.MAX_BYTES-3)return;
            write(file(app,profile,PENDING),bytes);
            scheduled.add(profile);
            MAIN.post(()->{scheduled.remove(profile);finishPending(app,profile);});
        }catch(Exception failure){Log.w(TAG,"Could not stage Home recovery point; current Home is unchanged by history",failure);}
    }

    private static void finishPending(Context context,String profile){
        if(busy||!HomeHistoryPolicy.profile(profile)||!profile.equals(ProfileStore.id()))return;
        AtomicFile pending=file(context,profile,PENDING);
        if(!exists(pending))return;
        try{
            JSONObject entry=object(read(pending,HomeHistoryPolicy.MAX_BYTES));validateEntry(entry,profile);
            JSONObject now=capture(context);
            if(!same(entry.getJSONObject("snapshot"),now))append(context,profile,entry);
            pending.delete();
        }catch(Exception failure){Log.w(TAG,"Keeping pending recovery point; it will be retried",failure);}
    }

    private static JSONObject capture(Context context)throws Exception{
        Map<String,Object> values=SetupStore.capture(context);
        JSONObject files=new JSONObject();
        for(String name:HomeHistoryPolicy.FILES)files.put(name,encoded(owned(context,ProfileStore.id(),name)));
        JSONObject result=new JSONObject().put("values",new JSONObject(values)).put("files",files);
        validateSnapshot(result);return result;
    }

    private static List<JSONObject> history(Context context,String profile)throws Exception{
        AtomicFile source=file(context,profile,HISTORY);List<JSONObject> rows=new ArrayList<>();
        if(!exists(source))return rows;
        JSONArray values=new JSONArray(new String(read(source,HomeHistoryPolicy.MAX_BYTES),StandardCharsets.UTF_8));
        if(values.length()>HomeHistoryPolicy.LIMIT)throw new IOException("Too many recovery points");
        for(int i=0;i<values.length();i++){JSONObject entry=values.getJSONObject(i);validateEntry(entry,profile);rows.add(entry);}
        return rows;
    }

    private static void append(Context context,String profile,JSONObject entry)throws Exception{
        List<JSONObject> rows=history(context,profile);
        if(!rows.isEmpty()&&rows.get(rows.size()-1).getString("id").equals(entry.getString("id")))return;
        if(!rows.isEmpty()&&same(rows.get(rows.size()-1).getJSONObject("snapshot"),entry.getJSONObject("snapshot")))return;
        List<String> encoded=new ArrayList<>();for(JSONObject row:rows)encoded.add(row.toString());encoded.add(entry.toString());
        List<String> bounded=HomeHistoryPolicy.trim(encoded);JSONArray result=new JSONArray();
        for(String row:bounded)result.put(new JSONObject(row));
        if(result.length()==0)throw new IOException("Recovery point exceeds the storage limit");
        write(file(context,profile,HISTORY),utf(result.toString()));
    }

    static void show(Activity activity){
        String profile=ProfileStore.id();finishPending(activity,profile);
        try{
            List<JSONObject> rows=history(activity,profile);Collections.reverse(rows);
            if(rows.isEmpty()){new AlertDialog.Builder(activity).setTitle("Recent changes").setMessage("Recovery points appear after you change this profile’s Home. Browsing settings creates none.").setPositiveButton(android.R.string.ok,null).show();return;}
            String[] labels=new String[rows.size()];DateFormat date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
            for(int i=0;i<rows.size();i++)labels[i]=rows.get(i).getString("reason")+"\n"+date.format(new Date(rows.get(i).getLong("time")));
            new AlertDialog.Builder(activity).setTitle("Recent changes").setItems(labels,(dialog,index)->{
                AlertDialog confirm=new AlertDialog.Builder(activity).setTitle("Restore this Home?")
                        .setMessage("Restore this profile’s app choices and appearance. Media files must still be available. Your current Home gets a recovery point first.")
                        .setNegativeButton(android.R.string.cancel,null).setPositiveButton("Restore",(d,w)->{
                            try{restore(activity,profile,rows.get(index));activity.finish();}
                            catch(Exception failure){Toast.makeText(activity,failure.getMessage()==null?"Home could not be restored":failure.getMessage(),Toast.LENGTH_LONG).show();}
                        }).create();
                confirm.show();confirm.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
            }).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception failure){Toast.makeText(activity,"Recovery history is unavailable; your Home has been kept",Toast.LENGTH_LONG).show();}
    }

    private static void restore(Context context,String profile,JSONObject entry)throws Exception{
        if(busy||!profile.equals(ProfileStore.id()))throw new IOException("The active profile changed. Open Recent changes again.");
        validateEntry(entry,profile);JSONObject target=entry.getJSONObject("snapshot");validateMedia(context,target.getJSONObject("values"));
        Context app=context.getApplicationContext();AtomicFile journal=journal(app);
        if(exists(journal))throw new IOException("An unfinished restore must be recovered first");
        JSONObject before=capture(context);SharedPreferences prefs=prefs(context,profile);
        JSONObject checkpoint=new JSONObject().put("schema",1).put("profile",profile).put("time",System.currentTimeMillis()).put("id",UUID.randomUUID().toString())
                .put("reason","Before restoring Home").put("snapshot",before);
        JSONObject recovery=new JSONObject().put("schema",1).put("profile",profile).put("committed",false)
                .put("rawPrefs",rawPrefs(prefs)).put("files",before.getJSONObject("files"))
                .put("historyBefore",encoded(file(app,profile,HISTORY).getBaseFile()));
        write(journal,utf(recovery.toString()));busy=true;
        try{
            restoreFiles(app,profile,target.getJSONObject("files"));
            Map<String,Object> values=SetupStore.decode(target.getJSONObject("values"));
            if(values.containsKey(DesktopLayout.STATE))values.put(DesktopLayout.STATE,DesktopLayout.restamp(context,(String)values.get(DesktopLayout.STATE)));
            Map<String,Object> changes=new HashMap<>();for(String key:SetupValues.KEYS)changes.put(key,values.get(key));
            for(String key:UNDO)changes.put(key,null);
            changes.put(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
            if(!SetupValues.commit(prefs,changes))throw new IOException("Home could not be saved");
            append(app,profile,checkpoint);
            recovery.put("committed",true);write(journal,utf(recovery.toString()));journal.delete();
            context.getContentResolver().notifyChange(DesktopLayout.uri(context),null);
        }catch(Exception failure){
            try{rollback(app,recovery);journal.delete();}
            catch(Exception recoveryFailure){Log.e(TAG,"Restore rollback retained for next startup",recoveryFailure);throw new IOException("Restore interrupted. Home will recover when reopened.",failure);}
            throw failure;
        }finally{busy=false;}
    }

    static void clearProfile(Context context,String profile){
        if(!HomeHistoryPolicy.profile(profile)||profile.isEmpty())return;
        scheduled.remove(profile);file(context,profile,HISTORY).delete();file(context,profile,PENDING).delete();
        try{AtomicFile journal=journal(context);if(exists(journal)&&profile.equals(object(read(journal,HomeHistoryPolicy.MAX_BYTES*4)).getString("profile")))journal.delete();}
        catch(Exception failure){Log.w(TAG,"Could not inspect deleted profile recovery journal",failure);}
    }

    /** Removal is an explicit, infrequent operation; unreadable history fails closed. */
    static boolean themeInUse(Context context,String token){
        if(token==null||token.isEmpty())return false;
        try{
            List<String> profiles=new ArrayList<>();profiles.add("");
            for(LocalProfiles.Profile profile:ProfileStore.read(context))if(!profiles.contains(profile.id)&&profiles.size()<17)profiles.add(profile.id);
            for(String profile:profiles){
                for(JSONObject entry:history(context,profile))if(usesTheme(entry.getJSONObject("snapshot").getJSONObject("values"),token))return true;
                AtomicFile pending=file(context,profile,PENDING);
                if(exists(pending)){JSONObject entry=object(read(pending,HomeHistoryPolicy.MAX_BYTES));validateEntry(entry,profile);if(usesTheme(entry.getJSONObject("snapshot").getJSONObject("values"),token))return true;}
            }
            AtomicFile journal=journal(context);
            if(exists(journal)&&usesTheme(object(read(journal,HomeHistoryPolicy.MAX_BYTES*4)).getJSONObject("rawPrefs"),token))return true;
            return false;
        }catch(Exception failure){Log.w(TAG,"Keeping theme because recovery references cannot be checked",failure);return true;}
    }
    private static boolean usesTheme(JSONObject values,String token){return token.equals(values.optString(ThemeLibrary.CHOICE,""))||WallpaperPlaylistChoice.references(values.optString(WallpaperPlaylistChoice.KEY,""),token);}

    private static void rollback(Context context,JSONObject recovery)throws Exception{
        exact(recovery,"schema","profile","committed","rawPrefs","files","historyBefore");
        if(!integer(recovery.get("schema"))||recovery.getInt("schema")!=1||!(recovery.get("committed") instanceof Boolean)||!HomeHistoryPolicy.profile(recovery.getString("profile")))throw new IOException("Invalid recovery journal");
        String profile=recovery.getString("profile");JSONObject raw=recovery.getJSONObject("rawPrefs");
        Map<String,Object> values=decodeRaw(raw);validateFiles(recovery.getJSONObject("files"));
        byte[] history=recovery.isNull("historyBefore")?null:decode(recovery.getString("historyBefore"),HomeHistoryPolicy.MAX_BYTES);
        validateHistoryBytes(history,profile);
        restoreFiles(context,profile,recovery.getJSONObject("files"));
        if(!SetupValues.commit(prefs(context,profile),values))throw new IOException("Could not recover Home preferences");
        restoreFile(file(context,profile,HISTORY),history);
    }

    private static JSONObject rawPrefs(SharedPreferences prefs)throws Exception{
        JSONObject raw=new JSONObject();Map<String,?> all=prefs.getAll();
        for(String key:rawKeys())raw.put(key,all.containsKey(key)?all.get(key):JSONObject.NULL);
        return raw;
    }
    private static Set<String> rawKeys(){Set<String> keys=new HashSet<>(SetupValues.KEYS);keys.addAll(Arrays.asList(UNDO));keys.add(Config.SETTINGS_REVISION_KEY);return keys;}
    private static Map<String,Object> decodeRaw(JSONObject raw)throws Exception{
        exact(raw,rawKeys().toArray(new String[0]));Map<String,Object> result=new HashMap<>();Map<String,Object> ordinary=new HashMap<>();
        for(Iterator<String> it=raw.keys();it.hasNext();){String key=it.next();Object value=raw.isNull(key)?null:raw.get(key);
            if(value!=null&&SetupValues.KEYS.contains(key))ordinary.put(key,value);
            else if(value!=null&&key.equals(Config.SETTINGS_REVISION_KEY)&&!(value instanceof Integer||value instanceof Long))throw new IOException("Invalid recovery revision");
            else if(value!=null&&!SetupValues.KEYS.contains(key)&&!key.equals(Config.SETTINGS_REVISION_KEY)&&(!(value instanceof String)||((String)value).length()>32768))throw new IOException("Invalid recovery Undo");
            result.put(key,value);
        }
        SetupValues.validate(ordinary);return result;
    }

    private static void validateEntry(JSONObject entry,String profile)throws Exception{
        exact(entry,"schema","profile","time","id","reason","snapshot");
        if(!integer(entry.get("schema"))||entry.getInt("schema")!=1||!profile.equals(entry.getString("profile"))||!HomeHistoryPolicy.profile(profile))throw new IOException("Recovery profile does not match");
        UUID.fromString(entry.getString("id"));if(!integer(entry.get("time"))||entry.getLong("time")<0)throw new IOException("Invalid recovery time");
        if(!HomeHistoryPolicy.reason(entry.getString("reason")).equals(entry.getString("reason")))throw new IOException("Invalid recovery description");
        validateSnapshot(entry.getJSONObject("snapshot"));
    }
    private static void validateSnapshot(JSONObject snapshot)throws Exception{
        exact(snapshot,"values","files");Map<String,Object> values=SetupStore.decode(snapshot.getJSONObject("values"));
        if(!values.containsKey(DesktopLayout.STATE))throw new IOException("Recovery point has no app layout");
        DesktopLayout.draftFromSaved((String)values.get(DesktopLayout.STATE));validateFiles(snapshot.getJSONObject("files"));
    }
    private static void validateFiles(JSONObject files)throws Exception{
        exact(files,HomeHistoryPolicy.FILES);
        for(String name:HomeHistoryPolicy.FILES)if(!files.isNull(name)){
            byte[] bytes=decode(files.getString(name),HomeHistoryPolicy.FILE_BYTES);String text=new String(bytes,StandardCharsets.UTF_8);
            if(name.equals(Order.FILE_NAME)){if(Order.decode(text)==null)throw new IOException("Invalid saved app order");}
            else LibraryApps.decode(text);
        }
    }
    static void validateMedia(Context context,JSONObject object)throws Exception{
        Map<String,Object> values=SetupStore.decode(object);String token=(String)values.get(ThemeLibrary.CHOICE);File dir=Config.dir(context);
        ProductFeatures.validateMedia(context,values);
        if(token!=null&&!token.isEmpty()){
            if(!ThemeLibrary.validToken(token))throw new IOException("Invalid saved theme");
            dir=new File(context.getFilesDir(),"product-themes/"+token);
            if(!dir.isDirectory()||Files.isSymbolicLink(dir.toPath())||!new File(dir,Config.WALLPAPER_NAME).isFile())throw new IOException("Saved theme is unavailable");
            ThemeArchive.read(dir);
        }
        Object name=values.get(Config.WALLPAPER_CHOICE_KEY);
        if(name!=null&&!name.equals("none")){
            String file=(String)name;if(file.isEmpty()||file.contains("/")||file.contains("\\")||file.equals(".")||file.equals(".."))throw new IOException("Invalid saved wallpaper");
            if(dir==null)throw new IOException("Wallpaper storage is unavailable");
            File image=file.equals(Config.WALLPAPER_NAME)?new File(dir,file):new File(new File(dir,"wallpapers"),file);
            if(!image.isFile()||Files.isSymbolicLink(image.toPath()))throw new IOException("Saved wallpaper is unavailable");
        }
        if(Boolean.TRUE.equals(values.get(ThemeLibrary.LIVE))&&(token==null||token.isEmpty()||dir==null||!new File(dir,"video.mp4").isFile()))throw new IOException("Saved live wallpaper is unavailable");
    }

    private static boolean same(JSONObject a,JSONObject b)throws Exception{return canonical(comparable(a)).equals(canonical(comparable(b)));}
    private static boolean integer(Object value){return value instanceof Integer||value instanceof Long;}
    private static void validateHistoryBytes(byte[] bytes,String profile)throws Exception{
        if(bytes==null)return;if(bytes.length>HomeHistoryPolicy.MAX_BYTES)throw new IOException("Recovery history is too large");
        JSONArray rows=new JSONArray(new String(bytes,StandardCharsets.UTF_8));if(rows.length()>HomeHistoryPolicy.LIMIT)throw new IOException("Too many recovery points");
        for(int i=0;i<rows.length();i++)validateEntry(rows.getJSONObject(i),profile);
    }
    private static JSONObject comparable(JSONObject value)throws Exception{
        JSONObject copy=new JSONObject(value.toString());JSONObject values=copy.getJSONObject("values");
        if(values.has(DesktopLayout.STATE)){JSONObject layout=new JSONObject(values.getString(DesktopLayout.STATE));layout.remove("orderStamp");values.put(DesktopLayout.STATE,canonical(layout));}
        return copy;
    }
    private static String canonical(Object value)throws Exception{
        if(value instanceof JSONObject){JSONObject object=(JSONObject)value;List<String> rows=new ArrayList<>();TreeSet<String> keys=new TreeSet<>();for(Iterator<String> it=object.keys();it.hasNext();)keys.add(it.next());for(String key:keys)rows.add(JSONObject.quote(key)+":"+canonical(object.get(key)));return "{"+String.join(",",rows)+"}";}
        if(value instanceof JSONArray){JSONArray array=(JSONArray)value;List<String> rows=new ArrayList<>();for(int i=0;i<array.length();i++)rows.add(canonical(array.get(i)));return "["+String.join(",",rows)+"]";}
        return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);
    }

    private static SharedPreferences prefs(Context context,String profile){SharedPreferences global=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);return profile.isEmpty()?global:new ProfilePreferences(global,profile);}
    private static File owned(Context context,String profile,String name)throws IOException{
        if(!HomeHistoryPolicy.profile(profile)||!Arrays.asList(HomeHistoryPolicy.FILES).contains(name))throw new IOException("Invalid recovery file");
        File file=new File(context.getFilesDir(),LocalProfiles.filename(profile,name));
        if(!file.getCanonicalFile().getParentFile().equals(context.getFilesDir().getCanonicalFile())||Files.isSymbolicLink(file.toPath()))throw new IOException("Unsafe recovery file");return file;
    }
    private static AtomicFile file(Context context,String profile,String name){if(!HomeHistoryPolicy.profile(profile))throw new IllegalArgumentException("Invalid profile");return new AtomicFile(new File(context.getFilesDir(),LocalProfiles.filename(profile,name)));}
    private static AtomicFile journal(Context context){return new AtomicFile(new File(context.getFilesDir(),JOURNAL));}
    private static boolean exists(AtomicFile file){return file.getBaseFile().exists()||new File(file.getBaseFile()+".bak").exists();}
    private static Object encoded(File file)throws Exception{AtomicFile source=new AtomicFile(file);if(!exists(source))return JSONObject.NULL;if(Files.isSymbolicLink(file.toPath())||Files.isSymbolicLink(new File(file+".bak").toPath()))throw new IOException("Invalid recovery source");return Base64.getEncoder().encodeToString(read(source,file.getName().endsWith(HISTORY)?HomeHistoryPolicy.MAX_BYTES:HomeHistoryPolicy.FILE_BYTES));}
    private static byte[] decode(String value,int max)throws IOException{if(value.length()>max*4L/3+8)throw new IOException("Recovery content is too large");try{byte[] out=Base64.getDecoder().decode(value);if(out.length>max)throw new IOException("Recovery content is too large");return out;}catch(IllegalArgumentException invalid){throw new IOException("Invalid recovery data",invalid);}}
    private static byte[] read(AtomicFile file,int max)throws IOException{try(java.io.InputStream input=file.openRead();java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){if(output.size()+count>max)throw new IOException("Recovery file is too large");output.write(buffer,0,count);}return output.toByteArray();}}
    private static JSONObject object(byte[] bytes)throws Exception{String text=new String(bytes,StandardCharsets.UTF_8);ThemeArchive.validateJsonObject(text);return new JSONObject(text);}
    private static byte[] utf(String value){return value.getBytes(StandardCharsets.UTF_8);}
    private static void write(AtomicFile file,byte[] bytes)throws IOException{FileOutputStream output=null;try{output=file.startWrite();output.write(bytes);file.finishWrite(output);}catch(IOException failure){if(output!=null)file.failWrite(output);throw failure;}}
    private static void restoreFile(AtomicFile file,byte[] bytes)throws IOException{if(bytes!=null)write(file,bytes);else{file.delete();if(exists(file))throw new IOException("Could not clear old app choices");}}
    private static void restoreFiles(Context context,String profile,JSONObject files)throws Exception{validateFiles(files);for(String name:HomeHistoryPolicy.FILES)restoreFile(new AtomicFile(owned(context,profile,name)),files.isNull(name)?null:decode(files.getString(name),HomeHistoryPolicy.FILE_BYTES));}
    private static void exact(JSONObject object,String... names)throws IOException{Set<String> actual=new HashSet<>();for(Iterator<String> it=object.keys();it.hasNext();)actual.add(it.next());if(!actual.equals(new HashSet<>(Arrays.asList(names))))throw new IOException("Invalid recovery fields");}
    private HomeHistory(){}
}
