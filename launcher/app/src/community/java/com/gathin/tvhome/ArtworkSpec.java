package com.gathin.tvhome;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Small non-executable package-to-art map. Media stays outside preferences and the APK. */
final class ArtworkSpec {
    static final int MAX_TEXT=16384,MAX_ITEMS=64;
    static boolean token(String value){return "icon".equals(value)||value!=null&&(value.matches("banner_[a-z0-9_]{1,80}")||value.matches("image:[a-f0-9]{64}"));}
    static Map<String,String> parse(String text){
        Map<String,String> result=new LinkedHashMap<>();
        if(text==null||text.isEmpty())return result;
        if(text.length()>MAX_TEXT||!text.startsWith("v1\n"))throw new IllegalArgumentException("Invalid artwork choices");
        String[] lines=text.substring(3).split("\n",-1);
        if(lines.length>MAX_ITEMS+1)throw new IllegalArgumentException("Too many artwork choices");
        for(int i=0;i<lines.length;i++){
            String line=lines[i];if(line.isEmpty()&&i==lines.length-1)continue;
            int at=line.indexOf('=');if(at<1)throw new IllegalArgumentException("Invalid artwork choice");
            String pkg=line.substring(0,at),value=line.substring(at+1);
            if(!LayoutDraft.packageName(pkg)||!token(value)||result.put(pkg,value)!=null||result.size()>MAX_ITEMS)throw new IllegalArgumentException("Invalid artwork choice");
        }
        return result;
    }
    static String encode(Map<String,String> values){
        if(values.isEmpty())return "";StringBuilder text=new StringBuilder("v1\n");
        for(Map.Entry<String,String> entry:new TreeMap<>(values).entrySet())text.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        String result=text.toString();parse(result);return result;
    }
    static String name(String value){
        if(value==null)throw new IllegalArgumentException("Enter a name");
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))throw new IllegalArgumentException("Invalid name");
        String text=value.trim();
        if(text.isEmpty()||text.length()>60)throw new IllegalArgumentException("Use 1–60 characters");
        return text;
    }
    static Map<String,String> named(Map<String,String> labels,String pkg,String value){
        if(!LayoutDraft.packageName(pkg))throw new IllegalArgumentException("Invalid app");
        Map<String,String> result=new LinkedHashMap<>(labels);if(value==null)result.remove(pkg);else result.put(pkg,name(value));return result;
    }
    private ArtworkSpec(){}
}
