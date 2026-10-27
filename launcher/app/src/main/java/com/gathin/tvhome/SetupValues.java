package com.gathin.tvhome;

import android.content.SharedPreferences;
import java.util.*;

/** A saved Home is a bounded allowlist of app/appearance choices, never device settings or credentials. */
final class SetupValues {
    static final Set<String> KEYS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "columns","rows_below_dock","dock_opacity","animations","advanced_effects","wallpaper_choice",
        "home_style_choice","theme_pack_id","live_wallpaper","focus_outline_choice","tile_gap_choice",
        "desktop_layout","show_audio_indicator","app_position","wallpaper_dim_choice","cinematic_idle_seconds",
        "adaptive_header","refined_motion","text_ink_choice","app_names_choice")));
    private static final Set<String> FLAGS=new HashSet<>(Arrays.asList("animations","advanced_effects","live_wallpaper","show_audio_indicator","adaptive_header","refined_motion"));
    static String name(String text){
        if(text==null)throw new IllegalArgumentException("Name required");String value=text.trim();
        if(value.isEmpty()||value.length()>40)throw new IllegalArgumentException("Use 1–40 characters");
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))throw new IllegalArgumentException("Invalid name");
        return value;
    }
    static Map<String,Object> validate(Map<String,?> values){
        if(!KEYS.containsAll(values.keySet()))throw new IllegalArgumentException("Unsupported Home setting");
        Map<String,Object> out=new HashMap<>();
        for(Map.Entry<String,?> entry:values.entrySet()){
            String key=entry.getKey();Object value=entry.getValue();
            if(FLAGS.contains(key)){if(!(value instanceof Boolean))throw new IllegalArgumentException("Invalid switch");}
            else if(Arrays.asList("columns","rows_below_dock","dock_opacity","tile_gap_choice","wallpaper_dim_choice","cinematic_idle_seconds","text_ink_choice","app_names_choice").contains(key)){
                if(!(value instanceof Integer||value instanceof Long))throw new IllegalArgumentException("Invalid number");
                long n=((Number)value).longValue();int lo=0,hi=90;
                if(key.equals("columns")){lo=2;hi=8;}if(key.equals("rows_below_dock"))hi=3;
                if(key.equals("dock_opacity")){lo=30;hi=100;}if(key.equals("tile_gap_choice"))hi=32;
                if(key.equals("text_ink_choice"))hi=2;if(key.equals("app_names_choice"))hi=3;
                if(key.equals("cinematic_idle_seconds")&&n!=0&&n!=30&&n!=60&&n!=120)throw new IllegalArgumentException("Invalid idle time");
                if(key.equals("cinematic_idle_seconds"))hi=120;
                if(n<lo||n>hi)throw new IllegalArgumentException("Invalid range");value=(int)n;
            }else{
                if(!(value instanceof String)||((String)value).length()>(key.equals("desktop_layout")?32768:240))throw new IllegalArgumentException("Invalid text");
                if(key.equals("app_position")&&!AppPlacement.valid((String)value))throw new IllegalArgumentException("Invalid position");
                if(key.equals("home_style_choice")&&!HomeStyles.valid((String)value))throw new IllegalArgumentException("Invalid style");
                if(key.equals("theme_pack_id")&&!((String)value).isEmpty()&&!ThemeLibrary.validToken((String)value))throw new IllegalArgumentException("Invalid theme");
            }
            out.put(key,value);
        }
        return out;
    }
    static void put(SharedPreferences.Editor editor,String key,Object value){
        if(value==null)editor.remove(key);else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
        else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Long)editor.putLong(key,(Long)value);
        else if(value instanceof String)editor.putString(key,(String)value);else throw new IllegalArgumentException("Unsupported value");
    }
    /** One disk commit and exact in-memory repair, including originally absent keys. */
    static boolean commit(SharedPreferences prefs,Map<String,Object> changes){
        Map<String,?> before=prefs.getAll();SharedPreferences.Editor editor=prefs.edit();
        for(Map.Entry<String,Object> entry:changes.entrySet())put(editor,entry.getKey(),entry.getValue());
        if(editor.commit())return true;
        SharedPreferences.Editor repair=prefs.edit();for(String key:changes.keySet())put(repair,key,before.get(key));repair.commit();return false;
    }
    private SetupValues(){}
}
