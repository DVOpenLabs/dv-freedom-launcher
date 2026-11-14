package com.gathin.tvhome;
import android.app.*;
import android.widget.Toast;
import java.util.*;
/** One guarded appearance undo per profile in this process. Persistent recovery stays in Recent changes. */
final class StudioUndo {
    private static final Map<String,Entry> entries=new LinkedHashMap<>();
    private static final class Entry {Map<String,Object> restore;Map<String,?> expected;}
    static void remember(String profile,Map<String,?> before,Map<String,?> after,Set<String> changed){Entry e=new Entry();e.restore=new HashMap<>();for(String k:changed)if(!k.equals(Config.SETTINGS_REVISION_KEY))e.restore.put(k,before.get(k));e.expected=new HashMap<>(after);entries.put(profile,e);while(entries.size()>16)entries.remove(entries.keySet().iterator().next());}
    static boolean available(){return entries.containsKey(ProfileStore.id());}
    static void apply(Activity a){if(!ProductFeatures.allowEdit(a,()->apply(a)))return;String id=ProfileStore.id();Entry e=entries.get(id);if(e==null)return;android.content.SharedPreferences p=ProfileStore.preferences(a);if(!e.expected.equals(p.getAll())){Toast.makeText(a,"Home changed since that apply. Use Recent changes to review a restore.",Toast.LENGTH_LONG).show();return;}Map<String,Object> changes=new HashMap<>(e.restore);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);ProductFeatures.beforeChange(a,"Undo Personalisation Studio");if(SetupValues.commit(p,changes)){entries.remove(id);a.recreate();}else Toast.makeText(a,"Undo could not save. Your current choices remain.",Toast.LENGTH_LONG).show();}
    private StudioUndo(){}
}
