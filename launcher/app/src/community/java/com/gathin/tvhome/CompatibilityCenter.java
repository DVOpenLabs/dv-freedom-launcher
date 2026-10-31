package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.tv.TvInputManager;
import android.provider.Settings;
import android.widget.*;
import java.util.*;

/** Read-only local checks with explicit routes to existing recovery/system controls. */
final class CompatibilityCenter {
    static void show(Activity a){
        LinearLayout b=FreedomUi.body(a);
        b.addView(FreedomUi.text(a,"Check this device before changing anything. These checks do not enable services, grant permissions or change TV settings.",17));
        String home="No default selected";
        try{ResolveInfo r=a.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
            if(r!=null&&r.activityInfo!=null)home=a.getPackageName().equals(r.activityInfo.packageName)?"DV Freedom is selected":"Another Home or system chooser is selected";
        }catch(RuntimeException ignored){}
        section(a,b,"Home & recovery",home);
        int visible=0;
        try{for(ResolveInfo r:a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))
            if(r.activityInfo!=null&&r.activityInfo.enabled&&r.activityInfo.exported&&!a.getPackageName().equals(r.activityInfo.packageName)
                    &&!r.activityInfo.name.toLowerCase(Locale.ROOT).matches(".*(fallback|recovery|resolver|setup|factory).*"))visible++;
        }catch(RuntimeException ignored){}
        section(a,b,"Other Home apps",visible==0?"No alternate Home visible to this app. Desktop Setup keeps its own recovery point.":visible+" visible Home candidate(s). Availability does not prove casting or remote support.");
        FreedomUi.action(a,b,"Saved setups",()->SetupUi.show(a));
        FreedomUi.action(a,b,"Recent changes",()->HomeHistory.show(a));
        section(a,b,"Optional content",ContentShelves.granted(a)?"TV listings permission allowed · shelf "+ContentShelves.status(a):"TV listings permission not allowed · shelves unavailable");
        FreedomUi.action(a,b,"Content shelf controls",()->ContentShelves.settings(a));
        String media="Access could not be checked on this firmware";try{media=NowPlaying.access(a)?"Notification listener access allowed. Now Playing in this profile: "+NowPlaying.status(a):"Notification listener access off. Streaming playback does not require it.";}catch(RuntimeException unavailable){}
        section(a,b,"Media access",media);
        section(a,b,"Weather",WeatherClient.status(a)+". Weather sharing and location are configured separately in its optional companion.");
        String saver="Availability depends on the TV";
        try{saver=Settings.Secure.getInt(a.getContentResolver(),"screensaver_enabled",0)==1?"System screensaver enabled":"System screensaver disabled";}catch(RuntimeException ignored){}
        section(a,b,"Screensaver",saver+". Idle Home and the Android screensaver are separate.");
        FreedomUi.action(a,b,"Open system screensaver settings",()->system(a,Settings.ACTION_DREAM_SETTINGS));
        boolean inputs=false;try{TvInputManager m=a.getSystemService(TvInputManager.class);inputs=m!=null&&!m.getTvInputList().isEmpty();}catch(RuntimeException ignored){}
        section(a,b,"Inputs",inputs?"This device exposes TV inputs. Use Inputs to see available sources.":"No native TV inputs exposed here. An external streaming box cannot enumerate the television’s HDMI ports; use the TV remote’s Input/Source button.");
        FreedomUi.action(a,b,"Inputs",()->InputPicker.show(a,true));
        FreedomUi.action(a,b,"Storage & USB",()->storage(a));
        section(a,b,"After an update","Check the real remote’s Home, Back, Input, Settings and held volume, then play a video in your apps. Opening an app alone does not verify playback. Desktop recovery files and ADB access are managed separately from this screen.");
        FreedomUi.sheet(a,"Compatibility & recovery",b,"Close");
    }
    static void storage(Activity a){LinearLayout b=FreedomUi.body(a);b.addView(FreedomUi.text(a,"Use Android’s Storage page to inspect space or safely eject a drive. Finish file transfers and stop media from that drive first. DV Freedom does not format, unmount or erase it.",18));FreedomUi.action(a,b,"Open system Storage",()->system(a,Settings.ACTION_INTERNAL_STORAGE_SETTINGS));FreedomUi.action(a,b,"TV settings",()->system(a,Settings.ACTION_SETTINGS));FreedomUi.sheet(a,"Storage & USB",b,"Close");}
    static void system(Activity a,String action){try{Intent target=SystemSettings.intent(a,action);if(target==null||a.getPackageManager().resolveActivity(target,PackageManager.MATCH_DEFAULT_ONLY)==null)throw new IllegalStateException();a.startActivity(target);}catch(RuntimeException absent){Toast.makeText(a,"This device does not expose that page. Use the TV’s own settings.",Toast.LENGTH_LONG).show();}}
    private static void section(Activity a,LinearLayout b,String title,String message){TextView h=FreedomUi.text(a,title,20);h.setTypeface(SettingsTypography.medium(a));h.setPadding(0,FreedomUi.dp(a,18),0,FreedomUi.dp(a,5));b.addView(h);TextView t=FreedomUi.text(a,message,17);t.setTextColor(a.getColor(R.color.freedom_secondary));t.setLineSpacing(FreedomUi.dp(a,3),1);b.addView(t);}
    private CompatibilityCenter(){}
}
