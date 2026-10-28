package com.gathin.tvhome;

import java.util.*;

/** The shared guided/AI contract. No commands, permissions, URLs, credentials or device settings. */
final class SetupProposal {
    static final int MAX_BYTES=16384;
    static List<String> visibleDefaults(List<String> defaults,Set<String> installed){List<String> result=new ArrayList<>();for(String pkg:defaults)if(result.size()<32&&LayoutDraft.packageName(pkg)&&installed.contains(pkg)&&!result.contains(pkg))result.add(pkg);return result;}
    final String name;final Map<String,Object> values;final List<String> favorites;
    SetupProposal(Map<String,?> root,Set<String> installed){
        fields(root,"schema","name","settings","favorites");
        if(!Integer.valueOf(1).equals(root.get("schema")))throw new IllegalArgumentException("Unsupported setup format");
        Object title=root.get("name");name=SetupValues.name(title==null?"My Home":string(title));
        if(!(root.get("settings") instanceof Map))throw new IllegalArgumentException("Setup settings required");
        Map<?,?> s=(Map<?,?>)root.get("settings");fields(s,"experience","position","columns","text","textSize","names","strongFocus","idleSeconds");Map<String,Object> next=new LinkedHashMap<>();
        if(s.containsKey("experience")){String v=string(s.get("experience"));if(!ExperiencePolicy.valid(v))throw new IllegalArgumentException("Unknown Home experience");next.put(ExperiencePolicy.KEY,v);}
        if(s.containsKey("position")){String v=string(s.get("position"));if(!AppPlacement.valid(v))throw new IllegalArgumentException("Unknown app position");next.put(AppPlacement.KEY,v);}
        if(s.containsKey("columns"))next.put("columns",number(s.get("columns"),2,8));
        if(s.containsKey("text")){String v=string(s.get("text"));int at=Arrays.asList("auto","light","dark").indexOf(v);if(at<0)throw new IllegalArgumentException("Unknown text color");next.put(HomePolish.HEADER,true);next.put(HomePolish.INK,at);}
        if(s.containsKey("textSize")){int n=number(s.get("textSize"),85,120);if(!Arrays.asList(85,100,110,120).contains(n))throw new IllegalArgumentException("Unsupported text size");next.put(HomeAppearance.KEY,"v1,"+n+","+n+","+n);}
        if(s.containsKey("names")){int n=Arrays.asList("theme","all","selected","hidden").indexOf(string(s.get("names")));if(n<0)throw new IllegalArgumentException("Unknown app-name option");next.put(HomePolish.NAMES,n);}
        if(s.containsKey("strongFocus")){Object v=s.get("strongFocus");if(!(v instanceof Boolean))throw new IllegalArgumentException("Invalid focus option");next.put(TileSpacing.FOCUS,v);}
        if(s.containsKey("idleSeconds")){int n=number(s.get("idleSeconds"),0,120);if(!PolishPolicy.validSeconds(n))throw new IllegalArgumentException("Unsupported idle time");next.put(HomePolish.IDLE,n);}
        values=Collections.unmodifiableMap(next);
        if(root.containsKey("favorites")){
            if(!(root.get("favorites") instanceof List))throw new IllegalArgumentException("Favorite list required");List<String> apps=new ArrayList<>();
            for(Object v:(List<?>)root.get("favorites")){String p=string(v);if(!LayoutDraft.packageName(p)||!installed.contains(p)||apps.contains(p))throw new IllegalArgumentException("Unavailable or repeated favorite app");apps.add(p);}
            if(apps.isEmpty()||apps.size()>32)throw new IllegalArgumentException("Choose 1–32 favorite apps");favorites=Collections.unmodifiableList(apps);
        }else favorites=null;
        if(values.isEmpty()&&favorites==null)throw new IllegalArgumentException("The draft has no changes");
    }
    static String string(Object value){if(!(value instanceof String))throw new IllegalArgumentException("Invalid setup text");return (String)value;}
    static int number(Object v,int lo,int hi){if(!(v instanceof Integer)||((Integer)v)<lo||((Integer)v)>hi)throw new IllegalArgumentException("Invalid setup number");return (Integer)v;}
    static void fields(Map<?,?> values,String...keys){Set<String> allowed=new HashSet<>(Arrays.asList(keys));if(!allowed.containsAll(values.keySet()))throw new IllegalArgumentException("Unsupported setup field");}
}
