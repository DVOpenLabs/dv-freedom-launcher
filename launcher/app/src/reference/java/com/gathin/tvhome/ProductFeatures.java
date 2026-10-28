package com.gathin.tvhome;

/** Reference build extension points; R8 removes these no-ops and their calls. */
final class ProductFeatures {
    static void init(android.content.Context c){}
    static int tileWidth(android.content.Context c,int width){return width;}
    static boolean settingsAccess(android.app.Activity a){return true;}
    static boolean allowEdit(android.app.Activity a,Runnable after){return true;}
    static boolean layoutEditable(android.content.Context c){return true;}
    static android.os.Bundle setupOperation(android.content.Context c,String method,String arg)throws Exception{return null;}
    static void requireLayoutEditable(android.content.Context c){}
    static boolean profilePicker(android.app.Activity a,boolean allowImport){return false;}
    static void beforeChange(String reason){}
    static void beforeChange(android.content.Context c,String reason){}
    static void resumed(android.app.Activity a){}
    static boolean routeHome(MainActivity a){return false;}
    static boolean homeAction(MainActivity a,android.content.Intent i){return false;}
    static boolean playlistEnabled(android.content.Context c){return false;}
    static void profileKeys(java.util.Set<String> keys){}
    static void setupKeys(java.util.Set<String> keys){}
    static boolean validateValue(String key,Object value){return false;}
    static void validateMedia(android.content.Context c,java.util.Map<String,Object> values)throws java.io.IOException{}
    static void clearProfile(android.content.Context c,String id){}
    static boolean themeInUse(android.content.Context c,String token){return false;}
    static String label(android.content.Context c,String pkg,String fallback){return fallback;}
    static android.graphics.drawable.Drawable banner(android.content.Context c,String pkg){return null;}
    static void styleName(android.widget.TextView name){}
    static void tileActions(android.app.Activity a,android.widget.LinearLayout rows,android.widget.PopupWindow popup,String pkg){}
    static boolean activityResult(android.app.Activity a,int request,int result,android.content.Intent data){return false;}
}
