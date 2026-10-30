package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.provider.Settings;
import android.widget.*;
import java.util.*;

/** Personal quick controls use ordinary exported system screens, not hidden settings writes. */
final class FreedomControls {
    static final String KEY="freedom_control_favorites";
    static final String[] IDS={"inputs","sound","picture","network","bluetooth","apps","profiles","companions"};
    static final String[] LABELS={"Inputs","Sound output","Picture settings","Network","Pair an accessory","All apps","Profiles","Add-on Center"};
    static void validate(String s){Set<String> seen=new HashSet<>();if(s.length()>160)throw new IllegalArgumentException("Too many controls");for(String id:s.split(","))if(!Arrays.asList(IDS).contains(id)||!seen.add(id))throw new IllegalArgumentException("Invalid favorite control");}
    static void show(Activity a){Config cfg=Config.load(a);LinearLayout b=FreedomUi.body(a);b.addView(FreedomUi.text(a,"Quick access to this device’s controls. Availability depends on your TV and audio equipment.",17));
        String raw=ProfileStore.preferences(a).getString(KEY,"inputs,sound,picture,network,bluetooth");List<String> chosen=Arrays.asList(raw.split(","));for(String id:chosen){int at=Arrays.asList(IDS).indexOf(id);if(at>=0)FreedomUi.action(a,b,LABELS[at],()->open(a,id));}
        FreedomUi.action(a,b,"Choose quick controls",()->choose(a));FreedomUi.action(a,b,"Launcher settings",()->a.startActivity(new Intent(a,LauncherSettingsActivity.class)));FreedomUi.sheet(a,"Control Center",b);
    }
    static void open(Activity a,String id){try{switch(id){case "inputs":InputPicker.show(a,false);break;case "profiles":ProfilePicker.show(a,false);break;case "apps":AllAppsSurface.show(a,Config.load(a),HomeCategories.catalog(a,Config.load(a)));break;case "companions":OptionalPanels.show(a);break;case "bluetooth":CompanionControls.openPairing(a);break;default:String action=id.equals("sound")?Settings.ACTION_SOUND_SETTINGS:id.equals("picture")?Settings.ACTION_DISPLAY_SETTINGS:Settings.ACTION_WIFI_SETTINGS;Intent target=SystemSettings.intent(a,action);if(target==null||a.getPackageManager().resolveActivity(target,0)==null)throw new IllegalStateException();a.startActivity(target);}}catch(RuntimeException missing){Toast.makeText(a,"This device does not expose that control. Open TV Settings instead.",Toast.LENGTH_LONG).show();}}
    static void choose(Activity a){if(!LauncherLock.require(a,()->choose(a)))return;String profile=ProfileStore.id(),before=ProfileStore.preferences(a).getString(KEY,"inputs,sound,picture,network,bluetooth");boolean[] on=new boolean[IDS.length];for(int i=0;i<on.length;i++)on[i]=Arrays.asList(before.split(",")).contains(IDS[i]);FreedomUi.checklist(a,"Quick controls",LABELS,on,dialog->{if(!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getString(KEY,"inputs,sound,picture,network,bluetooth")))return;List<String> next=new ArrayList<>();for(int i=0;i<on.length;i++)if(on[i])next.add(IDS[i]);if(next.isEmpty()){Toast.makeText(a,"Keep at least one control",Toast.LENGTH_LONG).show();return;}ProductFeatures.beforeChange(a,"Quick controls");Map<String,Object> values=new HashMap<>();values.put(KEY,String.join(",",next));values.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);if(!SetupValues.commit(ProfileStore.preferences(a),values))Toast.makeText(a,"Could not save quick controls",Toast.LENGTH_LONG).show();else{a.getContentResolver().notifyChange(DesktopLayout.uri(a),null);dialog.dismiss();}});}
    private FreedomControls(){}
}
