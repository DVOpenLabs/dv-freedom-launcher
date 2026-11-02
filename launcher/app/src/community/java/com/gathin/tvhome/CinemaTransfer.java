package com.gathin.tvhome;
import android.content.*;
import android.os.*;
import org.json.*;
import java.util.*;
/** Invoked only behind DesktopLayout's existing WRITE_SECURE_SETTINGS caller check. Drafts never apply. */
final class CinemaTransfer {
    private static Pending pending;
    private static final class Pending{final String token=UUID.randomUUID().toString(),raw,profile,revision;final long until=SystemClock.elapsedRealtime()+120000;Pending(Context c,String value){raw=value;profile=ProfileStore.id();revision=CinemaStore.revision(c);}}
    static Bundle call(Context c,String method,String arg)throws Exception{
        if(!method.equals("cinema_read")&&!method.equals("cinema_stage"))return null;Config.load(c);JSONObject out=new JSONObject().put("schema",1);
        if(method.equals("cinema_read")){JSONArray apps=new JSONArray();for(HomeCategories.App app:HomeCategories.catalog(c,Config.load(c)))if(FamilyMode.allowed(c,app.pkg))apps.put(new JSONObject().put("package",app.pkg).put("name",app.label));JSONArray artwork=new JSONArray();for(Map.Entry<String,String> image:CinemaAssets.library(c).entrySet())artwork.put(new JSONObject().put("id",image.getKey()).put("name",image.getValue()));JSONArray wallpapers=new JSONArray();long libraryBytes=0;for(ThemeLibrary.Entry e:ThemeLibrary.list(c)){long bytes=ThemeLibrary.bytes(e.directory);libraryBytes+=bytes;wallpapers.put(new JSONObject().put("token",e.token).put("id",e.pack.id).put("name",e.pack.name).put("video",new java.io.File(e.directory,"video.mp4").isFile()).put("bytes",bytes));}out.put("wallpapers",wallpapers).put("libraryBytes",libraryBytes).put("wallpaperCapacity",ThemeLibrary.MAX_PACKS).put("wallpaperPlaylist",ProfileStore.preferences(c).getString(WallpaperPlaylistChoice.KEY,"")).put("package",c.getPackageName()).put("artwork",artwork).put("profile",ProfileStore.id()).put("revision",SetupTransfer.revision(c)).put("apps",apps).put("draft",CinemaStore.read(c).json);}
        else{ProductFeatures.requireLayoutEditable(c);if(arg==null||arg.length()>CinemaSpec.MAX_TEXT+2048)throw new IllegalArgumentException("Cinema draft is too large");ThemeArchive.validateJsonObject(arg,800);JSONObject request=new JSONObject(arg);CinemaSpec.fields(request,"schema","profile","revision","draft");if(request.getInt("schema")!=1||!request.getString("profile").equals(ProfileStore.id())||!request.getString("revision").equals(SetupTransfer.revision(c)))throw new IllegalArgumentException("Home changed. Read this TV again.");CinemaSpec spec=new CinemaSpec(request.getJSONObject("draft").toString());CinemaAssets.validate(c,spec);pending=new Pending(c,spec.json.toString());c.startActivity(new Intent(c,CinemaActivity.class).putExtra("cinema_token",pending.token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));out.put("reviewRequired",true).put("expiresInSeconds",120);}
        Bundle result=new Bundle();result.putString("json",out.toString());return result;
    }
    static String take(Context c,String token){Pending p=pending;pending=null;if(p==null||!p.token.equals(token)||SystemClock.elapsedRealtime()>p.until||!p.profile.equals(ProfileStore.id())||!p.revision.equals(CinemaStore.revision(c)))throw new IllegalArgumentException("Cinema draft expired or Home changed. Send a fresh draft.");return p.raw;}
}
