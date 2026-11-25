package com.dvopenlabs.freedom.panchang;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class ObservancesTest {
    private static boolean contains(List<Observances.Entry> entries, String date, int tithi) {
        for (Observances.Entry entry : entries)
            if (entry.date.equals(LocalDate.parse(date)) && entry.tithi==tithi) return true;
        return false;
    }
    @Test public void knownFestivalSeasonTithisAreMarkersNotFestivalPredictions() {
        City delhi=City.cityNamed("Delhi");
        assertTrue(contains(Observances.forMonth(MonthPlan.of(LocalDate.parse("2024-11-01"),delhi,Region.NORTH)),"2024-11-01",30));
        assertTrue(contains(Observances.forMonth(MonthPlan.of(LocalDate.parse("2025-03-14"),delhi,Region.NORTH)),"2025-03-14",15));
        assertEquals("पूर्णिमा",new Observances.Entry(LocalDate.parse("2025-03-14"),15).label(true));
        assertEquals("Purnima",new Observances.Entry(LocalDate.parse("2025-03-14"),15).label(false));
    }
    @Test public void nextMonthUsesItsOwnDatesAndRegionalBoundaries() {
        City city=City.cityNamed("Delhi");
        MonthPlan first=MonthPlan.of(LocalDate.parse("2025-03-14"),city,Region.NORTH);
        MonthPlan next=MonthPlan.of(first.last.plusDays(1),city,Region.NORTH);
        assertEquals(first.last.plusDays(1),next.first);
        for(Observances.Entry entry:Observances.forMonth(next)) {
            assertFalse(entry.date.isBefore(next.first));assertFalse(entry.date.isAfter(next.last));
        }
    }
    @Test public void manualCityMatchesListedCityAcrossDaylightSaving() {
        City named=City.cityNamed("New York");
        City manual=City.manual("Custom",named.lat,named.lon,named.zone.getId());
        for(String date:new String[]{"2025-03-09","2025-11-02"}) {
            MonthPlan a=MonthPlan.of(LocalDate.parse(date),named,Region.NORTH);
            MonthPlan b=MonthPlan.of(LocalDate.parse(date),manual,Region.NORTH);
            List<Observances.Entry> expected=Observances.forMonth(a), actual=Observances.forMonth(b);
            assertEquals(expected.size(),actual.size());
            for(int i=0;i<expected.size();i++) {
                assertEquals(expected.get(i).date,actual.get(i).date);
                assertEquals(expected.get(i).tithi,actual.get(i).tithi);
            }
        }
    }
}
