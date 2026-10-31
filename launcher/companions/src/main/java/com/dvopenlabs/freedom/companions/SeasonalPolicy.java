package com.dvopenlabs.freedom.companions;
import java.util.*;

/** Local calendar-day expiry. No location lookup or automatic season guessing. */
final class SeasonalPolicy {
    static final String KEY="seasonal_v1";
    static final String[] IDS={"autumn","winter","spring","summer"};
    static final String[] NAMES={"Autumn glow","Winter light","Spring bloom","Summer tide"};
    static final class Spec {
        final String id;final long first,last;final boolean motion;
        Spec(String id,long first,long last,boolean motion){
            if(!Arrays.asList(IDS).contains(id)||first<0||last>200000||last<first||last-first>89)throw new IllegalArgumentException("Choose a pack for 1–90 days");
            this.id=id;this.first=first;this.last=last;this.motion=motion;
        }
        boolean active(long today){return today>=first&&today<=last;}
        String encode(){return "v1|"+id+"|"+first+"|"+last+"|"+(motion?1:0);}
        String name(){return NAMES[Arrays.asList(IDS).indexOf(id)];}
    }
    static Spec parse(String raw){
        if(raw==null||raw.isEmpty())return null;
        if(raw.length()>100)throw new IllegalArgumentException("Invalid seasonal pack");
        String[] p=raw.split("\\|",-1);if(p.length!=5||!p[0].equals("v1")||!p[2].matches("[0-9]{1,6}")||!p[3].matches("[0-9]{1,6}")||!p[4].matches("[01]"))throw new IllegalArgumentException("Invalid seasonal pack");
        return new Spec(p[1],Long.parseLong(p[2]),Long.parseLong(p[3]),p[4].equals("1"));
    }
    private SeasonalPolicy(){}
}
