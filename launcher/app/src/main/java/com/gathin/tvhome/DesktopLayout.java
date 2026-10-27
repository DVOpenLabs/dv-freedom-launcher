package com.gathin.tvhome;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Shell-permission protected, app-only edits. One atomic preference transaction; base themes untouched. */
final class DesktopLayout {
    static final String STATE="desktop_layout",UNDO="desktop_layout_undo",GUARD="desktop_layout_undo_guard";
    static final int MAX_BYTES=32768;
    static Uri uri(Context ctx){return Uri.parse("content://"+ctx.getPackageName()+".theme/product-layout");}
    static final class Selection {
        final LayoutDraft draft;
        final List<String> dock;
        final boolean priority;
        Selection(LayoutDraft draft,List<String> dock,boolean priority){this.draft=draft;this.dock=dock;this.priority=priority;}
    }
    static Selection selected(Context ctx) {
        String text=ProfileStore.preferences(ctx).getString(STATE,"");
        if(text.isEmpty())return null;
        try {
            JSONObject saved=object(text);
            if(!saved.has("layout")){LayoutDraft draft=parse(saved);return new Selection(draft,draft.favorites,false);}
            LayoutDraft draft=parse(saved.getJSONObject("layout"));
            List<String> dock=strings(saved.getJSONArray("dock"));LayoutDraft.validatePackages(dock);
            boolean priority=saved.getBoolean("priority")&&saved.getString("orderStamp").equals(orderStamp(ctx));
            return new Selection(draft,dock,priority);
        }catch(Exception invalid){return null;}
    }
    static Bundle call(Context ctx,String method,String arg) {
        // ContentProvider.call does not inherit openFile's permission enforcement.
        ctx.enforceCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS,"Desktop editor requires authorized ADB");
        // Serialize with the native profile picker; never race ProfileStore's active-profile routing.
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())return perform(ctx,method,arg);
        java.util.concurrent.FutureTask<Bundle> task=new java.util.concurrent.FutureTask<>(()->perform(ctx,method,arg));
        new android.os.Handler(android.os.Looper.getMainLooper()).post(task);
        try{return task.get(20,java.util.concurrent.TimeUnit.SECONDS);}
        catch(Exception failure){task.cancel(false);throw new IllegalStateException("Editor request did not finish. Reload before retrying.",failure);}
    }
    private static Bundle perform(Context ctx,String method,String arg) {
        try {
            LayoutDraft.requireEditable(MainActivity.desktopLayoutEditing,StyleSession.preview()!=null);
            Config config=Config.load(ctx);
            SharedPreferences prefs=ProfileStore.preferences(ctx);
            if("product_layout_apply".equals(method)||"product_layout_undo".equals(method)) {
                JSONObject request=object(arg);
                fields(request,"revision","profile","layout");
                LayoutDraft.requireCurrent(request.getString("revision"),revision(ctx,prefs.getAll()),request.getString("profile"),ProfileStore.id());
                long revision=prefs.getLong(Config.SETTINGS_REVISION_KEY,0);
                String next;
                if("product_layout_apply".equals(method)) {
                    LayoutDraft draft=parse(request.getJSONObject("layout"));
                    Map<String,String> apps=apps(ctx);
                    for(String pkg:draft.favorites)LayoutDraft.check(apps.containsKey(pkg),"Favorite app is unavailable: "+pkg);
                    // A rename must retain native order/dock membership. An explicit reorder wins until
                    // the owner next saves an order with the remote; never overwrite order.txt here.
                    boolean same=draft.favorites.equals(favorites(config,Order.load(Order.file(ctx))));
                    next=new JSONObject().put("layout",json(draft)).put("dock",new JSONArray(same?config.dockApps:draft.favorites))
                            .put("priority",!same||config.desktopOrder).put("orderStamp",orderStamp(ctx)).toString();
                    LayoutDraft.check(next.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES,"Layout is too large");
                } else {
                    LayoutDraft.check(revision(ctx,prefs.getAll()).equals(prefs.getString(GUARD,"")),"Undo is no longer current. Reload first.");
                    next=prefs.getString(UNDO,"");
                }
                Map<String,Object> predicted=new HashMap<>(prefs.getAll());
                if(next.isEmpty())predicted.remove(STATE);else predicted.put(STATE,next);
                predicted.put(Config.SETTINGS_REVISION_KEY,revision+1);
                if(!commit(prefs,next,revision(ctx,predicted),"product_layout_undo".equals(method)))
                    throw new IllegalStateException("Could not save layout. Reload before trying again.");
                ctx.getContentResolver().notifyChange(uri(ctx),null);
                config=Config.load(ctx);
            } else if(!"product_layout_snapshot".equals(method))throw new IllegalArgumentException("Unknown editor operation");
            Bundle result=new Bundle();result.putString("json",snapshot(ctx,config).toString());return result;
        } catch(SecurityException denied){throw denied;}
        catch(Exception failure){throw new IllegalArgumentException(failure.getMessage(),failure);}
    }
    static boolean commit(SharedPreferences prefs,String next,String guard,boolean undo) {
        String old=prefs.getString(STATE,null),oldUndo=prefs.getString(UNDO,null),oldGuard=prefs.getString(GUARD,null);
        long revision=prefs.getLong(Config.SETTINGS_REVISION_KEY,0);
        SharedPreferences.Editor edit=prefs.edit();
        if(next.isEmpty())edit.remove(STATE);else edit.putString(STATE,next);
        if(undo)edit.remove(UNDO).remove(GUARD);
        else edit.putString(UNDO,old==null?"":old).putString(GUARD,guard);
        edit.putLong(Config.SETTINGS_REVISION_KEY,revision+1);
        if(edit.commit())return true;
        SharedPreferences.Editor repair=prefs.edit();
        if(old==null)repair.remove(STATE);else repair.putString(STATE,old);
        if(oldUndo==null)repair.remove(UNDO);else repair.putString(UNDO,oldUndo);
        if(oldGuard==null)repair.remove(GUARD);else repair.putString(GUARD,oldGuard);
        repair.putLong(Config.SETTINGS_REVISION_KEY,revision).commit();return false;
    }
    private static JSONObject snapshot(Context ctx,Config config)throws Exception {
        SharedPreferences p=ProfileStore.preferences(ctx);
        List<String> favorites=favorites(config,Order.load(Order.file(ctx)));
        LayoutDraft draft=new LayoutDraft(favorites,config.hiddenApps,config.appLabels);
        String token=revision(ctx,p.getAll());
        JSONObject state=new JSONObject().put("schema",1).put("revision",token).put("profile",ProfileStore.id()).put("layout",json(draft));
        JSONArray catalog=new JSONArray();
        for(Map.Entry<String,String> app:apps(ctx).entrySet())catalog.put(new JSONObject().put("package",app.getKey()).put("label",app.getValue()));
        return state.put("apps",catalog).put("canUndo",p.contains(UNDO)&&token.equals(p.getString(GUARD,"")));
    }
    private static List<String> favorites(Config config,Order order) {
        List<String> favorites=new ArrayList<>(config.dockApps);
        if(!config.desktopOrder && order!=null && order.dock.equals(Order.dockKey(config.dockApps))) {
            List<String> ranked=new ArrayList<>();
            int count=order.docked<0?order.keys.size():order.docked;
            for(String component:order.keys.subList(0,Math.min(count,order.keys.size()))) {
                String pkg=component.split("/",2)[0];
                if(LayoutDraft.packageName(pkg) && (order.docked>=0 || favorites.contains(pkg)) && !ranked.contains(pkg))ranked.add(pkg);
            }
            for(String pkg:favorites)if(!ranked.contains(pkg))ranked.add(pkg);
            favorites=ranked;
        }
        return favorites;
    }
    private static String orderStamp(Context ctx)throws Exception {
        File file=Order.file(ctx);
        LayoutDraft.check(file.length()<=262144,"Configuration too large for desktop editing");
        return file.lastModified()+":"+file.length()+":"+hex(MessageDigest.getInstance("SHA-256").digest(file.isFile()?Files.readAllBytes(file.toPath()):new byte[0]));
    }
    private static Map<String,String> apps(Context ctx) {
        Map<String,String> result=new TreeMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})
            for(ResolveInfo item:ctx.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0))
                if(item.activityInfo!=null && item.activityInfo.exported && item.activityInfo.enabled && item.activityInfo.applicationInfo.enabled)
                    result.put(item.activityInfo.packageName,item.loadLabel(ctx.getPackageManager()).toString());
        return result;
    }
    private static String revision(Context ctx,Map<String,?> values)throws Exception {
        TreeMap<String,String> stable=new TreeMap<>();
        for(Map.Entry<String,?> entry:values.entrySet())if(!entry.getKey().equals(UNDO)&&!entry.getKey().equals(GUARD)) {
            Object value=entry.getValue();
            if(value instanceof java.util.Set){List<String> sorted=new ArrayList<>((java.util.Set<String>)value);Collections.sort(sorted);value=sorted;}
            stable.put(entry.getKey(),String.valueOf(value));
        }
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        digest.update((ProfileStore.id()+"\n"+new JSONObject(stable)).getBytes(StandardCharsets.UTF_8));
        LayoutDraft.check(Config.dir(ctx)!=null,"TV storage is unavailable");
        for(File file:new File[]{Order.file(ctx),new File(Config.dir(ctx),"config.json"),new File(Config.dir(ctx),"theme_revision")}) {
            digest.update(file.getName().getBytes(StandardCharsets.UTF_8));
            if(file.isFile()){LayoutDraft.check(file.length()<=262144,"Configuration too large for desktop editing");digest.update(Files.readAllBytes(file.toPath()));}
            if(file.equals(Order.file(ctx)))digest.update(Long.toString(file.lastModified()).getBytes(StandardCharsets.UTF_8));
        }
        return hex(digest.digest());
    }
    private static String hex(byte[] bytes){StringBuilder text=new StringBuilder();for(byte b:bytes)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return text.toString();}
    private static JSONObject object(String text)throws Exception {
        LayoutDraft.check(text!=null && text.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES,"Editor request is too large");
        ThemeArchive.validateJsonObject(text);return new JSONObject(text);
    }
    private static LayoutDraft parse(JSONObject value)throws Exception {
        fields(value,"schema","favorites","hidden","labels");
        Object version=value.get("schema");
        LayoutDraft.check((version instanceof Integer||version instanceof Long) && ((Number)version).longValue()==1,"Unsupported layout version");
        Map<String,String> labels=new TreeMap<>();JSONObject names=value.getJSONObject("labels");
        for(Iterator<String> it=names.keys();it.hasNext();){String key=it.next();LayoutDraft.check(names.get(key) instanceof String,"Invalid app name");labels.put(key,names.getString(key));}
        return new LayoutDraft(strings(value.getJSONArray("favorites")),strings(value.getJSONArray("hidden")),labels);
    }
    private static List<String> strings(JSONArray array)throws Exception {
        List<String> values=new ArrayList<>();for(int i=0;i<array.length();i++){LayoutDraft.check(array.get(i) instanceof String,"Expected app package");values.add(array.getString(i));}return values;
    }
    private static JSONObject json(LayoutDraft value)throws Exception {
        return new JSONObject().put("schema",1).put("favorites",new JSONArray(value.favorites)).put("hidden",new JSONArray(value.hidden)).put("labels",new JSONObject(value.labels));
    }
    private static void fields(JSONObject value,String... allowed) {
        java.util.Set<String> keys=new java.util.HashSet<>(java.util.Arrays.asList(allowed));
        for(Iterator<String> it=value.keys();it.hasNext();)LayoutDraft.check(keys.contains(it.next()),"Unsupported editor field");
    }
    private DesktopLayout(){}
}
