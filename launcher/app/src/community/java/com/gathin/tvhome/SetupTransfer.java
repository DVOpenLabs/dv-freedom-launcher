package com.gathin.tvhome;
import android.content.*;
import android.os.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Existing shell-authorized provider boundary. Stage opens review; it cannot apply a draft. */
final class SetupTransfer {
    private static Pending pending;
    static final class Pending {final String token=UUID.randomUUID().toString(),profile=ProfileStore.id(),revision;final Map<String,?> before;final long deadline=SystemClock.elapsedRealtime()+120000;final JSONObject draft;Pending(Context c,JSONObject d)throws Exception{before=ProfileStore.preferences(c).getAll();revision=revision(c);draft=d;}}
    static String revision(Context c)throws Exception{return DesktopLayout.revision(c,ProfileStore.preferences(c).getAll());}
    static Bundle call(Context c,String method,String arg)throws Exception{
        if(!method.equals("product_setup_inventory")&&!method.equals("product_setup_stage"))return null;
        Config config=Config.load(c);List<HomeCategories.App> apps=HomeCategories.catalog(c,config);Set<String> installed=new HashSet<>();JSONArray rows=new JSONArray();for(HomeCategories.App app:apps){installed.add(app.pkg);rows.put(new JSONObject().put("package",app.pkg).put("name",app.label));}
        JSONObject out=new JSONObject().put("schema",1).put("revision",revision(c)).put("profile",ProfileStore.id());
        if(method.equals("product_setup_inventory")){out.put("apps",rows).put("favorites",new JSONArray(DesktopLayout.favorites(config,Order.load(Order.file(c))))).put("experience",ExperienceChoice.saved(c));}
        else{if(arg==null||arg.getBytes(StandardCharsets.UTF_8).length>SetupProposal.MAX_BYTES+2048)throw new IllegalArgumentException("Setup request too large");ThemeArchive.validateJsonObject(arg);JSONObject request=new JSONObject(arg);SetupProposal.fields(ProfileStore.object(request),"schema","revision","profile","draft");if(!Integer.valueOf(1).equals(request.get("schema"))||!request.getString("revision").equals(revision(c))||!request.getString("profile").equals(ProfileStore.id()))throw new IllegalArgumentException("Home changed. Read a fresh inventory and review the draft again.");JSONObject draft=request.getJSONObject("draft");new SetupProposal(ProfileStore.object(draft),installed);pending=new Pending(c,draft);c.startActivity(new Intent(c,GuidedSetupActivity.class).putExtra("staged_setup_token",pending.token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));out.put("staged",true).put("reviewRequired",true);}
        Bundle result=new Bundle();result.putString("json",out.toString());return result;
    }
    static JSONObject take(Context c,String token)throws Exception{Pending p=pending;pending=null;if(p==null||!p.token.equals(token)||SystemClock.elapsedRealtime()>p.deadline||!p.profile.equals(ProfileStore.id())||!p.before.equals(ProfileStore.preferences(c).getAll())||!p.revision.equals(revision(c)))throw new IllegalArgumentException("This draft expired or Home changed. Stage it again.");return p.draft;}
}
