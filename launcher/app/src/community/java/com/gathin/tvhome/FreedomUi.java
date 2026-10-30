package com.gathin.tvhome;

import android.app.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Shared native sheet typography, focus and scroll behavior. Does not select a Home theme. */
final class FreedomUi {
    static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static TextView text(Activity a,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(0xfff5f7fa);t.setTypeface(SettingsTypography.regular(a));return t;}
    static LinearLayout body(Activity a){LinearLayout b=new LinearLayout(a);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(a,24),dp(a,16),dp(a,24),dp(a,16));return b;}
    static TextView action(Activity a,LinearLayout b,String label,Runnable click){
        TextView t=text(a,label,18);t.setPadding(dp(a,16),dp(a,12),dp(a,16),dp(a,12));t.setMinHeight(dp(a,48));t.setFocusable(true);t.setClickable(true);t.setBackgroundResource(R.drawable.freedom_settings_row);
        t.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[0]},new int[]{0xfff5f7fa,0xfff5f7fa}));
        t.setOnClickListener(v->click.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(a,8);b.addView(t,p);return t;
    }
    static AlertDialog sheet(Activity a,String title,LinearLayout body){
        LinearLayout root=body(a);root.setBackgroundColor(0xff111c2b);root.addView(text(a,title,26));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(a);root.addView(footer);AlertDialog dialog=new AlertDialog.Builder(a).setView(root).create();TextView close=action(a,footer,a.getString(android.R.string.cancel),dialog::dismiss);
        dialog.setOnShowListener(v->{bound(a,dialog);root.post(()->{if(!first(body))close.requestFocusFromTouch();});});dialog.show();return dialog;
    }
    static AlertDialog checklist(Activity a,String title,String[] labels,boolean[] checked,java.util.function.Consumer<AlertDialog> apply){
        LinearLayout root=body(a);root.setBackgroundColor(0xff192534);root.addView(text(a,title,26));
        ScrollView scroll=new ScrollView(a);LinearLayout choices=new LinearLayout(a);choices.setOrientation(LinearLayout.VERTICAL);scroll.addView(choices);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<labels.length;i++){final int at=i;CheckBox box=new CheckBox(a);box.setText(labels[i]);box.setTextColor(0xfff5f7fa);box.setTextSize(18);box.setTypeface(SettingsTypography.regular(a));box.setMinHeight(dp(a,52));box.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));box.setBackgroundResource(R.drawable.freedom_settings_row);box.setChecked(checked[i]);box.setOnCheckedChangeListener((v,on)->checked[at]=on);choices.addView(box,new LinearLayout.LayoutParams(-1,-2));}
        LinearLayout footer=new LinearLayout(a);LinearLayout cancel=new LinearLayout(a),save=new LinearLayout(a);footer.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));footer.addView(save,new LinearLayout.LayoutParams(0,-2,1));root.addView(footer);
        AlertDialog dialog=new AlertDialog.Builder(a).setView(root).create();action(a,cancel,a.getString(android.R.string.cancel),dialog::dismiss);action(a,save,"Apply",()->apply.accept(dialog));
        dialog.setOnShowListener(v->{bound(a,dialog);choices.post(()->{if(choices.getChildCount()>0)choices.getChildAt(0).requestFocusFromTouch();});});dialog.show();return dialog;
    }
    static void bound(Activity a,AlertDialog d){if(d.getWindow()!=null)d.getWindow().setLayout(Math.min(dp(a,720),a.getResources().getDisplayMetrics().widthPixels-dp(a,64)),Math.min(dp(a,460),a.getResources().getDisplayMetrics().heightPixels-dp(a,64)));}
    static boolean first(View v){if(v.isFocusable()&&v.isClickable())return v.isInTouchMode()?v.requestFocusFromTouch():v.requestFocus();if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)if(first(((android.view.ViewGroup)v).getChildAt(i)))return true;return false;}
    private FreedomUi(){}
}
