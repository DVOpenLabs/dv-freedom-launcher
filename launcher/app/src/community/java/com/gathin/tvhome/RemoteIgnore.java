package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.text.*;
import android.view.KeyEvent;
import android.widget.*;
import java.util.*;

/** Per-TV opt-in ignore rules, read by already enabled helpers without rebinding or adding grants. */
final class RemoteIgnore {
    static final String KEY="remote_ignore_v1";
    static boolean essential(int key){return key==3||key==4||key>=19&&key<=26||key==66||key==82||key==91||key==164||key==170||key==176||key==178||key==223||key==224;}
    static String rule(boolean scan,String value){if(value==null||!value.matches("[0-9]{1,6}")||Integer.parseInt(value)<1||!scan&&essential(Integer.parseInt(value)))throw new IllegalArgumentException("Use a non-navigation keycode or scancode from TV Inspector");return (scan?"scan:":"key:")+Integer.parseInt(value);}
    static Set<String> parse(String raw){if(raw==null||raw.length()>1024)throw new IllegalArgumentException("Too many ignored buttons");Set<String> out=new LinkedHashSet<>();if(!raw.isEmpty())for(String row:raw.split("\n",-1)){String[] p=row.split(":",-1);if(p.length!=2||!p[0].equals("key")&&!p[0].equals("scan")||!row.equals(rule(p[0].equals("scan"),p[1]))||!out.add(row)||out.size()>32)throw new IllegalArgumentException("Invalid ignored button");}return out;}
    static boolean matches(Set<String> rules,int code,int scan){return !essential(code)&&(rules.contains("key:"+code)||scan>0&&rules.contains("scan:"+scan));}
    static Set<String> read(Context c){try{return parse(c.getSharedPreferences(Config.SETTINGS_NAME,0).getString(KEY,""));}catch(RuntimeException bad){return Collections.emptySet();}}
    static boolean handle(Context c,KeyEvent e){return !essential(e.getKeyCode())&&matches(read(c),e.getKeyCode(),e.getScanCode());}
    static void show(Activity a){
        if(!LauncherLock.require(a,()->show(a)))return;SharedPreferences p=a.getSharedPreferences(Config.SETTINGS_NAME,0);String before=p.getString(KEY,"");LinearLayout b=FreedomUi.body(a);
        b.addView(FreedomUi.text(a,"This TV · all profiles. Requires an existing remote key helper; no helper is enabled here. Use TV Inspector to read an app button's decimal code. Home, Back, arrows, OK, volume, power and Input always stay available.",17));
        EditText codes=new EditText(a);codes.setHint("key:190 or scan:490 · one rule per line");codes.setText(before);codes.setMaxLines(4);codes.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1024)});b.addView(codes);
        final AlertDialog[] sheet={null};FreedomUi.action(a,b,"Apply ignored buttons",()->{try{Set<String> checked=parse(codes.getText().toString().trim());if(!LauncherLock.open(a)||!before.equals(p.getString(KEY,"")))throw new IllegalArgumentException("Remote choices changed. Open this page again.");String raw=String.join("\n",checked);if(!raw.equals(before)&&!p.edit().putString(KEY,raw).commit())throw new IllegalArgumentException("Could not save ignored buttons");sheet[0].dismiss();a.recreate();}catch(RuntimeException bad){codes.setError(bad.getMessage());}});sheet[0]=FreedomUi.sheet(a,"Ignore accidental app buttons",b);
    }
    private RemoteIgnore(){}
}
