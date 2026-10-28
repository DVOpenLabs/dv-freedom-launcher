package com.gathin.tvhome;

import java.util.*;

/** Bounded local-media choices and a clock that consumes time only while Home is visible. */
final class PlaylistPolicy {
    static final int MAX_ITEMS=8,DEFAULT_SECONDS=900;
    static final class Spec {
        final boolean enabled,shuffle;final int seconds;final List<String> tokens;
        Spec(boolean enabled,int seconds,boolean shuffle,List<String> tokens){
            if(!seconds(seconds)||tokens==null||tokens.size()>MAX_ITEMS)throw new IllegalArgumentException("Invalid wallpaper playlist");
            Set<String> seen=new HashSet<>();for(String token:tokens)if(!ThemeLibrary.validToken(token)||!seen.add(token))throw new IllegalArgumentException("Invalid or duplicate wallpaper");
            if(enabled&&tokens.size()<2)throw new IllegalArgumentException("Choose at least two wallpapers");
            this.enabled=enabled;this.seconds=seconds;this.shuffle=shuffle;this.tokens=Collections.unmodifiableList(new ArrayList<>(tokens));
        }
        String encode(){return "v1|"+(enabled?"on":"off")+"|"+seconds+"|"+(shuffle?"shuffle":"sequential")+"|"+String.join(",",tokens);}
    }
    static boolean seconds(int value){return value==60||value==300||value==900||value==1800||value==3600;}
    static Spec parse(String raw){
        if(raw==null||raw.isEmpty())return new Spec(false,DEFAULT_SECONDS,false,Collections.emptyList());
        if(raw.length()>2048)throw new IllegalArgumentException("Playlist is too large");String[] parts=raw.split("\\|",-1);
        if(parts.length!=5||!parts[0].equals("v1")||!Arrays.asList("on","off").contains(parts[1])||!Arrays.asList("shuffle","sequential").contains(parts[3])||!parts[2].matches("[0-9]{2,4}"))throw new IllegalArgumentException("Invalid wallpaper playlist");
        return new Spec(parts[1].equals("on"),Integer.parseInt(parts[2]),parts[3].equals("shuffle"),parts[4].isEmpty()?Collections.emptyList():Arrays.asList(parts[4].split(",",-1)));
    }
    static int next(int count,int current,boolean shuffle,Random random){
        if(count<=1)return 0;if(!shuffle)return (Math.max(-1,current)+1)%count;
        int selected=random.nextInt(count-1);return selected>=current?selected+1:selected;
    }
    static final class Clock {
        final long period;private long left,since;private boolean running;
        Clock(long period,long remaining){if(period<=0)throw new IllegalArgumentException();this.period=period;left=Math.max(0,Math.min(period,remaining));}
        long remaining(long now){return running?Math.max(0,left-Math.max(0,now-since)):left;}
        void start(long now){if(!running){since=now;running=true;}}
        void stop(long now){left=remaining(now);running=false;}
        void reset(long now){left=period;since=now;}
        void restore(long remaining){left=Math.max(0,Math.min(period,remaining));running=false;}
        boolean due(long now){return running&&remaining(now)==0;}
    }
    private PlaylistPolicy(){}
}
