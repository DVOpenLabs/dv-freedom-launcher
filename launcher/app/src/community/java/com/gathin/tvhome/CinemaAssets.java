package com.gathin.tvhome;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.util.LruCache;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

/** Local alpha-preserving artwork. No network, executable packs or persisted URI grants. */
final class CinemaAssets {
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(12*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
    private static File directory(Context c)throws IOException{File d=new File(c.getFilesDir(),"cinema-art");if(Files.isSymbolicLink(d.toPath())||!d.exists()&&!d.mkdir())throw new IOException("Artwork storage unavailable");return d;}
    static File file(Context c,String id)throws IOException{CinemaSpec.asset(id);if(id.isEmpty())throw new IOException("Choose artwork first");File f=new File(directory(c),id+".png");if(Files.isSymbolicLink(f.toPath()))throw new IOException("Invalid artwork storage");return f;}
    static void validate(Context c,CinemaSpec s)throws IOException{Set<String> tokens=new HashSet<>();for(ThemeLibrary.Entry entry:ThemeLibrary.list(c))tokens.add(entry.token);for(CinemaWorld world:s.worlds)if(!tokens.contains(world.wallpaper))throw new IOException("Import the matching world wallpaper on this TV first.");for(String id:s.assets()){File f=file(c,id);if(!f.isFile()||f.length()<1||f.length()>2*1024*1024)throw new IOException("Artwork is missing. Import it on this TV first.");BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);if(o.outWidth<1||o.outHeight<1||o.outWidth>1600||o.outHeight>1600||(long)o.outWidth*o.outHeight>2000000)throw new IOException("Invalid cinema artwork");}}
    static String importImage(Context c,Uri uri)throws Exception{try(InputStream input=c.getContentResolver().openInputStream(uri)){return importStream(c,input);}}
    static String importStream(Context c,InputStream input)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=input){if(in==null)throw new IOException("Image unavailable");byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;){if(bytes.size()+n>8*1024*1024)throw new IOException("Choose an image below 8 MiB");bytes.write(buffer,0,n);}}
        byte[] raw=bytes.toByteArray();BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(raw,0,raw.length,o);if(o.outWidth<1||o.outHeight<1||o.outWidth>8192||o.outHeight>8192||(long)o.outWidth*o.outHeight>20000000)throw new IOException("Choose a smaller image");int sample=1;while(Math.max(o.outWidth,o.outHeight)/sample>1600)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap b=BitmapFactory.decodeByteArray(raw,0,raw.length,o);if(b==null)throw new IOException("Unsupported image");
        try{ByteArrayOutputStream png=new ByteArrayOutputStream();if(!b.compress(Bitmap.CompressFormat.PNG,100,png)||png.size()>2*1024*1024)throw new IOException("Choose simpler artwork below 2 MiB after preparation");byte[] out=png.toByteArray();StringBuilder hash=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(out))hash.append(String.format(Locale.ROOT,"%02x",v&255));String id=hash.toString();File f=file(c,id);if(f.isFile())return id;File[] files=directory(c).listFiles();if(files==null)throw new IOException("Artwork storage unavailable");long total=0;for(File existing:files)total+=existing.length();if(files.length>=96||total+out.length>64L*1024*1024)throw new IOException("Cinema artwork library is full");android.util.AtomicFile atomic=new android.util.AtomicFile(f);FileOutputStream stream=null;try{stream=atomic.startWrite();stream.write(out);atomic.finishWrite(stream);}catch(Exception fail){if(stream!=null)atomic.failWrite(stream);throw fail;}return id;}finally{b.recycle();}
    }
    static Bitmap read(Context c,String id){if(id==null||id.isEmpty())return null;String key=c.getFilesDir()+"/"+id;Bitmap hit=CACHE.get(key);if(hit!=null)return hit;try{File f=file(c,id);if(!f.isFile()||f.length()>2*1024*1024)return null;BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);if(o.outWidth<1||o.outHeight<1||o.outWidth>1600||o.outHeight>1600)return null;o.inJustDecodeBounds=false;o.inSampleSize=2;o.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap b=BitmapFactory.decodeFile(f.getPath(),o);if(b!=null)CACHE.put(key,b);return b;}catch(Exception|OutOfMemoryError unavailable){return null;}}
    static Drawable logo(Context c,String pkg){return logo(c,pkg,CinemaStore.read(c));}
    static Drawable logo(Context c,String pkg,CinemaSpec s){if(!s.logos)return null;Bitmap b=read(c,s.appLogos.get(pkg));if(b!=null)return new Logo(b);if(s.iconStyle.equals("original"))return null;String label=pkg;try{label=c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}return new Emblem(pkg,label,s.iconStyle);}
    static void trim(){CACHE.evictAll();}
    static void named(Context c,String id,String name){c.getSharedPreferences("cinema-art-names",0).edit().putString(id,name.length()>80?name.substring(0,80):name).apply();}
    static Map<String,String> library(Context c){Map<String,String> result=new LinkedHashMap<>();try{File[] files=directory(c).listFiles();if(files==null)return result;Arrays.sort(files,Comparator.comparing(File::getName));for(File f:files)if(f.getName().matches("[a-f0-9]{64}\\.png")){String id=f.getName().substring(0,64);result.put(id,c.getSharedPreferences("cinema-art-names",0).getString(id,"Artwork "+id.substring(0,8)));}}catch(IOException ignored){}return result;}
    /** Original theme emblems, not copied brand-logo files. Imported exact marks take precedence. */
    static final class Emblem extends Drawable {
        final Paint p=new Paint(3);final String pkg,label;final int colour;final boolean ornate;int alpha=255;
        Emblem(String pkg,String label,String style){this(pkg,label,style.equals("gold")?0xffe7cd8c:style.equals("silver")?0xffd8e6ef:0xffffba8c);}
        Emblem(String pkg,String label,int color){this(pkg,label,color,true);}
        Emblem(String pkg,String label,int color,boolean ornament){this.pkg=pkg.toLowerCase(Locale.ROOT);this.label=label;colour=color;ornate=ornament;}
        public int getIntrinsicWidth(){return 420;}public int getIntrinsicHeight(){return 220;}
        public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);float w=b.width(),h=b.height(),cx=w/2,cy=h*.47f;p.setColor(colour);p.setAlpha(alpha);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,h*.018f));
            // Two small sprigs coordinate the emblems without surrounding them with a card.
            if(ornate)for(int sign:new int[]{-1,1}){float x=cx+sign*w*.39f;c.drawLine(x,cy+h*.2f,x-sign*w*.055f,cy-h*.19f,p);for(int i=0;i<3;i++)c.drawLine(x-sign*w*.018f*i,cy+h*(.13f-i*.1f),x+sign*w*.025f,cy+h*(.06f-i*.1f),p);}
            if(!ornate){c.drawLine(w*.12f,h*.18f,w*.23f,h*.18f,p);c.drawLine(w*.77f,h*.8f,w*.88f,h*.8f,p);c.drawLine(w*.12f,h*.18f,w*.12f,h*.35f,p);c.drawLine(w*.88f,h*.62f,w*.88f,h*.8f,p);}
            p.setStyle(Paint.Style.FILL);String word;
            if(pkg.contains("youtube")){Path play=new Path();play.moveTo(cx-w*.055f,cy-h*.14f);play.lineTo(cx+w*.075f,cy);play.lineTo(cx-w*.055f,cy+h*.14f);play.close();c.drawPath(play,p);p.setStyle(Paint.Style.STROKE);c.drawRoundRect(cx-w*.16f,cy-h*.23f,cx+w*.16f,cy+h*.23f,h*.09f,h*.09f,p);p.setStyle(Paint.Style.FILL);word="YouTube";}
            else if(pkg.contains("stb")){p.setStyle(Paint.Style.STROKE);c.drawRoundRect(cx-w*.18f,cy-h*.24f,cx+w*.18f,cy+h*.21f,8,8,p);c.drawLine(cx-w*.06f,cy+h*.28f,cx+w*.06f,cy+h*.28f,p);p.setStyle(Paint.Style.FILL);word="STB EMU";}
            else if(pkg.equals("com.android.vending")){Path play=new Path();play.moveTo(cx-w*.11f,cy-h*.27f);play.lineTo(cx+w*.16f,cy);play.lineTo(cx-w*.11f,cy+h*.27f);play.close();p.setStyle(Paint.Style.STROKE);c.drawPath(play,p);c.drawLine(cx-w*.11f,cy-h*.27f,cx+w*.055f,cy,p);c.drawLine(cx-w*.11f,cy+h*.27f,cx+w*.055f,cy,p);p.setStyle(Paint.Style.FILL);}
            else{word=pkg.contains("netflix")?"N":pkg.contains("amazon")?"prime":pkg.contains("crunchy")?"CR":pkg.contains("apple")?"tv+":pkg.contains("cnn")?"CNN":pkg.contains("fandango")?"F":label;p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(h*(word.length()<3?.64f:.38f));float width=p.measureText(word);if(width>w*.56f)p.setTextSize(p.getTextSize()*w*.56f/width);p.setTextAlign(Paint.Align.CENTER);c.drawText(word,cx,cy-(p.ascent()+p.descent())/2,p);}
            c.restore();}
        public void setAlpha(int value){alpha=value;}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    /** Normalise apparent ink area once, preserve aspect ratio and alpha. */
    static final class Logo extends Drawable {
        final Bitmap bitmap;final Paint paint=new Paint(3);final RectF dst=new RectF();final float weight;
        Logo(Bitmap b){bitmap=b;long sum=0;int[] row=new int[b.getWidth()];for(int y=0;y<b.getHeight();y++){b.getPixels(row,0,row.length,0,y,row.length,1);for(int v:row)sum+=v>>>24;}float fill=Math.max(.03f,sum/(255f*b.getWidth()*b.getHeight()));weight=Math.max(.75f,Math.min(1.3f,(float)Math.pow(.5/fill,.2)));}
        protected void onBoundsChange(Rect r){float factor=Math.min(r.width()*.78f/bitmap.getWidth(),r.height()*.75f/bitmap.getHeight())*weight;factor=Math.min(factor,Math.min(r.width()*.92f/bitmap.getWidth(),r.height()*.9f/bitmap.getHeight()));float w=bitmap.getWidth()*factor,h=bitmap.getHeight()*factor;dst.set(r.exactCenterX()-w/2,r.exactCenterY()-h/2,r.exactCenterX()+w/2,r.exactCenterY()+h/2);}
        public void draw(Canvas c){c.drawBitmap(bitmap,null,dst,paint);}public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
