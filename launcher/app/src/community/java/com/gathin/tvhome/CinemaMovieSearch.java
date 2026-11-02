package com.gathin.tvhome;

import java.net.URLEncoder;

/** Public title search only. Never treats the first result as owned or auto-selects an offer. */
final class CinemaMovieSearch {
    static boolean youtube(String pkg){return "com.google.android.youtube.tv".equals(pkg);}
    static String url(CinemaWorld world,int page,int selected){
        world.targetId(page,selected);String title=(page==0?CinemaWorld.TITLES: CinemaWorld.OBJECTS)[world.index][selected];
        if(world.index==0&&page==0){
            if(selected==0)title="Sorcerer's Stone";
            if(selected==6)title="Deathly Hallows Part 1";
            if(selected==7)title="Deathly Hallows Part 2";
            title="Harry Potter and the "+title;
        }else if(world.index==1&&page==0)title="The Hobbit: "+title;
        try{return "https://www.youtube.com/results?search_query="+URLEncoder.encode(title+" movie","UTF-8").replace("+","%20");}
        catch(java.io.UnsupportedEncodingException impossible){throw new IllegalStateException(impossible);}
    }
}
