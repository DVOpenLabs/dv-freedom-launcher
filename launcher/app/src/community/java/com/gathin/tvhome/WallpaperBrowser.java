package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** Local visual browser. Focus previews a still; only Apply writes an appearance choice. */
final class WallpaperBrowser {
    private static final class Item {
        final String token,title,detail;final File image;final boolean theme;
        Item(String token,String title,String detail,File image,boolean theme){this.token=token;this.title=title;this.detail=detail;this.image=image;this.theme=theme;}
    }
    private final Activity activity;
    private final List<Item> items=new ArrayList<>();
    private final ExecutorService decoder=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final String profile;
    private final Map<String,?> initial;
    private ImageView image;
    private TextView title,detail;
    private Button apply;
    private int selected;
    private volatile int generation;
    private volatile boolean closed;
    private Future<?> pending;
    private AlertDialog dialog;
    private WallpaperBrowser(Activity a,boolean themes){
        activity=a;Config config=Config.load(a);profile=ProfileStore.id();initial=ProfileStore.preferences(a).getAll();
        if(themes){
            File original=Config.dir(a);
            items.add(new Item("","Original theme","Use the original theme and its local wallpaper.",original==null?null:new File(original,Config.WALLPAPER_NAME),true));
            for(ThemeLibrary.Entry entry:ThemeLibrary.list(a)){
                File preview=new File(entry.directory,"preview.jpg");if(!preview.isFile())preview=new File(entry.directory,Config.WALLPAPER_NAME);
                String note="Version "+entry.pack.version+(new File(entry.directory,"video.mp4").isFile()?" · Silent live wallpaper available":" · Still wallpaper");
                items.add(new Item(entry.token,entry.pack.name,note,preview,true));
            }
        }else{
            File directory=ThemeLibrary.active()==null?Config.dir(a):ThemeLibrary.active().directory;
            File original=directory==null?null:new File(directory,Config.WALLPAPER_NAME);
            items.add(new Item(null,"Theme default","Use the theme’s wallpaper collection and rotation settings.",original,false));
            if(original!=null&&original.isFile())items.add(new Item(original.getName(),"Main wallpaper","Keep this local image on Home.",original,false));
            List<File> files=WallpaperRotation.images(directory==null?null:new File(directory,"wallpapers"));
            int count=0;for(File file:files){if(++count>128)break;items.add(new Item(file.getName(),file.getName(),"Local image · Apply to keep it on Home.",file,false));}
            items.add(new Item("none","No wallpaper","Use the theme background color.",null,false));
        }
        String current=ProfileStore.preferences(a).getString(themes?ThemeLibrary.CHOICE:Config.WALLPAPER_CHOICE_KEY,themes?"":null);
        for(int i=0;i<items.size();i++)if(Objects.equals(items.get(i).token,current))selected=i;
    }
    static void show(Activity a,boolean themes){
        if(themes&&!ThemeLibrary.enabled()){Toast.makeText(a,"Offline themes are not enabled for this Home.",Toast.LENGTH_LONG).show();return;}
        new WallpaperBrowser(a,themes).open(themes);
    }
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private TextView text(int size){TextView v=new TextView(activity);v.setTextSize(size);v.setTextColor(0xfff5f7fa);v.setPadding(0,dp(6),0,dp(6));return v;}
    private void open(boolean themes){
        LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(12),dp(4),dp(12),dp(4));row.setMinimumHeight(dp(280));
        ListView list=new ListView(activity);list.setId(View.generateViewId());list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);list.setDividerHeight(dp(4));
        String[] labels=new String[items.size()];for(int i=0;i<labels.length;i++)labels[i]=items.get(i).title;
        list.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_single_choice,labels){@Override public View getView(int position,View convert,ViewGroup parent){TextView view=(TextView)super.getView(position,convert,parent);view.setTextSize(17);view.setTextColor(0xfff5f7fa);view.setMinHeight(dp(52));return view;}});
        row.addView(list,new LinearLayout.LayoutParams(0,-1,4));
        LinearLayout preview=new LinearLayout(activity);preview.setOrientation(LinearLayout.VERTICAL);preview.setPadding(dp(24),0,0,0);
        title=text(22);detail=text(15);image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setBackgroundColor(0xff111215);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        apply=new Button(activity);apply.setId(View.generateViewId());apply.setText("Apply");apply.setOnClickListener(v->apply());list.setNextFocusRightId(apply.getId());apply.setNextFocusLeftId(list.getId());
        preview.addView(title,new LinearLayout.LayoutParams(-1,-2));preview.addView(image,new LinearLayout.LayoutParams(-1,0,1));preview.addView(detail,new LinearLayout.LayoutParams(-1,-2));preview.addView(apply,new LinearLayout.LayoutParams(-1,dp(52)));
        row.addView(preview,new LinearLayout.LayoutParams(0,-1,6));
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int position,long id){select(position);}public void onNothingSelected(AdapterView<?> p){}});
        list.setOnItemClickListener((p,v,position,id)->{list.setItemChecked(position,true);select(position);apply.requestFocus();});
        dialog=new AlertDialog.Builder(activity).setTitle(themes?"Browse offline themes":"Choose wallpaper").setView(row).setNegativeButton(android.R.string.cancel,null).create();
        dialog.setOnDismissListener(d->{closed=true;generation++;if(pending!=null)pending.cancel(true);decoder.shutdownNow();image.setImageDrawable(null);});
        dialog.setOnShowListener(d->{int w=activity.getResources().getDisplayMetrics().widthPixels,h=activity.getResources().getDisplayMetrics().heightPixels;dialog.getWindow().setLayout(Math.min(dp(900),w-dp(32)),Math.min(dp(460),h-dp(32)));list.setItemChecked(selected,true);list.setSelection(selected);list.requestFocus();select(selected);});dialog.show();
    }
    private void select(int index){
        selected=index;Item item=items.get(index);title.setText(item.title);detail.setText(item.detail);image.setImageDrawable(null);apply.setEnabled(true);
        int expected=++generation;if(pending!=null)pending.cancel(true);
        if(item.image==null||!item.image.isFile())return;
        pending=decoder.submit(()->{
            if(closed||expected!=generation)return;Bitmap bitmap=thumbnail(item.image);
            main.post(()->{if(closed||expected!=generation){return;}if(bitmap==null)detail.setText(item.detail+"\nPreview unavailable; the saved media is unchanged.");else image.setImageBitmap(bitmap);});
        });
    }
    private static Bitmap thumbnail(File file){
        try{
            if(file.length()>8L*1024*1024)return null;
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);
            if(!WallpaperSize.accepted(options.outWidth,options.outHeight))return null;
            int sample=1;while(options.outWidth/sample>1280||options.outHeight/sample>720)sample*=2;
            options.inJustDecodeBounds=false;options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap source=BitmapFactory.decodeFile(file.getPath(),options);if(source==null)return null;
            float scale=Math.min(1f,Math.min(640f/source.getWidth(),360f/source.getHeight()));
            if(scale>=1)return source;Bitmap result=Bitmap.createScaledBitmap(source,Math.max(1,Math.round(source.getWidth()*scale)),Math.max(1,Math.round(source.getHeight()*scale)),true);if(result!=source)source.recycle();return result;
        }catch(RuntimeException|OutOfMemoryError unavailable){return null;}
    }
    private void apply(){
        Item item=items.get(selected);
        try{
            Config.load(activity);SharedPreferences p=ProfileStore.preferences(activity);
            if(!profile.equals(ProfileStore.id())||!initial.equals(p.getAll()))throw new IllegalStateException("Home changed. Reopen the wallpaper browser.");
            String key=item.theme?ThemeLibrary.CHOICE:Config.WALLPAPER_CHOICE_KEY;
            if(Objects.equals(item.token,p.getString(key,item.theme?"":null))){dialog.dismiss();return;}
            if(item.theme&&!item.token.isEmpty()&&ThemeLibrary.list(activity).stream().noneMatch(e->e.token.equals(item.token)))throw new IllegalStateException("This theme is no longer available.");
            if(!item.theme&&item.image!=null&&!item.image.isFile())throw new IllegalStateException("This wallpaper is no longer available.");
            ProductFeatures.beforeChange(activity,item.theme?"Change theme":"Change wallpaper");
            if(item.theme){if(!ThemeLibrary.select(activity,item.token))throw new IllegalStateException("Could not save theme");}
            else{Map<String,Object> changes=new HashMap<>();changes.put(key,item.token);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);if(!SetupValues.commit(p,changes))throw new IllegalStateException("Could not save wallpaper");}
            dialog.dismiss();Toast.makeText(activity,"Wallpaper choice saved",Toast.LENGTH_SHORT).show();activity.recreate();
        }catch(Exception failure){Toast.makeText(activity,failure.getMessage(),Toast.LENGTH_LONG).show();}
    }
}
