package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.widget.*;

/** A visible source shortcut, with an honest remote-control fallback on external streamers. */
final class InputShortcut {
    static final String KEY="show_inputs_shortcut";
    static void settings(Activity a){
        SharedPreferences p=a.getSharedPreferences(Config.SETTINGS_NAME,0);boolean on=Config.load(a).showInputsPill;
        TextView row=a.findViewById(R.id.inputsShortcutChoice);
        row.setText(a.getString(R.string.inputs_shortcut)+" · "+a.getString(on?R.string.live_on:R.string.live_off));
        row.setOnClickListener(v->{if(CompanionControls.saveBoolean(p,KEY,!on))a.finish();else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_SHORT).show();});
    }
    static void help(Activity a){
        new AlertDialog.Builder(a).setTitle(R.string.inputs).setMessage(R.string.inputs_external_help)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.tv_settings,(d,w)->{
                try{a.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException e){Toast.makeText(a,R.string.not_available,Toast.LENGTH_SHORT).show();}
            }).show();
    }
    private InputShortcut(){}
}
