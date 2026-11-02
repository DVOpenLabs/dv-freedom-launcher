package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
/** A bounded, semantic D-pad mood card. Decorative posters never become separate focus targets. */
final class CinemaMoodCard extends View {
    interface Listener{void choose(String mood);}
    private final CinemaSpec spec;private final Set<String> available;private final Listener listener;private final List<String> moods;private final Paint p=new Paint(3);private int index;private final Bitmap[] posters=new Bitmap[3];
    CinemaMoodCard(Context c,CinemaSpec s,Set<String> apps,Listener callback){super(c);spec=s;available=apps;listener=callback;moods=new ArrayList<>(s.moods);moods.add("Anything");setFocusable(true);setClickable(true);setOnClickListener(v->listener.choose(moods.get(index)));setOnFocusChangeListener((v,on)->invalidate());load();}
    private void load(){List<CinemaSpec.Title> titles=spec.pool(moods.get(index),available,null);for(int i=0;i<3;i++)posters[i]=i<titles.size()?CinemaAssets.read(getContext(),titles.get(i).poster):null;setContentDescription(moods.get(index)+", "+titles.size()+" titles. Left skips mood; OK or Right picks a title.");invalidate();}
    protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cw=Math.min(w*.6f,h*1.4f),ch=h*.87f,x=(w-cw)/2,y=h*.04f;for(int i=2;i>=0;i--){c.save();c.rotate(i==2?-5:i==1?4:0,w/2,h/2);p.setColor(i==2?0xff283735:i==1?0xff31454d:0xff17343f);c.drawRoundRect(x+i*5,y+i*5,x+cw+i*5,y+ch+i*5,18,18,p);c.restore();}List<CinemaSpec.Title> titles=spec.pool(moods.get(index),available,null);float pw=cw*.22f,ph=ch*.42f;
        for(int i=0;i<Math.min(3,titles.size());i++){float cx=w/2+(i-1)*pw*.85f,py=y+ch*.08f;c.save();c.rotate((i-1)*8,cx,py+ph/2);RectF dest=new RectF(cx-pw/2,py,cx+pw/2,py+ph);p.setColor(Color.parseColor(titles.get(i).accent));c.drawRoundRect(dest,6,6,p);if(posters[i]!=null)c.drawBitmap(posters[i],null,dest,p);else{p.setColor(0xff10202a);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(ph*.22f);c.drawText(titles.get(i).name.substring(0,1),cx,py+ph*.6f,p);}c.restore();}
        p.setColor(0xfff1f7fb);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));String name=moods.get(index);p.setTextSize(Math.min(ch*.12f,cw*.75f/Math.max(5,name.length())*1.5f));c.drawText(name,w/2,y+ch*.73f,p);p.setColor(0xff99bfbf);p.setTextSize(ch*.075f);c.drawText(titles.size()+" titles",w/2,y+ch*.86f,p);p.setTextSize(ch*.065f);c.drawText("‹ Skip",x-cw*.21f,y+ch*.55f,p);c.drawText("Pick ›",x+cw*1.21f,y+ch*.55f,p);if(isFocused()){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xffdcebb8);c.drawRoundRect(x,y,x+cw,y+ch,18,18,p);p.setStyle(Paint.Style.FILL);}}
    public boolean onKeyDown(int key,KeyEvent e){if(e.getRepeatCount()>0&&(key==21||key==22||key==23||key==66))return true;if(key==21){index=(index+1)%moods.size();load();return true;}if(key==22||key==23||key==66){listener.choose(moods.get(index));return true;}return super.onKeyDown(key,e);}
}
