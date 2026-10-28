package com.gathin.tvhome;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
/** Use the existing locale font override, including the Vietnamese system-font fallback. */
final class SettingsTypography {
    static Typeface regular(Context c){return resolve(c,false);}
    static Typeface heading(Context c){return resolve(c,true);}
    private static Typeface resolve(Context c,boolean heading){
        try{TypedValue value=new TypedValue();c.getResources().getValue(R.string.font_regular,value,true);
            if(value.resourceId!=0&&"font".equals(c.getResources().getResourceTypeName(value.resourceId)))return c.getResources().getFont(heading?R.font.freedom_semibold:value.resourceId);
            return Typeface.create(value.string==null?"sans-serif":value.string.toString(),heading?Typeface.BOLD:Typeface.NORMAL);
        }catch(android.content.res.Resources.NotFoundException missing){return heading?Typeface.DEFAULT_BOLD:Typeface.DEFAULT;}
    }
    private SettingsTypography(){}
}
