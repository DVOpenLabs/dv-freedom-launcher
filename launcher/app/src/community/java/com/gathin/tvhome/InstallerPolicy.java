package com.gathin.tvhome;

import java.util.*;

/** Bounded desktop installer choices; never accepts commands, permissions or arbitrary preferences. */
final class InstallerPolicy {
    static final int VERSION=1;
    static Map<String,Object> choices(Map<String,?> input,Set<String> installed){
        SetupProposal.fields(input,"style","position","size","spacing","wallpaper","apps");
        if(input.size()!=6)throw new IllegalArgumentException("Incomplete Home choices");
        String style=one(input.get("style"),"classic","clean","glass","cinema");
        String position=one(input.get("position"),"top","middle","bottom");
        String size=one(input.get("size"),"small","medium","large");
        String gap=one(input.get("spacing"),"compact","comfortable","wide");
        one(input.get("wallpaper"),"keep","coast","dusk","forest");
        if(!(input.get("apps") instanceof List))throw new IllegalArgumentException("Choose apps");
        List<String> apps=new ArrayList<>();for(Object item:(List<?>)input.get("apps")){
            if(!(item instanceof String)||!LayoutDraft.packageName((String)item)||!installed.contains(item)||apps.contains(item))throw new IllegalArgumentException("An app is unavailable or repeated");apps.add((String)item);
        }
        if(apps.isEmpty()||apps.size()>8)throw new IllegalArgumentException("Choose one to eight apps");
        Map<String,Object> out=new HashMap<>();out.put(ExperiencePolicy.KEY,style.equals("classic")?"legacy":style);out.put(AppPlacement.KEY,position);
        out.put(IconSizePolicy.KEY,size);out.put(TileSpacing.CHOICE,gap.equals("compact")?0:gap.equals("wide")?24:12);
        out.put("columns",apps.size()<2?2:apps.size());out.put(HomePolish.HEADER,true);out.put(HomePolish.INK,0);return out;
    }
    static String one(Object value,String...options){if(!(value instanceof String)||!Arrays.asList(options).contains(value))throw new IllegalArgumentException("Unsupported Home choice");return (String)value;}
    private InstallerPolicy(){}
}
