package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.widget.*;
import java.util.*;

final class ShelfOptions {
    static final String KEY="shelf_options_v1";
    static final class Spec {
        int count=2;boolean watch=true,preview=true;Set<String> hidden=new HashSet<>();
        Spec(String raw){if(raw==null||raw.isEmpty())return;String[] parts=raw.split("\\|",-1);if(parts.length!=5||!parts[0].equals("v1")||!Arrays.asList("1","2","4").contains(parts[1])||!parts[2].matches("[01]")||!parts[3].matches("[01]")||parts[4].length()>8192)throw new IllegalArgumentException();count=Integer.parseInt(parts[1]);watch=parts[2].equals("1");preview=parts[3].equals("1");if(!parts[4].isEmpty())for(String p:parts[4].split(",")){if(!p.matches("[a-zA-Z][a-zA-Z0-9_.]{1,200}"))throw new IllegalArgumentException();hidden.add(p);}}
        String save(){List<String> ids=new ArrayList<>(hidden);Collections.sort(ids);return "v1|"+count+"|"+(watch?1:0)+"|"+(preview?1:0)+"|"+String.join(",",ids);}
        boolean accepts(String pkg){return !hidden.contains(pkg);}
    }
    static Spec read(Context c){try{return new Spec(ProfileStore.preferences(c).getString(KEY,""));}catch(Exception bad){return new Spec("");}}
    static void show(Activity a){Spec spec=read(a);LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);int pad=Math.round(20*a.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
        CheckBox watch=new CheckBox(a);watch.setText("Continue watching / Watch Next");watch.setChecked(spec.watch);box.addView(watch);CheckBox previews=new CheckBox(a);previews.setText("Suggestions shared by apps");previews.setChecked(spec.preview);box.addView(previews);
        TextView label=new TextView(a);label.setText("Maximum titles per app (12 overall)");box.addView(label);Spinner count=new Spinner(a);count.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"1 title","2 titles","4 titles"}));count.setSelection(spec.count==1?0:spec.count==4?2:1);box.addView(count);
        TextView hint=new TextView(a);hint.setText("Choose which apps may appear. Hidden apps’ items are filtered before artwork is loaded. Hold OK on a title for details.");box.addView(hint);List<HomeCategories.App> apps=HomeCategories.catalog(a,Config.load(a));List<CheckBox> sources=new ArrayList<>();for(HomeCategories.App app:apps){CheckBox source=new CheckBox(a);source.setText(app.label);source.setChecked(!spec.hidden.contains(app.pkg));source.setTag(app.pkg);box.addView(source);sources.add(source);}
        ScrollView scroll=new ScrollView(a);scroll.addView(box);new AlertDialog.Builder(a).setTitle("Shelf sources & limits").setView(scroll).setPositiveButton("Apply",(d,w)->{if(!LauncherLock.require(a,()->show(a)))return;spec.watch=watch.isChecked();spec.preview=previews.isChecked();spec.count=new int[]{1,2,4}[count.getSelectedItemPosition()];for(CheckBox source:sources){String id=(String)source.getTag();if(source.isChecked())spec.hidden.remove(id);else spec.hidden.add(id);}Map<String,Object> values=new HashMap<>();values.put(KEY,spec.save());values.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);ProductFeatures.beforeChange(a,"Change content shelf sources");if(SetupValues.commit(ProfileStore.preferences(a),values))a.finish();else Toast.makeText(a,"Could not save shelf choices",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancel",null).show();
    }
}
