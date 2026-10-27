package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Optional, profile-scoped text sizes. Preview is local to the dialog until Apply. */
final class HomeAppearance {
    static final String KEY="home_appearance_v1";
    static final int[] SIZES={85,100,110,120};
    static int[] parse(String raw){
        if(raw==null||raw.isEmpty())return new int[]{100,100,100};
        String[] parts=raw.split(",",-1);if(parts.length!=4||!parts[0].equals("v1"))throw new IllegalArgumentException("Invalid text sizes");
        int[] values=new int[3];
        for(int i=0;i<3;i++){values[i]=Integer.parseInt(parts[i+1]);if(values[i]!=85&&values[i]!=100&&values[i]!=110&&values[i]!=120)throw new IllegalArgumentException("Invalid text size");}
        return values;
    }
    static String encode(int[] values){String raw="v1,"+values[0]+","+values[1]+","+values[2];parse(raw);return raw;}
    private static int[] current(Context c){try{return parse(ProfileStore.preferences(c).getString(KEY,""));}catch(RuntimeException invalid){return new int[]{100,100,100};}}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static void size(TextView view,int percent){
        if(view==null)return;Object tag=view.getTag(R.id.studioBaseTextSize);float base=tag instanceof Float?(Float)tag:view.getTextSize();
        view.setTag(R.id.studioBaseTextSize,base);view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,base*percent/100f);
    }
    static void name(TextView view){if(ProfileStore.preferences(view.getContext()).contains(KEY))size(view,current(view.getContext())[2]);}
    private static void header(View view,int percent){
        if(view==null)return;if(view instanceof TextView&&view.getId()!=R.id.clock)size((TextView)view,percent);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)header(((ViewGroup)view).getChildAt(i),percent);
    }
    static void apply(Activity a){
        if(!ProfileStore.preferences(a).contains(KEY))return;int[] v=current(a);
        View clock=a.findViewById(R.id.clock);if(clock instanceof TextView)size((TextView)clock,v[0]);header(a.findViewById(R.id.leftBlock),v[1]);
    }
    static void show(Activity a){
        String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();int[] draft=current(a);
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(a,24),dp(a,8),dp(a,24),dp(a,12));
        TextView clock=new TextView(a),header=new TextView(a),names=new TextView(a);TextView[] sample={clock,header,names};
        String[] labels={"Clock size","Header size","App name size"};float[] sizes={38,18,16};
        clock.setText("6:42 PM");header.setText("Tuesday 27 October   68°F / 20°C");names.setText("YouTube       Netflix       Crunchyroll");
        TextView template=a.findViewById(R.id.categoryLayout);Typeface font=template==null?Typeface.DEFAULT:template.getTypeface();
        for(TextView view:sample){view.setTypeface(font);view.setTextColor(0xfff3f4f6);view.setPadding(0,dp(a,5),0,dp(a,5));content.addView(view);}
        TextView[] buttons=new TextView[3];Runnable refresh=()->{for(int i=0;i<3;i++){sample[i].setTextSize(sizes[i]*draft[i]/100f);if(buttons[i]!=null)buttons[i].setText(labels[i]+"  ·  "+draft[i]+"%");}};
        for(int i=0;i<3;i++){final int n=i;TextView button=new TextView(a);buttons[i]=button;button.setTextSize(17);button.setTypeface(font);button.setTextColor(-1);button.setGravity(Gravity.CENTER_VERTICAL);button.setFocusable(true);button.setClickable(true);button.setBackgroundResource(R.drawable.settings_choice_bg);button.setPadding(dp(a,16),0,dp(a,16),0);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(a,48));p.topMargin=dp(a,6);content.addView(button,p);
            button.setOnClickListener(v->{String[] options={"85%","100%","110%","120%"};int selected=0;for(int k=0;k<SIZES.length;k++)if(SIZES[k]==draft[n])selected=k;
                new AlertDialog.Builder(a).setTitle(labels[n]).setSingleChoiceItems(options,selected,(d,which)->{draft[n]=SIZES[which];refresh.run();d.dismiss();}).setNegativeButton(android.R.string.cancel,null).show();});}
        refresh.run();AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Text sizes · preview").setView(content).setNegativeButton(android.R.string.cancel,null).setNeutralButton("Theme sizes",(d,w)->save(a,profile,before,new int[]{100,100,100})).setPositiveButton("Apply",(d,w)->save(a,profile,before,draft)).create();
        dialog.setOnShowListener(d->buttons[0].requestFocus());dialog.show();
    }
    private static void save(Activity a,String profile,Map<String,?> before,int[] values){
        if(!profile.equals(ProfileStore.id())||!before.equals(ProfileStore.preferences(a).getAll())){Toast.makeText(a,"Home changed. Open Text sizes again.",Toast.LENGTH_LONG).show();return;}
        String raw=Arrays.equals(values,new int[]{100,100,100})?null:encode(values);SharedPreferences p=ProfileStore.preferences(a);if(Objects.equals(raw,p.getString(KEY,null)))return;
        ProductFeatures.beforeChange(a,"Change text sizes");Map<String,Object> changes=new HashMap<>();changes.put(KEY,raw);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
        if(SetupValues.commit(p,changes))a.finish();else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_LONG).show();
    }
}
