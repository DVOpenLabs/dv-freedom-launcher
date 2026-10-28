package com.gathin.tvhome;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
/** Use the existing locale font override, including the Vietnamese system-font fallback. */
final class SettingsTypography {
    static Typeface regular(Context c){return resolve(c,400);}
    static Typeface medium(Context c){return resolve(c,500);}
    static Typeface heading(Context c){return resolve(c,600);}
    private static Typeface resolve(Context c,int weight){
        try{TypedValue value=new TypedValue();c.getResources().getValue(R.string.font_regular,value,true);
            if(value.resourceId!=0&&"font".equals(c.getResources().getResourceTypeName(value.resourceId)))return c.getResources().getFont(weight==600?R.font.freedom_semibold:weight==500?R.font.outfit_medium:value.resourceId);
            return Typeface.create(value.string==null?"sans-serif":value.string.toString(),weight==600?Typeface.BOLD:Typeface.NORMAL);
        }catch(android.content.res.Resources.NotFoundException missing){return weight==600?Typeface.DEFAULT_BOLD:Typeface.DEFAULT;}
    }
    private SettingsTypography(){}
}
