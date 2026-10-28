package com.gathin.tvhome;

import java.util.*;

/** Shared bounds for three presentations of one Home state. No app, media or device mutations. */
final class ExperiencePolicy {
    static final String KEY="home_experience_v1";
    static final String LEGACY="legacy",GLASS="glass",CLEAN="clean",CINEMA="cinema";
    static final List<String> IDS=Collections.unmodifiableList(Arrays.asList(LEGACY,GLASS,CLEAN,CINEMA));
    static boolean valid(String id){return IDS.contains(id);}
    static String name(String id){if(GLASS.equals(id))return "Freedom Glass";if(CLEAN.equals(id))return "Freedom Clean";if(CINEMA.equals(id))return "Freedom Cinema";return "Current layout";}
    static String description(String id){
        if(GLASS.equals(id))return "A floating dock, compact controls and soft depth.";
        if(CLEAN.equals(id))return "Your apps clearly arranged, with the space and position you choose.";
        if(CINEMA.equals(id))return "Room for your wallpaper with a quiet, focused app ribbon.";
        return "Your existing grid or category layout, with its saved appearance.";
    }
    static String defaultPosition(String id){return CLEAN.equals(id)?"top":"bottom";}
    static final class Geometry {
        final float left,top,cardWidth,cardHeight,gap,labelTop;
        final int count;
        Geometry(String look,String position,int count){
            this(look,position,count,0);
        }
        Geometry(String look,String position,int count,int extraGap){
            if(!valid(look)||LEGACY.equals(look)||!Arrays.asList("top","middle","bottom").contains(position)||count<1||count>8)throw new IllegalArgumentException("Invalid experience geometry");
            if(!TileSpacing.valid(extraGap))throw new IllegalArgumentException("Invalid spacing");this.count=count;gap=(CLEAN.equals(look)?17:14)+extraGap;
            float available=CINEMA.equals(look)?806:852;
            cardWidth=Math.min(CINEMA.equals(look)?94:116,(available-(count-1)*gap)/count);
            cardHeight=cardWidth/1.67f;left=(960-count*cardWidth-(count-1)*gap)/2;
            top="top".equals(position)?154:"middle".equals(position)?(540-cardHeight-25)/2:540-48-cardHeight-25;
            labelTop=top+cardHeight+10;
        }
        float x(int index){if(index<0||index>=count)throw new IllegalArgumentException();return left+index*(cardWidth+gap);}
    }
    /** Choose solid ink from a stable poster region, retaining explicit light/dark preferences. */
    static int ink(int manualChoice,int[] region){return manualChoice==1?0xfff5f7fa:manualChoice==2?0xff182334:PolishPolicy.ink(region);}
    static int next(int selected,int direction,int count,boolean rtl){
        if(count<1||count>512)throw new IllegalArgumentException();int delta=rtl?-direction:direction;
        return Math.max(0,Math.min(count-1,selected+delta));
    }
    private ExperiencePolicy(){}
}
