package com.dvopenlabs.freedom.panchang;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Bounded local-only civil dates and titles. */
final class CustomObservances {
    static final int MAX_ENTRIES=200, MAX_TITLE=80;
    static final class Entry {
        final LocalDate date; final String title;
        Entry(String date,String title) {
            if(date==null || date.length()!=10)throw new IllegalArgumentException("Use YYYY-MM-DD");
            this.date=LocalDate.parse(date);
            if(!this.date.toString().equals(date))throw new IllegalArgumentException("Use ISO date");
            if(title==null||title.trim().isEmpty()||title.length()>MAX_TITLE)throw new IllegalArgumentException("Invalid title");
            for(int i=0;i<title.length();i++)if(Character.isISOControl(title.charAt(i)))throw new IllegalArgumentException("Invalid title");
            this.title=title.trim();
        }
    }
    static List<Entry> parse(String data) {
        List<Entry> out=new ArrayList<>();
        if(data==null||data.length()>MAX_ENTRIES*(MAX_TITLE+12))return out;
        for(String line:data.split("\n")) {
            int separator=line.indexOf('\t');
            if(separator<0||out.size()>=MAX_ENTRIES)continue;
            try { add(out,new Entry(line.substring(0,separator),line.substring(separator+1))); }
            catch(RuntimeException ignored) { /* Ignore damaged local entries. */ }
        }
        return out;
    }
    static void add(List<Entry> entries,Entry entry) {
        if(entries.size()>=MAX_ENTRIES)throw new IllegalArgumentException("Too many entries");
        for(Entry e:entries)if(e.date.equals(entry.date)&&e.title.equalsIgnoreCase(entry.title))throw new IllegalArgumentException("Duplicate");
        entries.add(entry);
        entries.sort((a,b)->{int date=a.date.compareTo(b.date);return date!=0?date:a.title.compareToIgnoreCase(b.title);});
    }
    static String encode(List<Entry> entries) {
        StringBuilder data=new StringBuilder();
        for(Entry entry:entries)data.append(entry.date).append('\t').append(entry.title).append('\n');
        return data.toString();
    }
    private CustomObservances() { }
}
