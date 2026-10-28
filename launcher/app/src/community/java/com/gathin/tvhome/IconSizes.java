package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Profile-only size draft. No display-density or app-order changes. */
final class IconSizes {
    static String current(Context c){String value=ProfileStore.preferences(c).getString(IconSizePolicy.KEY,"");return IconSizePolicy.valid(value)?value:"";}
    private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static void show(Activity a){
        String profile=ProfileStore.id();Map<String,?> before=ProfileStore.preferences(a).getAll();String[] draft={current(a)};
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(a,24),dp(a,12),dp(a,24),dp(a,12));
        TextView help=new TextView(a);help.setTypeface(SettingsTypography.regular(a));help.setTextSize(17);help.setTextColor(0xffc4d0df);help.setText("Preview banner and icon size. App order and apps per row stay the same. Large uses the largest size that safely fits your row; fewer apps per row allows larger tiles. The still illustration leaves your wallpaper unchanged.");content.addView(help);
        HomeMiniPreview sample=new HomeMiniPreview(a,SettingsTypography.regular(a));int sampleWidth=Math.max(dp(a,160),Math.min(dp(a,320),(int)(a.getResources().getDisplayMetrics().widthPixels*.56f)-dp(a,48)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(sampleWidth,Math.round(sampleWidth*9f/16));sp.gravity=Gravity.CENTER_HORIZONTAL;sp.topMargin=dp(a,16);sp.bottomMargin=dp(a,16);content.addView(sample,sp);
        LinearLayout options=new LinearLayout(a);content.addView(options);TextView[] chips=new TextView[4];
        Runnable refresh=()->{sample.inspect("Icon size",null,draft[0],false);for(int k=0;k<4;k++)if(chips[k]!=null){chips[k].setText((k==0?"Current":IconSizePolicy.label(IconSizePolicy.IDS[k]))+(draft[0].equals(IconSizePolicy.IDS[k])?" ✓":""));chips[k].setSelected(draft[0].equals(IconSizePolicy.IDS[k]));}};
        for(int i=0;i<4;i++){final int at=i;TextView chip=new TextView(a);chips[i]=chip;chip.setTypeface(SettingsTypography.regular(a));chip.setTextColor(-1);chip.setTextSize(17);chip.setGravity(Gravity.CENTER);chip.setFocusable(true);chip.setClickable(true);chip.setBackgroundResource(R.drawable.freedom_settings_chip);chip.setContentDescription(IconSizePolicy.label(IconSizePolicy.IDS[i]));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(a,52),1);cp.setMarginEnd(dp(a,6));options.addView(chip,cp);chip.setOnClickListener(v->{draft[0]=IconSizePolicy.IDS[at];refresh.run();});}
        refresh.run();ScrollView scroll=new ScrollView(a);scroll.addView(content);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Icon size · preview").setView(scroll).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Apply",(d,w)->{
            android.content.SharedPreferences p=ProfileStore.preferences(a);if(!profile.equals(ProfileStore.id())||!before.equals(p.getAll())){Toast.makeText(a,"Home changed. Open Icon size again.",Toast.LENGTH_LONG).show();return;}
            if(draft[0].equals(current(a)))return;Map<String,Object> changes=new HashMap<>();changes.put(IconSizePolicy.KEY,draft[0].isEmpty()?null:draft[0]);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);ProductFeatures.beforeChange(a,"Change icon size");if(SetupValues.commit(p,changes)){a.setResult(Activity.RESULT_OK);a.finish();}else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_LONG).show();
        }).create();dialog.setOnShowListener(d->chips[Arrays.asList(IconSizePolicy.IDS).indexOf(draft[0])].requestFocusFromTouch());dialog.show();
    }
    private IconSizes(){}
}
