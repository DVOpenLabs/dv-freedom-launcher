package com.gathin.tvhome;

import android.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shared local app search. Results use the same catalog, labels and validated launch route as Home. */
final class AllAppsSurface {
    static void show(Activity a,Config config,List<HomeCategories.App> catalog){
        show(a,config,catalog,true);
    }
    static void show(Activity a,Config config,List<HomeCategories.App> catalog,boolean allowEditing){
        float dp=a.getResources().getDisplayMetrics().density;LinearLayout body=new LinearLayout(a);body.setPadding(Math.round(24*dp),0,Math.round(24*dp),0);body.setOrientation(LinearLayout.VERTICAL);
        EditText search=new EditText(a);search.setSingleLine(true);search.setHint("Search apps");search.setContentDescription("Search installed apps");body.addView(search,new LinearLayout.LayoutParams(-1,-2));
        ListView list=new ListView(a);list.setItemsCanFocus(false);body.addView(list,new LinearLayout.LayoutParams(-1,Math.round(240*dp)));TextView empty=new TextView(a);empty.setText("No apps match your search");empty.setPadding(0,Math.round(16*dp),0,Math.round(16*dp));body.addView(empty);list.setEmptyView(empty);
        List<HomeCategories.App> matches=new ArrayList<>();ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_list_item_1,new ArrayList<>());list.setAdapter(adapter);
        Runnable filter=()->{matches.clear();List<String> labels=new ArrayList<>();for(HomeCategories.App app:catalog)if(AppSearch.matches(app.label,app.pkg,search.getText().toString())){matches.add(app);labels.add(app.label);}adapter.clear();adapter.addAll(labels);adapter.notifyDataSetChanged();};filter.run();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){filter.run();}public void afterTextChanged(Editable s){}});
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("All apps").setView(body).setNegativeButton(android.R.string.cancel,null).create();list.setOnItemClickListener((parent,v,at,id)->{if(at>=0&&at<matches.size()){HomeCategories.App app=matches.get(at);dialog.dismiss();app.launch(a,config);}});list.setOnItemLongClickListener((parent,v,at,id)->{if(at<0||at>=matches.size())return false;if(!allowEditing){Toast.makeText(a,"Keep or revert the preview before editing apps",Toast.LENGTH_SHORT).show();return true;}String pkg=matches.get(at).pkg;dialog.dismiss();NativeTileEditor.show(a,pkg);return true;});
        search.setOnKeyListener((v,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_DOWN&&adapter.getCount()>0){list.requestFocus();list.setSelection(0);return true;}return false;});dialog.setOnShowListener(d->{dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);if(adapter.getCount()>0){list.requestFocus();list.setSelection(0);}else search.requestFocus();});dialog.show();
    }
    private AllAppsSurface(){}
}
