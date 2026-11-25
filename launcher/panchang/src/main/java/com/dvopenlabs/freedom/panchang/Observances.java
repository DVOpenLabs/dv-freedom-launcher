package com.dvopenlabs.freedom.panchang;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Sunrise tithi markers, not festival or fasting-date decisions. */
final class Observances {
    static final class Entry {
        final LocalDate date;
        final int tithi;
        Entry(LocalDate date, int tithi) { this.date=date; this.tithi=tithi; }
        String label(boolean hindi) {
            if(tithi==15)return hindi?"पूर्णिमा":"Purnima";
            if(tithi==30)return hindi?"अमावस्या":"Amavasya";
            if(tithi==11)return hindi?"शुक्ल एकादशी":"Shukla Ekadashi";
            return hindi?"कृष्ण एकादशी":"Krishna Ekadashi";
        }
    }
    static List<Entry> forMonth(MonthPlan plan) {
        List<Entry> result=new ArrayList<>();
        for(MonthPlan.Cell cell:plan.cells) if(cell!=null&&cell.marked())result.add(new Entry(cell.date,cell.tithi));
        return result;
    }
    private Observances() { }
}
