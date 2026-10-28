package com.gathin.tvhome;
/** Scale within the existing safe cell; the original theme is unchanged without an override. */
final class IconSizePolicy {
    static final String KEY="icon_size_v1";
    static final String[] IDS={"","small","medium","large"};
    static boolean valid(String value){for(String id:IDS)if(id.equals(value))return true;return false;}
    static int percent(String value){return "small".equals(value)?72:"medium".equals(value)?86:100;}
    static int width(int original,String value){return percent(value)==100?original:Math.max(1,Math.round(original*percent(value)/100f));}
    static String label(String value){return "small".equals(value)?"Small":"medium".equals(value)?"Medium":"large".equals(value)?"Large":"Current theme size";}
    private IconSizePolicy(){}
}
