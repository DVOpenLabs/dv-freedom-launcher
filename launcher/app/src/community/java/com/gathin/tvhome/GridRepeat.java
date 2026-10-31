package com.gathin.tvhome;

import android.app.*;
import android.view.KeyEvent;
import android.widget.Toast;
import java.util.*;

final class GridRepeat {
    private static final Map<MainActivity,GridRepeatPolicy> owners=new WeakHashMap<>();
    static boolean key(MainActivity a,KeyEvent e){
        int interval=ProfileStore.preferences(a).getInt(GridRepeatPolicy.KEY,0);
        return owners.computeIfAbsent(a,x->new GridRepeatPolicy()).consume(e.getKeyCode(),e.getAction()==KeyEvent.ACTION_DOWN,
                e.getRepeatCount(),e.getEventTime(),interval,a.productGridRepeatAllowed());
    }
    static void stop(Activity a){GridRepeatPolicy p=owners.remove(a);if(p!=null)p.reset();}
    static String label(Activity a){int value=ProfileStore.preferences(a).getInt(GridRepeatPolicy.KEY,0);return value==180?"Gentle":value==280?"Slow":"System";}
    static void show(Activity a){
        String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();
        String[] labels={"System · unchanged","Gentle · up to one repeat every 180 ms","Slow · up to one repeat every 280 ms"};int[] values={0,180,280};
        android.widget.LinearLayout body=FreedomUi.body(a);
        body.addView(FreedomUi.text(a,"Slows held arrows in Classic’s multi-row app grid. The first press is immediate. Single-row layouts, other Home experiences, menus, volume and Input use their normal behavior.",17));
        final AlertDialog[] dialog={null};
        for(int i=0;i<labels.length;i++){final int n=i;FreedomUi.action(a,body,labels[i],()->{
            dialog[0].dismiss();
            if(!ProductFeatures.allowEdit(a,()->show(a)))return;
            if(!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll())){Toast.makeText(a,"Home changed. Open held arrow speed again.",Toast.LENGTH_LONG).show();return;}
            if(ProfileStore.preferences(a).getInt(GridRepeatPolicy.KEY,0)==values[n])return;
            ProductFeatures.beforeChange(a,"Held arrow speed");Map<String,Object> changes=new HashMap<>();changes.put(GridRepeatPolicy.KEY,values[n]);changes.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);
            if(SetupValues.commit(ProfileStore.preferences(a),changes))a.finish();else Toast.makeText(a,"Could not save arrow speed",Toast.LENGTH_LONG).show();
        });}
        dialog[0]=FreedomUi.sheet(a,"Held arrow speed · Classic grid",body);

    }
    private GridRepeat(){}
}
