package com.dvopenlabs.freedom.companions;

/** Shared preview/dream geometry and bounded refresh policy; no platform or network side effects. */
final class AmbientPolicy {
    static boolean portrait(int w,int h){return w>0&&h>w*1.15f;}
    static float[] fit(int w,int h,float l,float t,float r,float b){if(w<1||h<1||r<=l||b<=t)throw new IllegalArgumentException();float scale=Math.min((r-l)/w,(b-t)/h),x=(l+r-w*scale)/2,y=(t+b-h*scale)/2;return new float[]{x,y,x+w*scale,y+h*scale};}
    static long interval(int seconds){return new int[]{15,30,60,300}[Math.max(0,Math.min(3,seconds))]*1000L;}
    static boolean refresh(boolean optedIn,boolean configured,long now,long last){return optedIn&&configured&&(last<=0||now-last>=30*60_000L)&&now>=last;}
    private AmbientPolicy(){}
}
