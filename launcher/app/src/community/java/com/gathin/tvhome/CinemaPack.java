package com.gathin.tvhome;
import android.app.*;
import android.widget.Toast;
import org.json.*;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

/** Optional cinema data in an existing bounded ThemeArchive. Imports never select Home or wallpaper. */
final class CinemaPack {
    static final String SELF="p-00000000-0000-0000-0000-000000000000";
    static CinemaSpec portable(File folder)throws Exception{
        CinemaSpec spec=new CinemaSpec(ThemeArchive.readSmall(new File(folder,"cinema.json")));
        if(!spec.titles.isEmpty()||!spec.taglines.isEmpty()||spec.worlds.size()!=1)throw new IOException("Portable packs exclude movie links, personal taglines and multiple worlds");
        for(CinemaWorld w:spec.worlds)if(!SELF.equals(w.wallpaper))throw new IOException("Portable world must reference its own wallpaper");
        for(String id:spec.assets()){File file=new File(folder,"art/"+id+".png");if(!file.isFile()||Files.isSymbolicLink(file.toPath())||file.length()>2*1024*1024)throw new IOException("Pack artwork is missing or oversized");if(!hash(Files.readAllBytes(file.toPath())).equals(id))throw new IOException("Pack artwork hash mismatch");}
        return spec;
    }
    static String hash(byte[] bytes)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static void review(Activity a,ThemeLibrary.Entry entry){
        if(!ProductFeatures.allowEdit(a,()->review(a,entry)))return;
        String profile=ProfileStore.id(),revision=CinemaStore.revision(a);
        Toast.makeText(a,"Preparing local artwork for review…",Toast.LENGTH_SHORT).show();
        new Thread(()->{try{
            CinemaSpec spec=portable(entry.directory);Map<String,String> assets=new HashMap<>();for(String id:spec.assets())try(InputStream in=new FileInputStream(new File(entry.directory,"art/"+id+".png"))){assets.put(id,CinemaAssets.importStream(a.getApplicationContext(),in));}
            JSONObject worldDraft=new JSONObject(remap(spec.json,assets,entry.token).toString());
            // Merge only the paired world and pack appearance; existing user film links/taglines are retained.
            JSONObject merged=new JSONObject(CinemaStore.read(a).json.toString());JSONArray old=merged.optJSONArray("worlds"),worlds=new JSONArray();String replace=spec.worlds.isEmpty()?"":spec.worlds.get(0).id;
            if(old!=null)for(int i=0;i<old.length();i++){JSONObject w=old.getJSONObject(i);if(!w.getString("id").equals(replace)&&!w.getString("wallpaper").equals(entry.token))worlds.put(w);}
            JSONArray added=worldDraft.optJSONArray("worlds");if(added!=null)for(int i=0;i<added.length();i++)worlds.put(added.get(i));
            merged.put("worlds",worlds).put("worldsEnabled",true);
            for(String key:new String[]{"logos","iconStyle","scene","mascot","glow"})if(worldDraft.has(key))merged.put(key,worldDraft.get(key));
            JSONObject logos=merged.optJSONObject("appLogos");if(logos==null)logos=new JSONObject();JSONObject packLogos=worldDraft.optJSONObject("appLogos");if(packLogos!=null){Iterator<String> keys=packLogos.keys();while(keys.hasNext()){String key=keys.next();if(!logos.has(key))logos.put(key,packLogos.get(key));}}merged.put("appLogos",logos);
            JSONObject request=new JSONObject().put("schema",1).put("profile",profile).put("revision",revision).put("draft",merged);
            a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;try{CinemaTransfer.call(a,"cinema_stage",request.toString());}catch(Exception error){Toast.makeText(a,error.getMessage(),Toast.LENGTH_LONG).show();}});
        }catch(Exception error){a.runOnUiThread(()->{if(!a.isDestroyed())Toast.makeText(a,error.getMessage(),Toast.LENGTH_LONG).show();});}},"cinema-pack-review").start();
    }
    private static Object remap(Object value,Map<String,String> assets,String token)throws Exception{
        if(value instanceof String){String s=(String)value;return SELF.equals(s)?token:assets.getOrDefault(s,s);}
        if(value instanceof JSONObject){JSONObject out=new JSONObject();Iterator<String> keys=((JSONObject)value).keys();while(keys.hasNext()){String k=keys.next();out.put(k,remap(((JSONObject)value).get(k),assets,token));}return out;}
        if(value instanceof JSONArray){JSONArray out=new JSONArray();for(int i=0;i<((JSONArray)value).length();i++)out.put(remap(((JSONArray)value).get(i),assets,token));return out;}return value;
    }
    static void choose(Activity a){List<ThemeLibrary.Entry> entries=new ArrayList<>();for(ThemeLibrary.Entry e:ThemeLibrary.list(a))if(new File(e.directory,"cinema.json").isFile())entries.add(e);if(entries.isEmpty()){new AlertDialog.Builder(a).setTitle("Complete theme packs").setMessage("Import an offline theme ZIP containing cinema.json and its artwork through Wallpaper → Offline theme library. Import adds files only. Review the paired scene here afterward; your Home and wallpaper stay selected.").setPositiveButton("Close",null).show();return;}new AlertDialog.Builder(a).setTitle("Review an imported world").setItems(entries.stream().map(e->e.pack.name).toArray(String[]::new),(d,n)->review(a,entries.get(n))).setNegativeButton("Cancel",null).show();}
    private CinemaPack(){}
}
