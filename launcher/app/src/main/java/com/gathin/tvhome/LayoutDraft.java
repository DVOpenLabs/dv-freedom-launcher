package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The desktop editor's deliberately narrow app-only contract, independent of Android. */
final class LayoutDraft {
    final List<String> favorites, hidden;
    final Map<String,String> labels;
    LayoutDraft(List<String> favorites,List<String> hidden,Map<String,String> labels) {
        check(favorites!=null && !favorites.isEmpty() && favorites.size()<=32,"Choose 1–32 favorites");
        check(hidden!=null && hidden.size()<=256,"Too many hidden apps");
        check(labels!=null && labels.size()<=64,"Too many app names");
        validatePackages(favorites);validatePackages(hidden);
        check(Collections.disjoint(favorites,hidden),"A favorite cannot also be hidden");
        for(Map.Entry<String,String> entry:labels.entrySet()) {
            check(packageName(entry.getKey()),"Invalid app package");
            String name=entry.getValue();
            check(name!=null && !name.trim().isEmpty() && name.length()<=60,"App names must be 1–60 characters");
            for(int i=0;i<name.length();i++)check(!Character.isISOControl(name.charAt(i)),"Control character in app name");
        }
        this.favorites=Collections.unmodifiableList(new ArrayList<>(favorites));
        this.hidden=Collections.unmodifiableList(new ArrayList<>(hidden));
        this.labels=Collections.unmodifiableMap(new LinkedHashMap<>(labels));
    }
    static boolean packageName(String value) {
        return value!=null && value.length()<=200 && value.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }
    static void validatePackages(List<String> values) {
        Set<String> seen=new HashSet<>();
        for(String value:values)check(packageName(value)&&seen.add(value),"Invalid or repeated app package");
    }
    static void requireCurrent(String expected,String actual,String expectedProfile,String actualProfile) {
        check(expected!=null && expected.matches("[a-f0-9]{64}") && expected.equals(actual)
                && expectedProfile!=null && expectedProfile.equals(actualProfile),
                "The launcher changed. Reload and review your draft again.");
    }
    static void requireEditable(boolean reordering,boolean previewing) {
        check(!reordering&&!previewing,"Finish the on-TV layout edit before using the desktop editor.");
    }
    /** A desktop favorite move takes precedence; native order still sorts the remaining apps. */
    static int compareOrder(boolean desktop,List<String> favorites,Map<String,Integer> ranks,String pa,String ka,String pb,String kb) {
        int favorite=favoriteOrder(favorites,pa,pb);
        if(desktop&&favorite!=0)return favorite;
        int saved=Order.compare(ranks,ka,kb);
        return saved!=0?saved:favorite;
    }
    private static int favoriteOrder(List<String> favorites,String a,String b) {
        int fa=favorites.indexOf(a),fb=favorites.indexOf(b);
        if(fa<0&&fb<0)return 0;
        if(fa<0)return 1;
        if(fb<0)return -1;
        return Integer.compare(fa,fb);
    }
    static void check(boolean value,String message) {if(!value)throw new IllegalArgumentException(message);}
}
