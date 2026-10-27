package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.widget.Toast;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Locally created profiles are separate from imported libraries and commit with their copied choices. */
final class ProfileSetup {
    static final String CREATED="created_launcher_profiles",ENABLED="created_profiles_enabled";
    static SharedPreferences global(Context c){return c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);}
    static List<LocalProfiles.Profile> read(Context ctx){
        String text=global(ctx).getString(CREATED,"");if(text.isEmpty())return Collections.emptyList();
        try{return ProfileStore.parse(text.getBytes(StandardCharsets.UTF_8));}catch(Exception bad){return Collections.emptyList();}
    }
    private static String encode(List<LocalProfiles.Profile> rows)throws Exception{
        if(rows.isEmpty())return null;
        Map<String,String> apps=new TreeMap<>();for(LocalProfiles.Profile row:rows)for(List<String> values:Arrays.asList(row.favorites,row.hidden))for(String pkg:values)if(!apps.containsKey(pkg))apps.put(pkg,"app"+apps.size());
        JSONObject catalog=new JSONObject();for(Map.Entry<String,String> entry:apps.entrySet())catalog.put(entry.getValue(),new JSONObject().put("android",entry.getKey()));
        JSONArray profiles=new JSONArray();for(LocalProfiles.Profile row:rows){
            JSONArray favorites=new JSONArray(),hidden=new JSONArray();for(String pkg:row.favorites)favorites.put(apps.get(pkg));for(String pkg:row.hidden)hidden.put(apps.get(pkg));
            profiles.put(new JSONObject().put("id",row.id).put("name",row.name).put("columns",row.columns).put("favorites",favorites).put("hiddenApps",hidden));
        }
        String text=new JSONObject().put("schemaVersion",1).put("apps",catalog).put("profiles",profiles).toString();ProfileStore.parse(text.getBytes(StandardCharsets.UTF_8));return text;
    }
    static String create(Context ctx,String name)throws Exception{
        name=SetupValues.name(name);List<LocalProfiles.Profile> all=ProfileStore.read(ctx),rows=new ArrayList<>(read(ctx));
        if(all.size()>=16)throw new IllegalArgumentException("Maximum 16 profiles");checkName(ctx,name,"",all);
        Config config=Config.load(ctx);Map<String,Object> values=SetupStore.capture(ctx);
        LayoutDraft draft=DesktopLayout.draftFromSaved((String)values.get(DesktopLayout.STATE));
        String id="local_"+UUID.randomUUID().toString();rows.add(new LocalProfiles.Profile(id,name,draft.favorites,draft.hidden,config.columns));
        SharedPreferences p=global(ctx);Map<String,Object> changes=new HashMap<>();
        for(Map.Entry<String,Object> value:values.entrySet())changes.put(LocalProfiles.key(id,value.getKey()),value.getValue());
        String signature=ProfileStore.preferences(ctx).getString(Config.THEME_SIGNATURE_KEY,null);if(signature!=null)changes.put(LocalProfiles.key(id,Config.THEME_SIGNATURE_KEY),signature);
        changes.put(CREATED,encode(rows));changes.put(ENABLED,true);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(!SetupValues.commit(p,changes))throw new IllegalStateException("Could not create profile");
        Config.load(ctx);return id;
    }
    private static void checkName(Context ctx,String name,String id,List<LocalProfiles.Profile> rows){
        if(name.equalsIgnoreCase(ctx.getString(R.string.profile_current)))throw new IllegalArgumentException("Choose a different name");
        for(LocalProfiles.Profile row:rows)if(!row.id.equals(id)&&name.equalsIgnoreCase(row.name))throw new IllegalArgumentException("That name already exists");
    }
    static boolean change(Context ctx,String id,String name)throws Exception{
        List<LocalProfiles.Profile> rows=new ArrayList<>(read(ctx));int index=-1;
        for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(id))index=i;
        if(index<0)throw new IllegalArgumentException("Profile unavailable");
        if(name!=null){name=SetupValues.name(name);checkName(ctx,name,id,ProfileStore.read(ctx));LocalProfiles.Profile old=rows.get(index);rows.set(index,new LocalProfiles.Profile(id,name,old.favorites,old.hidden,old.columns));}
        else rows.remove(index);
        SharedPreferences p=global(ctx);Map<String,Object> changes=new HashMap<>();changes.put(CREATED,encode(rows));
        if(rows.isEmpty())changes.put(ENABLED,null);
        if(name==null){for(String key:p.getAll().keySet())if(key.startsWith("profile."+id+"."))changes.put(key,null);if(ProfileStore.id().equals(id))changes.put(ProfileStore.ACTIVE,"");}
        changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        boolean saved=SetupValues.commit(p,changes);if(saved){if(name==null)ctx.getSharedPreferences(RecentHistory.scope(id),Context.MODE_PRIVATE).edit().clear().commit();Config.load(ctx);}return saved;
    }
    static void show(Activity a){
        String[] names=ProfileStore.enabled()?new String[]{a.getString(R.string.profile_switch),a.getString(R.string.profile_create),a.getString(R.string.profile_manage)}:new String[]{a.getString(R.string.profile_create)};
        new AlertDialog.Builder(a).setTitle(R.string.local_profiles).setItems(names,(d,which)->{
            if(!ProfileStore.enabled()||which==1)SetupUi.name(a,R.string.profile_create,"",name->{try{create(a,name);a.finish();}catch(Exception e){throw new IllegalArgumentException(e.getMessage(),e);}});
            else if(which==0)ProfilePicker.show(a);
            else manage(a);
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private static void manage(Activity a){
        List<LocalProfiles.Profile> rows=read(a);if(rows.isEmpty()){Toast.makeText(a,R.string.profile_none_created,Toast.LENGTH_SHORT).show();return;}
        String[] names=new String[rows.size()];for(int i=0;i<names.length;i++)names[i]=rows.get(i).name;
        new AlertDialog.Builder(a).setTitle(R.string.profile_manage).setItems(names,(d,index)->{
            LocalProfiles.Profile row=rows.get(index);
            new AlertDialog.Builder(a).setTitle(row.name).setItems(new String[]{a.getString(R.string.setup_rename),a.getString(R.string.setup_delete)},(x,which)->{
                if(which==0)SetupUi.name(a,R.string.setup_rename,row.name,name->{try{if(!change(a,row.id,name))throw new IllegalStateException("Could not save profile");a.finish();}catch(Exception e){throw new IllegalArgumentException(e.getMessage(),e);}});
                else SetupUi.confirm(a,R.string.setup_delete,R.string.profile_delete_help,()->{try{if(!change(a,row.id,null))throw new IllegalStateException("Could not delete profile");a.finish();}catch(Exception e){throw new IllegalArgumentException(e.getMessage(),e);}});
            }).setNegativeButton(android.R.string.cancel,null).show();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private ProfileSetup(){}
}
