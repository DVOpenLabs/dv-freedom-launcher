package com.gathin.tvhome;

/** Slows existing OS repeats only. Never generates keys or delays the first press. */
final class GridRepeatPolicy {
    static final String KEY="freedom_grid_repeat";
    private int held;
    private long last=-1;
    static boolean valid(Object value){return value instanceof Integer&&((Integer)value==0||(Integer)value==180||(Integer)value==280);}
    boolean consume(int key,boolean down,int repeats,long now,int interval,boolean eligible){
        boolean arrow=key>=19&&key<=22;
        if(!eligible||interval==0||!arrow||!down){reset();return false;}
        if(repeats==0||held!=key||last<0||now<last){held=key;last=now;return false;}
        if(now-last<interval)return true;
        last=now;return false;
    }
    void reset(){held=0;last=-1;}
}
