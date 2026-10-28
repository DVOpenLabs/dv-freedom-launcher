package com.gathin.tvhome;
import java.text.Normalizer;
import java.util.Locale;
/** Bounded, local-only lookup. Search results navigate; they never execute settings. */
final class SettingsSearch {
    static String normalize(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFKD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
    static boolean matches(String query,String title,String help,String path){
        String q=normalize(query);if(q.isEmpty()||q.length()>120)return false;
        String name=normalize(title),hay=normalize(title+" "+help+" "+path)+" "+aliases(name);
        for(String word:q.split(" +"))if(!hay.contains(word))return false;return true;
    }
    private static String aliases(String title){
        if(title.equals("inputs shortcut")||title.equals("input visibility"))return "hdmi source ports television";
        if(title.equals("text sizes")||title.equals("home text")||title.equals("app names"))return "font fonts typography readable readability color colour size letters";
        if(title.equals("app position"))return "top middle bottom alignment dock location";
        if(title.equals("extra tile spacing"))return "gap gaps space padding distance";
        if(title.equals("focus outline"))return "selection selected highlight border remote cursor";
        if(title.equals("wallpaper playlist"))return "rotate rotation slideshow backgrounds interval";
        if(title.equals("silent live wallpaper"))return "animation animated video background sound";
        if(title.equals("layout lock"))return "pin password protect editing";
        if(title.equals("setup help"))return "ai assistant onboarding wizard beginner";
        return "";
    }
    static boolean shared(String title){return title.equals("Inputs shortcut")||title.equals("Input visibility")||title.equals("TV settings")||title.equals("Default Home")||title.equals("Bluetooth & accessories")||title.equals("Remote helper")||title.equals("TV button")||title.equals("Open on start")||title.equals("Manage offline themes")||title.startsWith("About");}
    private SettingsSearch(){}
}
