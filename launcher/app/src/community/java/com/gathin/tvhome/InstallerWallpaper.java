package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import java.io.*;
/** Original small starter scene, generated locally; never overwrites an existing file. */
final class InstallerWallpaper {
    static String create(Context c,String variant)throws IOException{
        InstallerPolicy.one(variant,"coast","dusk","forest");File root=Config.dir(c);if(root==null)throw new IOException("Wallpaper storage is unavailable");File dir=new File(root,"wallpapers");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Could not prepare wallpaper storage");String name="freedom-starter-"+variant+"-v1.png";File file=new File(dir,name);if(file.isFile()){BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);if(file.length()>10*1024*1024||options.outWidth!=1280||options.outHeight!=720)throw new IOException("The starter wallpaper path contains another file; choose Keep current");return name;}
        Bitmap bitmap=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int top=variant.equals("forest")?0xff18493f:variant.equals("dusk")?0xff292e55:0xff133763;int horizon=variant.equals("forest")?0xffa7c8b4:variant.equals("dusk")?0xffc99caa:0xffe7bc9d;
        p.setShader(new LinearGradient(0,0,0,450,new int[]{top,0xff9bb9cb,horizon},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,1280,720,p);p.setShader(null);p.setColor(0xffffdfad);canvas.drawCircle(925,322,54,p);
        p.setShader(new LinearGradient(0,390,0,720,0xff3e879e,0xff102f4a,Shader.TileMode.CLAMP));canvas.drawRect(0,400,1280,720,p);p.setShader(null);p.setColor(0xff52758a);Path mountain=new Path();mountain.moveTo(0,480);mountain.lineTo(100,420);mountain.lineTo(170,330);mountain.lineTo(220,359);mountain.lineTo(270,300);mountain.lineTo(370,454);mountain.lineTo(470,479);mountain.lineTo(0,600);mountain.close();canvas.drawPath(mountain,p);
        p.setColor(0xff17384d);Path land=new Path();land.moveTo(0,520);land.lineTo(190,423);land.lineTo(330,511);land.lineTo(445,524);land.lineTo(0,625);land.close();canvas.drawPath(land,p);p.setColor(0xff102d42);Path right=new Path();right.moveTo(1280,720);right.lineTo(1120,545);right.lineTo(1000,518);right.lineTo(918,465);right.lineTo(866,480);right.lineTo(790,555);right.lineTo(638,720);right.close();canvas.drawPath(right,p);
        android.util.AtomicFile atomic=new android.util.AtomicFile(file);FileOutputStream out=null;try{out=atomic.startWrite();if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Could not save starter wallpaper");atomic.finishWrite(out);}catch(IOException failed){if(out!=null)atomic.failWrite(out);throw failed;}finally{bitmap.recycle();}return name;
    }
    private InstallerWallpaper(){}
}
