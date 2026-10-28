package com.gathin.tvhome;
import java.util.*;
/** Reorder a bounded copy; it cannot add, hide or remove an app. */
final class FavoriteOrderPolicy {
    static List<String> move(List<String> apps,int from,int delta){if(apps==null||apps.isEmpty()||apps.size()>32||new HashSet<>(apps).size()!=apps.size()||from<0||from>=apps.size()||Math.abs(delta)!=1)throw new IllegalArgumentException("Invalid app move");List<String> copy=new ArrayList<>(apps);int to=Math.max(0,Math.min(copy.size()-1,from+delta));Collections.swap(copy,from,to);return copy;}
    private FavoriteOrderPolicy(){}
}
