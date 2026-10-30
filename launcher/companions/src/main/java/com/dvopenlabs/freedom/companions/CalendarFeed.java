package com.dvopenlabs.freedom.companions;

import biweekly.*;
import biweekly.component.*;
import java.util.*;
import org.json.*;

/** Bounded foreground calendar parsing, with recurrence, exceptions and replacement instances. */
final class CalendarFeed {
    static JSONArray parse(String text,long now,String source)throws Exception {
        if(text.length()>1024*1024||!text.contains("BEGIN:VCALENDAR"))throw new IllegalArgumentException();
        ICalendar calendar=Biweekly.parse(text).first();
        if(calendar==null||calendar.getEvents().size()>2000)throw new IllegalArgumentException();
        long end=now+90L*86400000;List<JSONObject> out=new ArrayList<>();Set<String> replaced=new HashSet<>(),seen=new HashSet<>();
        for(VEvent event:calendar.getEvents())if(event.getRecurrenceId()!=null&&event.getUid()!=null)replaced.add(event.getUid().getValue()+":"+event.getRecurrenceId().getValue().getTime());
        for(VEvent e:calendar.getEvents()){
            if(e.getDateStart()==null||e.getSummary()==null||e.getStatus()!=null&&"CANCELLED".equalsIgnoreCase(e.getStatus().getValue()))continue;
            if(e.getRecurrenceRule()!=null&&e.getRecurrenceRule().getValue()!=null&&Arrays.asList("SECONDLY","MINUTELY","HOURLY").contains(e.getRecurrenceRule().getValue().getFrequency().name()))continue;
            String title=e.getSummary().getValue();if(title==null||title.isEmpty()||title.length()>160)continue;
            String uid=e.getUid()==null?title:e.getUid().getValue();
            TimeZone zone=TimeZone.getDefault();var assignment=calendar.getTimezoneInfo().getTimezone(e.getDateStart());if(assignment!=null)zone=assignment.getTimeZone();
            var dates=e.getDateIterator(zone);dates.advanceTo(new Date(now-86400000));int count=0;
            while(dates.hasNext()&&count++<128){Date d=dates.next();long at=d.getTime();if(at>end)break;
                boolean allDay=!e.getDateStart().getValue().hasTime();if(at<now&&( !allDay||at<now-86400000))continue;
                String key=uid+":"+at;if(e.getRecurrenceId()==null&&replaced.contains(key)||!seen.add(key))continue;
                out.add(new JSONObject().put("title",title).put("at",at).put("allDay",allDay).put("source",source));
            }
        }
        out.sort(Comparator.comparingLong(x->x.optLong("at")));JSONArray result=new JSONArray();for(JSONObject item:out){if(result.length()==12)break;result.put(item);}return result;
    }
}
