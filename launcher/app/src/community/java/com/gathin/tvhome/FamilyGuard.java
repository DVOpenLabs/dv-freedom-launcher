package com.gathin.tvhome;
import android.accessibilityservice.*;
import android.content.*;
import android.view.accessibility.AccessibilityEvent;
import android.os.*;
import java.lang.ref.WeakReference;

/** Optional explicit Accessibility guard. Package names only; no nodes, text, history or key filtering. */
public final class FamilyGuard extends AccessibilityService {
    static WeakReference<FamilyGuard> active=new WeakReference<>(null);long last;String previous="";final java.util.Set<String> inputs=new java.util.HashSet<>();
    @Override public void onServiceConnected(){active=new WeakReference<>(this);Config.load(this);update();}
    static void refresh(Context c){FamilyGuard service=active.get();if(service!=null)service.update();}
    void update(){inputs.clear();try{android.media.tv.TvInputManager manager=(android.media.tv.TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);if(manager!=null)for(android.media.tv.TvInputInfo input:manager.getTvInputList())inputs.add(input.getServiceInfo().packageName);}catch(RuntimeException ignored){}AccessibilityServiceInfo info=getServiceInfo();if(info!=null){info.eventTypes=FamilyMode.guard(this)?AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:0;setServiceInfo(info);}}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){if(!FamilyMode.guard(this)||event==null||event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||event.getPackageName()==null)return;String pkg=event.getPackageName().toString();if(pkg.equals(getPackageName())||FamilyMode.allowed(this,pkg)||FamilyGuardPolicy.system(pkg,inputs))return;long now=SystemClock.elapsedRealtime();if(pkg.equals(previous)&&now-last<2000)return;previous=pkg;last=now;try{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(RuntimeException denied){/* Firmware may decline; never change default Home or escalate. */}}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(active.get()==this)active.clear();super.onDestroy();}
}
