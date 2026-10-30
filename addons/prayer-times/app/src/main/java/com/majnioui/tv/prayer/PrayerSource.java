package com.majnioui.tv.prayer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
/** Fixed optional provider endpoint, with encoded user-reviewed coarse location. */
final class PrayerSource {
    static String url(String date,String city,String country,int method){if(date==null||!date.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")||!valid(city)||!valid(country)||method<0||method>23)throw new IllegalArgumentException();return "https://api.aladhan.com/v1/timingsByCity/"+date+"?city="+encode(city)+"&country="+encode(country)+"&method="+method;}
    static boolean valid(String text){return text!=null&&!text.trim().isEmpty()&&text.length()<=80&&!text.matches("(?s).*[\\p{Cntrl}].*");}
    static String encode(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(java.io.UnsupportedEncodingException impossible){throw new IllegalStateException(impossible);}}
    private PrayerSource(){}
}
