package com.gathin.tvhome;
import java.util.*;
/** Bounded process-local focus by app identity, isolated per profile/surface. No usage history. */
final class FocusMemory {
    private static final LinkedHashMap<String,String> recent=new LinkedHashMap<>();
    static void remember(String profile,String surface,String item){String key=profile+"\n"+surface;recent.remove(key);recent.put(key,item);while(recent.size()>32)recent.remove(recent.keySet().iterator().next());}
    static int index(String profile,String surface,List<String> items,int fallback){int at=items.indexOf(recent.get(profile+"\n"+surface));return at>=0?at:Math.max(0,Math.min(Math.max(0,items.size()-1),fallback));}
    private FocusMemory(){}
}
