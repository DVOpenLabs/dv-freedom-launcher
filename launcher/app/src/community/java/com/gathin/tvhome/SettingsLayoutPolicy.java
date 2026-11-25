package com.gathin.tvhome;
/** Reserve room for readable controls/help before decorative still illustrations. */
final class SettingsLayoutPolicy {
 static boolean previews(float heightDp,float fontScale){return heightDp>=500&&fontScale<=1.15f;}
 static boolean previews(float heightDp,float fontScale,int helpLength){return previews(heightDp,fontScale)&&helpLength<=180;}
 private SettingsLayoutPolicy(){}
}
