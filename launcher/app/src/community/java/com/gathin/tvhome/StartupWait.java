package com.gathin.tvhome;

/** One foreground startup attempt, driven by connectivity events and a single deadline. */
final class StartupWait {
    private long deadline;private boolean pending;
    void begin(long now,int seconds){if(seconds<0||seconds>600)throw new IllegalArgumentException("Wait must be 0–600 seconds");deadline=now+seconds*1000L;pending=true;}
    boolean take(boolean resumed,boolean validated,long now){if(!pending||!resumed||!validated&&now<deadline)return false;pending=false;return true;}
    void cancel(){pending=false;}
    boolean pending(){return pending;}
}
