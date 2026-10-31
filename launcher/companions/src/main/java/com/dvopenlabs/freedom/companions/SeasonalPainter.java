package com.dvopenlabs.freedom.companions;

import android.content.Context;
import android.graphics.*;
import android.text.format.DateFormat;
import java.time.LocalDate;
import java.util.*;

/** Original procedural scenes: one shader, 18 small motifs, a slowly drifting clock. No media assets. */
final class SeasonalPainter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path leaf=new Path(),wave=new Path();
    private Shader backdrop;private String shaderId="";private long minute=-1;private String clock="",date="",badge="",badgeKey="";private Typeface font;
    private static final int[][] COLORS={{0xff261a23,0xff10242e,0xffd69d64},{0xff101a30,0xff153344,0xffa3d9e7},{0xff231d35,0xff18333a,0xffdcb2ce},{0xff102b39,0xff183e44,0xff83d1c4}};
    SeasonalPainter(){leaf.moveTo(0,-12);leaf.cubicTo(12,-9,13,5,0,13);leaf.cubicTo(-13,5,-12,-9,0,-12);leaf.close();}
    String description(){return clock+". "+date;}
    void draw(Canvas canvas,int width,int height,SeasonalPolicy.Spec spec,long elapsed,long now,Context context,boolean motion,boolean showClock){
        int at=Arrays.asList(SeasonalPolicy.IDS).indexOf(spec.id);int[] colors=COLORS[at];
        if(!shaderId.equals(spec.id)){shaderId=spec.id;backdrop=new LinearGradient(0,0,1280,720,colors[0],colors[1],Shader.TileMode.CLAMP);}
        int saved=canvas.save();canvas.scale(width/1280f,height/720f);paint.setShader(backdrop);canvas.drawRect(0,0,1280,720,paint);paint.setShader(null);
        float seconds=motion?elapsed/1000f:0;
        paint.setColor(colors[2]);paint.setAlpha(42);
        if(at==3){
            for(int band=0;band<3;band++){wave.reset();wave.moveTo(0,570+band*35);for(int x=0;x<=1280;x+=32)wave.lineTo(x,570+band*35+(float)Math.sin(x/240f+seconds/25+band)*16);wave.lineTo(1280,720);wave.lineTo(0,720);wave.close();canvas.drawPath(wave,paint);}
        }else for(int i=0;i<18;i++){
            float x=(i*277f+seconds*(at==1?2:4))%1400-60,y=(i*149f+seconds*(at==1?5:2))%840-60;
            int n=canvas.save();canvas.translate(x,y);canvas.rotate(i*31+seconds/4);
            if(at==1)canvas.drawCircle(0,0,2+i%3,paint);else{canvas.scale(at==0?1:.7f,at==0?1:.7f);canvas.drawPath(leaf,paint);}canvas.restoreToCount(n);
        }
        paint.setAlpha(255);
        if(showClock){
            if(now/60000!=minute){minute=now/60000;Date time=new Date(now);clock=DateFormat.getTimeFormat(context).format(time);date=DateFormat.getLongDateFormat(context).format(time);}
            float driftX=motion?(float)Math.sin(seconds/130)*70:0,driftY=motion?(float)Math.cos(seconds/170)*24:0;
            if(font==null)font=context.getResources().getFont(R.font.outfit);paint.setTypeface(font);paint.setColor(0xfff3f6fc);paint.setTextAlign(Paint.Align.CENTER);paint.setShadowLayer(3,0,1,0xff101923);
            paint.setTextSize(76);canvas.drawText(clock,640+driftX,300+driftY,paint);paint.setTextSize(25);canvas.drawText(date,640+driftX,351+driftY,paint);
            paint.setColor(0xffd0dce7);paint.setTextSize(22);String key=spec.id+spec.last;if(!key.equals(badgeKey)){badgeKey=key;badge=spec.name()+" · until "+LocalDate.ofEpochDay(spec.last);}canvas.drawText(badge,640+driftX,401+driftY,paint);paint.clearShadowLayer();paint.setTextAlign(Paint.Align.LEFT);
        }
        canvas.restoreToCount(saved);
    }
}
