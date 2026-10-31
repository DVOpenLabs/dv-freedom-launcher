package com.dvopenlabs.freedom.companions;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.graphics.Canvas;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

/** Preview is independent of Apply and of Android's screensaver selection. */
public final class SeasonalSettingsActivity extends PanelActivity {
    private boolean initialized,on,motion=true;private int selected,duration=0;private String before;private long first,last;
    private static final int[] DAYS={7,30,90};
    private SeasonalPolicy.Spec draft(){return new SeasonalPolicy.Spec(SeasonalPolicy.IDS[selected],first,last,motion);}
    @Override void home(){
        var p=getSharedPreferences("ambient",0);
        if(!initialized){initialized=true;first=LocalDate.now().toEpochDay();last=first+6;before=p.getString(SeasonalPolicy.KEY,"");try{SeasonalPolicy.Spec saved=SeasonalPolicy.parse(before);if(saved!=null){on=saved.active(LocalDate.now().toEpochDay());selected=Arrays.asList(SeasonalPolicy.IDS).indexOf(saved.id);motion=saved.motion;if(on){first=saved.first;last=saved.last;long days=last-first+1;duration=days<=7?0:days<=30?1:2;}}}catch(Exception invalid){on=false;}}
        page("Seasonal ambient looks","Original offline scenes with an expiry date. Preview saves nothing. Apply affects this companion’s ambient view; Home wallpaper and Android’s selected screensaver stay unchanged. This is not a guarantee against OLED image retention.",false);
        String status="Off";try{var saved=SeasonalPolicy.parse(before);if(saved!=null)status=(saved.active(LocalDate.now().toEpochDay())?"Active: ":"Expired: ")+saved.name()+" · until "+LocalDate.ofEpochDay(saved.last);}catch(Exception ignored){}page.addView(text("Saved seasonal view · "+status,18));
        button("Seasonal view · "+(on?"On":"Off"),()->{on=!on;if(on&&last<LocalDate.now().toEpochDay()){first=LocalDate.now().toEpochDay();last=first+DAYS[duration]-1;}refresh("Seasonal view");});
        button("Scene · "+SeasonalPolicy.NAMES[selected],()->new AlertDialog.Builder(this).setTitle("Seasonal scene").setItems(SeasonalPolicy.NAMES,(d,n)->{selected=n;refresh("Scene ·");}).setNegativeButton("Cancel",null).show());
        SeasonalPolicy.Spec preview=draft();View illustration=new View(this){final SeasonalPainter painter=new SeasonalPainter();@Override protected void onDraw(Canvas canvas){painter.draw(canvas,getWidth(),getHeight(),preview,0,System.currentTimeMillis(),this.getContext(),false,false);}};
        page.addView(illustration,new LinearLayout.LayoutParams(-1,dp(110)));
        button("Expires · "+LocalDate.ofEpochDay(last)+" · change duration",()->{duration=(duration+1)%DAYS.length;first=LocalDate.now().toEpochDay();last=first+DAYS[duration]-1;refresh("Expires ·");});
        button("Gentle motion & clock drift · "+(motion?"On":"Off"),()->{motion=!motion;refresh("Gentle motion");});
        button("Preview this scene",()->startActivity(new Intent(this,AmbientActivity.class).putExtra("seasonal-preview",draft().encode())));
        button("Apply seasonal choice",()->{if(!before.equals(p.getString(SeasonalPolicy.KEY,""))){Toast.makeText(this,"The saved scene changed. Close and review it again.",Toast.LENGTH_LONG).show();return;}String value=on?draft().encode():"";if(!p.edit().putString(SeasonalPolicy.KEY,value).commit()){Toast.makeText(this,"Could not save the scene",Toast.LENGTH_LONG).show();return;}finish();});
        button("Cancel",this::finish);
    }
    private void refresh(String label){home();page.post(()->{for(int i=0;i<page.getChildCount();i++){View v=page.getChildAt(i);if(v instanceof TextView&&((TextView)v).getText().toString().startsWith(label)){v.requestFocus();break;}}});}
    @Override public void onBackPressed(){finish();}
}
