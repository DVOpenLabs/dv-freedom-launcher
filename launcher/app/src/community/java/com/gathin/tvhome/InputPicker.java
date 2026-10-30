package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.media.tv.*;
import android.os.*;
import android.widget.*;
import java.util.*;

/** One foreground-only public TV input model, including CEC children and native custom names. */
final class InputPicker {
    static final class Item {final String id,label;final int state;final Intent target;Item(String id,String label,int state){this(id,label,state,new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(id)));}Item(String id,String label,int state,Intent target){this.id=id;this.label=label;this.state=state;this.target=target;}}
    static List<Item> list(Activity a,Config cfg,boolean all){
        List<Item> result=new ArrayList<>();TvInputManager m=a.getSystemService(TvInputManager.class);if(m==null)return result;
        try{
            List<TvInputInfo> inputs=new ArrayList<>(m.getTvInputList());inputs.sort(Comparator.comparing(x->String.valueOf(x.loadLabel(a))));
            Map<String,TvInputInfo> by=new HashMap<>();Set<String> parents=new HashSet<>();for(TvInputInfo i:inputs)by.put(i.getId(),i);
            for(TvInputInfo i:inputs)if(i.isPassthroughInput()&&!i.isHidden(a)&&i.getParentId()!=null&&by.containsKey(i.getParentId()))parents.add(i.getParentId());
            int ports=0,connected=0;for(TvInputInfo i:inputs)if(i.getType()==TvInputInfo.TYPE_HDMI&&i.getParentId()==null){ports++;if(state(m,i.getId())==TvInputManager.INPUT_STATE_CONNECTED)connected++;}
            boolean unknown=ports>1&&ports==connected;
            for(TvInputInfo i:inputs){if(result.size()>=64)break;if(!i.isPassthroughInput()||i.isHidden(a)||parents.contains(i.getId()))continue;
                boolean hdmi=i.getType()==TvInputInfo.TYPE_HDMI,child=i.getParentId()!=null;int s=!hdmi||unknown?-1:state(m,i.getId());
                if(!all&&((cfg.inputsConnectedOnly&&InputFold.folds(hdmi,s))||(!child&&hdmi&&!HdmiVisibility.visible(cfg.hideInactiveHdmi,s))))continue;
                String label=InputLabelPolicy.title(i.loadCustomLabel(a),i.loadLabel(a),hdmi&&!child);if(label==null)label=i.getId();
                TvInputInfo parent=by.get(i.getParentId());if(parent!=null){String port=InputLabelPolicy.title(null,parent.loadLabel(a),parent.getType()==TvInputInfo.TYPE_HDMI);if(!label.equals(port))label+=" · "+port;}
                Intent target=new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(i.getId()));if(a.getPackageManager().resolveActivity(target,0)!=null)result.add(new Item(i.getId(),label,s));
            }
        }catch(RuntimeException unavailable){/* A denied/missing TV provider leaves native settings available. */}
        if(!cfg.liveTvPackage.isEmpty())try{Intent tv=a.getPackageManager().getLeanbackLaunchIntentForPackage(cfg.liveTvPackage);if(tv==null&&!cfg.liveTvAction.isEmpty()){Intent action=new Intent(cfg.liveTvAction).setPackage(cfg.liveTvPackage);if(action.resolveActivity(a.getPackageManager())!=null)tv=action;}if(tv!=null)result.add(new Item("tv:"+cfg.liveTvPackage,a.getString(R.string.live_tv),-1,tv));}catch(RuntimeException unavailable){}
        return result;
    }
    static int state(TvInputManager m,String id){try{return m.getInputState(id);}catch(RuntimeException unknown){return -1;}}
    static void show(Activity a,boolean all){
        Config cfg=Config.load(a);List<Item> items=list(a,cfg,all);
        if(items.isEmpty()&&all){InputShortcut.help(a);return;}
        ListView list=new ListView(a);List<String> labels=new ArrayList<>();for(Item i:items)labels.add(i.label+(i.state==0?" · Connected":i.state==2?" · Disconnected":""));
        if(!all)labels.add(a.getString(R.string.freedom_inputs_all));labels.add(a.getString(R.string.tv_settings));
        list.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_list_item_1,labels));AlertDialog d=new AlertDialog.Builder(a).setTitle(R.string.inputs).setView(list).setNegativeButton("Back",null).create();
        list.setOnItemClickListener((p,v,n,id)->{d.dismiss();if(n<items.size()){try{a.startActivity(items.get(n).target);}catch(RuntimeException bad){Toast.makeText(a,R.string.not_available,Toast.LENGTH_LONG).show();}}else if(!all&&n==items.size())show(a,true);else try{a.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}catch(RuntimeException bad){Toast.makeText(a,R.string.not_available,Toast.LENGTH_LONG).show();}});
        TvInputManager manager=a.getSystemService(TvInputManager.class);Handler h=new Handler(Looper.getMainLooper());
        TvInputManager.TvInputCallback callback=new TvInputManager.TvInputCallback(){void refresh(){if(!d.isShowing())return;int selection=list.getSelectedItemPosition();String selected=selection>=0&&selection<items.size()?items.get(selection).id:"";List<Item> next=list(a,cfg,all);items.clear();items.addAll(next);labels.clear();for(Item i:items)labels.add(i.label+(i.state==0?" · Connected":i.state==2?" · Disconnected":""));if(!all)labels.add(a.getString(R.string.freedom_inputs_all));labels.add(a.getString(R.string.tv_settings));((ArrayAdapter<?>)list.getAdapter()).notifyDataSetChanged();int at=0;for(int n=0;n<items.size();n++)if(items.get(n).id.equals(selected))at=n;list.setSelection(at);}public void onInputStateChanged(String id,int state){refresh();}public void onInputAdded(String id){refresh();}public void onInputRemoved(String id){refresh();}};
        Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity x,Bundle b){}public void onActivityStarted(Activity x){}public void onActivityStopped(Activity x){}public void onActivitySaveInstanceState(Activity x,Bundle b){}public void onActivityResumed(Activity x){}public void onActivityPaused(Activity x){if(x==a)d.dismiss();}public void onActivityDestroyed(Activity x){if(x==a)d.dismiss();}};
        d.setOnShowListener(x->{a.getApplication().registerActivityLifecycleCallbacks(lifecycle);list.requestFocusFromTouch();list.setSelection(0);if(manager!=null)try{manager.registerCallback(callback,h);}catch(RuntimeException ignored){}});
        d.setOnDismissListener(x->{a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);if(manager!=null)try{manager.unregisterCallback(callback);}catch(RuntimeException ignored){}h.removeCallbacksAndMessages(null);});d.show();
    }
    private InputPicker(){}
}
