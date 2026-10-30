package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.widget.*;
import java.util.*;

/** Device-wide startup choice shared by every Home experience. No network requests or polling. */
final class StartupLaunch {
    static final String WAIT="open_on_start_network_wait";
    private static final Map<Activity,Session> ACTIVE=new WeakHashMap<>();
    static String label(Context c){SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);String pkg=p.getString(OpenOnStart.KEY,"");String name=pkg;try{name=String.valueOf(c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg,0)));}catch(Exception ignored){}return pkg.isEmpty()?"Off":name+" · "+p.getInt(WAIT,0)+" s network wait";}
    static void start(Activity a){
        if(ACTIVE.containsKey(a)||a.isFinishing()||a.isDestroyed()||!a.hasWindowFocus()||PlacementSession.preview()!=null||StyleSession.preview()!=null||ExperienceChoice.preview()!=null)return;
        Config cfg=Config.load(a);SharedPreferences global=a.getSharedPreferences(Config.SETTINGS_NAME,0);String pkg=global.getString(OpenOnStart.KEY,"");
        if(!cfg.bootToLiveTv&&!cfg.openOnStartChoice&&!global.contains(WAIT))return;
        boolean configuredLive=cfg.bootToLiveTv&&!global.contains(WAIT);
        String target=configuredLive?cfg.liveTvPackage:pkg;int boot=OpenOnStart.boot(a);SharedPreferences handled=a.getSharedPreferences(OpenOnStart.BOOT_PREFS,0);
        if(!OpenOnStart.due(boot,handled.getInt(OpenOnStart.BOOT_KEY,-1),target,a.getPackageName()))return;
        Intent intent=OpenOnStart.intent(a.getPackageManager(),target);
        if(intent==null&&configuredLive&&!cfg.liveTvAction.isEmpty()){Intent action=new Intent(cfg.liveTvAction).setPackage(target);if(a.getPackageManager().resolveActivity(action,0)!=null)intent=action;}
        // A missing app consumes no boot marker; selecting another one still takes effect next boot.
        if(intent==null||!handled.edit().putInt(OpenOnStart.BOOT_KEY,boot).commit())return;
        Session s=new Session(a,intent,Math.max(0,Math.min(600,global.getInt(WAIT,0))));ACTIVE.put(a,s);s.begin();
    }
    static void stop(Activity a){Session s=ACTIVE.remove(a);if(s!=null)s.close();}
    static final class Session {
        final Activity a;final Intent intent;final ConnectivityManager cm;final Handler main=new Handler(Looper.getMainLooper());final StartupWait wait=new StartupWait();final int seconds;boolean closed,registered;
        final Runnable check=()->checkNow();
        final ConnectivityManager.NetworkCallback callback=new ConnectivityManager.NetworkCallback(){@Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){if(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))main.post(check);}};
        Session(Activity a,Intent i,int seconds){this.a=a;intent=i;this.seconds=seconds;cm=a.getSystemService(ConnectivityManager.class);}
        boolean online(){try{Network n=cm==null?null:cm.getActiveNetwork();NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);return caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);}catch(RuntimeException missing){return false;}}
        void begin(){wait.begin(SystemClock.elapsedRealtime(),seconds);if(seconds>0){try{if(cm!=null){cm.registerDefaultNetworkCallback(callback);registered=true;}}catch(RuntimeException missing){}main.postDelayed(check,seconds*1000L);}checkNow();}
        void checkNow(){if(closed)return;if(a.isFinishing()||a.isDestroyed()){stop(a);return;}if(wait.take(true,online(),SystemClock.elapsedRealtime())){Intent target=intent;stop(a);try{a.startActivity(target);}catch(RuntimeException missing){Toast.makeText(a,"Startup app is unavailable. Home is still ready.",Toast.LENGTH_LONG).show();}}}
        void close(){closed=true;wait.cancel();main.removeCallbacksAndMessages(null);if(registered)try{cm.unregisterNetworkCallback(callback);}catch(RuntimeException ignored){}registered=false;}
    }
    static void show(Activity a){
        if(!LauncherLock.require(a,()->show(a)))return;SharedPreferences global=a.getSharedPreferences(Config.SETTINGS_NAME,0);Map<String,?> before=global.getAll();String[] chosen={global.getString(OpenOnStart.KEY,"")};int[] wait={Math.max(0,Math.min(600,global.getInt(WAIT,0)))};
        LinearLayout body=FreedomUi.body(a);body.addView(FreedomUi.text(a,"Applies to this TV, across launcher profiles. Starts once after a device boot. Leaving Home cancels any wait. No reboot is performed here.",17));
        TextView app=FreedomUi.action(a,body,"App · "+(chosen[0].isEmpty()?"Off":chosen[0]),()->{});app.setOnClickListener(v->{List<HomeCategories.App> apps=HomeCategories.catalog(a,Config.load(a));apps.removeIf(x->!LibraryVisibility.allowed(a,x.pkg));String[] names=new String[apps.size()+1];names[0]="Off · stay on Home";for(int i=0;i<apps.size();i++)names[i+1]=apps.get(i).label;new AlertDialog.Builder(a).setTitle("Startup app").setItems(names,(d,n)->{chosen[0]=n==0?"":apps.get(n-1).pkg;app.setText("App · "+names[n]);}).setNegativeButton(android.R.string.cancel,null).show();});
        TextView delay=FreedomUi.action(a,body,"Network wait · "+wait[0]+" seconds",()->{});delay.setOnClickListener(v->{String[] labels={"No wait","15 seconds","30 seconds","60 seconds","2 minutes","5 minutes","10 minutes"};int[] values={0,15,30,60,120,300,600};new AlertDialog.Builder(a).setTitle("Wait for validated internet, up to…").setItems(labels,(d,n)->{wait[0]=values[n];delay.setText("Network wait · "+labels[n]);}).setNegativeButton(android.R.string.cancel,null).show();});
        final AlertDialog[] sheet={null};FreedomUi.action(a,body,"Apply startup choices",()->{if(!LauncherLock.open(a)||!before.equals(global.getAll())){Toast.makeText(a,"Settings changed. Open startup choices again.",Toast.LENGTH_LONG).show();return;}Map<String,Object> writes=new HashMap<>();writes.put(OpenOnStart.KEY,chosen[0]);writes.put(WAIT,wait[0]);if(!SetupValues.commit(global,writes)){Toast.makeText(a,"Could not save startup choices",Toast.LENGTH_LONG).show();return;}OpenOnStart.skipThisBoot(a);sheet[0].dismiss();a.recreate();});sheet[0]=FreedomUi.sheet(a,"Open on start",body);
    }
    private StartupLaunch(){}
}
