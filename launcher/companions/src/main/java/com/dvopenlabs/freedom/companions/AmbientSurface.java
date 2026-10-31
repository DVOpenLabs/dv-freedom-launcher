package com.dvopenlabs.freedom.companions;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import org.json.JSONObject;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** A single foreground renderer for photo preview and dream. No idle takeover or permanent worker. */
final class AmbientSurface extends View {
    final Handler ui=new Handler(Looper.getMainLooper());final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    final String forcedSeason;SeasonalPolicy.Spec seasonal;boolean seasonalMode,seasonalToday;long seasonMinute=Long.MIN_VALUE,seasonStarted;final SeasonalPainter seasonalPainter=new SeasonalPainter();
    final Boolean forcedArt;ExecutorService worker;volatile boolean running;int generation,index;boolean loading;Bitmap first,second,previousFirst,previousSecond,fill;File firstFile,secondFile;long changed;String notice="";
    final Runnable next=()->{if(running){index+=second==null?1:2;load();}};
    final Runnable refresh=new Runnable(){public void run(){if(!running)return;var source=getContext().getSharedPreferences("photo-sources",0);var p=getContext().getSharedPreferences("ambient",0);long now=System.currentTimeMillis();if(!art()&&AmbientPolicy.refresh(p.getBoolean("refresh",false),!source.getString("url","").isEmpty(),now,source.getLong("last-refresh",0))){int token=generation;Map<String,?> snapshot=source.getAll();worker.submit(()->{try{PhotoLibrary.refresh(getContext(),snapshot,()->running&&token==generation);if(running&&token==generation){source.edit().putLong("last-refresh",System.currentTimeMillis()).apply();ui.post(()->{if(running&&token==generation){notice="";load();}});}}catch(Exception unavailable){ui.post(()->{if(running&&token==generation){notice="Photo refresh unavailable · showing cached photos";invalidate();}});}});}ui.postDelayed(this,30*60_000L);}};
    AmbientSurface(Context c){this(c,null,null);}
    AmbientSurface(Context c,Boolean art){this(c,art,null);}
    AmbientSurface(Context c,Boolean art,String season){super(c);forcedArt=art;forcedSeason=season;setFocusable(true);setContentDescription("DV Freedom photo ambient preview. Left or right changes the photo; Back exits.");}
    boolean art(){return forcedArt!=null?forcedArt:getContext().getSharedPreferences("ambient",0).getBoolean("art",false);}
    void start(){if(running)return;running=true;generation++;worker=Executors.newSingleThreadExecutor();seasonMinute=Long.MIN_VALUE;seasonStarted=SystemClock.uptimeMillis();try{seasonal=SeasonalPolicy.parse(forcedSeason!=null?forcedSeason:forcedArt==null?getContext().getSharedPreferences("ambient",0).getString(SeasonalPolicy.KEY,""):"");}catch(Exception invalid){seasonal=null;}seasonalMode=seasonalActive();if(seasonalMode)invalidate();else{load();ui.post(refresh);}}
    boolean seasonalActive(){long minute=System.currentTimeMillis()/60000;if(minute!=seasonMinute){seasonMinute=minute;seasonalToday=seasonal!=null&&seasonal.active(java.time.LocalDate.now().toEpochDay());}return seasonalToday;}
    void stop(){running=false;loading=false;generation++;ui.removeCallbacksAndMessages(null);if(worker!=null)worker.shutdownNow();recycle();}
    void release(Bitmap b){if(b!=null&&!b.isRecycled())b.recycle();}
    void recycle(){release(first);release(second);release(previousFirst);release(previousSecond);release(fill);first=second=previousFirst=previousSecond=fill=null;}
    File[] files(){return art()?MuseumArt.photos(getContext()):PhotoLibrary.photos(getContext());}
    void load(){if(!running||loading)return;loading=true;int token=generation;File[] files=files();if(files.length==0){loading=false;invalidate();return;}File a=files[Math.floorMod(index,files.length)],b=files[Math.floorMod(index+1,files.length)];boolean pair=getContext().getSharedPreferences("ambient",0).getBoolean("pairs",true);
        worker.submit(()->{Bitmap x=PhotoLibrary.decode(a,1920*1080),y=null,bg=null;try{if(x!=null&&pair&&files.length>1&&AmbientPolicy.portrait(x.getWidth(),x.getHeight())){y=PhotoLibrary.decode(b,1920*1080);if(y!=null&&!AmbientPolicy.portrait(y.getWidth(),y.getHeight())){y.recycle();y=null;}}if(x!=null)bg=Bitmap.createScaledBitmap(x,40,Math.max(1,Math.min(160,40*x.getHeight()/x.getWidth())),true);}catch(RuntimeException bad){release(y);y=null;}final Bitmap one=x,two=y,blur=bg;
            ui.post(()->{if(!running||token!=generation){release(one);release(two);release(blur);return;}loading=false;release(previousFirst);release(previousSecond);release(fill);previousFirst=first;previousSecond=second;first=one;second=two;fill=blur;firstFile=a;secondFile=two==null?null:b;changed=SystemClock.uptimeMillis();invalidate();ui.removeCallbacks(next);ui.postDelayed(next,AmbientPolicy.interval(getContext().getSharedPreferences("ambient",0).getInt("interval",1)));});
        });
    }
    @Override protected void onDraw(Canvas c){super.onDraw(c);
        if(seasonalActive()){boolean moving=seasonal.motion&&android.animation.ValueAnimator.areAnimatorsEnabled();seasonalPainter.draw(c,getWidth(),getHeight(),seasonal,SystemClock.uptimeMillis()-seasonStarted,System.currentTimeMillis(),getContext(),moving,true);String description=seasonal.name()+". "+seasonalPainter.description()+". Back exits.";if(!description.contentEquals(getContentDescription()==null?"":getContentDescription()))setContentDescription(description);if(running)postInvalidateDelayed(moving?67:Math.max(1,60000-System.currentTimeMillis()%60000));return;}
        if(seasonalMode&&running){seasonalMode=false;load();ui.post(refresh);}
        var prefs=getContext().getSharedPreferences("ambient",0);int tone=prefs.getInt("mat",0);int[] colors={0xff101923,0xffe5dfd4,0xff263c43};c.drawColor(colors[Math.max(0,Math.min(2,tone))]);if(first==null){paint.setColor(tone==1?0xff172838:Color.WHITE);paint.setTextSize(28*getResources().getDisplayMetrics().scaledDensity);c.drawText("Add photos or choose museum art in DV Freedom Companions",getWidth()*.06f,getHeight()*.5f,paint);return;}
        if(prefs.getBoolean("fill",true)&&fill!=null){paint.setAlpha(90);c.drawBitmap(fill,null,new Rect(0,0,getWidth(),getHeight()),paint);paint.setAlpha(255);}
        boolean motion=android.provider.Settings.Global.getFloat(getContext().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;
        float progress=motion?Math.min(1,(SystemClock.uptimeMillis()-changed)/600f):1;
        if(previousFirst!=null&&progress<1)drawPair(c,previousFirst,previousSecond,(int)(255*(1-progress)),prefs);
        drawPair(c,first,second,(int)(255*progress),prefs);paint.setAlpha(255);
        String caption=caption(firstFile,prefs);if(secondFile!=null){String other=caption(secondFile,prefs);if(!other.isEmpty())caption+=(caption.isEmpty()?"":"    /    ")+other;}if(!notice.isEmpty())caption=notice;
        if(!caption.isEmpty()){float density=getResources().getDisplayMetrics().scaledDensity;paint.setTextSize(18*density);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));float max=getWidth()*.84f;while(caption.length()>1&&paint.measureText(caption)>max)caption=caption.substring(0,caption.length()-2)+"…";float width=paint.measureText(caption),x=(getWidth()-width)/2,y=getHeight()*.92f;paint.setColor(0xcc101923);c.drawRoundRect(x-18,y-28*density,x+width+18,y+12,10,10,paint);paint.setColor(Color.WHITE);c.drawText(caption,x,y,paint);}
        if(progress<1)postInvalidateOnAnimation();else if(previousFirst!=null){release(previousFirst);release(previousSecond);previousFirst=previousSecond=null;}
    }
    String caption(File file,android.content.SharedPreferences p){if(file==null)return "";JSONObject m=PhotoLibrary.metadata(file);String credit=m.optString("credit");if(!credit.isEmpty())return m.optString("title")+" · "+credit;List<String> a=new ArrayList<>();if(p.getBoolean("dates",false)){String d=m.optString("date");if(d.length()>10)d=d.substring(0,10);if(!d.isEmpty())a.add(d);}if(p.getBoolean("places",false)&&!m.optString("place").isEmpty())a.add(m.optString("place"));if(p.getBoolean("people",false)&&!m.optString("people").isEmpty())a.add(m.optString("people"));return String.join(" · ",a);}
    void drawPair(Canvas c,Bitmap one,Bitmap two,int alpha,android.content.SharedPreferences p){paint.setAlpha(alpha);float inset=p.getBoolean("matte",true)?getHeight()*.065f:getHeight()*.02f,caption=getHeight()*.10f,gap=getWidth()*.025f;if(two==null)drawImage(c,one,inset,inset,getWidth()-inset,getHeight()-caption,alpha);else{drawImage(c,one,inset,inset,getWidth()/2f-gap/2,getHeight()-caption,alpha);drawImage(c,two,getWidth()/2f+gap/2,inset,getWidth()-inset,getHeight()-caption,alpha);}}
    void drawImage(Canvas c,Bitmap b,float l,float t,float r,float bottom,int alpha){if(b==null||b.isRecycled())return;float[] box=AmbientPolicy.fit(b.getWidth(),b.getHeight(),l,t,r,bottom);RectF dest=new RectF(box[0],box[1],box[2],box[3]);paint.setColor(0x66000000);paint.setAlpha(alpha/2);c.drawRect(dest.left+5,dest.top+8,dest.right+5,dest.bottom+8,paint);paint.setAlpha(alpha);c.drawBitmap(b,null,dest,paint);}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(!seasonalActive()&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){if(event.getRepeatCount()==0){index+=key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1;load();}return true;}return super.onKeyDown(key,event);}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
