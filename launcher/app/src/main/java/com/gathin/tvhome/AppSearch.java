package com.gathin.tvhome;

import java.text.Normalizer;
import java.util.Locale;

/** Local label/package matching only. No service catalogue, history or network. */
final class AppSearch {
    static boolean matches(String label,String pkg,String query) {
        String haystack=normalize(label+" "+pkg);
        for(String word:normalize(query).trim().split("\\s+"))if(!haystack.contains(word))return false;
        return true;
    }
    private static String normalize(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
    }
    private AppSearch(){}
}
