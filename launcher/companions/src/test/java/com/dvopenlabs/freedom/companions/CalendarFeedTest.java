package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import org.json.*;
import java.time.*;
import static org.junit.Assert.*;
public class CalendarFeedTest {
    String calendar(String events){return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//DV Freedom Test//EN\r\n"+events+"END:VCALENDAR\r\n";}
    String event(String fields){return "BEGIN:VEVENT\r\n"+fields+"\r\nEND:VEVENT\r\n";}
    long time(String s){return Instant.parse(s).toEpochMilli();}
    @Test public void recurrenceAndExceptionProduceNextOccurrence()throws Exception{JSONArray result=CalendarFeed.parse(calendar(event("UID:weekly\r\nDTSTART:20261001T120000Z\r\nSUMMARY:Team game\r\nRRULE:FREQ=DAILY;COUNT=8\r\nEXDATE:20261003T120000Z")),time("2026-10-03T00:00:00Z"),"Game Day");assertEquals(5,result.length());assertEquals(time("2026-10-04T12:00:00Z"),result.getJSONObject(0).getLong("at"));}
    @Test public void cancelledInstanceReplacesOriginal()throws Exception{String base=event("UID:a\r\nDTSTART:20261001T120000Z\r\nSUMMARY:Family\r\nRRULE:FREQ=DAILY;COUNT=3");String exception=event("UID:a\r\nRECURRENCE-ID:20261002T120000Z\r\nDTSTART:20261002T120000Z\r\nSUMMARY:Family\r\nSTATUS:CANCELLED");JSONArray r=CalendarFeed.parse(calendar(base+exception),time("2026-10-02T00:00:00Z"),"Family");assertEquals(1,r.length());assertEquals(time("2026-10-03T12:00:00Z"),r.getJSONObject(0).getLong("at"));}
    @Test public void movedInstanceIsNotDuplicated()throws Exception{String base=event("UID:a\r\nDTSTART:20261001T120000Z\r\nSUMMARY:Game\r\nRRULE:FREQ=DAILY;COUNT=2");String moved=event("UID:a\r\nRECURRENCE-ID:20261002T120000Z\r\nDTSTART:20261002T140000Z\r\nSUMMARY:New time");JSONArray r=CalendarFeed.parse(calendar(base+moved),time("2026-10-02T00:00:00Z"),"Game Day");assertEquals(1,r.length());assertEquals("New time",r.getJSONObject(0).getString("title"));}
    @Test public void foldedTitlesAndAllDayAreRetained()throws Exception{JSONArray r=CalendarFeed.parse(calendar(event("UID:day\r\nDTSTART;VALUE=DATE:20261004\r\nSUMMARY:Family birth\r\n day")),time("2026-10-03T00:00:00Z"),"Family");assertEquals("Family birthday",r.getJSONObject(0).getString("title"));assertTrue(r.getJSONObject(0).getBoolean("allDay"));}
    @Test public void boundsInfiniteRecurrenceAndOutput()throws Exception{JSONArray r=CalendarFeed.parse(calendar(event("UID:forever\r\nDTSTART:20200101T120000Z\r\nSUMMARY:Daily\r\nRRULE:FREQ=DAILY")),time("2026-10-03T00:00:00Z"),"Family");assertEquals(12,r.length());}
    @Test(expected=IllegalArgumentException.class)public void rejectsNonCalendar()throws Exception{CalendarFeed.parse("not a calendar",0,"Family");}
    @Test public void weeklyLocalTimeSurvivesDaylightSavingChange()throws Exception{
        String zone="BEGIN:VTIMEZONE\r\nTZID:America/New_York\r\nBEGIN:STANDARD\r\nDTSTART:19701101T020000\r\nTZOFFSETFROM:-0400\r\nTZOFFSETTO:-0500\r\nRRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU\r\nEND:STANDARD\r\nBEGIN:DAYLIGHT\r\nDTSTART:19700308T020000\r\nTZOFFSETFROM:-0500\r\nTZOFFSETTO:-0400\r\nRRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU\r\nEND:DAYLIGHT\r\nEND:VTIMEZONE\r\n";
        JSONArray result=CalendarFeed.parse(calendar(zone+event("UID:weekly-zone\r\nDTSTART;TZID=America/New_York:20261025T180000\r\nSUMMARY:Evening game\r\nRRULE:FREQ=WEEKLY;COUNT=2")),time("2026-10-24T00:00:00Z"),"Game Day");
        assertEquals(2,result.length());assertEquals(time("2026-10-25T22:00:00Z"),result.getJSONObject(0).getLong("at"));assertEquals(time("2026-11-01T23:00:00Z"),result.getJSONObject(1).getLong("at"));
    }
    @Test public void subdailyExpansionIsIntentionallyExcluded()throws Exception{assertEquals(0,CalendarFeed.parse(calendar(event("UID:fast\r\nDTSTART:20200101T120000Z\r\nSUMMARY:Too frequent\r\nRRULE:FREQ=SECONDLY")),time("2026-10-24T00:00:00Z"),"Family").length());}
    @Test public void controlsExcludeLocksAndArbitraryServices(){assertTrue(HubActivity.supported("scene.movie_night"));assertTrue(HubActivity.supported("light.sofa"));assertFalse(HubActivity.supported("lock.front_door"));assertFalse(HubActivity.supported("script.open_door"));assertFalse(HubActivity.supported("scene.x/../../service"));}
}
