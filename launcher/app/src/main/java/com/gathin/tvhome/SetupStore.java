package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Five named, profile-scoped restore points. No media copies or filesystem/theme pushes. */
final class SetupStore {
    static final String KEY="saved_home_setups";
    static final int LIMIT=5;
    static Map<String,Object> capture(Context ctx)throws Exception{
        Config c=Config.load(ctx);Map<String,Object> values=new HashMap<>();SharedPreferences p=ProfileStore.preferences(ctx);
        for(String key:SetupValues.KEYS)if(p.contains(key))values.put(key,p.getAll().get(key));
        values.put("columns",c.columns);values.put(Config.ROWS_BELOW_DOCK_KEY,c.rowsBelowDock);values.put(Config.DOCK_OPACITY_KEY,c.dockOpacity);
        values.put(WallpaperDim.KEY,c.wallpaperDim);values.put(DesktopLayout.STATE,DesktopLayout.capture(ctx,c));
        return SetupValues.validate(values);
    }
    static JSONArray read(Context ctx)throws Exception{
        String text=ProfileStore.preferences(ctx).getString(KEY,"[]");if(text.length()>200000)throw new IllegalArgumentException("Saved setups too large");
        JSONArray rows=new JSONArray(text);if(rows.length()>LIMIT)throw new IllegalArgumentException("Too many setups");
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);SetupValues.name(row.getString("name"));decode(row.getJSONObject("values"));}
        return rows;
    }
    static Map<String,Object> decode(JSONObject object)throws Exception{
        Map<String,Object> values=new HashMap<>();for(Iterator<String> it=object.keys();it.hasNext();){String key=it.next();values.put(key,object.get(key));}
        return SetupValues.validate(values);
    }
    static boolean store(Context ctx,JSONArray rows){
        if(rows.length()>LIMIT||rows.toString().length()>200000)return false;
        Map<String,Object> changes=new HashMap<>();changes.put(KEY,rows.toString());return SetupValues.commit(ProfileStore.preferences(ctx),changes);
    }
    static boolean restore(Context ctx,JSONObject row)throws Exception{
        Map<String,Object> values=decode(row.getJSONObject("values"));
        Object pack=values.get(ThemeLibrary.CHOICE);
        if(pack!=null&&!new java.io.File(ctx.getFilesDir(),"product-themes/"+pack).isDirectory())throw new IllegalArgumentException("Saved theme is unavailable");
        if(values.containsKey(DesktopLayout.STATE))values.put(DesktopLayout.STATE,DesktopLayout.restamp(ctx,(String)values.get(DesktopLayout.STATE)));
        SharedPreferences p=ProfileStore.preferences(ctx);Map<String,Object> changes=new HashMap<>();
        for(String key:SetupValues.KEYS)changes.put(key,values.get(key));
        for(String key:new String[]{HomeStyles.UNDO,DesktopLayout.UNDO,DesktopLayout.GUARD,DesktopLayout.UNDO_POSITION})changes.put(key,null);
        changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        return SetupValues.commit(p,changes);
    }
    private SetupStore(){}
}
