package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.*;
import org.json.*;

/** Optional full-product features, compiled into the existing APK and data identity. */
final class ProductFeatures {
    private static Context app;
    private static boolean initialized;
    static void init(Context c){app=c.getApplicationContext();if(!initialized){initialized=true;HomeHistory.initialize(c);}}
    static void beforeChange(String reason){if(app!=null)HomeHistory.before(app,reason);}
    static void beforeChange(Context c,String reason){HomeHistory.before(c,reason);}
    static boolean routeHome(MainActivity a){return ExperienceChoice.route(a);}
    static boolean homeAction(MainActivity a,Intent intent){
        if(intent==null||!ExperienceChoice.enabled(a))return false;String action=intent.getAction();
        if(MainActivity.ACTION_LAUNCHER_SETTINGS.equals(action)){a.startActivity(new Intent(a,LauncherSettingsActivity.class).putExtra("experience_context",true));return true;}
        if(MainActivity.ACTION_VIEW_INPUTS.equals(action)||MainActivity.ACTION_LIBRARY.equals(action)||MainActivity.ACTION_IDLE_PREVIEW.equals(action)){
            a.startActivity(new Intent(a,ExperienceHomeActivity.class).putExtra("home_action",action).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));return true;
        }return false;
    }
    static void wallpaperSample(Activity a,android.graphics.Bitmap bitmap){if(a instanceof ExperienceHomeActivity)((ExperienceHomeActivity)a).sample(bitmap);}
    static void resumed(Activity a){
        if(a instanceof ExperienceHomeActivity||(a instanceof MainActivity||a instanceof CategoryHomeActivity)&&!(a instanceof MainActivity&&(HomeCategories.enabled(a)||ExperienceChoice.enabled(a))))WallpaperPlaylist.attach(a);
        a.getWindow().getDecorView().postDelayed(()->{
            if(a.isDestroyed()||a.isFinishing()||!a.hasWindowFocus())return;
            HomeAppearance.apply(a);
            if(a instanceof MainActivity&&!ExperienceChoice.enabled(a)&&PlacementSession.preview()==null&&StyleSession.preview()==null&&HomeCategories.enabled(a)){CategoryHomeActivity.openIfEnabled((MainActivity)a,Config.load(a));return;}
        },240);
    }
    static boolean playlistEnabled(Context c){return WallpaperPlaylistChoice.enabled(c);}
    static void profileKeys(Set<String> keys){setupKeys(keys);}
    static void setupKeys(Set<String> keys){keys.add(HomeCategories.KEY);keys.add(NativeTileEditor.KEY);keys.add(HomeAppearance.KEY);keys.add(WallpaperPlaylistChoice.KEY);keys.add(ExperiencePolicy.KEY);}
    static boolean validateValue(String key,Object value){
        if(key.equals(ExperiencePolicy.KEY)){if(!(value instanceof String)||!ExperiencePolicy.valid((String)value))throw new IllegalArgumentException("Invalid Home experience");return true;}
        if(!key.equals(HomeCategories.KEY)&&!key.equals(NativeTileEditor.KEY)&&!key.equals(HomeAppearance.KEY)&&!key.equals(WallpaperPlaylistChoice.KEY))return false;
        if(!(value instanceof String))throw new IllegalArgumentException("Invalid product setting");String raw=(String)value;
        boolean valid=key.equals(HomeCategories.KEY)?HomeCategories.valid(raw):key.equals(NativeTileEditor.KEY)?NativeTileEditor.validate(raw):key.equals(WallpaperPlaylistChoice.KEY)?validPlaylist(raw):validSizes(raw);
        if(!valid)throw new IllegalArgumentException("Invalid product setting");return true;
    }
    private static boolean validSizes(String raw){try{HomeAppearance.parse(raw);return true;}catch(RuntimeException invalid){return false;}}
    private static boolean validPlaylist(String raw){try{PlaylistPolicy.parse(raw);return true;}catch(RuntimeException invalid){return false;}}
    static void validateMedia(Context c,Map<String,Object> values)throws IOException{if(values.containsKey(NativeTileEditor.KEY))NativeTileEditor.validateAssets(c,(String)values.get(NativeTileEditor.KEY));if(values.containsKey(WallpaperPlaylistChoice.KEY))WallpaperPlaylistChoice.validateMedia(c,(String)values.get(WallpaperPlaylistChoice.KEY));}
    static void clearProfile(Context c,String id){
        HomeHistory.clearProfile(c,id);if(id==null||id.isEmpty()||!LocalProfiles.validId(id))return;
        c.deleteSharedPreferences(LocalProfiles.filename(id,WallpaperPlaylist.PLAYBACK));
        for(String name:HomeHistoryPolicy.FILES)try{java.io.File f=new java.io.File(c.getFilesDir(),LocalProfiles.filename(id,name));if(f.getCanonicalFile().getParentFile().equals(c.getFilesDir().getCanonicalFile())&&!java.nio.file.Files.isSymbolicLink(f.toPath()))new android.util.AtomicFile(f).delete();}catch(IOException ignored){}
    }
    static boolean themeInUse(Context c,String token){
        if(HomeHistory.themeInUse(c,token))return true;
        for(Map.Entry<String,?> e:c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE).getAll().entrySet()){
            if((e.getKey().equals(WallpaperPlaylistChoice.KEY)||e.getKey().endsWith("."+WallpaperPlaylistChoice.KEY))&&WallpaperPlaylistChoice.references(String.valueOf(e.getValue()),token))return true;
            if(e.getKey().equals(SetupStore.KEY)||e.getKey().endsWith("."+SetupStore.KEY)){
                try{JSONArray rows=new JSONArray(String.valueOf(e.getValue()));for(int i=0;i<rows.length();i++){JSONObject values=rows.getJSONObject(i).getJSONObject("values");if(token.equals(values.optString(ThemeLibrary.CHOICE))||WallpaperPlaylistChoice.references(values.optString(WallpaperPlaylistChoice.KEY,""),token))return true;}}catch(Exception invalid){return true;}
            }
        }return false;
    }
    static String label(Context c,String pkg,String fallback){return NativeTileEditor.label(c,pkg,fallback);}
    static Drawable banner(Context c,String pkg){return NativeTileEditor.banner(c,pkg);}
    static Drawable banner(Context c,String pkg,Drawable fallback){Drawable custom=banner(c,pkg);return custom==null?fallback:custom;}
    static void styleName(TextView name){HomeAppearance.name(name);}
    static void tileActions(Activity a,LinearLayout rows,PopupWindow popup,String pkg){
        View row=a.getLayoutInflater().inflate(R.layout.item_input,rows,false);row.findViewById(R.id.icon).setVisibility(View.GONE);row.findViewById(R.id.dot).setVisibility(View.GONE);row.findViewById(R.id.status).setVisibility(View.GONE);((TextView)row.findViewById(R.id.title)).setText("Customize app");
        row.setOnClickListener(v->{popup.dismiss();NativeTileEditor.show(a,pkg);});final boolean[] down={false};row.setOnKeyListener((v,key,event)->{if(key!=KeyEvent.KEYCODE_DPAD_CENTER&&key!=KeyEvent.KEYCODE_ENTER)return false;if(event.getAction()==KeyEvent.ACTION_DOWN){down[0]=true;return true;}if(down[0]){down[0]=false;v.performClick();}return true;});rows.addView(row);
    }
    static void categoryHeader(Activity a,ViewGroup header,Config config){HomeAppearance.apply(a);}
    static boolean activityResult(Activity a,int request,int result,Intent data){if(request!=NativeTileEditor.IMPORT)return false;NativeTileEditor.onActivityResult(a,request,result,data);return true;}
}
