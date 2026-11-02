package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.widget.Toast;
import java.util.*;
/** Same profile/revision guard as the other native appearance editors. */
final class CinemaStore {
    static String revision(Context c){try{return SetupTransfer.revision(c);}catch(Exception unavailable){throw new IllegalStateException("Could not read Home. Open the editor again.");}}
    static CinemaSpec read(Context c){try{return new CinemaSpec(ProfileStore.preferences(c).getString(CinemaSpec.KEY,""));}catch(RuntimeException invalid){return new CinemaSpec("");}}
    static final class Draft {
        final String profile,revision;final CinemaSpec spec;
        Draft(Context c){Config.load(c);profile=ProfileStore.id();revision=CinemaStore.revision(c);spec=read(c);}
        void save(Activity a,String raw)throws Exception{
            if(!ProductFeatures.layoutEditable(a))throw new IllegalStateException("Unlock editing on the TV first");Config.load(a);
            if(!profile.equals(ProfileStore.id())||!revision.equals(SetupTransfer.revision(a)))throw new IllegalStateException("Home changed. Open Cinema Studio again.");
            CinemaSpec next=new CinemaSpec(raw);CinemaAssets.validate(a,next);String value=next.json.toString();if(value.equals(spec.json.toString()))return;
            ProductFeatures.beforeChange(a,"Cinema Studio");Map<String,Object> changes=new HashMap<>();changes.put(CinemaSpec.KEY,value);changes.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);
            if(!SetupValues.commit(ProfileStore.preferences(a),changes))throw new IllegalStateException("Could not save Cinema Studio");a.getContentResolver().notifyChange(DesktopLayout.uri(a),null);
        }
    }
}
