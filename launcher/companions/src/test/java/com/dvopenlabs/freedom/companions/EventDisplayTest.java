package com.dvopenlabs.freedom.companions;
import java.time.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class EventDisplayTest {
    @Test public void duplicateSubscriptionsProduceOneUpcomingItem()throws Exception{
        JSONArray input=new JSONArray();long now=1000000000L;
        input.put(new JSONObject().put("title","Team   Game").put("at",now+3600000).put("source","Family"));
        input.put(new JSONObject().put("title","team game").put("at",now+3600000).put("source","Game Day"));
        input.put(new JSONObject().put("title","Finished game").put("at",now-1).put("source","Game Day"));
        JSONArray merged=EventDisplay.merge(input,now);assertEquals(1,merged.length());assertEquals("Family / Game Day",merged.getJSONObject(0).getString("source"));
    }
    @Test public void countdownDoesNotCallEveryEventToday(){
        assertEquals("Starts in 15 min",EventDisplay.countdown(900000,0,false,ZoneOffset.UTC));
        assertEquals("Starts in 2 h",EventDisplay.countdown(7200000,0,false,ZoneOffset.UTC));
        assertEquals("Tomorrow · all day",EventDisplay.countdown(86400000,0,true,ZoneOffset.UTC));
    }
    @Test public void allDayTomorrowUsesCalendarDayAcrossDst(){
        long now=Instant.parse("2026-11-01T03:30:00Z").toEpochMilli(),at=Instant.parse("2026-11-01T04:00:00Z").toEpochMilli();
        assertEquals("Tomorrow · all day",EventDisplay.countdown(at,now,true,ZoneId.of("America/New_York")));
    }
}
