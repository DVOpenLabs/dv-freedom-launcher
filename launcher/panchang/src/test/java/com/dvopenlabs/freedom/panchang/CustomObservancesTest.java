package com.dvopenlabs.freedom.panchang;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
public class CustomObservancesTest {
    @Test public void leapDateAndRoundTrip() {
        List<CustomObservances.Entry> entries=new ArrayList<>();
        CustomObservances.add(entries,new CustomObservances.Entry("2024-02-29","Festival"));
        List<CustomObservances.Entry> result=CustomObservances.parse(CustomObservances.encode(entries));
        assertEquals(1,result.size());assertEquals("2024-02-29",result.get(0).date.toString());
        assertEquals("Festival",result.get(0).title);
    }
    @Test public void invalidDateAndTitleAreRejected() {
        invalid("2025-02-29","Festival");invalid("2024-2-29","Festival");invalid("+10000-01-01","Festival");
        invalid("2024-02-29","");invalid("2024-02-29","bad\nname");
        invalid("2024-02-29",new String(new char[81]).replace('\0','x'));
    }
    @Test public void duplicatesRejectedAndDifferentDatesAllowed() {
        List<CustomObservances.Entry> entries=new ArrayList<>();
        CustomObservances.add(entries,new CustomObservances.Entry("2024-02-29","Festival"));
        try {CustomObservances.add(entries,new CustomObservances.Entry("2024-02-29","festival"));fail();}catch(IllegalArgumentException expected) { }
        CustomObservances.add(entries,new CustomObservances.Entry("2025-02-28","festival"));
        assertEquals(2,entries.size());
    }
    private static void invalid(String date,String title) {
        try {new CustomObservances.Entry(date,title);fail();}catch(RuntimeException expected) { }
    }
}
