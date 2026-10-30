package com.gathin.tvhome;

final class NowPlayingPolicy {
    static String text(CharSequence raw,int limit){
        if(raw==null)return "";StringBuilder text=new StringBuilder();
        for(int i=0;i<raw.length()&&text.length()<limit;i++){char c=raw.charAt(i);text.append(Character.isISOControl(c)?' ':c);}
        return text.toString().trim();
    }
    static int rank(int state){return state==3?3:state==6||state==8?2:state==2?1:0;}
    private NowPlayingPolicy(){}
}
