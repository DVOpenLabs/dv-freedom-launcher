package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Profile-owned collections. Browsing never writes; Apply participates in the shared recovery contract. */
final class Folders {
    static List<FolderPolicy.Folder> read(Context c){try{return FolderPolicy.parse(ProfileStore.preferences(c).getString(FolderPolicy.KEY,""));}catch(RuntimeException bad){return Collections.emptyList();}}
    static void show(Activity a){
        Config config=Config.load(a);LinearLayout b=FreedomUi.body(a);b.addView(FreedomUi.text(a,"Group your apps. Removing a folder keeps the apps and your saved app order.",17));
        FreedomUi.action(a,b,"Create folder",()->edit(a,null));for(FolderPolicy.Folder f:read(a))FreedomUi.action(a,b,f.name+" · "+f.apps.size()+" apps",()->options(a,f));FreedomUi.sheet(a,"Your folders",b);
    }
    static void options(Activity a,FolderPolicy.Folder f){new AlertDialog.Builder(a).setTitle(f.name).setItems(new String[]{"Open folder","Edit name and apps","Arrange apps","Move earlier","Move later","Remove folder"},(d,n)->{
        if(n==0)open(a,f);else if(n==1)edit(a,f);else if(n==2)arrange(a,f);else{
            if(!LauncherLock.require(a,()->options(a,f)))return;String before=stamp(a);List<FolderPolicy.Folder> all=new ArrayList<>(read(a));int at=-1;for(int i=0;i<all.size();i++)if(all.get(i).id.equals(f.id))at=i;if(at<0)return;
            if(n==5){int index=at;new AlertDialog.Builder(a).setTitle("Remove "+f.name+"?").setMessage("Apps stay installed and return to their original Home positions.").setNegativeButton("Cancel",null).setPositiveButton("Remove folder",(x,w)->{all.remove(index);save(a,before,all);}).show();}
            else{int to=at+(n==3?-1:1);if(to>=0&&to<all.size()){Collections.swap(all,at,to);save(a,before,all);}}
        }
    }).setNegativeButton("Back",null).show();}
    static void arrange(Activity a,FolderPolicy.Folder f){
        if(!LauncherLock.require(a,()->arrange(a,f)))return;String before=stamp(a);
        Map<String,String> labels=new HashMap<>();for(HomeCategories.App app:HomeCategories.catalog(a,Config.load(a)))labels.put(app.pkg,app.label);
        String[] names=new String[f.apps.size()];for(int i=0;i<names.length;i++)names[i]=labels.getOrDefault(f.apps.get(i),f.apps.get(i)+" (unavailable)");
        new AlertDialog.Builder(a).setTitle(f.name+" · Arrange apps").setItems(names,(d,n)->new AlertDialog.Builder(a).setTitle(names[n]).setItems(new String[]{"Move earlier","Move later"},(x,direction)->{
            int to=n+(direction==0?-1:1);if(to<0||to>=f.apps.size())return;List<String> members=new ArrayList<>(f.apps);Collections.swap(members,n,to);
            List<FolderPolicy.Folder> all=new ArrayList<>(read(a));FolderPolicy.Folder next=new FolderPolicy.Folder(f.id,f.name,members);for(int i=0;i<all.size();i++)if(all.get(i).id.equals(f.id))all.set(i,next);
            if(save(a,before,all))arrange(a,next);
        }).setNegativeButton(android.R.string.cancel,null).show()).setNegativeButton(android.R.string.cancel,null).show();
    }
    static String stamp(Context c){return ProfileStore.id()+"\u0000"+ProfileStore.preferences(c).getLong(Config.SETTINGS_REVISION_KEY,0)+"\u0000"+raw(c);}
    static String raw(Context c){return ProfileStore.preferences(c).getString(FolderPolicy.KEY,"");}
    static void edit(Activity a,FolderPolicy.Folder f){
        if(!LauncherLock.require(a,()->edit(a,f)))return;String profile=ProfileStore.id(),before=stamp(a);long revision=ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0);
        EditText name=new EditText(a);name.setSingleLine(true);name.setText(f==null?"":f.name);name.setHint("Folder name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
        AlertDialog prompt=new AlertDialog.Builder(a).setTitle(f==null?"Create folder":"Rename folder").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Choose apps",null).create();
        prompt.setOnShowListener(v->prompt.getButton(-1).setOnClickListener(x->{String label;try{label=SetupValues.name(name.getText().toString());}catch(RuntimeException bad){name.setError(bad.getMessage());return;}
            List<HomeCategories.App> apps=HomeCategories.catalog(a,Config.load(a));Set<String> taken=new HashSet<>();for(FolderPolicy.Folder other:read(a))if(f==null||!other.id.equals(f.id))taken.addAll(other.apps);apps.removeIf(app->taken.contains(app.pkg)||!LibraryVisibility.allowed(a,app.pkg)&&(f==null||!f.apps.contains(app.pkg)));
            boolean[] selected=new boolean[apps.size()];String[] labels=new String[apps.size()];for(int i=0;i<apps.size();i++){labels[i]=apps.get(i).label;selected[i]=f!=null&&f.apps.contains(apps.get(i).pkg);}
            FreedomUi.checklist(a,label,labels,selected,picker->{try{
                if(!profile.equals(ProfileStore.id())||revision!=ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0))throw new IllegalStateException("Home changed. Open this editor again.");
                List<String> members=new ArrayList<>();if(f!=null)for(String p:f.apps)for(int i=0;i<apps.size();i++)if(selected[i]&&p.equals(apps.get(i).pkg))members.add(p);Set<String> available=new HashSet<>();for(int i=0;i<apps.size();i++){available.add(apps.get(i).pkg);if(selected[i]&&!members.contains(apps.get(i).pkg))members.add(apps.get(i).pkg);}if(f!=null)for(String pkg:f.apps)if(!available.contains(pkg))members.add(pkg);
                List<FolderPolicy.Folder> all=new ArrayList<>(read(a));FolderPolicy.Folder next=new FolderPolicy.Folder(f==null?UUID.randomUUID().toString().replace("-",""):f.id,label,members);int at=-1;for(int i=0;i<all.size();i++)if(all.get(i).id.equals(next.id))at=i;if(at<0)all.add(next);else all.set(at,next);
                if(save(a,before,all)){picker.dismiss();prompt.dismiss();}
            }catch(RuntimeException bad){Toast.makeText(a,bad.getMessage(),Toast.LENGTH_LONG).show();}});
        }));prompt.show();
    }
    static boolean save(Activity a,String before,List<FolderPolicy.Folder> folders){
        try{if(!LauncherLock.require(a,()->{}))return false;if(!before.equals(stamp(a)))throw new IllegalStateException("Folders changed. Open them again.");String value=FolderPolicy.encode(folders);if(raw(a).equals(value))return true;ProductFeatures.beforeChange(a,"Folders");Map<String,Object> change=new HashMap<>();change.put(FolderPolicy.KEY,value);change.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);if(!SetupValues.commit(ProfileStore.preferences(a),change))throw new IllegalStateException("Could not save folders");a.getContentResolver().notifyChange(DesktopLayout.uri(a),null);return true;}catch(RuntimeException bad){Toast.makeText(a,bad.getMessage(),Toast.LENGTH_LONG).show();return false;}
    }
    static void open(Activity a,FolderPolicy.Folder f){
        Config config=Config.load(a);Map<String,HomeCategories.App> by=new HashMap<>();for(HomeCategories.App app:HomeCategories.catalog(a,config))by.put(app.pkg,app);LinearLayout b=FreedomUi.body(a);boolean found=false;
        for(String pkg:f.apps){HomeCategories.App app=by.get(pkg);if(app!=null){found=true;FreedomUi.action(a,b,app.label,()->app.launch(a,config));}}
        if(!found)b.addView(FreedomUi.text(a,"These apps are temporarily unavailable. Their folder membership is saved.",18));FreedomUi.sheet(a,f.name,b);
    }
    static List<HomeCategories.App> decorate(Context c,List<HomeCategories.App> favorites,List<HomeCategories.App> catalog){
        Map<String,HomeCategories.App> by=new HashMap<>();for(HomeCategories.App app:catalog)by.put(app.pkg,app);Map<String,FolderApp> members=new HashMap<>();List<FolderApp> collections=new ArrayList<>();
        for(FolderPolicy.Folder f:read(c)){FolderApp app=new FolderApp(f,by);collections.add(app);for(String p:f.apps)members.put(p,app);}
        List<HomeCategories.App> out=new ArrayList<>();Set<String> used=new HashSet<>();for(HomeCategories.App original:favorites){HomeCategories.App app=members.containsKey(original.pkg)?members.get(original.pkg):original;if(used.add(app.pkg))out.add(app);}
        for(FolderApp app:collections)if(used.add(app.pkg))out.add(app);return out;
    }
    static final class FolderApp extends HomeCategories.App {
        final FolderPolicy.Folder folder;final Map<String,HomeCategories.App> by;
        FolderApp(FolderPolicy.Folder f,Map<String,HomeCategories.App> by){super("folder."+f.id,f.name,null,null,false);folder=f;this.by=by;}
        @Override Drawable artwork(Context c,Config config){List<Drawable> art=new ArrayList<>();for(String pkg:folder.apps){HomeCategories.App app=by.get(pkg);if(app!=null&&art.size()<4)art.add(app.artwork(c,config));}return new Mosaic(art);}
        @Override void launch(Activity a,Config c){open(a,folder);}
    }
    static final class Mosaic extends Drawable {
        final List<Drawable> images;final Paint paint=new Paint(3);Mosaic(List<Drawable> images){this.images=images;}
        public void draw(Canvas c){android.graphics.Rect r=getBounds();paint.setColor(0xff203448);c.drawRoundRect(new RectF(r),r.height()*.1f,r.height()*.1f,paint);for(int i=0;i<images.size();i++){Drawable d=images.get(i);if(d==null)continue;int w=r.width()/2,h=r.height()/2;Rect old=new Rect(d.getBounds());int x=r.left+(i%2)*w,y=r.top+(i/2)*h;d.setBounds(x+w/8,y+h/8,x+w*7/8,y+h*7/8);d.draw(c);d.setBounds(old);}}
        public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}public int getIntrinsicWidth(){return 320;}public int getIntrinsicHeight(){return 180;}
    }
    private Folders(){}
}
