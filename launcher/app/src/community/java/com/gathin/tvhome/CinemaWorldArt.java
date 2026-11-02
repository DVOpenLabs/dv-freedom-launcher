package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;

/** Original world ornaments and leather/metal book treatments, not redistributed film posters. */
final class CinemaWorldArt extends View {
    final CinemaWorld world;final int number;final boolean gallery;final Paint p=new Paint(3);final Bitmap image;boolean selected;
    CinemaWorldArt(Context c,CinemaWorld w,int n,boolean objects){super(c);world=w;number=n;gallery=objects;image=!objects&&n<w.covers.size()?CinemaAssets.read(c,w.covers.get(n)):null;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;int accent=world.accent();p.setStyle(Paint.Style.FILL);float left=w*.1f,right=w*.9f,top=h*.035f,bottom=h*.94f;
        if(gallery&&!world.galleryArt.isEmpty()){if(selected){p.setShader(new RadialGradient(w/2,h*.5f,w*.48f,new int[]{(accent&0xffffff)|0x20000000,0x00000000},null,Shader.TileMode.CLAMP));c.drawOval(0,0,w,h,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(accent);p.setStrokeWidth(3);c.drawOval(w*.2f,h*.89f,w*.8f,h*.96f,p);p.setStyle(Paint.Style.FILL);}return;}
        if(gallery){p.setShader(new RadialGradient(w/2,h*.42f,w*.5f,new int[]{selected?(accent&0xffffff)|0x48000000:0x17252c34,0x00101822},null,Shader.TileMode.CLAMP));c.drawOval(0,h*.12f,w,h*.92f,p);p.setShader(null);artifact(c,w/2,h*.44f,w*.23f,selected?accent:0xff74818d);p.setColor(0xff26313b);c.drawOval(w*.15f,h*.78f,w*.85f,h*.9f,p);p.setColor(accent);p.setAlpha(selected?210:65);c.drawArc(w*.15f,h*.78f,w*.85f,h*.9f,0,180,false,p);p.setAlpha(255);return;}
        p.setShader(new LinearGradient(left,0,right,h,new int[]{0xff14191c,world.index<4?0xff473725:0xff193447,0xff121a22},null,Shader.TileMode.CLAMP));c.drawRoundRect(left,top,right,bottom,5,5,p);p.setShader(null);
        if(image!=null){p.setAlpha(selected?255:180);c.drawBitmap(image,null,new RectF(left+w*.06f,top+h*.02f,right-w*.035f,bottom-h*.035f),p);p.setAlpha(255);}else{
            p.setColor(accent);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.009f));c.drawRoundRect(left+w*.07f,top+h*.05f,right-w*.07f,bottom-h*.06f,3,3,p);CinemaWorldMotion.drawSymbol(c,p,world.index,w*.54f,h*.33f,w*.14f,accent,number);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create(world.serif()?"serif":"sans-serif-medium",Typeface.NORMAL));p.setTextSize(w*.11f);c.drawText(roman(number+1),w*.54f,h*.16f,p);coverTitle(c,w,h);p.setTextSize(w*.057f);c.drawText(world.index<4?"COLLECTED STORIES":"CINEMA ARCHIVE",w*.54f,h*.84f,p);
        }
        p.setColor(0xff080c10);c.drawRect(left,top,left+w*.045f,bottom,p);p.setColor(accent);p.setStrokeWidth(Math.max(1,w*.012f));c.drawLine(left+w*.055f,top+h*.04f,left+w*.055f,bottom-h*.04f,p);for(float yy:new float[]{.12f,.18f,.78f,.84f})c.drawLine(left,top+h*yy,left+w*.06f,top+h*yy,p);
        if(selected){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.018f));p.setColor(accent);c.drawRoundRect(left-3,top-3,right+3,bottom+3,6,6,p);p.setStyle(Paint.Style.FILL);}
    }
    private void coverTitle(Canvas c,float w,float h){p.setTextSize(w*.085f);String title=CinemaWorld.TITLES[world.index][number];java.util.List<String> lines=new java.util.ArrayList<>();String line="";for(String word:title.split(" ")){String next=line.isEmpty()?word:line+" "+word;if(p.measureText(next)>w*.56f&&!line.isEmpty()){lines.add(line);line=word;}else line=next;}if(!line.isEmpty())lines.add(line);float y=h*.55f;for(int i=0;i<Math.min(4,lines.size());i++){String s=lines.get(i);float old=p.getTextSize();if(p.measureText(s)>w*.56f)p.setTextSize(old*w*.56f/p.measureText(s));c.drawText(s,w*.54f,y+i*h*.064f,p);p.setTextSize(old);}}
    private void artifact(Canvas c,float x,float y,float r,int color){
        if(world.index==6){int[] colors={0xff83c9ff,0xffffda76,0xffff8f91,0xffd1a0ff,0xff91e9b0,0xffffb575};int chosen=selected?colors[number]:color;p.setColor(chosen);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.07f);Path gem=new Path();gem.moveTo(x,y-r);gem.lineTo(x+r*.62f,y-r*.25f);gem.lineTo(x+r*.4f,y+r*.65f);gem.lineTo(x,y+r);gem.lineTo(x-r*.4f,y+r*.65f);gem.lineTo(x-r*.62f,y-r*.25f);gem.close();c.drawPath(gem,p);c.drawLine(x,y-r,x,y+r,p);c.drawLine(x-r*.62f,y-r*.25f,x+r*.62f,y-r*.25f,p);p.setStyle(Paint.Style.FILL);return;}
        if(number==0){CinemaWorldMotion.drawSymbol(c,p,world.index,x,y,r,color,0);return;}c.save();c.translate(x,y);p.setColor(color);p.setStrokeWidth(r*.065f);p.setStyle(Paint.Style.STROKE);
        if(world.index==1&&number==1){c.drawRect(-r,-r*.7f,r,r*.7f,p);c.drawLine(-r*.3f,-r*.7f,-r*.3f,r*.7f,p);c.drawLine(r*.3f,-r*.7f,r*.3f,r*.7f,p);c.drawCircle(r*.45f,-r*.12f,r*.18f,p);}
        else if(world.index==1){c.drawOval(-r*.5f,-r*.2f,r*.5f,r*.9f,p);c.drawArc(-r*.6f,-r*.4f,r*.6f,r*.3f,180,180,false,p);c.drawLine(0,-r*.3f,r*.15f,-r*.7f,p);}
        else if(world.index==2&&number==1){c.drawLine(0,-r,0,r,p);for(int i=0;i<3;i++){float yy=-r*.6f+i*r*.4f;c.drawLine(0,yy,-r*.55f,yy-r*.3f,p);c.drawLine(0,yy,r*.55f,yy-r*.3f,p);}}
        else if(world.index==2){c.drawOval(-r*.7f,-r*.9f,r*.7f,r*.9f,p);c.drawOval(-r*.45f,-r*.65f,r*.45f,r*.65f,p);}
        else if(world.index==3&&number==1){c.drawArc(-r,-r,r*.6f,r,270,180,false,p);c.drawLine(-r*.2f,-r,-r*.2f,r,p);c.drawLine(-r*.8f,0,r*.9f,0,p);}
        else if(world.index==3){for(int i=-1;i<=1;i++){float xx=i*r*.65f;c.drawArc(xx-r*.3f,r*.1f,xx+r*.3f,r*.5f,0,180,false,p);c.drawOval(xx-r*.07f,-r*.55f,xx+r*.07f,0,p);}}
        else if(world.index==4&&number==1){Path jet=new Path();jet.moveTo(-r,0);jet.lineTo(-r*.2f,-r*.2f);jet.lineTo(0,-r);jet.lineTo(r*.2f,-r*.2f);jet.lineTo(r,0);jet.lineTo(r*.2f,r*.2f);jet.lineTo(0,r*.8f);jet.lineTo(-r*.2f,r*.2f);jet.close();c.drawPath(jet,p);}
        else if(world.index==4){c.drawRoundRect(-r*.5f,-r*.8f,r*.5f,r*.8f,r*.15f,r*.15f,p);c.drawLine(-r*.25f,0,r*.25f,0,p);c.drawLine(0,-r*.3f,0,r*.3f,p);}
        else if(world.index==5&&number==1){for(float rr:new float[]{1,.72f,.38f})c.drawCircle(0,0,r*rr,p);}
        else if(world.index==5){for(int i=0;i<3;i++){float xx=(i-1)*r*.6f;c.drawRect(xx-r*.2f,-r*(i==1?1:.55f),xx+r*.2f,r*.8f,p);}}
        else if(world.index==7&&number==1){c.drawOval(-r*.65f,-r,r*.65f,r,p);c.drawOval(-r*.4f,-r*.75f,r*.4f,r*.75f,p);c.drawLine(-r*.8f,r,r*.8f,r,p);}
        else{c.drawCircle(0,0,r,p);for(int i=0;i<6;i++){c.save();c.rotate(i*60);c.drawLine(0,-r*.5f,0,-r*.9f,p);c.restore();}}
        p.setStyle(Paint.Style.FILL);c.restore();
    }
    private static String roman(int n){return new String[]{"I","II","III","IV","V","VI","VII","VIII"}[Math.min(7,n-1)];}
    static Drawable logo(Context c,String pkg,String label,CinemaWorld w){Bitmap exact=CinemaAssets.read(c,CinemaStore.read(c).appLogos.get(pkg));if(exact!=null){Drawable d=new CinemaAssets.Logo(exact);d.setColorFilter(new PorterDuffColorFilter(w.accent(),PorterDuff.Mode.SRC_IN));return d;}return new CinemaAssets.Emblem(pkg,label,w.accent(),w.serif());}
    static Drawable focus(int accent){GradientDrawable border=new GradientDrawable();border.setColor(0x16101822);border.setCornerRadius(12);border.setStroke(3,accent);return border;}
}
