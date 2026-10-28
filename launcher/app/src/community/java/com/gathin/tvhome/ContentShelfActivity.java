package com.gathin.tvhome;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.*;
/** Optional shared-content browser for the original grid, without changing its layout. */
public final class ContentShelfActivity extends Activity {
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LauncherLocale.wrap(c));}
    @Override protected void onCreate(Bundle b){super.onCreate(b);}
    @Override protected void onResume(){super.onResume();Config.load(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);int p=Math.round(36*getResources().getDisplayMetrics().density);root.setPadding(p,p,p,p);root.setBackgroundColor(0xff111820);TextView title=new TextView(this);title.setTypeface(SettingsTypography.regular(this));title.setText("Shared items from your apps");title.setTextColor(-1);title.setTextSize(28);root.addView(title);TextView back=new TextView(this);back.setTypeface(SettingsTypography.regular(this));back.setText("Back to Home");back.setTextColor(-1);back.setTextSize(18);back.setPadding(20,16,20,16);back.setFocusable(true);back.setClickable(true);back.setBackgroundResource(R.drawable.settings_choice_bg);back.setOnClickListener(v->finish());root.addView(back);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);root.addView(body);setContentView(root);if(ContentShelves.enabled(this))new ContentShelves.Row(this,body,back,true);else{TextView empty=new TextView(this);empty.setTypeface(SettingsTypography.regular(this));empty.setText("Content shelves are off. Enable them in Launcher settings → Apps & rows → Content shelves.");empty.setTextColor(-1);body.addView(empty);}back.requestFocus();}
}
