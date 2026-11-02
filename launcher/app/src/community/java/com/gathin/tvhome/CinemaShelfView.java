package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.function.IntConsumer;

/** Original leather spines turn into the owner's local covers. Animates only while selection settles. */
final class CinemaShelfView extends View {
    private final CinemaWorld world;
    private final Paint p=new Paint(3);
    private final Bitmap[] covers;
    private final float[] opened;
    private final boolean motion;
    private final IntConsumer changed;
    private final Runnable action,down,up;
    private int selected;
    private final Matrix projection=new Matrix();
    private final Runnable frame=this::invalidate;
    CinemaShelfView(Context c,CinemaWorld w,int initial,boolean animate,IntConsumer select,Runnable activate,Runnable next,Runnable previous){
        super(c);world=w;motion=animate;changed=select;action=activate;down=next;up=previous;int count=CinemaWorld.TITLES[w.index].length;selected=Math.max(0,Math.min(count-1,initial));covers=new Bitmap[count];opened=new float[count];opened[selected]=1;
        for(int i=0;i<count&&i<w.covers.size();i++)covers[i]=CinemaAssets.read(c,w.covers.get(i));setFocusable(true);setClickable(true);setOnClickListener(v->action.run());setContentDescription(CinemaWorld.TITLES[w.index][selected]);
        setOnKeyListener((v,key,e)->{if(e.getAction()!=android.view.KeyEvent.ACTION_DOWN)return false;if(key==android.view.KeyEvent.KEYCODE_DPAD_LEFT||key==android.view.KeyEvent.KEYCODE_DPAD_RIGHT){selected=Math.max(0,Math.min(count-1,selected+(key==android.view.KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));setContentDescription(CinemaWorld.TITLES[w.index][selected]);changed.accept(selected);invalidate();return true;}if(key==android.view.KeyEvent.KEYCODE_DPAD_DOWN){down.run();return true;}if(key==android.view.KeyEvent.KEYCODE_DPAD_UP){up.run();return true;}return false;});
    }
    protected void onDraw(Canvas canvas){
        float scale=getWidth()/960f;canvas.save();canvas.scale(scale,scale);boolean settling=false;float total=0;
        for(int i=0;i<opened.length;i++){float goal=i==selected?1:0;opened[i]=motion?opened[i]+(goal-opened[i])*.24f:goal;if(Math.abs(goal-opened[i])<.003f)opened[i]=goal;else settling=true;total+=24+80*opened[i];}
        float x=(960-total)/2,base=345,height=145;int[] leather={0xff713a31,0xff244b44,0xff35456c,0xff66517a,0xff4b6244,0xff574b70,0xff725b3b,0xff384c57};
        for(int i=0;i<opened.length;i++){
            float width=24+80*opened[i],f=opened[i],top=base-height-3*f;
            p.setColor(0x80000000);canvas.drawOval(x-4,base-2,x+width+8+8*f,base+7+2*f,p);
            p.setShader(new LinearGradient(x,0,x+width,0,new int[]{0xff101417,leather[i%leather.length],0xff171c1b},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(x,top,x+width-2,base,2,2,p);p.setShader(null);
            if(f>.02f){
                float edge=Math.max(5,20*(1-f)),hinge=x+edge,right=x+width-4,skew=11*(float)Math.sin(f*Math.PI);
                // Project the front around its binding; the outer edge recedes while the book turns.
                projection.reset();projection.setPolyToPoly(new float[]{0,0,100,0,100,145,0,145},0,new float[]{hinge,top+2,right,top+2+skew,right,base-2-skew,hinge,base-2},0,4);
                canvas.save();canvas.concat(projection);
                if(covers[i]!=null){p.setAlpha(255);canvas.drawBitmap(covers[i],null,new RectF(0,0,100,145),p);}
                else{p.setColor(world.accent());p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(10);String[] words=CinemaWorld.TITLES[world.index][i].split(" ");for(int j=0;j<words.length&&j<6;j++)canvas.drawText(words[j],50,43+j*13,p);}
                p.setShader(new LinearGradient(0,0,100,0,new int[]{0xa0000000,0x00000000,0x24fff0ca,0x44000000},new float[]{0,.12f,.76f,1},Shader.TileMode.CLAMP));canvas.drawRect(0,0,100,145,p);p.setShader(null);
                p.setColor(0xffd0bd91);p.setAlpha((int)(95*f));p.setStrokeWidth(.7f);for(int line=0;line<3;line++)canvas.drawLine(97+line,3,97+line,142,p);p.setAlpha(255);
                canvas.restore();
                p.setColor(0xff100e0c);p.setStrokeWidth(1.2f);canvas.drawLine(hinge,top+2,hinge,base-2,p);
            }
            if(f<.95f){canvas.save();canvas.translate(x+width/2,top+height/2);canvas.rotate(-90);p.setColor(0xffe8d6a1);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(9);String title=CinemaWorld.TITLES[world.index][i];float measured=p.measureText(title);if(measured>height-23)p.setTextSize(9*(height-23)/measured);p.setAlpha((int)(255*(1-f)));canvas.drawText(title,0,3,p);p.setAlpha(255);canvas.restore();}
            p.setColor(world.accent());p.setAlpha(i==selected?230:115);p.setStrokeWidth(i==selected?1.5f:.6f);p.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(x+1,top+1,x+width-2,base-1,2,2,p);for(float y:new float[]{top+12,top+18,base-16,base-10})canvas.drawLine(x+2,y,x+Math.min(width-4,20),y,p);p.setStyle(Paint.Style.FILL);p.setAlpha(255);x+=width;
        }
        canvas.restore();removeCallbacks(frame);if(settling&&isShown())postDelayed(frame,32);
    }
    protected void onDetachedFromWindow(){removeCallbacks(frame);super.onDetachedFromWindow();}
}
