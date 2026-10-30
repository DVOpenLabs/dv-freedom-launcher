package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.widget.*;

/** Explicit launches only; no install, grant, pairing or system screensaver changes. */
final class OptionalTools {
    static final String[] PACKAGES={"io.github.shaharukhs.tvinspector","com.gathin.btconnect","io.github.dennisoleary.digitalrain","com.gathin.newsdream","io.github.maxhayim.headphones","com.lionlev.tv.capacity","io.github.maxhayim.tvhub","io.github.maxhayim.adblocking","io.github.cleanyourtv.flock","com.lionlev.tv.jewishcalendar","com.oneyuv.tv.nepalicalendar","com.majnioui.tv.prayer","com.dvopenlabs.freedom.camera"};
    static void show(Activity a){String[] names={"TV Inspector · remote codes and capabilities","Bluetooth Connect · paired audio devices","Digital Rain · local ambient preview","News & RSS · configure your own feeds","Headphones · output, battery and optional disconnect guard","Device capacity · storage and memory information","TV Hub · your configured Live TV sources","Ad Blocking · your existing Pi-hole/AdGuard server","Flock · local ambient scene","Jewish calendar · offline calendar","Nepali calendar · offline calendar","Prayer times · configure city and calculation method","DV Freedom Camera · optional silent camera view"};LinearLayout b=FreedomUi.body(a);b.addView(FreedomUi.text(a,"Install optional tools one at a time through Setup → Add-on Centre. Opening a tool never selects a screensaver or connects Bluetooth. Availability varies by firmware.",17));for(int i=0;i<names.length;i++){String pkg=PACKAGES[i];FreedomUi.action(a,b,names[i],()->open(a,pkg));}FreedomUi.sheet(a,"Optional TV tools",b);}
    static void open(Activity a,String pkg){Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(pkg);ResolveInfo r=a.getPackageManager().resolveActivity(i,0);if(r==null||r.activityInfo==null||!r.activityInfo.exported||!r.activityInfo.enabled){Toast.makeText(a,"Install this optional tool from desktop Setup → Add-on Center first.",Toast.LENGTH_LONG).show();return;}try{a.startActivity(i.setComponent(new ComponentName(pkg,r.activityInfo.name)));}catch(RuntimeException denied){Toast.makeText(a,"This tool could not open. Try TV Settings instead.",Toast.LENGTH_LONG).show();}}
    private OptionalTools(){}
}
