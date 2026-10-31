package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.text.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParser;

/** Reads local pack data/resources; never launches a pack, loads its classes or downloads artwork. */
final class IconPacks {
    static final class Pack {
        final String pkg,label;final long version,updated;
        Pack(Context c,ResolveInfo info)throws Exception{
            pkg=info.activityInfo.packageName;PackageInfo p=c.getPackageManager().getPackageInfo(pkg,0);
            version=version(p);updated=p.lastUpdateTime;
            String name=info.loadLabel(c.getPackageManager()).toString().replaceAll("[\\p{Cntrl}]","").trim();
            label=name.substring(0,Math.min(80,name.length()));
        }
        boolean current(Context c){try{PackageInfo p=c.getPackageManager().getPackageInfo(pkg,0);return version(p)==version&&p.lastUpdateTime==updated;}catch(Exception absent){return false;}}
    }
    private static long version(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
    static List<Pack> installed(Context c){
        Map<String,Pack> packs=new TreeMap<>();List<Intent> intents=Arrays.asList(new Intent("org.adw.launcher.THEMES"),
                new Intent("com.gau.go.launcherex.theme"),new Intent(Intent.ACTION_MAIN).addCategory("com.teslacoilsw.launcher.THEME"));
        for(Intent intent:intents)for(ResolveInfo info:c.getPackageManager().queryIntentActivities(intent,0)){
            if(packs.size()>=128)break;
            try{if(info.activityInfo!=null&&info.activityInfo.enabled&&info.activityInfo.applicationInfo.enabled&&!c.getPackageName().equals(info.activityInfo.packageName))packs.putIfAbsent(info.activityInfo.packageName,new Pack(c,info));}catch(Exception unavailable){}
        }
        List<Pack> out=new ArrayList<>(packs.values());out.sort(Comparator.comparing(p->p.label,String.CASE_INSENSITIVE_ORDER));return out;
    }
    static IconPackFormat.Mapping mapping(Context c,Pack pack)throws Exception{
        if(!pack.current(c))throw new IOException("The icon pack changed. Choose it again.");
        Resources res=c.getPackageManager().getResourcesForApplication(pack.pkg);int id=res.getIdentifier("appfilter","xml",pack.pkg);
        if(id!=0){
            try(InputStream in=res.openRawResource(id)){read(in,IconPackFormat.MAX_BYTES);}
            IconPackFormat.Builder builder=new IconPackFormat.Builder();int depth=0,events=0;
            try(XmlResourceParser parser=res.getXml(id)){
                for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.nextToken()){
                    if(Thread.currentThread().isInterrupted()||++events>IconPackFormat.MAX_EVENTS)throw new IOException("Icon mapping interrupted or too large");
                    if(event==XmlPullParser.DOCDECL||event==XmlPullParser.ENTITY_REF)throw new IOException("Unsupported icon mapping declaration");
                    if(event==XmlPullParser.START_TAG){if(++depth>16)throw new IOException("Icon mapping is too complex");if("item".equals(parser.getName()))builder.item(parser.getAttributeValue(null,"component"),parser.getAttributeValue(null,"drawable"));}
                    else if(event==XmlPullParser.END_TAG)depth--;
                }
            }return builder.build();
        }
        try(InputStream in=res.getAssets().open("appfilter.xml")){return IconPackFormat.parse(read(in,IconPackFormat.MAX_BYTES));}
    }
    static byte[] read(InputStream in,int limit)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
        for(int n;(n=in.read(buffer))!=-1;){if(Thread.currentThread().isInterrupted()||out.size()+n>limit)throw new IOException("Icon data is too large or interrupted");out.write(buffer,0,n);}return out.toByteArray();
    }
    static byte[] picture(Context c,Pack pack,String name)throws Exception{
        if(!pack.current(c)||!IconPackFormat.resource(name))throw new IOException("Icon is unavailable");
        Resources res=c.getPackageManager().getResourcesForApplication(pack.pkg);int id=res.getIdentifier(name,"drawable",pack.pkg);
        if(id==0)throw new IOException("Icon is missing from this pack");
        android.util.TypedValue value=new android.util.TypedValue();res.getValue(id,value,true);
        Drawable icon;Bitmap decoded=null;
        String path=value.string==null?"":value.string.toString();
        if(path.endsWith(".xml")){
            try(InputStream in=res.openRawResource(id)){read(in,256*1024);}
            int depth=0,events=0,chars=0;try(XmlResourceParser xml=res.getXml(id)){for(int event=xml.getEventType();event!=XmlPullParser.END_DOCUMENT;event=xml.next()){if(++events>4096)throw new IOException("Vector icon is too complex");if(event==XmlPullParser.START_TAG){if(++depth>16||!Arrays.asList("vector","path","group","clip-path").contains(xml.getName())||depth==1&&!xml.getName().equals("vector"))throw new IOException("Choose a static raster or vector icon");for(int n=0;n<xml.getAttributeCount();n++)if((chars+=xml.getAttributeValue(n).length())>131072)throw new IOException("Vector icon is too large");}else if(event==XmlPullParser.END_TAG)depth--;}}
            icon=res.getDrawable(id,null);
        }
        else{
            byte[] bytes;try(InputStream in=res.openRawResource(id)){bytes=read(in,8*1024*1024);}
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
            if(o.outWidth<1||o.outHeight<1||o.outWidth>4096||o.outHeight>4096||(long)o.outWidth*o.outHeight>8_000_000)throw new IOException("Icon dimensions are unsupported");
            int sample=1;while(o.outWidth/sample>512||o.outHeight/sample>512)sample*=2;
            o.inJustDecodeBounds=false;o.inSampleSize=sample;Bitmap b=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
            if(b==null)throw new IOException("Icon could not be decoded");decoded=b;icon=new android.graphics.drawable.BitmapDrawable(res,b);
        }
        if(icon instanceof android.graphics.drawable.Animatable)((android.graphics.drawable.Animatable)icon).stop();
        Bitmap target=null;
        try{
            icon=icon.mutate();target=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(target);canvas.drawColor(0xff182334);
            int iw=Math.max(1,icon.getIntrinsicWidth()),ih=Math.max(1,icon.getIntrinsicHeight());float scale=Math.min(378f/iw,378f/ih);int w=Math.max(1,Math.round(iw*scale)),h=Math.max(1,Math.round(ih*scale));icon.setBounds(480-w/2,270-h/2,480+(w+1)/2,270+(h+1)/2);icon.draw(canvas);
            ByteArrayOutputStream out=new ByteArrayOutputStream();if(!target.compress(Bitmap.CompressFormat.JPEG,90,out)||out.size()>1024*1024)throw new IOException("Icon could not be prepared");return out.toByteArray();
        }finally{if(target!=null)target.recycle();if(decoded!=null)decoded.recycle();}
    }
    private interface Job<T>{T run()throws Exception;}
    private static <T> void load(Activity a,String label,Job<T> job,Consumer<T> success){
        String profile=ProfileStore.id();ExecutorService worker=Executors.newSingleThreadExecutor();Handler main=new Handler(Looper.getMainLooper());boolean[] closed={false},registered={false};
        AlertDialog progress=new AlertDialog.Builder(a).setTitle(label).setMessage("Reading local icon data…").setNegativeButton("Cancel",null).create();
        Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity x,Bundle b){}public void onActivityStarted(Activity x){}public void onActivityResumed(Activity x){}public void onActivityStopped(Activity x){}public void onActivitySaveInstanceState(Activity x,Bundle b){}public void onActivityPaused(Activity x){if(x==a)progress.dismiss();}public void onActivityDestroyed(Activity x){if(x==a)progress.dismiss();}};
        progress.setOnDismissListener(d->{closed[0]=true;worker.shutdownNow();if(registered[0])a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);});
        progress.setOnShowListener(d->{registered[0]=true;a.getApplication().registerActivityLifecycleCallbacks(lifecycle);});progress.show();
        worker.execute(()->{try{T result=job.run();main.post(()->{if(closed[0]||a.isFinishing()||a.isDestroyed())return;progress.dismiss();if(!profile.equals(ProfileStore.id())){error(a,"Profile changed. Choose the icon pack again.");return;}success.accept(result);});}
            catch(Exception|OutOfMemoryError failure){main.post(()->{if(!closed[0]){progress.dismiss();error(a,"This pack could not be read. Choose a supported appfilter pack or keep original artwork.");}});}finally{worker.shutdown();}});
    }
    private static void choose(Activity a,Consumer<Pack> next){
        load(a,"Installed icon packs",()->installed(a),packs->{
            if(packs.isEmpty()){error(a,"No supported installed icon pack found. Install an ADW, GO or Nova-compatible pack yourself, then return here. Nothing is downloaded or installed by the launcher.");return;}
            LinearLayout body=FreedomUi.body(a);body.addView(FreedomUi.text(a,"Read appfilter mappings and static icons from a pack you installed. Preview before importing; pack code is never run. Imported copies stay on this TV and are not included in portable setup exports.",17));
            final AlertDialog[] d={null};for(Pack pack:packs)FreedomUi.action(a,body,pack.label,()->{d[0].dismiss();next.accept(pack);});d[0]=FreedomUi.sheet(a,"Installed icon packs",body);
        });
    }
    static void show(Activity a){
        Config.load(a);if(!ProductFeatures.allowEdit(a,()->show(a)))return;
        NativeTileEditor.PackGuard guard;try{guard=new NativeTileEditor.PackGuard(a);}catch(Exception unavailable){error(a,"Home is unavailable. Open icon packs again.");return;}
        Config config=Config.load(a);List<String> favorites=DesktopLayout.favorites(config,Order.load(Order.file(a)));
        choose(a,pack->load(a,pack.label,()->{
            IconPackFormat.Mapping mapping=mapping(a,pack);List<HomeCategories.App> apps=HomeCategories.catalog(a,config);
            apps.sort(Comparator.comparingInt(app->{int i=favorites.indexOf(app.pkg);return i<0?Integer.MAX_VALUE:i;}));
            Map<HomeCategories.App,String> matches=new LinkedHashMap<>();for(HomeCategories.App app:apps){String name=mapping.find(app.pkg,app.component.getClassName());if(name!=null&&matches.size()<64)matches.put(app,name);}return matches;
        },matches->{
            if(matches.isEmpty()){error(a,"No exact mappings for the visible apps. Use Customize apps → Artwork → Installed icon pack to choose an icon manually.");return;}
            List<HomeCategories.App> apps=new ArrayList<>(matches.keySet());String[] names=new String[apps.size()];boolean[] on=new boolean[apps.size()];
            for(int i=0;i<apps.size();i++){HomeCategories.App app=apps.get(i);boolean custom=NativeTileEditor.hasArtwork(a,app.pkg);names[i]=app.label+(custom?" · custom artwork kept unless checked":"");on[i]=favorites.contains(app.pkg)&&!custom;}
            FreedomUi.checklist(a,"Matched apps · preview before Apply",names,on,d->{
                Map<String,String> selected=new LinkedHashMap<>();for(int i=0;i<apps.size();i++)if(on[i])selected.put(apps.get(i).pkg,matches.get(apps.get(i)));
                if(selected.isEmpty()){error(a,"Choose at least one app");return;}d.dismiss();
                load(a,"Prepare icon preview",()->{Map<String,byte[]> images=new LinkedHashMap<>();int total=0;for(var e:selected.entrySet()){byte[] bytes=picture(a,pack,e.getValue());if((total+=bytes.length)>8*1024*1024)throw new IOException("Choose fewer icons");images.put(e.getKey(),bytes);}return images;},images->review(a,images,()->{try{if(!pack.current(a))throw new IOException();NativeTileEditor.applyPack(a,images,guard);}catch(Exception failed){error(a,"Home or the icon pack changed, or artwork storage is full. No app choices were applied.");}}));
            });
        }));
    }
    static void pick(Activity a,Consumer<byte[]> next){
        choose(a,pack->load(a,pack.label,()->mapping(a,pack).names(),names->{
            LinearLayout body=FreedomUi.body(a);EditText search=new EditText(a);search.setSingleLine(true);search.setTextColor(a.getColor(R.color.freedom_text));search.setHintTextColor(a.getColor(R.color.freedom_secondary));search.setHint("Search icon names");search.setContentDescription("Search icon names in this pack");search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});body.addView(search);
            LinearLayout results=new LinearLayout(a);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);final AlertDialog[] dialog={null};
            Runnable filter=()->{results.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int count=0;
                for(String name:names)if(name.contains(query)){if(++count>40)break;FreedomUi.action(a,results,name,()->{dialog[0].dismiss();load(a,"Prepare icon preview",()->picture(a,pack,name),bytes->{Map<String,byte[]> image=Collections.singletonMap("Selected icon",bytes);review(a,image,()->{if(pack.current(a))next.accept(bytes);else error(a,"The pack changed. Choose it again.");});});});}
                if(count==0)results.addView(FreedomUi.text(a,"No matching icon names. Try another word or pack.",17));};
            search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void onTextChanged(CharSequence s,int st,int before,int n){filter.run();}public void afterTextChanged(Editable e){}});filter.run();dialog[0]=FreedomUi.sheet(a,"Choose an icon · first 40 matches",body);
        }));
    }
    private static void review(Activity a,Map<String,byte[]> images,Runnable apply){
        LinearLayout body=FreedomUi.body(a);body.addView(FreedomUi.text(a,images.size()+" icon(s) ready. Apply changes only the selected app artwork. Names, app order, wallpaper and TV settings stay saved.",17));
        int shown=0;LinearLayout row=null;
        for(var e:images.entrySet()){
            if(shown==4)break;
            if(shown%2==0){row=new LinearLayout(a);body.addView(row);}
            shown++;LinearLayout cell=new LinearLayout(a);cell.setOrientation(LinearLayout.VERTICAL);cell.setPadding(FreedomUi.dp(a,8),0,FreedomUi.dp(a,8),FreedomUi.dp(a,8));row.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
            String label=e.getKey();try{label=a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(e.getKey(),0)).toString();}catch(Exception manual){}
            cell.addView(FreedomUi.text(a,label,16));ImageView image=new ImageView(a);image.setContentDescription("Preview artwork for "+label);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;image.setImageBitmap(BitmapFactory.decodeByteArray(e.getValue(),0,e.getValue().length,options));cell.addView(image,new LinearLayout.LayoutParams(-1,FreedomUi.dp(a,72)));
        }
        final AlertDialog[] d={null};FreedomUi.action(a,body,"Apply selected icons",()->{d[0].dismiss();apply.run();});d[0]=FreedomUi.sheet(a,"Review icon import",body);
    }
    private static void error(Activity a,String message){new AlertDialog.Builder(a).setTitle("Icon packs").setMessage(message).setPositiveButton("Close",null).show();}
    private IconPacks(){}
}
