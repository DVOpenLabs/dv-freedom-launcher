package com.dvopenlabs.freedom.companions;

import java.util.*;
import java.time.*;
import org.json.*;

/** Shared next-event ordering and wording; no feed URLs, accounts or live-score claims. */
final class EventDisplay {
    static JSONArray merge(JSONArray input,long now)throws JSONException {
        Map<String,JSONObject> unique=new LinkedHashMap<>();
        for(int n=0;n<Math.min(48,input.length());n++){
            JSONObject item=input.optJSONObject(n);if(item==null)continue;
            String title=item.optString("title").trim().replaceAll("\\s+"," ");long at=item.optLong("at");
            if(title.isEmpty()||title.length()>160||at<now-86400000L||at>now+90L*86400000L)continue;
            if(!item.optBoolean("allDay")&&at<now)continue;
            String key=title.toLowerCase(Locale.ROOT)+":"+at+":"+item.optBoolean("allDay");
            if(!unique.containsKey(key))unique.put(key,new JSONObject(item.toString()).put("title",title));
            else {JSONObject old=unique.get(key);if(!old.optString("source").equals(item.optString("source")))old.put("source","Family / Game Day");}
        }
        List<JSONObject> sorted=new ArrayList<>(unique.values());sorted.sort(Comparator.comparingLong(x->x.optLong("at")));
        JSONArray result=new JSONArray();for(JSONObject item:sorted){if(result.length()==12)break;result.put(item);}return result;
    }
    static String countdown(long at,long now,boolean allDay,ZoneId zone){
        long days=java.time.temporal.ChronoUnit.DAYS.between(Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),Instant.ofEpochMilli(at).atZone(zone).toLocalDate());
        if(allDay)return days<=0?"Today · all day":days==1?"Tomorrow · all day":"In "+days+" days · all day";
        long minutes=Math.max(0,(at-now+59999)/60000);
        if(minutes==0)return "Starting now";
        if(minutes<60)return "Starts in "+minutes+" min";
        if(minutes<1440)return "Starts in "+minutes/60+" h"+(minutes%60==0?"":" "+minutes%60+" min");
        return days==1?"Tomorrow":"In "+Math.max(1,days)+" days";
    }
    private EventDisplay(){}
}
