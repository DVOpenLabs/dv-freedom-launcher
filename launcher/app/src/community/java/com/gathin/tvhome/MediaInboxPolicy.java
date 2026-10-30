package com.gathin.tvhome;

/** Bounds for shell-authorized transfers. Import is always a separate on-TV action. */
final class MediaInboxPolicy {
    static final long MAX_IMAGE=8L*1024*1024, MAX_VIDEO=48L*1024*1024, TTL=30*60_000L;
    static boolean id(String value){return value!=null&&value.matches("[0-9a-f]{32}");}
    static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    static boolean kind(String value){return "jpg".equals(value)||"png".equals(value)||"mp4".equals(value);}
    static boolean bytes(String kind,long bytes){return kind(kind)&&bytes>=12&&bytes<=("mp4".equals(kind)?MAX_VIDEO:MAX_IMAGE);}
    static boolean fresh(long created,long now){return created>0&&created<=now+60_000&&now-created<=TTL;}
    static boolean picture(int width,int height){return width>0&&height>0&&width<=16384&&height<=16384&&(long)width*height<=32_000_000;}
    static boolean video(int width,int height,long duration){return width>0&&height>0&&width<=3840&&height<=3840&&(long)width*height<=8_294_400&&duration>0&&duration<=60_000;}
    static String name(String value){
        if(value==null||value.trim().isEmpty()||value.length()>128)throw new IllegalArgumentException("Invalid file name");
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i))||value.charAt(i)=='/'||value.charAt(i)=='\\')throw new IllegalArgumentException("Invalid file name");
        return value.trim();
    }
    private MediaInboxPolicy(){}
}
