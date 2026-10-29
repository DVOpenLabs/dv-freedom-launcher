package com.gathin.tvhome;

import android.content.*;
import android.os.*;
import org.json.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Uses the existing ADB-authorized provider. Durable, temporary-profile preview and scoped restore. */
final class InstallerTransfer {
    private static final String JOURNAL="freedom_installer_pending",RESULT="freedom_installer_result";
    private static String live,prompted;
    private static final Handler timer=new Handler(Looper.getMainLooper());
    static JSONObject status(Context c)throws Exception{
        recover(c);Config config=Config.load(c);JSONArray apps=new JSONArray();for(HomeCategories.App app:HomeCategories.catalog(c,config))apps.put(new JSONObject().put("package",app.pkg).put("name",app.label));
        SharedPreferences global=ProfileSetup.global(c);String pending=global.getString(JOURNAL,"");
        return new JSONObject().put("schema",1).put("api",InstallerPolicy.VERSION).put("package",c.getPackageName()).put("profile",ProfileStore.id()).put("revision",SetupTransfer.revision(c)).put("apps",apps)
            .put("pending",pending.isEmpty()?JSONObject.NULL:new JSONObject(pending)).put("result",global.getString(RESULT,"")).put("backup",backup(c));
    }
    static JSONObject backup(Context c)throws Exception{
        JSONObject values=new JSONObject(SetupStore.capture(c));
        return new JSONObject().put("schema",1).put("package",c.getPackageName()).put("device",device(c)).put("profile",ProfileStore.id()).put("values",values)
            .put("revision",SetupTransfer.revision(c)).put("mediaIncluded",false);
    }
    static Bundle call(Context c,String method,String arg)throws Exception{
        if(!method.startsWith("product_installer_"))return null;
        recover(c);Config.load(c);
        if(method.equals("product_installer_status")||method.equals("product_installer_backup")){Bundle b=new Bundle();b.putString("json",(method.endsWith("status")?status(c):backup(c)).toString());return b;}
        if(arg==null||arg.getBytes(StandardCharsets.UTF_8).length>60000)throw new IllegalArgumentException("Invalid installer request");
        ThemeArchive.validateJsonObject(arg);JSONObject request=new JSONObject(arg);ProductFeatures.requireLayoutEditable(c);
        if(method.equals("product_installer_preview"))preview(c,request);
        else if(method.equals("product_installer_revert")){SetupProposal.fields(ProfileStore.object(request),"id");revert(c,request.getString("id"));}
        else if(method.equals("product_installer_restore"))restore(c,request);
        else if(method.equals("product_installer_home")||method.equals("product_installer_accessibility")){
            if(!ProfileSetup.global(c).getString(JOURNAL,"").isEmpty())throw new IllegalArgumentException("Keep or revert the preview before choosing Home");
            SetupProposal.fields(ProfileStore.object(request),"revision");if(!request.getString("revision").equals(SetupTransfer.revision(c)))throw new IllegalArgumentException("Home changed. Review again.");
            Intent choose=new Intent(method.endsWith("accessibility")?android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS:android.provider.Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(choose.resolveActivity(c.getPackageManager())==null)choose=new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(choose);
        }else throw new IllegalArgumentException("Unknown installer action");
        Bundle b=new Bundle();b.putString("json",status(c).toString());return b;
    }
    private static void preview(Context c,JSONObject request)throws Exception{
        SetupProposal.fields(ProfileStore.object(request),"revision","profile","choices","requestId");SharedPreferences global=ProfileSetup.global(c);String requestId=request.getString("requestId");if(!requestId.matches("[0-9a-f]{32}"))throw new IllegalArgumentException("Invalid setup transaction");
        String existing=global.getString(JOURNAL,"");if(!existing.isEmpty()){JSONObject active=new JSONObject(existing);if(requestId.equals(active.getString("requestId"))&&ProfileStore.object(request.getJSONObject("choices")).equals(ProfileStore.object(active.getJSONObject("choices"))))return;throw new IllegalArgumentException("Finish or revert the current preview first");}
        String previousResult=global.getString(RESULT,"");if(previousResult.startsWith("{")){JSONObject receipt=new JSONObject(previousResult);if(requestId.equals(receipt.optString("requestId")))return;}
        if(!request.getString("revision").equals(SetupTransfer.revision(c))||!request.getString("profile").equals(ProfileStore.id()))throw new IllegalArgumentException("Home changed. Back up and review again.");
        Config config=Config.load(c);Set<String> installed=new HashSet<>();for(HomeCategories.App app:HomeCategories.catalog(c,config))installed.add(app.pkg);
        JSONObject choices=request.getJSONObject("choices");Map<String,Object> values=new HashMap<>(SetupStore.capture(c));values.putAll(InstallerPolicy.choices(ProfileStore.object(choices),installed));
        List<String> selected=new ArrayList<>();JSONArray wanted=choices.getJSONArray("apps");for(int i=0;i<wanted.length();i++)selected.add(wanted.getString(i));
        JSONObject layout=new JSONObject((String)values.get(DesktopLayout.STATE));JSONObject body=layout.getJSONObject("layout");body.put("favorites",wanted);JSONArray hidden=new JSONArray();for(String pkg:config.hiddenApps)if(!selected.contains(pkg))hidden.put(pkg);body.put("hidden",hidden);layout.put("dock",wanted).put("priority",true);
        String id="local_"+UUID.randomUUID(),token=UUID.randomUUID().toString();layout.put("orderStamp",DesktopLayout.orderStamp(new File(c.getFilesDir(),LocalProfiles.filename(id,Order.FILE_NAME))));values.put(DesktopLayout.STATE,layout.toString());
        List<LocalProfiles.Profile> all=ProfileStore.read(c);if(all.size()>=16)throw new IllegalArgumentException("Maximum 16 profiles. Remove an unused profile first.");
        if(!choices.getString("wallpaper").equals("keep")){String file=InstallerWallpaper.create(c,choices.getString("wallpaper"));values.put(ThemeLibrary.CHOICE,"");values.put(Config.WALLPAPER_CHOICE_KEY,file);values.put(WallpaperPlaylistChoice.KEY,"");values.put("live_wallpaper",false);values.put(SceneSchedules.KEY,"");}
        SetupValues.validate(values);ProductFeatures.validateMedia(c,values);List<LocalProfiles.Profile> rows=new ArrayList<>(ProfileSetup.read(c));
        Set<String> names=new HashSet<>();for(LocalProfiles.Profile row:all)names.add(row.name.toLowerCase(Locale.ROOT));String name="My Freedom Home";int suffix=2;while(names.contains(name.toLowerCase(Locale.ROOT)))name="My Freedom Home "+suffix++;rows.add(new LocalProfiles.Profile(id,name,selected,config.hiddenApps,(Integer)values.get("columns")));
        JSONObject before=new JSONObject();for(String key:Arrays.asList(ProfileSetup.CREATED,ProfileSetup.ENABLED,ProfileStore.ACTIVE,Config.SETTINGS_REVISION_KEY)){Object v=global.getAll().get(key);before.put(key,v==null?JSONObject.NULL:v);}
        JSONObject journal=new JSONObject().put("id",token).put("requestId",requestId).put("choices",choices).put("created",id).put("previous",ProfileStore.id()).put("before",before).put("expectedCreated",ProfileSetup.encode(rows)).put("expectedRevision",global.getLong(Config.SETTINGS_REVISION_KEY,0)+2).put("expiresAt",System.currentTimeMillis()+45000);
        Map<String,Object> writes=new HashMap<>();for(Map.Entry<String,Object> e:values.entrySet())writes.put(LocalProfiles.key(id,e.getKey()),e.getValue());
        String signature=ProfileStore.preferences(c).getString(Config.THEME_SIGNATURE_KEY,null);if(signature!=null)writes.put(LocalProfiles.key(id,Config.THEME_SIGNATURE_KEY),signature);
        writes.put(ProfileSetup.CREATED,ProfileSetup.encode(rows));writes.put(ProfileSetup.ENABLED,true);writes.put(ProfileStore.ACTIVE,id);writes.put(Config.SETTINGS_REVISION_KEY,global.getLong(Config.SETTINGS_REVISION_KEY,0)+1);writes.put(JOURNAL,journal.toString());writes.put(RESULT,"pending:"+token);
        if(!SetupValues.commit(global,writes))throw new IllegalStateException("Could not start preview");live=token;Config.load(c);
        // Config may refresh a copied theme signature. Apply the validated draft after that refresh.
        values.put(Config.SETTINGS_REVISION_KEY,global.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(!SetupValues.commit(ProfileStore.preferences(c),values)){revert(c,token);throw new IllegalStateException("Could not apply preview choices");}
        timer.postDelayed(()->{try{revert(c,token);}catch(Exception ignored){}},45000);
        c.startActivity(new Intent(c,MainActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("installer_preview_id",token));
    }
    static void maybePrompt(android.app.Activity a){try{String text=ProfileSetup.global(a).getString(JOURNAL,"");if(!text.isEmpty()){String id=new JSONObject(text).getString("id");if(!id.equals(prompted)){prompted=id;a.startActivity(new Intent(a,InstallerPreviewActivity.class).putExtra("id",id));}}}catch(Exception failed){recover(a);}}
    static long deadline(Context c,String id)throws Exception{return journal(c,id).getLong("expiresAt");}
    static void keep(Context c,String id)throws Exception{
        JSONObject j=journal(c,id);if(j.getLong("expiresAt")<=System.currentTimeMillis()){revert(c,id);throw new IllegalArgumentException("Preview expired");}
        Config.load(c);if(!ProfileStore.id().equals(j.getString("created")))throw new IllegalArgumentException("Home profile changed; preview not kept");
        if(!ProfileSetup.global(c).edit().remove(JOURNAL).putString(RESULT,receipt(j,"kept").toString()).commit())throw new IllegalStateException("Could not keep preview");live=null;
    }
    private static JSONObject journal(Context c,String id)throws Exception{String text=ProfileSetup.global(c).getString(JOURNAL,"");if(text.isEmpty())throw new IllegalArgumentException("No pending preview");JSONObject j=new JSONObject(text);if(!j.getString("id").equals(id))throw new IllegalArgumentException("Preview changed");return j;}
    static void revert(Context c,String id)throws Exception{
        SharedPreferences p=ProfileSetup.global(c);String text=p.getString(JOURNAL,"");if(text.isEmpty())return;JSONObject j=journal(c,id);String created=j.getString("created");
        Map<String,Object> writes=new HashMap<>();JSONObject old=j.getJSONObject("before");boolean unchanged=j.getString("expectedCreated").equals(p.getString(ProfileSetup.CREATED,""))&&created.equals(p.getString(ProfileStore.ACTIVE,""))&&j.getLong("expectedRevision")==p.getLong(Config.SETTINGS_REVISION_KEY,0);for(Iterator<String> it=old.keys();it.hasNext();){String key=it.next();Object v=old.get(key);if(key.equals(Config.SETTINGS_REVISION_KEY)&&v instanceof Number)v=((Number)v).longValue();writes.put(key,v==JSONObject.NULL?null:v);}
        if(!unchanged){List<LocalProfiles.Profile> rows=new ArrayList<>(ProfileSetup.read(c));rows.removeIf(row->row.id.equals(created));writes.put(ProfileSetup.CREATED,ProfileSetup.encode(rows));writes.put(ProfileSetup.ENABLED,rows.isEmpty()?null:true);if(!created.equals(p.getString(ProfileStore.ACTIVE,"")))writes.remove(ProfileStore.ACTIVE);writes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);}
        // A preview owns only its new profile and four routing/list metadata keys. No owner values/files are written.
        for(String key:p.getAll().keySet())if(key.startsWith("profile."+created+"."))writes.put(key,null);
        writes.put(JOURNAL,null);writes.put(RESULT,receipt(j,"reverted").toString());if(!SetupValues.commit(p,writes))throw new IllegalStateException("Could not revert preview");live=null;Config.load(c);ProductFeatures.clearProfile(c,created);
        c.getContentResolver().notifyChange(DesktopLayout.uri(c),null);
    }
    static void recover(Context c){try{String s=ProfileSetup.global(c).getString(JOURNAL,"");if(!s.isEmpty()){JSONObject j=new JSONObject(s);if(!j.getString("id").equals(live)||j.getLong("expiresAt")<=System.currentTimeMillis())revert(c,j.getString("id"));}}catch(Exception failed){throw new IllegalStateException("A pending setup needs recovery before editing",failed);}}
    private static void restore(Context c,JSONObject request)throws Exception{
        if(!ProfileSetup.global(c).getString(JOURNAL,"").isEmpty())throw new IllegalArgumentException("Revert the pending preview before restoring");
        SetupProposal.fields(ProfileStore.object(request),"revision","backup");if(!request.getString("revision").equals(SetupTransfer.revision(c)))throw new IllegalArgumentException("Home changed. Review restore again.");
        JSONObject b=request.getJSONObject("backup");SetupProposal.fields(ProfileStore.object(b),"schema","package","device","profile","values","revision","mediaIncluded");if(!Integer.valueOf(1).equals(b.get("schema"))||!b.getString("package").equals(c.getPackageName())||!device(c).equals(b.getString("device"))||b.getBoolean("mediaIncluded"))throw new IllegalArgumentException("Backup belongs to another device, app or format");
        String profile=b.getString("profile");boolean exists=profile.isEmpty();for(LocalProfiles.Profile p:ProfileStore.read(c))if(p.id.equals(profile))exists=true;if(!exists)throw new IllegalArgumentException("Backup profile is unavailable");
        Map<String,Object> values=SetupStore.decode(b.getJSONObject("values"));ProductFeatures.validateMedia(c,values);Object theme=values.get(ThemeLibrary.CHOICE);if(theme!=null&&!String.valueOf(theme).isEmpty()&&!new File(c.getFilesDir(),"product-themes/"+theme).isDirectory())throw new IllegalArgumentException("The backup's theme files are unavailable");
        String layout=(String)values.get(DesktopLayout.STATE);if(layout!=null){DesktopLayout.draftFromSaved(layout);JSONObject data=new JSONObject(layout);data.put("orderStamp",DesktopLayout.orderStamp(new File(c.getFilesDir(),LocalProfiles.filename(profile,Order.FILE_NAME))));data.put("priority",true);values.put(DesktopLayout.STATE,data.toString());}
        Map<String,Object> changes=new HashMap<>();for(String key:SetupValues.KEYS)changes.put(LocalProfiles.key(profile,key),values.get(key));changes.put(ProfileStore.ACTIVE,profile);if(!profile.isEmpty())changes.put(ProfileSetup.ENABLED,true);SharedPreferences global=ProfileSetup.global(c);changes.put(Config.SETTINGS_REVISION_KEY,global.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(!SetupValues.commit(global,changes))throw new IllegalStateException("Could not restore Home");Config.load(c);c.getContentResolver().notifyChange(DesktopLayout.uri(c),null);
    }
    private static JSONObject receipt(JSONObject journal,String outcome)throws Exception{return new JSONObject().put("id",journal.getString("id")).put("requestId",journal.getString("requestId")).put("outcome",outcome);}
    private static String device(Context c)throws Exception{String value=android.provider.Settings.Secure.getString(c.getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);if(value==null||value.isEmpty())throw new IllegalStateException("Device identity is unavailable");byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest((c.getPackageName()+":"+value).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
