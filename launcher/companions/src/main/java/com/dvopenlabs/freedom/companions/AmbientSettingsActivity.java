package com.dvopenlabs.freedom.companions;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.TextView;

/** Native Apply/Cancel draft. Preview and system selection are independent actions. */
public final class AmbientSettingsActivity extends PanelActivity {
    @Override void home(){var p=getSharedPreferences("ambient",0);page("Photo & Art appearance","Preview uses your saved choices. Photos stay in this companion; date/place/people captions start off. No system screensaver or launcher wallpaper changes here.",false);boolean[] values={p.getBoolean("art",false),p.getBoolean("pairs",true),p.getBoolean("matte",true),p.getBoolean("fill",true),p.getBoolean("dates",false),p.getBoolean("places",false),p.getBoolean("people",false),p.getBoolean("refresh",false)};
        String[] keys={"art","pairs","matte","fill","dates","places","people","refresh"},labels={"Museum art instead of personal photos","Pair portrait photos","Art mat around images","Soft background fill","Show dates","Show places","Show people’s names","Refresh personal source every 30 minutes while ambient is visible"};
        for(int i=0;i<labels.length;i++){final int n=i;TextView b=button(labels[i]+" · "+(values[i]?"On":"Off"),()->{});b.setOnClickListener(v->{values[n]=!values[n];b.setText(labels[n]+" · "+(values[n]?"On":"Off"));});}
        int[] interval={Math.max(0,Math.min(3,p.getInt("interval",1)))},mat={Math.max(0,Math.min(2,p.getInt("mat",0)))};String[] times={"15 seconds","30 seconds","1 minute","5 minutes"},mats={"Midnight","Warm paper","Deep teal"};TextView time=button("Change image · "+times[interval[0]],()->{});time.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Change image").setItems(times,(d,n)->{interval[0]=n;time.setText("Change image · "+times[n]);}).setNegativeButton("Cancel",null).show());TextView color=button("Mat colour · "+mats[mat[0]],()->{});color.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Mat colour").setItems(mats,(d,n)->{mat[0]=n;color.setText("Mat colour · "+mats[n]);}).setNegativeButton("Cancel",null).show());
        button("Apply",()->{var e=p.edit();for(int i=0;i<keys.length;i++)e.putBoolean(keys[i],values[i]);e.putInt("interval",interval[0]).putInt("mat",mat[0]).apply();finish();});button("Cancel",this::finish);button("Preview saved appearance",()->startActivity(new Intent(this,AmbientActivity.class).putExtra("art-preview",p.getBoolean("art",false))));button("Open system screensaver settings",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_DREAM_SETTINGS));}catch(RuntimeException unavailable){android.widget.Toast.makeText(this,"Open your TV’s screensaver settings using the remote",android.widget.Toast.LENGTH_LONG).show();}});
    }
    @Override public void onBackPressed(){finish();}
}
