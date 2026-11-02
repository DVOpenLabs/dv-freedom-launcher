package com.gathin.tvhome;
import android.app.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Edits only the enclosing Cinema draft. Home is untouched until that draft is applied. */
final class CinemaWorldChoice {
    static void preview(Activity a){CinemaSpec saved=CinemaStore.read(a);if(saved.worlds.isEmpty()){Toast.makeText(a,"Pair and Apply cinematic worlds first",Toast.LENGTH_LONG).show();return;}AlertDialog d=new AlertDialog.Builder(a).setTitle("Preview a world · does not save").setItems(saved.worlds.stream().map(CinemaWorld::name).toArray(String[]::new),(x,n)->CinemaWorldPreview.start(a,saved.worlds.get(n).wallpaper)).setNegativeButton("Cancel",null).create();d.show();android.view.ViewGroup.LayoutParams p=d.getListView().getLayoutParams();p.height=Math.min(FreedomUi.dp(a,56)*6,a.getResources().getDisplayMetrics().heightPixels*3/5);d.getListView().setLayoutParams(p);}
    static void edit(Activity a,JSONObject draft,Runnable refresh){
        LinearLayout body=FreedomUi.body(a);TextView hint=FreedomUi.text(a,"Pair a world with an imported wallpaper. Its dock, typography, companion and collection follow that entry in your playlist. Clean, Glass and Cinema layouts are supported.",16);body.addView(hint);
        for(int i=0;i<CinemaWorld.IDS.length;i++){final int at=i;FreedomUi.action(a,body,CinemaWorld.NAMES[i],()->world(a,draft,at,refresh));}
        FreedomUi.sheet(a,"Cinematic worlds · draft",body,"Back to Cinema draft");
    }
    private static JSONObject entry(JSONObject draft,String id)throws Exception{JSONArray all=draft.optJSONArray("worlds");if(all==null){all=new JSONArray();draft.put("worlds",all);}for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(id))return all.getJSONObject(i);JSONObject o=new JSONObject().put("id",id);all.put(o);return o;}
    private static void world(Activity a,JSONObject draft,int at,Runnable refresh){
        LinearLayout body=FreedomUi.body(a);String id=CinemaWorld.IDS[at];JSONObject found=null;JSONArray all=draft.optJSONArray("worlds");if(all!=null)for(int i=0;i<all.length();i++)try{if(id.equals(all.getJSONObject(i).getString("id")))found=all.getJSONObject(i);}catch(Exception ignored){}
        final JSONObject prior=found;List<ThemeLibrary.Entry> library=ThemeLibrary.list(a);
        FreedomUi.action(a,body,"Choose matching wallpaper",()->{if(library.isEmpty()){Toast.makeText(a,"Import a wallpaper first",Toast.LENGTH_LONG).show();return;}String[] names=library.stream().map(e->e.pack.name).toArray(String[]::new);new AlertDialog.Builder(a).setTitle("Pair "+CinemaWorld.NAMES[at]).setItems(names,(d,n)->{try{entry(draft,id).put("wallpaper",library.get(n).token);refresh.run();}catch(Exception e){error(a,e);}}).setNegativeButton("Cancel",null).show();});
        if(prior!=null){String[] keys={"libraryArt","galleryArt","idleArt","hopArt"};String[] names={"Library background","Gallery background","Companion idle atlas · 12 × 2","Companion hop atlas · 6 × 4"};for(int i=0;i<keys.length;i++){final String key=keys[i];FreedomUi.action(a,body,names[i],()->art(a,prior,key));}
            FreedomUi.action(a,body,"Book covers · choose each film",()->{LinearLayout books=FreedomUi.body(a);for(int i=0;i<CinemaWorld.TITLES[at].length&&i<8;i++){final int n=i;FreedomUi.action(a,books,CinemaWorld.TITLES[at][i],()->{Map<String,String> art=CinemaAssets.library(a);List<String> ids=new ArrayList<>(art.keySet());String[] labels=ids.stream().map(art::get).toArray(String[]::new);new AlertDialog.Builder(a).setTitle("Local cover").setItems(labels,(d,k)->{try{JSONArray covers=prior.optJSONArray("covers");if(covers==null){covers=new JSONArray();prior.put("covers",covers);}while(covers.length()<=n)covers.put("");covers.put(n,ids.get(k));}catch(Exception e){error(a,e);}}).setNegativeButton("Cancel",null).show();});}FreedomUi.sheet(a,"Book covers · draft",books);});
            FreedomUi.action(a,body,"Unpair this world",()->{try{JSONArray next=new JSONArray();JSONArray rows=draft.getJSONArray("worlds");for(int i=0;i<rows.length();i++)if(!id.equals(rows.getJSONObject(i).getString("id")))next.put(rows.getJSONObject(i));draft.put("worlds",next);refresh.run();}catch(Exception e){error(a,e);}});
        }else body.addView(FreedomUi.text(a,"Choose a wallpaper before attaching optional artwork. Without personal artwork the world uses original DV scenery, book covers and symbols.",16));
        FreedomUi.sheet(a,CinemaWorld.NAMES[at]+" · draft",body);
    }
    private static void art(Activity a,JSONObject o,String key){Map<String,String> library=CinemaAssets.library(a);List<String> ids=new ArrayList<>(library.keySet());ids.add(0,"");String[] names=ids.stream().map(id->id.isEmpty()?"Original DV treatment":library.get(id)).toArray(String[]::new);new AlertDialog.Builder(a).setTitle("Choose local artwork").setItems(names,(d,n)->{try{o.put(key,ids.get(n));}catch(Exception e){error(a,e);}}).setNegativeButton("Cancel",null).show();}
    private static void error(Activity a,Exception e){Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show();}
    private CinemaWorldChoice(){}
}
