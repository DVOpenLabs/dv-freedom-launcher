package com.gathin.tvhome;
import java.util.*;
/** Provider metadata only. No invented viewing progress, scraping or watch-history collection. */
final class ContentShelfPolicy {
    static final long MAX_AGE=30L*24*60*60*1000;
    static boolean accepted(String pkg,String title,int type,long engagement,long position,long duration,long now,Set<String> visible){
        if(!visible.contains(pkg)||title==null||title.trim().isEmpty()||title.length()>160||type<0||type>3||engagement<=0||engagement>now+300000||now-engagement>MAX_AGE)return false;
        for(int i=0;i<title.length();i++)if(Character.isISOControl(title.charAt(i)))return false;
        if(type==0&&duration>0&&position>=0){double fraction=position/(double)duration;if(fraction<.05||fraction>.95)return false;}return true;
    }
    static boolean scheme(String scheme){return scheme!=null&&scheme.matches("[A-Za-z][A-Za-z0-9+.-]{0,39}")&&!Arrays.asList("file","content","data","javascript","market","tel","sms","mailto","geo","settings","intent","android-app").contains(scheme.toLowerCase(Locale.ROOT));}
    static String kind(int type){return new String[]{"Continue watching","Up next","New episode","Watchlist"}[type];}
}
