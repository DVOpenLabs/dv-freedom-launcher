package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;

/** Native, profile-scoped app customization. Never changes or reinstalls the app itself. */
final class NativeTileEditor {
    static final String KEY="app_customize_v1";
    static final int IMPORT=4208;
    private static final long MAX_INPUT=8L*1024*1024,MAX_LIBRARY=32L*1024*1024;
    private static final Map<Activity,Pending> PENDING=new WeakHashMap<>();
    private static final Map<Activity,Integer> GENERATIONS=new WeakHashMap<>();
    private static int generation(Activity a){int value=GENERATIONS.getOrDefault(a,0)+1;GENERATIONS.put(a,value);return value;}
    private static boolean current(Activity a,int generation){return !a.isDestroyed()&&!a.isFinishing()&&Objects.equals(GENERATIONS.get(a),generation);}
    private static final class Pending {
        final String pkg;final Guard guard;
        Pending(String pkg,Guard guard){this.pkg=pkg;this.guard=guard;}
    }
    private static final class Guard {
        final String profile,order;final Map<String,?> values;
        Guard(Context c)throws Exception{Config.load(c);profile=ProfileStore.id();values=ProfileStore.preferences(c).getAll();order=DesktopLayout.orderStamp(c);}
        void check(Context c)throws Exception{
            Config.load(c);
            if(!profile.equals(ProfileStore.id())||!values.equals(ProfileStore.preferences(c).getAll())||!order.equals(DesktopLayout.orderStamp(c)))throw new IllegalStateException("Home changed. Open this editor again.");
        }
    }
    static boolean validate(String raw){try{ArtworkSpec.parse(raw);return true;}catch(RuntimeException invalid){return false;}}
    static void validateAssets(Context c,String raw)throws IOException{
        try{for(String value:ArtworkSpec.parse(raw).values()){
            if(value.startsWith("image:")){
                File file=asset(c,value);if(!file.isFile()||file.length()<1||file.length()>1024*1024||Files.isSymbolicLink(file.toPath()))throw new IOException("Saved app artwork is unavailable");
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),bounds);
                if(bounds.outWidth!=960||bounds.outHeight!=540)throw new IOException("Saved app artwork is invalid");
            }
            if(value.startsWith("banner_")&&c.getResources().getIdentifier(value,"drawable",c.getPackageName())==0)throw new IOException("Saved app banner is unavailable");
        }}
        catch(IllegalArgumentException invalid){throw new IOException("Invalid saved artwork",invalid);}
    }
    static String label(Context c,String pkg,String fallback){return fallback;}
    private static Map<String,String> choices(Context c){try{return ArtworkSpec.parse(ProfileStore.preferences(c).getString(KEY,""));}catch(RuntimeException invalid){return new LinkedHashMap<>();}}
    private static File folder(Context c){return new File(c.getFilesDir(),"product-app-art");}
    private static File asset(Context c,String token){if(!token.matches("image:[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid artwork asset");return new File(folder(c),token.substring(6)+".jpg");}
    static Drawable banner(Context c,String pkg){
        String value=choices(c).get(pkg);if(value==null)return null;
        try{
            if("icon".equals(value))return new IconCard(c.getPackageManager().getApplicationIcon(pkg));
            if(value.startsWith("banner_")){int id=c.getResources().getIdentifier(value,"drawable",c.getPackageName());return id==0?null:c.getDrawable(id);}
            File file=asset(c,value);if(!file.isFile()||file.length()>1024*1024)return null;
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),bounds);
            if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>960||bounds.outHeight>540)return null;
            bounds.inJustDecodeBounds=false;bounds.inSampleSize=2;bounds.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap bitmap=BitmapFactory.decodeFile(file.getPath(),bounds);return bitmap==null?null:new BitmapDrawable(c.getResources(),bitmap);
        }catch(Exception|OutOfMemoryError unavailable){return null;}
    }
    static void show(Activity a){
        Config config=Config.load(a);String profile=ProfileStore.id();int generation=generation(a);
        List<String> favorites=DesktopLayout.favorites(config,Order.load(Order.file(a)));
        AlertDialog loading=new AlertDialog.Builder(a).setTitle("Customize apps").setMessage("Loading apps…").setNegativeButton(android.R.string.cancel,null).show();
        java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
        worker.execute(()->{
            try{
            Map<String,ResolveInfo> apps=new TreeMap<>();
            for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo info:a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0))
                if(info.activityInfo!=null&&info.activityInfo.exported&&info.activityInfo.enabled&&info.activityInfo.applicationInfo.enabled&&!info.activityInfo.packageName.equals(a.getPackageName()))apps.putIfAbsent(info.activityInfo.packageName,info);
            Map<String,String> labels=new HashMap<>();for(ResolveInfo info:apps.values()){String pkg=info.activityInfo.packageName;String custom=config.appLabels.get(pkg);labels.put(pkg,custom==null?info.loadLabel(a.getPackageManager()).toString():custom);}
            List<ResolveInfo> rows=new ArrayList<>(apps.values());rows.sort((left,right)->{
                String lp=left.activityInfo.packageName,rp=right.activityInfo.packageName;int li=favorites.indexOf(lp),ri=favorites.indexOf(rp);
                int ranked=Integer.compare(li<0?Integer.MAX_VALUE:li,ri<0?Integer.MAX_VALUE:ri);if(ranked!=0)return ranked;
                int named=String.CASE_INSENSITIVE_ORDER.compare(labels.get(lp),labels.get(rp));return named!=0?named:lp.compareTo(rp);
            });
            String[] names=new String[rows.size()];for(int i=0;i<rows.size();i++)names[i]=labels.get(rows.get(i).activityInfo.packageName);
            new Handler(Looper.getMainLooper()).post(()->{
                if(!current(a,generation)||!loading.isShowing())return;loading.dismiss();
                if(!profile.equals(ProfileStore.id())){message(a,"Profile changed. Open the editor again.");return;}
                new AlertDialog.Builder(a).setTitle("Customize apps").setItems(names,(d,which)->app(a,rows.get(which))).setNegativeButton(android.R.string.cancel,null).show();
            });
            }catch(RuntimeException failure){new Handler(Looper.getMainLooper()).post(()->{if(current(a,generation)&&loading.isShowing()){loading.dismiss();message(a,"Could not load apps. Try again.");}});}
            finally{worker.shutdown();}
        });
        loading.setOnCancelListener(d->worker.shutdownNow());
    }
    static void show(Activity a,String pkg){
        if(!LayoutDraft.packageName(pkg)||pkg.equals(a.getPackageName()))return;
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo info:a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(pkg),0))
            if(info.activityInfo!=null&&info.activityInfo.exported&&info.activityInfo.enabled&&info.activityInfo.applicationInfo.enabled){app(a,info);return;}
        message(a,"This app is no longer available");
    }
    private static void app(Activity a,ResolveInfo info){
        try{
            String pkg=info.activityInfo.packageName;Config config=Config.load(a);String title=config.appLabels.getOrDefault(pkg,info.loadLabel(a.getPackageManager()).toString());
            ImageView preview=new ImageView(a);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setAdjustViewBounds(true);
            Drawable art=banner(a,pkg);if(art==null)art=info.activityInfo.loadBanner(a.getPackageManager());if(art==null)art=info.loadIcon(a.getPackageManager());preview.setImageDrawable(art);
            LinearLayout content=new LinearLayout(a);content.setPadding(24,12,24,12);content.addView(preview,new LinearLayout.LayoutParams(-1,Math.round(120*a.getResources().getDisplayMetrics().density)));
            new AlertDialog.Builder(a).setTitle(title).setView(content).setItems(new String[]{"Rename","Use app’s original name","Artwork"},(d,which)->{
                try{if(which==0)rename(a,pkg,title);else if(which==1)resetName(a,pkg,new Guard(a));else art(a,pkg);}
                catch(Exception failure){error(a,failure);}
            }).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception failure){error(a,failure);}catch(OutOfMemoryError memory){message(a,"Not enough memory to preview this artwork");}
    }
    private static void rename(Activity a,String pkg,String current)throws Exception{
        Guard guard=new Guard(a);EditText input=new EditText(a);input.setSingleLine();input.setText(current);input.setSelectAllOnFocus(true);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Rename app").setView(input).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->{input.requestFocus();d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{saveName(a,pkg,ArtworkSpec.name(input.getText().toString()),guard);d.dismiss();}catch(Exception failure){input.setError(failure.getMessage());}});});d.show();
    }
    private static void saveName(Activity a,String pkg,String name,Guard guard)throws Exception{
        commitName(a,pkg,ArtworkSpec.name(name),guard);
    }
    private static void resetName(Activity a,String pkg,Guard guard)throws Exception{commitName(a,pkg,null,guard);}
    private static void commitName(Activity a,String pkg,String name,Guard guard)throws Exception{
        guard.check(a);Config config=Config.load(a);Map<String,String> labels=ArtworkSpec.named(config.appLabels,pkg,name);
        if(labels.equals(config.appLabels))return;
        JSONObject state=new JSONObject(DesktopLayout.capture(a,config));state.put("priority",config.desktopOrder);
        state.getJSONObject("layout").put("labels",new JSONObject(labels));
        LayoutDraft draft=DesktopLayout.draftFromSaved(state.toString());
        if(draft.labels.size()>64||state.toString().length()>DesktopLayout.MAX_BYTES)throw new IllegalArgumentException("Too many custom app names");
        ProductFeatures.beforeChange(a,name==null?"Restore app name":"Rename app");
        SharedPreferences p=ProfileStore.preferences(a);Map<String,Object> changes=new HashMap<>();changes.put(DesktopLayout.STATE,state.toString());changes.put(DesktopLayout.UNDO,null);changes.put(DesktopLayout.GUARD,null);changes.put(DesktopLayout.UNDO_POSITION,null);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(!SetupValues.commit(p,changes))throw new IOException("Could not save app name");done(a);
    }
    private static void art(Activity a,String pkg)throws Exception{
        Guard guard=new Guard(a);
        new AlertDialog.Builder(a).setTitle("App artwork").setItems(new String[]{"Original artwork","App icon","TV banner","Choose image…"},(d,which)->{
            try{
                if(which==3){PENDING.put(a,new Pending(pkg,guard));try{a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}catch(android.content.ActivityNotFoundException unavailable){PENDING.remove(a);message(a,"This TV has no image picker. Choose original artwork, App icon or TV banner instead.");}}
                else saveArt(a,pkg,which==0?null:which==1?"icon":"banner_tv",guard);
            }catch(Exception failure){error(a,failure);}
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private static void saveArt(Activity a,String pkg,String token,Guard guard)throws Exception{
        guard.check(a);Map<String,String> values=choices(a);if(Objects.equals(values.get(pkg),token))return;
        if(token==null)values.remove(pkg);else values.put(pkg,token);String text=ArtworkSpec.encode(values);validateAssets(a,text);
        ProductFeatures.beforeChange(a,"Change app artwork");SharedPreferences p=ProfileStore.preferences(a);Map<String,Object> changes=new HashMap<>();changes.put(KEY,text.isEmpty()?null:text);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(!SetupValues.commit(p,changes))throw new IOException("Could not save artwork");done(a);
    }
    static void onActivityResult(Activity a,int request,int result,Intent data){
        if(request!=IMPORT)return;Pending pending=PENDING.remove(a);
        if(pending==null||result!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        int generation=generation(a);AlertDialog loading=new AlertDialog.Builder(a).setTitle("Import app artwork").setMessage("Preparing image…").setNegativeButton(android.R.string.cancel,null).show();
        java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
        worker.execute(()->{
            byte[] encoded=null;Exception error=null;
            try(InputStream input=a.getContentResolver().openInputStream(data.getData())){encoded=prepare(read(input));}catch(Exception failure){error=failure;}catch(OutOfMemoryError memory){error=new IOException("Not enough memory to prepare this image");}
            final byte[] bytes=encoded;final Exception failure=error;
            new Handler(Looper.getMainLooper()).post(()->{
                if(!current(a,generation)||!loading.isShowing())return;loading.dismiss();
                try{
                    if(failure!=null)throw failure;pending.guard.check(a);
                    ImageView preview=new ImageView(a);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setAdjustViewBounds(true);preview.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length));
                    AlertDialog confirm=new AlertDialog.Builder(a).setTitle("Use this app artwork?").setView(preview).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Use image",(d,which)->{try{pending.guard.check(a);String token=publish(a,bytes);saveArt(a,pending.pkg,token,pending.guard);}catch(Exception failed){error(a,failed);}}).create();
                    confirm.setOnDismissListener(d->preview.setImageDrawable(null));confirm.show();confirm.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
                }catch(Exception failed){error(a,failed);}
            });worker.shutdown();
        });loading.setOnCancelListener(d->worker.shutdownNow());
    }
    private static byte[] read(InputStream input)throws IOException{
        if(input==null)throw new IOException("Image is unavailable");ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
        for(int n;(n=input.read(buffer))!=-1;){if(output.size()+n>MAX_INPUT)throw new IOException("Choose an image under 8 MB");output.write(buffer,0,n);}return output.toByteArray();
    }
    private static byte[] prepare(byte[] bytes)throws IOException{
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(!WallpaperSize.accepted(options.outWidth,options.outHeight))throw new IOException("Choose an image up to 4K resolution");
        int sample=1;while(options.outWidth/sample>1920||options.outHeight/sample>1080)sample*=2;
        options.inJustDecodeBounds=false;options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap source=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(source==null)throw new IOException("Unsupported image");
        Bitmap target=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(target);canvas.drawColor(0xff182334);
        float scale=Math.max(960f/source.getWidth(),540f/source.getHeight());float w=source.getWidth()*scale,h=source.getHeight()*scale;
        canvas.drawBitmap(source,null,new RectF((960-w)/2,(540-h)/2,(960+w)/2,(540+h)/2),new Paint(Paint.FILTER_BITMAP_FLAG));source.recycle();
        ByteArrayOutputStream output=new ByteArrayOutputStream();boolean ok=target.compress(Bitmap.CompressFormat.JPEG,90,output);target.recycle();
        if(!ok||output.size()>1024*1024)throw new IOException("Could not prepare artwork");return output.toByteArray();
    }
    private static String publish(Context c,byte[] bytes)throws Exception{
        StringBuilder digest=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))digest.append(String.format(Locale.ROOT,"%02x",b&255));String token="image:"+digest;
        File dir=folder(c);if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Artwork storage unavailable");File target=asset(c,token);if(target.isFile())return token;
        File[] files=dir.listFiles();if(files==null)throw new IOException("Artwork storage unavailable");long total=0;int count=0;for(File file:files){if(Files.isSymbolicLink(file.toPath()))throw new IOException("Invalid artwork storage");if(file.getName().matches("[a-f0-9]{64}\\.jpg")){total+=file.length();count++;}}
        if(count>=64||total+bytes.length>MAX_LIBRARY)throw new IOException("Artwork library is full. Earlier artwork is retained for saved setups.");
        File temporary=File.createTempFile(".import-",".tmp",dir);try{try(FileOutputStream output=new FileOutputStream(temporary)){output.write(bytes);output.getFD().sync();}java.nio.file.Files.move(temporary.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE);}finally{temporary.delete();}return token;
    }
    private static final class IconCard extends Drawable {
        final Drawable icon;IconCard(Drawable icon){this.icon=icon.mutate();}
        @Override public void draw(Canvas canvas){Rect r=getBounds();Paint paint=new Paint();paint.setColor(0xff182334);canvas.drawRect(r,paint);int size=Math.round(r.height()*.7f);int x=r.centerX()-size/2,y=r.centerY()-size/2;icon.setBounds(x,y,x+size,y+size);icon.draw(canvas);}
        @Override public int getIntrinsicWidth(){return 320;}@Override public int getIntrinsicHeight(){return 180;}
        @Override public void setAlpha(int alpha){icon.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){icon.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
    private static void done(Activity a){message(a,"App customization saved");a.recreate();}
    private static void message(Context a,String text){Toast.makeText(a,text,Toast.LENGTH_LONG).show();}
    private static void error(Context a,Exception failure){message(a,failure.getMessage()==null?"Could not customize this app":failure.getMessage());}
    private NativeTileEditor(){}
}
