package io.github.luyzfernando08.sleeptimer;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
public class ScheduleRulesTest {
    private final ZoneId zone=ZoneId.of("America/New_York");
    private long instant(String value){return ZonedDateTime.parse(value).toInstant().toEpochMilli();}
    @Test public void wakeExclusionsDoNotSuppressSleep(){
        long now=instant("2026-11-14T08:00:00-05:00[America/New_York]");
        List<ScheduleRules.Rule> rules=Arrays.asList(new ScheduleRules.Rule(127,540,true),new ScheduleRules.Rule(127,600,false));
        List<ScheduleRules.Pause> pauses=Collections.singletonList(new ScheduleRules.Pause(LocalDate.of(2026,11,14),LocalDate.of(2026,11,16)));
        ScheduleRules.Event e=ScheduleRules.next(now,zone,rules,pauses);assertFalse(e.wake);assertEquals(instant("2026-11-14T10:00:00-05:00[America/New_York]"),e.at);
    }
    @Test public void sleepWinsSameMinuteAndPastEventsAreSkipped(){
        long now=instant("2026-11-14T08:00:00-05:00[America/New_York]");
        List<ScheduleRules.Rule> rules=Arrays.asList(new ScheduleRules.Rule(127,540,true),new ScheduleRules.Rule(127,540,false));
        ScheduleRules.Event e=ScheduleRules.next(now,zone,rules,Collections.emptyList());assertFalse(e.wake);
        assertTrue(ScheduleRules.next(e.at,zone,rules,Collections.emptyList()).at>e.at);
    }
    @Test public void dstGapMovesToFirstValidLocalTime(){
        long now=instant("2027-03-14T00:00:00-05:00[America/New_York]");
        ScheduleRules.Event e=ScheduleRules.next(now,zone,Collections.singletonList(new ScheduleRules.Rule(64,150,true)),Collections.emptyList());
        assertEquals(instant("2027-03-14T03:30:00-04:00[America/New_York]"),e.at);
    }
}
