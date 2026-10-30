package com.gathin.tvhome;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Same-signer Phone Remote may prepare a draft only after a separate profile opt-in. Never applies it. */
public final class PhoneSetupProvider extends ContentProvider {
    static final String ENABLED="private_phone_setup_allowed";
    static final class Pending {final String json,profile,revision;final long expires;Pending(String j,String p,String r){json=j;profile=p;revision=r;expires=SystemClock.elapsedRealtime()+120000;}}
    static Pending pending;static String phoneMediaId;
    public boolean onCreate(){return true;}
    void caller(){Context c=getContext();int uid=Binder.getCallingUid();String[] packages=c.getPackageManager().getPackagesForUid(uid);if(uid==android.os.Process.myUid())return;
        if(packages!=null)for(String pkg:packages)if(pkg.equals(OptionalPanels.REMOTE)&&c.getPackageManager().checkSignatures(c.getPackageName(),pkg)==android.content.pm.PackageManager.SIGNATURE_MATCH)return;throw new SecurityException("Only the trusted Phone Remote can prepare setup drafts");}
    private <T> T onMain(Callable<T> work){
        if(Looper.myLooper()==Looper.getMainLooper()){try{return work.call();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalArgumentException(e.getMessage());}}
        FutureTask<T> task=new FutureTask<>(work);new Handler(Looper.getMainLooper()).post(task);
        try{return task.get(8,TimeUnit.SECONDS);}catch(Exception e){task.cancel(false);Throwable cause=e instanceof ExecutionException?e.getCause():e;if(cause instanceof SecurityException)throw (SecurityException)cause;throw new IllegalArgumentException("Phone setup unavailable; read Home again");}
    }
    @Override public Bundle call(String method,String arg,Bundle extras){caller();return onMain(()->handle(method,arg));}
    private Bundle handle(String method,String arg){Context c=getContext();if("revoke".equals(method)){pending=null;if(phoneMediaId!=null)try{JSONObject media=MediaInbox.load(c);if(phoneMediaId.equals(media.optString("id"))&&Arrays.asList("prepared","receiving").contains(media.optString("state")))MediaInbox.state(c,phoneMediaId,"discarded");}catch(Exception ignored){}phoneMediaId=null;return new Bundle();}Config.load(c);if(!ProfileStore.preferences(c).getBoolean(ENABLED,false))throw new SecurityException("Enable phone setup sharing on the TV first");
        try{if("inventory".equals(method)){JSONArray apps=new JSONArray();for(HomeCategories.App app:HomeCategories.catalog(c,Config.load(c)))if(LibraryVisibility.allowed(c,app.pkg))apps.put(new JSONObject().put("package",app.pkg).put("name",app.label));Bundle b=new Bundle();b.putString("json",new JSONObject().put("schema",1).put("profile",ProfileStore.id()).put("revision",SetupTransfer.revision(c)).put("apps",apps).put("experience",ExperienceChoice.saved(c)).toString());return b;}
            if("media-prepare".equals(method)){Bundle b=new Bundle();JSONObject receipt=MediaInbox.prepare(c,arg);phoneMediaId=receipt.getString("id");b.putString("json",receipt.toString());return b;}
            if("media-status".equals(method)){Bundle b=new Bundle();b.putString("json",MediaInbox.status(c,arg).toString());return b;}
            if("revoke".equals(method)){pending=null;if(phoneMediaId!=null)try{JSONObject media=MediaInbox.load(c);if(phoneMediaId.equals(media.optString("id"))&&Arrays.asList("prepared","receiving").contains(media.optString("state")))MediaInbox.state(c,phoneMediaId,"discarded");}catch(Exception ignored){}phoneMediaId=null;return new Bundle();}
            if(!"stage".equals(method)||arg==null||arg.length()>SetupProposal.MAX_BYTES)throw new IllegalArgumentException("Invalid setup draft");ThemeArchive.validateJsonObject(arg);JSONObject envelope=new JSONObject(arg);if(envelope.length()!=3||!envelope.has("revision")||!envelope.has("profile")||!envelope.has("draft"))throw new IllegalArgumentException("Unsupported setup request");
            if(!ProfileStore.id().equals(envelope.getString("profile"))||!SetupTransfer.revision(c).equals(envelope.getString("revision")))throw new IllegalArgumentException("Home changed. Read setup again.");
            Set<String> installed=new HashSet<>();for(HomeCategories.App app:HomeCategories.catalog(c,Config.load(c)))if(LibraryVisibility.allowed(c,app.pkg))installed.add(app.pkg);JSONObject draft=envelope.getJSONObject("draft");new SetupProposal(ProfileStore.object(draft),installed);
            pending=new Pending(draft.toString(),ProfileStore.id(),SetupTransfer.revision(c));Bundle out=new Bundle();out.putString("json","{\"ok\":true,\"reviewRequired\":true}");return out;
        }catch(Exception bad){throw new IllegalArgumentException(bad.getMessage());}
    }
    static String take(Context c)throws Exception{Pending p=pending;pending=null;if(p==null||SystemClock.elapsedRealtime()>p.expires||!p.profile.equals(ProfileStore.id())||!p.revision.equals(SetupTransfer.revision(c))||!ProfileStore.preferences(c).getBoolean(ENABLED,false))throw new IllegalArgumentException("Phone draft expired or Home changed");return p.json;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws java.io.FileNotFoundException{
        caller();
        try{return onMain(()->{
            Context c=getContext();Config.load(c);
            if(!ProfileStore.preferences(c).getBoolean(ENABLED,false))throw new SecurityException("Phone setup sharing is off");
            if(uri.getPathSegments().size()!=2||!"upload".equals(uri.getPathSegments().get(0)))throw new IllegalArgumentException("Invalid upload");
            MediaInbox.bound(c,MediaInbox.load(c));
            return MediaInbox.open(c,uri.getLastPathSegment(),mode);
        });}catch(SecurityException denied){throw denied;}catch(RuntimeException bad){throw new java.io.FileNotFoundException("No prepared phone transfer");}
    }

    public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}public String getType(Uri u){return "application/json";}public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
