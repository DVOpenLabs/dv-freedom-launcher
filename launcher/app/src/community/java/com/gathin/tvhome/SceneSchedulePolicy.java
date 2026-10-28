package com.gathin.tvhome;
import java.util.*;
/** Local wall-clock windows. Monday is bit zero. Midnight windows belong to their start day. */
final class SceneSchedulePolicy {
    static final class Rule {
        final int days,start,end;final String token;
        Rule(int days,int start,int end,String token){if(days<1||days>127||start<0||start>=1440||end<0||end>=1440||start==end||!ThemeLibrary.validToken(token))throw new IllegalArgumentException("Choose days, two different times and an imported wallpaper");this.days=days;this.start=start;this.end=end;this.token=token;}
        boolean matches(int day,int minute){if(day<0||day>6||minute<0||minute>=1440)throw new IllegalArgumentException();return start<end?(days&(1<<day))!=0&&minute>=start&&minute<end:((days&(1<<day))!=0&&minute>=start)||((days&(1<<((day+6)%7)))!=0&&minute<end);}
    }
    static final class Spec {
        final boolean enabled;final List<Rule> rules;
        Spec(boolean enabled,List<Rule> rules){if(rules.size()>8||enabled&&rules.isEmpty())throw new IllegalArgumentException("Choose one to eight scene windows");boolean[] used=new boolean[10080];for(Rule r:rules)for(int day=0;day<7;day++)for(int minute=0;minute<1440;minute++)if(r.matches(day,minute)){int at=day*1440+minute;if(used[at])throw new IllegalArgumentException("Scene times overlap; adjust the days or times");used[at]=true;}this.enabled=enabled;this.rules=Collections.unmodifiableList(new ArrayList<>(rules));}
        String encode(){List<String> rows=new ArrayList<>();for(Rule r:rules)rows.add(r.days+","+r.start+","+r.end+","+r.token);return "v1|"+(enabled?"on":"off")+"|"+String.join(";",rows);}
        String select(int day,int minute){if(enabled)for(Rule r:rules)if(r.matches(day,minute))return r.token;return "";}
        void validatePlaylist(List<String> tokens){for(Rule r:rules)if(!tokens.contains(r.token))throw new IllegalArgumentException("Keep scheduled wallpapers in the playlist, or remove their scene windows first");}
    }
    static Spec parse(String raw){if(raw==null||raw.isEmpty())return new Spec(false,Collections.emptyList());if(raw.length()>2048)throw new IllegalArgumentException("Scene schedule too large");String[] parts=raw.split("\\|",-1);if(parts.length!=3||!parts[0].equals("v1")||!Arrays.asList("on","off").contains(parts[1]))throw new IllegalArgumentException("Invalid scene schedule");List<Rule> rows=new ArrayList<>();if(!parts[2].isEmpty())for(String row:parts[2].split(";",-1)){String[] fields=row.split(",",-1);if(fields.length!=4)throw new IllegalArgumentException("Invalid scene window");rows.add(new Rule(number(fields[0]),number(fields[1]),number(fields[2]),fields[3]));}Spec spec=new Spec(parts[1].equals("on"),rows);if(!spec.encode().equals(raw))throw new IllegalArgumentException("Invalid scene schedule encoding");return spec;}
    private static int number(String n){try{return Integer.parseInt(n);}catch(NumberFormatException bad){throw new IllegalArgumentException("Invalid scene time");}}
}
