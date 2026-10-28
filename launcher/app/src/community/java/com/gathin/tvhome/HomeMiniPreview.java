package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.File;

/** Small still-only settings illustration. It never opens a video decoder or selects media. */
final class HomeMiniPreview extends View {
    private final Config config;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final String look,position;
    private Bitmap bitmap;private boolean attached;private int generation;
    HomeMiniPreview(Context c,Typeface font){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setFocusable(false);config=Config.load(c);look=ExperienceChoice.saved(c);String selected=PlacementSession.selected(c);position=AppPlacement.valid(selected)?selected:ExperiencePolicy.defaultPosition(look);paint.setTypeface(font);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;final int token=++generation;ThemeLibrary.Entry entry=ThemeLibrary.active();File dir=entry==null?Config.dir(getContext()):entry.directory;if(dir==null)return;File file=new File(dir,Config.WALLPAPER_NAME);
        Thread loader=new Thread(()->{Bitmap image=null;try{if(!file.isFile()||file.length()>ThemeArchive.MAX_IMAGE)return;BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);if(WallpaperSize.accepted(options.outWidth,options.outHeight)){options.inSampleSize=WallpaperSize.sampleSize(options.outWidth,options.outHeight,480,270);options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;image=BitmapFactory.decodeFile(file.getPath(),options);}}catch(RuntimeException|OutOfMemoryError ignored){}final Bitmap result=image;post(()->{if(!attached||generation!=token){if(result!=null)result.recycle();return;}bitmap=result;invalidate();});},"Home still preview");loader.setDaemon(true);loader.start();
    }
    @Override protected void onDetachedFromWindow(){attached=false;generation++;bitmap=null;super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int saved=canvas.save();canvas.scale(getWidth()/960f,getHeight()/540f);canvas.clipRect(0,0,960,540);canvas.drawColor(config.backgroundColor);
        if(bitmap!=null&&!bitmap.isRecycled()){float scale=Math.max(960f/bitmap.getWidth(),540f/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;canvas.drawBitmap(bitmap,null,new RectF((960-w)/2,(540-h)/2,(960+w)/2,(540+h)/2),paint);}canvas.drawColor(0x66081020);
        paint.setColor(0xfff5f7fa);paint.setTextSize(43);canvas.drawText("9:41",40,70,paint);paint.setTextSize(18);canvas.drawText("Your Home",40,101,paint);canvas.drawText("Inputs     Settings",700,64,paint);
        String style=ExperiencePolicy.LEGACY.equals(look)?ExperiencePolicy.CLEAN:look;ExperiencePolicy.Geometry g=new ExperiencePolicy.Geometry(style,position,8,config.tileGap);
        if(!ExperiencePolicy.CLEAN.equals(style)){paint.setColor(0xcc192637);canvas.drawRoundRect(g.left-14,g.top-16,960-g.left+14,g.top+g.cardHeight+40,20,20,paint);}
        int[] colors={0xff079ddd,0xfff3f5f8,0xffd51f32,0xfff46a15,0xff1379cf,0xff232936,0xffc80c22,0xffdbe9f4};for(int i=0;i<8;i++){paint.setColor(colors[i]);canvas.drawRoundRect(g.x(i),g.top,g.x(i)+g.cardWidth,g.top+g.cardHeight,9,9,paint);}
        if(ExperiencePolicy.CINEMA.equals(style)){paint.setColor(0xfff5f7fa);paint.setTextSize(38);canvas.drawText("Your selected app",46,Math.max(140,g.top-34),paint);}canvas.restoreToCount(saved);
    }
}
