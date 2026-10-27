package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/** Bounded package-only MRU. No timestamps, usage access, playback state or app contents. */
final class RecentHistory {
    static final int LIMIT=6;
    static String scope(String profile){
        if(profile==null||!profile.isEmpty()&&!LocalProfiles.validId(profile))throw new IllegalArgumentException("Invalid profile");
        return "recent_apps."+profile;
    }
    static List<String> read(String text){
        List<String> result=new ArrayList<>();
        if(text==null||text.length()>1300)return result;
        for(String pkg:text.split("\n")){
            if(!LayoutDraft.packageName(pkg)||result.contains(pkg))continue;
            result.add(pkg);if(result.size()==LIMIT)break;
        }
        return result;
    }
    static String opened(String text,String pkg){
        if(!LayoutDraft.packageName(pkg))throw new IllegalArgumentException("Invalid app");
        List<String> values=read(text);values.remove(pkg);values.add(0,pkg);
        return String.join("\n",values.subList(0,Math.min(LIMIT,values.size())));
    }
    private RecentHistory(){}
}
