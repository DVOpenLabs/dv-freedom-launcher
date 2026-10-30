package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.text.InputFilter;
import android.widget.*;
import java.util.*;

/** One validated app route used by tiles, folders and top-bar app buttons. */
final class AppShortcuts {
    static Map<String,String> read(Context c){try{return ShortcutPolicy.parse(ProfileStore.preferences(c).getString(ShortcutPolicy.KEY,""));}catch(RuntimeException invalid){return Collections.emptyMap();}}
    static Intent intent(Context c,String pkg,String action,Intent fallback){
        String link=read(c).get(pkg);if(link!=null)action="view:"+link;if(action==null)return fallback;
        Intent candidate;
        try{candidate=action.startsWith("view:")?new Intent(Intent.ACTION_VIEW,Uri.parse(ShortcutPolicy.uri(action.substring(5)))):new Intent(action);candidate.setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo resolved=c.getPackageManager().resolveActivity(candidate,0);if(resolved==null||resolved.activityInfo==null||!resolved.activityInfo.exported||!resolved.activityInfo.enabled||!pkg.equals(resolved.activityInfo.packageName))return fallback;
            String permission=resolved.activityInfo.permission;if(permission!=null&&c.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED)return fallback;
            // Resolve once, then pin the exact exported component instead of allowing another package to take over.
            return candidate.setComponent(new ComponentName(pkg,resolved.activityInfo.name));
        }catch(RuntimeException unavailable){return fallback;}
    }
    static void show(Activity a){List<HomeCategories.App> apps=HomeCategories.catalog(a,Config.load(a));apps.removeIf(x->!LibraryVisibility.allowed(a,x.pkg));String[] labels=new String[apps.size()];for(int i=0;i<labels.length;i++)labels[i]=apps.get(i).label;new AlertDialog.Builder(a).setTitle("App links").setItems(labels,(d,n)->edit(a,apps.get(n).pkg)).setNegativeButton(android.R.string.cancel,null).show();}
    static void edit(Activity a,String pkg){
        if(!LauncherLock.require(a,()->edit(a,pkg)))return;Config.load(a);String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();Map<String,String> links=new LinkedHashMap<>(read(a));
        LinearLayout body=FreedomUi.body(a);body.addView(FreedomUi.text(a,"Open this app at a specific link. The app must support it; otherwise the normal app opens. No link is tested or opened while editing. Leave blank to use the theme/default route.",17));EditText input=new EditText(a);input.setSingleLine(true);input.setHint("https://… or an app-specific link");input.setText(links.getOrDefault(pkg,""));input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});body.addView(input);
        final AlertDialog[] sheet={null};FreedomUi.action(a,body,"Apply app link",()->{try{Config.load(a);if(!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll())||!LauncherLock.open(a))throw new IllegalArgumentException("Home changed. Open app links again.");String value=input.getText().toString().trim();if(value.isEmpty())links.remove(pkg);else links.put(pkg,ShortcutPolicy.uri(value));String raw=ShortcutPolicy.encode(links);
            if(!raw.equals(ProfileStore.preferences(a).getString(ShortcutPolicy.KEY,""))){ProductFeatures.beforeChange(a,"App link");Map<String,Object> writes=new HashMap<>();writes.put(ShortcutPolicy.KEY,raw);writes.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);if(!SetupValues.commit(ProfileStore.preferences(a),writes))throw new IllegalArgumentException("Could not save app link");}sheet[0].dismiss();a.recreate();
        }catch(RuntimeException bad){input.setError(bad.getMessage());}});String title=pkg;try{title=String.valueOf(a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(pkg,0)));}catch(Exception ignored){}sheet[0]=FreedomUi.sheet(a,"App link · "+title,body);
    }
    private AppShortcuts(){}
}
