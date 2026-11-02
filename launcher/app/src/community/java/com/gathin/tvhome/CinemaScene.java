package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original procedural atmosphere, not a video or an executable theme. Only ticks while visible. */
final class CinemaScene extends View {
    private final Paint p=new Paint(3);private final String scene;private final boolean mascot,motion;private boolean running;private long start;private float target=.5f,pos=.5f;private int accent=0xff75cfff;
    CinemaScene(Context c,String scene,boolean mascot,boolean motion){super(c);this.scene=scene;this.mascot=mascot;this.motion=motion;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setFocusable(false);setClickable(false);}
    void focus(float x,int colour){target=Math.max(.08f,Math.min(.92f,x));accent=colour;invalidate();}
    void start(){running=true;start=android.os.SystemClock.uptimeMillis();invalidate();}
    void stop(){running=false;}
    protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();if(w==0||h==0)return;float t=motion?(android.os.SystemClock.uptimeMillis()-start)/1000f:0;pos=motion?pos+(target-pos)*.12f:target;
        if(scene.equals("aurora")){for(int i=0;i<3;i++){float y=h*(.22f+i*.1f);Path path=new Path();path.moveTo(-w*.1f,y);for(int k=0;k<=12;k++){float x=w*k/12;path.lineTo(x,y+(float)Math.sin(k*.55+t*.22+i)*h*.045f);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.07f);p.setColor(i%2==0?0x1675cfff:0x139e83ff);c.drawPath(path,p);}p.setStyle(Paint.Style.FILL);}
        if(scene.equals("fireflies")){for(int i=0;i<22;i++){float x=(float)((i*.173+Math.sin(t*.13+i)*.035)%1)*w;if(x<0)x+=w;float y=(.3f+(i*37%60)/100f+(float)Math.sin(t*.2+i)*.025f)*h;p.setColor(Color.argb(45+(int)(35*(1+Math.sin(t+i))),190,230,160));c.drawCircle(x,y,h*(.0018f+i%3*.0005f),p);}}
        if(scene.equals("mist")){for(int i=0;i<4;i++){float x=(float)((i*.31+t*.007)%1.5-.25)*w,y=h*(.68f+i*.065f);p.setShader(new RadialGradient(x,y,w*.3f,new int[]{0x1bbed9ee,0x00bed9ee},null,Shader.TileMode.CLAMP));c.drawOval(x-w*.3f,y-h*.08f,x+w*.3f,y+h*.08f,p);p.setShader(null);}}
        if(mascot){float x=w*pos,y=h*.72f+(float)Math.sin(t*1.4)*h*.006f;Path star=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=h*(i%2==0?.015f:.007f),xx=x+(float)Math.cos(angle)*r,yy=y+(float)Math.sin(angle)*r;if(i==0)star.moveTo(xx,yy);else star.lineTo(xx,yy);}star.close();p.setColor(accent);c.drawPath(star,p);p.setColor(0xff142032);c.drawCircle(x-h*.003f,y-h*.001f,h*.0015f,p);c.drawCircle(x+h*.003f,y-h*.001f,h*.0015f,p);}
        if(running&&motion&&getWindowVisibility()==VISIBLE&&isShown())postInvalidateDelayed(33);
    }
}
