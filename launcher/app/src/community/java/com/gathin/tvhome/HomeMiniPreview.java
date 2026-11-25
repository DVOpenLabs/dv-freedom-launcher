package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.File;

/** Small still-only settings illustration. It never opens a video decoder or selects media. */
final class HomeMiniPreview extends View {
    private final Config config;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final String look,position;
    private String inspect="",draftPosition,draftValue;private Boolean bright;
    void inspect(String setting,String position,String value,boolean light){inspect=setting;draftPosition=position;draftValue=value;bright=light;invalidate();}
    private java.util.Map<String,Object> preset;
    void preset(java.util.Map<String,Object> values){preset=new java.util.HashMap<>(values);invalidate();}
    private Bitmap bitmap;private boolean attached;private int generation;private File override;
    private final java.util.List<android.graphics.drawable.Drawable> artwork=new java.util.ArrayList<>();
    private final java.util.List<String> names=new java.util.ArrayList<>();
    void personalApps(android.app.Activity a,Config cfg){personalApps(a,cfg,DesktopLayout.favorites(cfg,Order.load(Order.file(a))));}
    void personalApps(android.app.Activity a,Config cfg,java.util.List<String> order){artwork.clear();names.clear();java.util.Map<String,HomeCategories.App> catalog=new java.util.HashMap<>();for(HomeCategories.App app:HomeCategories.catalog(a,cfg))catalog.put(app.pkg,app);for(String pkg:order){HomeCategories.App app=catalog.get(pkg);if(app!=null&&FamilyMode.allowed(a,pkg)&&artwork.size()<8){artwork.add(app.artwork(a,cfg));names.add(app.label);}}invalidate();}
    void wallpaper(File file){override=file;if(attached)loadStill();}
    void release(){attached=false;generation++;bitmap=null;artwork.clear();names.clear();}

    HomeMiniPreview(Context c,Typeface font){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setFocusable(false);config=Config.load(c);look=ExperienceChoice.saved(c);String selected=PlacementSession.selected(c);position=AppPlacement.valid(selected)?selected:ExperiencePolicy.defaultPosition(look);paint.setTypeface(font);}
    private int previewGap(){if(preset!=null)return (Integer)preset.get("spacing");if((inspect.equals("Extra tile spacing")||inspect.equals("App spacing"))&&draftValue!=null&&!draftValue.equals("Theme"))try{return Integer.parseInt(draftValue.split(" ")[0]);}catch(NumberFormatException ignored){}return config.tileGap;}
    private int previewCount(){if(!artwork.isEmpty())return artwork.size();if(inspect.equals("Apps per row")&&draftValue!=null)try{return Math.max(2,Math.min(8,Integer.parseInt(draftValue)));}catch(NumberFormatException ignored){}return 8;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;loadStill();}
    private void loadStill(){final int token=++generation;ThemeLibrary.Entry entry=ThemeLibrary.active();String current=getContext().getSharedPreferences(LocalProfiles.filename(ProfileStore.id(),WallpaperPlaylist.PLAYBACK),Context.MODE_PRIVATE).getString("current","");java.util.List<ThemeLibrary.Entry> selected=WallpaperPlaylistChoice.entries(getContext(),WallpaperPlaylistChoice.read(getContext()));if(WallpaperPlaylistChoice.enabled(getContext())&&!selected.isEmpty())entry=selected.get(0);for(ThemeLibrary.Entry candidate:selected)if(candidate.token.equals(current))entry=candidate;File dir=entry==null?Config.dir(getContext()):entry.directory;if(dir==null)return;File file=override==null?new File(dir,Config.WALLPAPER_NAME):override;
        Thread loader=new Thread(()->{Bitmap image=null;try{if(!file.isFile()||file.length()>ThemeArchive.MAX_IMAGE)return;BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);if(WallpaperSize.accepted(options.outWidth,options.outHeight)){options.inSampleSize=WallpaperSize.sampleSize(options.outWidth,options.outHeight,480,270);options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;options.inPreferredColorSpace=android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB);image=BitmapFactory.decodeFile(file.getPath(),options);}}catch(RuntimeException|OutOfMemoryError ignored){}final Bitmap result=image;post(()->{if(!attached||generation!=token){if(result!=null)result.recycle();return;}bitmap=result;invalidate();});},"Home still preview");loader.setDaemon(true);loader.start();
    }
    @Override protected void onDetachedFromWindow(){attached=false;generation++;bitmap=null;super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int saved=canvas.save();canvas.scale(getWidth()/960f,getHeight()/540f);canvas.clipRect(0,0,960,540);canvas.drawColor(config.backgroundColor);
        if(bitmap!=null&&!bitmap.isRecycled()){float scale=Math.max(960f/bitmap.getWidth(),540f/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;canvas.drawBitmap(bitmap,null,new RectF((960-w)/2,(540-h)/2,(960+w)/2,(540+h)/2),paint);}canvas.drawColor(preset!=null&&preset.get("dim") instanceof Integer?(((Integer)preset.get("dim")*255/100)<<24):Boolean.TRUE.equals(bright)?0xbbe2eaf2:0x99081020);
        int ink=preset!=null&&"dark".equals(preset.get("text"))?0xff182334:preset!=null&&"light".equals(preset.get("text"))?0xfff5f7fa:Boolean.TRUE.equals(bright)?0xff182334:0xfff5f7fa;int[] sizes=HomeAppearance.parse(preset==null?"":String.valueOf(preset.getOrDefault("textSizes","")));paint.setColor(ink);paint.setTextSize(43*sizes[0]/100f);canvas.drawText("9:41",40,70,paint);paint.setTextSize(18*sizes[1]/100f);canvas.drawText("Your Home",40,101,paint);canvas.drawText("Inputs     Settings",700,64,paint);
        String selectedLook=preset==null?look:(String)preset.get("experience");String style=ExperiencePolicy.LEGACY.equals(selectedLook)?ExperiencePolicy.CLEAN:selectedLook;ExperiencePolicy.Geometry g=new ExperiencePolicy.Geometry(style,preset!=null?(String)preset.get("position"):draftPosition==null?position:draftPosition,previewCount(),previewGap(),preset!=null?(String)preset.get("iconSize"):inspect.equals("Icon size")&&IconSizePolicy.valid(draftValue)?draftValue:IconSizes.current(getContext()));
        if(!ExperiencePolicy.CLEAN.equals(style)){paint.setColor(0xcc192637);canvas.drawRoundRect(g.left-14,g.top-16,960-g.left+14,g.top+g.cardHeight+40,20,20,paint);}
        int[] colors={0xff079ddd,0xfff3f5f8,0xffd51f32,0xfff46a15,0xff1379cf,0xff232936,0xffc80c22,0xffdbe9f4};for(int i=0;i<g.count;i++){paint.setColor(colors[i]);canvas.drawRoundRect(g.x(i),g.top,g.x(i)+g.cardWidth,g.top+g.cardHeight,9,9,paint);if(i<artwork.size()&&artwork.get(i)!=null){android.graphics.drawable.Drawable art=artwork.get(i);art.setBounds((int)g.x(i),(int)g.top,(int)(g.x(i)+g.cardWidth),(int)(g.top+g.cardHeight));art.draw(canvas);}}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(7);paint.setColor(ink);canvas.drawRoundRect(g.x(0)-5,g.top-5,g.x(0)+g.cardWidth+5,g.top+g.cardHeight+5,12,12,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(ink);paint.setTextSize((inspect.equals("Text sizes")?25:20)*sizes[2]/100f);if(preset==null||!"hidden".equals(preset.get("names"))){int names=preset!=null&&"all".equals(preset.get("names"))?g.count:1;for(int i=0;i<names;i++){String label=i<this.names.size()?this.names.get(i):"Your app";if(paint.measureText(label)>g.cardWidth){while(label.length()>1&&paint.measureText(label+"…")>g.cardWidth)label=label.substring(0,label.length()-1);label+="…";}canvas.drawText(label,g.x(i),g.labelTop+16,paint);}}
        if(ExperiencePolicy.CINEMA.equals(style)){paint.setColor(ink);paint.setTextSize(38);canvas.drawText("Your selected app",46,Math.max(140,g.top-34),paint);}canvas.restoreToCount(saved);
    }
}
