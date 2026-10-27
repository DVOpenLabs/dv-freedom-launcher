package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/** Optional, bounded named app rows. No mutation of the owner's existing favorites or native order. */
final class HomeCategories {
    static final String KEY="home_categories";
    static final int MAX_ROWS=8, MAX_APPS=32, MAX_MEMBERS=128, MAX_BYTES=16384;
    static final class Row {
        final String id,name; final boolean visible; final List<String> apps;
        Row(String id,String name,boolean visible,List<String> apps){
            check(id!=null&&id.matches("row-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"),"Invalid category ID");
            this.id=id;this.name=SetupValues.name(name);this.visible=visible;
            check(apps!=null&&apps.size()<=MAX_APPS,"Choose up to 32 apps per category");
            LayoutDraft.validatePackages(apps);this.apps=Collections.unmodifiableList(new ArrayList<>(apps));
        }
        Row named(String value){return new Row(id,value,visible,apps);}
        Row shown(boolean value){return new Row(id,name,value,apps);}
        Row members(List<String> value){return new Row(id,name,visible,value);}
    }
    static final class State {
        final boolean enabled;final List<Row> rows;
        State(boolean enabled,List<Row> rows){
            check(rows!=null&&rows.size()<=MAX_ROWS,"Keep up to eight categories");
            Set<String> ids=new HashSet<>(),names=new HashSet<>();names.add("favorites");int members=0;
            for(Row row:rows){check(row!=null&&ids.add(row.id),"Repeated category ID");check(names.add(row.name.toLowerCase(Locale.ROOT)),"That category name already exists");members+=row.apps.size();}
            check(members<=MAX_MEMBERS,"Keep up to 128 app memberships");
            this.enabled=enabled;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));
        }
        State replace(String id,Row next){List<Row> copy=new ArrayList<>(rows);int at=index(id);if(next==null)copy.remove(at);else copy.set(at,next);return new State(enabled,copy);}
        State move(String id,int delta){List<Row> copy=new ArrayList<>(rows);int at=index(id),to=at+delta;check(to>=0&&to<copy.size(),"Already at the edge");Collections.swap(copy,at,to);return new State(enabled,copy);}
        int index(String id){for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(id))return i;throw new IllegalArgumentException("Category no longer exists");}
    }
    private static void check(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
    static State parse(String text){
        if(text==null||text.isEmpty())return new State(false,Collections.emptyList());
        check(text.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES,"Category data is too large");
        try{
            ThemeArchive.validateJsonObject(text);JSONObject json=new JSONObject(text);fields(json,"schema","enabled","rows");
            Object version=json.get("schema");check((version instanceof Integer||version instanceof Long)&&((Number)version).longValue()==1,"Unsupported category format");
            check(json.get("enabled") instanceof Boolean,"Invalid category switch");JSONArray array=json.getJSONArray("rows");List<Row> rows=new ArrayList<>();
            check(array.length()<=MAX_ROWS,"Too many categories");
            for(int i=0;i<array.length();i++){
                JSONObject value=array.getJSONObject(i);fields(value,"id","name","visible","apps");
                check(value.get("id") instanceof String&&value.get("name") instanceof String&&value.get("visible") instanceof Boolean,"Invalid category");
                JSONArray packages=value.getJSONArray("apps");check(packages.length()<=MAX_APPS,"Too many apps in category");List<String> apps=new ArrayList<>();
                for(int n=0;n<packages.length();n++){check(packages.get(n) instanceof String,"Invalid app package");apps.add(packages.getString(n));}
                rows.add(new Row(value.getString("id"),value.getString("name"),value.getBoolean("visible"),apps));
            }
            return new State(json.getBoolean("enabled"),rows);
        }catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Invalid category data",e);}
    }
    static boolean valid(String text){if(text==null||text.isEmpty())return false;try{parse(text);return true;}catch(Exception e){return false;}}
    static State read(Context context){try{return parse(ProfileStore.preferences(context).getString(KEY,""));}catch(Exception e){return new State(false,Collections.emptyList());}}
    static boolean enabled(Context context){return read(context).enabled;}
    static String encode(State state){
        try{
            JSONArray rows=new JSONArray();for(Row row:state.rows)rows.put(new JSONObject().put("id",row.id).put("name",row.name).put("visible",row.visible).put("apps",new JSONArray(row.apps)));
            String text=new JSONObject().put("schema",1).put("enabled",state.enabled).put("rows",rows).toString();check(text.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES,"Category data is too large");return text;
        }catch(JSONException e){throw new IllegalArgumentException("Cannot encode categories",e);}
    }
    private static void fields(JSONObject json,String... fields){Set<String> allowed=new HashSet<>(Arrays.asList(fields));check(json.length()==allowed.size(),"Missing category field");Iterator<String> keys=json.keys();while(keys.hasNext())check(allowed.contains(keys.next()),"Unsupported category field");}
    private static final class Edit {
        final String profile,raw;final State state;
        Edit(Context context){profile=ProfileStore.id();raw=ProfileStore.preferences(context).getString(KEY,"");state=parse(raw);}
    }
    private static boolean save(Activity activity,Edit expected,State next){
        try{
            check(expected.profile.equals(ProfileStore.id()),"The profile changed. Open categories again.");
            SharedPreferences prefs=ProfileStore.preferences(activity);check(expected.raw.equals(prefs.getString(KEY,"")),"Categories changed. Open them again.");
            String text=encode(next);if(text.equals(expected.raw))return true;
            ProductFeatures.beforeChange(activity,"Categories");Map<String,Object> changes=new HashMap<>();changes.put(KEY,text);changes.put(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
            check(SetupValues.commit(prefs,changes),"Could not save categories");return true;
        }catch(Exception e){error(activity,e);return false;}
    }
    private static void error(Activity a,Exception e){Toast.makeText(a,e.getMessage()==null?"Could not update categories":e.getMessage(),Toast.LENGTH_LONG).show();}
    static void show(Activity activity){
        try{
            Config.load(activity);Edit edit=new Edit(activity);String[] labels=new String[edit.state.rows.size()+3];
            labels[0]="Category layout · "+(edit.state.enabled?"On":"Off");labels[1]="Preview category layout";labels[2]="Create category";
            for(int i=0;i<edit.state.rows.size();i++){Row row=edit.state.rows.get(i);labels[i+3]=row.name+" · "+row.apps.size()+" apps"+(row.visible?"":" · Hidden");}
            new AlertDialog.Builder(activity).setTitle("Categories").setItems(labels,(d,which)->{
                if(which==0){
                    AlertDialog confirm=new AlertDialog.Builder(activity).setTitle(edit.state.enabled?"Return to your existing layout?":"Use category rows on Home?")
                        .setMessage("Your current favorites, wallpaper and original app layout stay saved. You can preview first and switch back here.")
                        .setNegativeButton(android.R.string.cancel,null).setPositiveButton(edit.state.enabled?"Use existing layout":"Use categories",(x,w)->{if(save(activity,edit,new State(!edit.state.enabled,edit.state.rows)))activity.finish();}).create();
                    confirm.show();confirm.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
                }else if(which==1)activity.startActivity(new Intent(activity,CategoryHomeActivity.class).putExtra(CategoryHomeActivity.PREVIEW,true));
                else if(which==2){
                    if(edit.state.rows.size()>=MAX_ROWS){Toast.makeText(activity,"Keep up to eight categories",Toast.LENGTH_LONG).show();return;}
                    name(activity,"New category","",value->{List<Row> rows=new ArrayList<>(edit.state.rows);Row created=new Row("row-"+UUID.randomUUID(),value,true,Collections.emptyList());rows.add(created);if(save(activity,edit,new State(edit.state.enabled,rows)))edit(activity,created.id);});
                }else edit(activity,edit.state.rows.get(which-3).id);
            }).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception e){error(activity,e);}
    }
    private static void edit(Activity activity,String id){
        try{
            Edit edit=new Edit(activity);Row row=edit.state.rows.get(edit.state.index(id));
            String[] labels={"Choose apps · "+row.apps.size(),"Arrange apps","Rename category",row.visible?"Hide category":"Show category","Move category up","Move category down","Delete category"};
            new AlertDialog.Builder(activity).setTitle(row.name).setItems(labels,(d,which)->{
                if(which==0)chooseApps(activity,edit,row);
                else if(which==1)arrange(activity,id);
                else if(which==2)name(activity,"Rename category",row.name,value->{if(save(activity,edit,edit.state.replace(id,row.named(value))))edit(activity,id);});
                else if(which==3){if(save(activity,edit,edit.state.replace(id,row.shown(!row.visible))))edit(activity,id);}
                else if(which==4||which==5){try{if(save(activity,edit,edit.state.move(id,which==4?-1:1)))show(activity);}catch(Exception e){error(activity,e);}}
                else{AlertDialog confirm=new AlertDialog.Builder(activity).setTitle("Delete "+row.name+"?").setMessage("The apps stay installed. Your original favorites stay saved.")
                    .setNegativeButton(android.R.string.cancel,null).setPositiveButton("Delete",(x,w)->{if(save(activity,edit,edit.state.replace(id,null)))show(activity);}).create();confirm.show();confirm.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();}
            }).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception e){error(activity,e);}
    }
    private static void chooseApps(Activity activity,Edit edit,Row row){
        List<App> catalog=catalog(activity,Config.load(activity));List<String> packages=new ArrayList<>(),labels=new ArrayList<>();
        for(App app:catalog){packages.add(app.pkg);labels.add(app.label);}
        for(String pkg:row.apps)if(!packages.contains(pkg)){packages.add(pkg);labels.add(pkg+" · Unavailable");}
        boolean[] selected=new boolean[packages.size()];for(int i=0;i<selected.length;i++)selected[i]=row.apps.contains(packages.get(i));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(row.name+" · Choose up to 32 apps")
            .setMultiChoiceItems(labels.toArray(new String[0]),selected,(d,which,on)->selected[which]=on)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                List<String> apps=new ArrayList<>();for(String pkg:row.apps)if(selected[packages.indexOf(pkg)])apps.add(pkg);
                for(int i=0;i<packages.size();i++)if(selected[i]&&!apps.contains(packages.get(i)))apps.add(packages.get(i));
                State next=edit.state.replace(row.id,row.members(apps));if(save(activity,edit,next)){dialog.dismiss();edit(activity,row.id);}
            }catch(Exception e){error(activity,e);}
        }));dialog.show();
    }
    private static void arrange(Activity activity,String id){
        try{
            Edit edit=new Edit(activity);Row row=edit.state.rows.get(edit.state.index(id));Map<String,App> catalog=new HashMap<>();for(App app:catalog(activity,Config.load(activity)))catalog.put(app.pkg,app);
            if(row.apps.isEmpty()){Toast.makeText(activity,"Choose apps for this category first",Toast.LENGTH_LONG).show();return;}
            String[] labels=new String[row.apps.size()];for(int i=0;i<labels.length;i++){App app=catalog.get(row.apps.get(i));labels[i]=app==null?row.apps.get(i):app.label;}
            new AlertDialog.Builder(activity).setTitle(row.name+" · Arrange apps").setItems(labels,(d,which)->new AlertDialog.Builder(activity).setTitle(labels[which])
                .setItems(new String[]{"Move left","Move right","Remove from category"},(x,action)->{
                    try{List<String> apps=new ArrayList<>(row.apps);if(action==2)apps.remove(which);else{int to=which+(action==0?-1:1);check(to>=0&&to<apps.size(),"Already at the edge");Collections.swap(apps,which,to);}if(save(activity,edit,edit.state.replace(id,row.members(apps))))arrange(activity,id);}catch(Exception e){error(activity,e);}
                }).setNegativeButton(android.R.string.cancel,null).show()).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception e){error(activity,e);}
    }
    private static void name(Activity activity,String title,String initial,Consumer<String> action){
        EditText field=new EditText(activity);field.setSingleLine(true);field.setText(initial);field.setSelectAllOnFocus(true);field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title).setView(field).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{action.accept(SetupValues.name(field.getText().toString()));dialog.dismiss();}catch(Exception e){field.setError(e.getMessage());}});field.requestFocus();});dialog.show();
    }
    static final class App {
        final String pkg,label;final ComponentName component;final ResolveInfo info;final boolean leanback;
        private java.lang.ref.WeakReference<Drawable.ConstantState> art=new java.lang.ref.WeakReference<>(null);
        App(String pkg,String label,ComponentName component,ResolveInfo info,boolean leanback){this.pkg=pkg;this.label=label;this.component=component;this.info=info;this.leanback=leanback;}
        Drawable artwork(Context context,Config config){
            Drawable.ConstantState cached=art.get();if(cached!=null)return cached.newDrawable(context.getResources());
            Drawable drawable=null;try{String bundled=config.appBanners.get(pkg);if(bundled!=null&&bundled.startsWith("banner_")){int id=context.getResources().getIdentifier(bundled,"drawable",context.getPackageName());if(id!=0)drawable=context.getDrawable(id);}if(drawable==null)drawable=info.activityInfo.loadBanner(context.getPackageManager());if(drawable==null)drawable=info.loadIcon(context.getPackageManager());}catch(RuntimeException ignored){}
            drawable=ProductFeatures.banner(context,pkg,drawable);if(drawable!=null)art=new java.lang.ref.WeakReference<>(drawable.getConstantState());return drawable;
        }
        void launch(Activity activity,Config config){
            Intent intent=new Intent(Intent.ACTION_MAIN).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);if(leanback)intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            String action=config.launchActions.get(pkg);if(action!=null){Intent candidate=new Intent(action).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(activity.getPackageManager().resolveActivity(candidate,0)!=null)intent=candidate;}
            try{activity.startActivity(intent);RecentApps.record(activity,pkg);if(config.cinematicHome)LastOpened.record(activity,pkg);}catch(Exception e){Toast.makeText(activity,"Cannot open "+label,Toast.LENGTH_LONG).show();}
        }
    }
    /** One metadata catalog per screen; tile artwork is loaded lazily by visible row holders. */
    static List<App> catalog(Context context,Config config){
        Map<String,App> apps=new LinkedHashMap<>();PackageManager pm=context.getPackageManager();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})
            for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){
                if(info.activityInfo==null||!info.activityInfo.enabled||!info.activityInfo.exported||!info.activityInfo.applicationInfo.enabled)continue;
                String pkg=info.activityInfo.packageName;if(!LayoutDraft.packageName(pkg)||pkg.equals(context.getPackageName())||config.hiddenApps.contains(pkg)||apps.containsKey(pkg)||apps.size()>=512)continue;
                String label=ProductFeatures.label(context,pkg,config.appLabels.containsKey(pkg)?config.appLabels.get(pkg):String.valueOf(info.loadLabel(pm)));
                apps.put(pkg,new App(pkg,Cards.cleanLabel(label,pkg),new ComponentName(pkg,info.activityInfo.name),info,Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)));
            }
        List<App> rows=new ArrayList<>(apps.values());rows.sort((a,b)->a.label.compareToIgnoreCase(b.label));return rows;
    }
    private HomeCategories(){}
}
