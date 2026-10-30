package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.widget.Toast;
import java.util.*;

/** Profile-owned browser exclusions. Never removes favorites, folder membership or Android apps. */
final class LibraryVisibility {
    static final String KEY="library_excluded_apps_v1";
    static Set<String> parse(String text){
        if(text==null||text.length()>16384)throw new IllegalArgumentException("App list is too large");
        Set<String> out=new LinkedHashSet<>();
        if(!text.isEmpty())for(String p:text.split("\n",-1))if(!LayoutDraft.packageName(p)||!out.add(p)||out.size()>64)throw new IllegalArgumentException("Invalid excluded app");
        return out;
    }
    static String encode(Collection<String> packages){String text=String.join("\n",new TreeSet<>(packages));parse(text);return text;}
    static Set<String> read(Context c){try{return parse(ProfileStore.preferences(c).getString(KEY,""));}catch(RuntimeException bad){return Collections.emptySet();}}
    static boolean allowed(Context c,String pkg){return !read(c).contains(pkg);}
    static void show(Activity a){
        if(!LauncherLock.require(a,()->show(a)))return;
        Config.load(a);String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();Set<String> current=read(a);
        Map<String,String> apps=new TreeMap<>();PackageManager pm=a.getPackageManager();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){
            if(r.activityInfo!=null&&r.activityInfo.exported&&r.activityInfo.enabled&&r.activityInfo.applicationInfo.enabled&&!r.activityInfo.packageName.equals(a.getPackageName()))apps.putIfAbsent(r.activityInfo.packageName,String.valueOf(r.loadLabel(pm)));
        }
        for(String pkg:current)apps.putIfAbsent(pkg,pkg+" (unavailable)");
        List<String> packages=new ArrayList<>(apps.keySet());packages.sort((l,r)->apps.get(l).compareToIgnoreCase(apps.get(r)));
        String[] labels=new String[packages.size()];boolean[] checked=new boolean[packages.size()];
        for(int i=0;i<labels.length;i++){String p=packages.get(i);labels[i]=apps.get(p);checked[i]=current.contains(p);}
        FreedomUi.checklist(a,"Hide from app browsers · existing Home shortcuts stay",labels,checked,d->{try{
            Config.load(a);if(!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll()))throw new IllegalStateException("Home changed. Open this list again.");
            if(!LauncherLock.open(a))throw new IllegalStateException("Unlock launcher editing first");
            Set<String> selected=new HashSet<>();for(int i=0;i<checked.length;i++)if(checked[i])selected.add(packages.get(i));String value=encode(selected);
            if(!value.equals(encode(current))){ProductFeatures.beforeChange(a,"App browser exclusions");Map<String,Object> writes=new HashMap<>();writes.put(KEY,value);writes.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);if(!SetupValues.commit(ProfileStore.preferences(a),writes))throw new IllegalStateException("Could not save app exclusions");}
            d.dismiss();a.recreate();
        }catch(RuntimeException bad){Toast.makeText(a,bad.getMessage(),Toast.LENGTH_LONG).show();}});
    }
    private LibraryVisibility(){}
}
