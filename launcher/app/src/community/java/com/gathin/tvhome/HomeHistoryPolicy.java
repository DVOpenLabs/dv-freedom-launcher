package com.gathin.tvhome;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Storage bounds shared by the native history and plain-JVM tests. */
final class HomeHistoryPolicy {
    static final int LIMIT=5, MAX_BYTES=256*1024, FILE_BYTES=32768;
    static final String[] FILES={"order.txt","library_apps.txt","library_home_removed.txt"};
    static boolean profile(String id){return id!=null&&(id.isEmpty()||LocalProfiles.validId(id));}
    static String reason(String value){
        if(value==null)return "Home change";
        String clean=value.replaceAll("[\\p{Cntrl}]+"," ").trim();
        return clean.isEmpty()?"Home change":clean.substring(0,Math.min(80,clean.length()));
    }
    static List<String> trim(List<String> rows){
        List<String> out=new ArrayList<>(rows);
        while(!out.isEmpty()&&(out.size()>LIMIT||bytes(out)>MAX_BYTES))out.remove(0);
        return out;
    }
    private static int bytes(List<String> rows){
        long count=2;for(String row:rows)count+=row.getBytes(StandardCharsets.UTF_8).length+1L;
        return count>Integer.MAX_VALUE?Integer.MAX_VALUE:(int)count;
    }
    private HomeHistoryPolicy(){}
}
