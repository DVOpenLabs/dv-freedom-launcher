package com.gathin.tvhome;

/** Vertical alignment only. Dimensions, item order and wallpapers stay owned by their existing settings. */
final class AppPlacement {
    static final String KEY="app_position";
    static final String[] IDS={"top","middle","bottom"};
    static boolean valid(String value){for(String id:IDS)if(id.equals(value))return true;return false;}
    static int padding(String value,int minimum,int maximum,int original){
        int end=Math.max(minimum,maximum);
        if("top".equals(value))return minimum;
        if("middle".equals(value))return minimum+(end-minimum)/2;
        if("bottom".equals(value))return end;
        return original;
    }
    private AppPlacement(){}
}
