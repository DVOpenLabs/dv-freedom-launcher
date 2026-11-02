package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** One bounded Canvas layer, no decoder. The supplied optional owl atlases stay private. */
final class CinemaWorldMotion extends View {
    final CinemaWorld world;final Paint p=new Paint(3);final boolean motion;final Bitmap idle,hop;
    float x=.5f,from=.5f,target=.5f,top=.72f,companionScale=1;long moved,start=android.os.SystemClock.uptimeMillis();boolean running,companion=true;
    CinemaWorldMotion(Context c,CinemaWorld w,boolean animate){super(c);world=w;motion=animate;idle=CinemaAssets.read(c,w.idleArt);hop=CinemaAssets.read(c,w.hopArt);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setFocusable(false);setClickable(false);}
    void focus(float position,float y){long now=android.os.SystemClock.uptimeMillis();from=x;target=Math.max(.03f,Math.min(.97f,position));top=y;if(Math.abs(target-x)>.003f)moved=now;invalidate();}
    void start(){running=true;removeCallbacks(tick);invalidate();}void stop(){running=false;removeCallbacks(tick);}private final Runnable tick=this::invalidate;
    protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();if(w<1||h<1)return;float t=motion?(android.os.SystemClock.uptimeMillis()-start)/1000f:0;float progress=motion?Math.min(1,(android.os.SystemClock.uptimeMillis()-moved)/850f):1;float ease=progress*progress*(3-2*progress);x=from+(target-from)*ease;
        p.setStyle(Paint.Style.FILL);int accent=world.accent();
        for(int i=0;i<20;i++){float xx=((i*73%997)/997f+(float)Math.sin(t*.13+i)*.012f)*w,yy=(.18f+(i*43%57)/100f)*h;int a=motion?30+(int)(24*(1+Math.sin(t*.65+i))):38;p.setColor((accent&0xffffff)|(a<<24));float r=h*(i%3==0?.0014f:.0007f);c.drawCircle(xx,yy,r,p);}
        if(world.index<4||world.index==7){for(int i=0;i<3;i++){float xx=(float)((i*.43+t*.006)%1.5-.25)*w,yy=h*(.60f+i*.075f);p.setShader(new RadialGradient(xx,yy,w*.27f,new int[]{0x16b8c8d8,0x00b8c8d8},null,Shader.TileMode.CLAMP));c.drawOval(xx-w*.27f,yy-h*.06f,xx+w*.27f,yy+h*.06f,p);p.setShader(null);}}
        if(companion){float foot=h*top-h*.008f;float lift=motion?(float)Math.sin(progress*Math.PI)*h*.065f:0;
        Bitmap sheet=progress<1&&hop!=null?hop:idle;
        if(world.index==0&&sheet!=null){boolean flying=sheet==hop&&progress<1;int cols=flying?6:12,rows=flying?4:2;int frame=motion?(flying?Math.min(23,(int)(progress*24)):(int)(t*12)%24):0;int cw=sheet.getWidth()/cols,ch=sheet.getHeight()/rows;Rect src=new Rect(frame%cols*cw,frame/cols*ch,frame%cols*cw+cw,frame/cols*ch+ch);float ih=h*(flying?.14f:.11f)*companionScale,iw=ih*cw/ch;float center=x*w;c.save();if(flying&&target<from)c.scale(-1,1,center,foot);p.setColor(-1);p.setAlpha(255);c.drawBitmap(sheet,src,new RectF(center-iw/2,foot-ih-lift,center+iw/2,foot-lift),p);c.restore();}
        else{float size=h*.026f,cy=foot-size-lift+(float)Math.sin(t*1.5)*h*.002f;drawSymbol(c,p,world.index,x*w,cy,size,accent,t);}
        }removeCallbacks(tick);if(running&&motion&&isShown())postDelayed(tick,50);
    }
    static void drawSymbol(Canvas c,Paint p,int style,float x,float y,float r,int color,float t){
        c.save();c.translate(x,y);p.setColor(color);p.setAlpha(255);p.setStrokeWidth(Math.max(1,r*.075f));p.setStyle(Paint.Style.STROKE);
        if(style==0||style==2){Path wing=new Path();wing.moveTo(-r,0);wing.quadTo(-r*1.5f,-r*.8f,-r*.18f,-r*.3f);wing.lineTo(0,r*.3f);wing.lineTo(r*.18f,-r*.3f);wing.quadTo(r*1.5f,-r*.8f,r,0);c.drawPath(wing,p);c.drawOval(-r*.2f,-r*.5f,r*.2f,r*.4f,p);}
        else if(style==1){c.drawCircle(0,0,r*.65f,p);c.drawArc(-r,-r,r,r,30,300,false,p);c.drawLine(0,-r*.65f,0,r*.65f,p);c.drawCircle(r*.3f,0,r*.06f,p);}
        else if(style==3){for(int i=-2;i<=2;i++){c.save();c.rotate(i*28);c.drawOval(-r*.22f,-r,r*.22f,r*.2f,p);c.restore();}c.drawArc(-r,-r*.2f,r,r,0,180,false,p);}
        else if(style==4){c.drawCircle(0,0,r*.8f,p);c.save();c.rotate(t*14);for(int i=0;i<3;i++){c.rotate(120);c.drawLine(0,-r*.25f,0,-r*.8f,p);}c.restore();c.drawCircle(0,0,r*.25f,p);}
        else if(style==5){c.drawCircle(0,0,r*.8f,p);Path arrow=new Path();arrow.moveTo(-r*.45f,r*.5f);arrow.lineTo(0,-r*.6f);arrow.lineTo(r*.45f,r*.5f);arrow.moveTo(-r*.2f,r*.12f);arrow.lineTo(r*.5f,r*.12f);c.drawPath(arrow,p);}
        else if(style==6){for(int i=0;i<6;i++){double a=t*.12+i*Math.PI/3;c.drawCircle((float)Math.cos(a)*r*.65f,(float)Math.sin(a)*r*.65f,r*.15f,p);}c.drawCircle(0,0,r*.2f,p);}
        else{Path rune=new Path();for(int i=0;i<7;i++){double a=-Math.PI/2+i*Math.PI/3;float xx=(float)Math.cos(a)*r,yy=(float)Math.sin(a)*r;if(i==0)rune.moveTo(xx,yy);else rune.lineTo(xx,yy);}c.drawPath(rune,p);c.drawLine(-r*.25f,-r*.5f,-r*.25f,r*.45f,p);c.drawLine(r*.25f,-r*.5f,r*.25f,r*.45f,p);}
        p.setStyle(Paint.Style.FILL);c.restore();
    }
}
