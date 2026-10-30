package com.gathin.tvhome;

import java.util.*;
import java.util.regex.*;

/** Offline intent vocabulary for setup. Unknown requests are explained; no model or commands are executed. */
final class LocalSetupCopilot {
    static Map<String,Object> interpret(String text){
        if(text==null||text.length()>500)throw new IllegalArgumentException("Use a short setup request");String t=text.toLowerCase(Locale.ROOT);Map<String,Object> out=new LinkedHashMap<>();
        int positions=0;for(String pattern:new String[]{"bottom|lower","top|upper","middle|center"})if(Pattern.compile("\\b("+pattern+")\\b").matcher(t).find())positions++;
        if(positions>1||t.contains("dark text")&&t.contains("light text")||t.contains("small icon")&&t.contains("large icon")||t.contains("more spacing")&&t.contains("less spacing"))throw new IllegalArgumentException("Choose one value for each setting; the request contains conflicting choices.");

        if(Pattern.compile("\\b(bottom|lower)\\b").matcher(t).find())out.put("position","bottom");else if(Pattern.compile("\\b(top|upper)\\b").matcher(t).find())out.put("position","top");else if(t.contains("middle")||t.contains("center"))out.put("position","middle");
        if(t.contains("larger label")||t.contains("larger text")||t.contains("bigger text")||t.contains("readable"))out.put("textSize",120);else if(t.contains("smaller text"))out.put("textSize",85);
        if(t.contains("large icon")||t.contains("big icon")||t.contains("bigger app"))out.put("iconSize","large");else if(t.contains("small icon")||t.contains("smaller app"))out.put("iconSize","small");else if(t.contains("medium icon"))out.put("iconSize","medium");
        if(t.contains("cinema")||t.contains("movie night"))out.put("experience","cinema");else if(t.contains("glass")||t.contains("apple style")||t.contains("apple tv"))out.put("experience","glass");else if(t.contains("clean")||t.contains("minimal"))out.put("experience","clean");
        if(t.contains("strong focus")||t.contains("clear selection")||t.contains("strong outline"))out.put("strongFocus",true);
        if(t.contains("more spacing")||t.contains("more space"))out.put("spacing",16);else if(t.contains("less spacing"))out.put("spacing",4);
        if(t.contains("dark text")||t.contains("dark label"))out.put("text","dark");else if(t.contains("light text")||t.contains("white text"))out.put("text","light");else if(t.contains("automatic text"))out.put("text","auto");
        if(t.contains("hide names"))out.put("names","hidden");else if(t.contains("selected name"))out.put("names","selected");else if(t.contains("show names")||t.contains("all names"))out.put("names","all");
        if(out.isEmpty())throw new IllegalArgumentException("Try “larger labels, apps at the bottom” or “glass, small icons, more spacing”. This offline helper handles appearance only. For another request, use guided setup or import an AI draft.");
        if(t.matches("(?s).*\\b(install|uninstall|disable|grant|password|token|adb|shell|delete|reset|reboot)\\b.*"))throw new IllegalArgumentException("This helper only prepares Home appearance drafts. Device, permission and app-management requests are excluded.");return out;
    }
    private LocalSetupCopilot(){}
}
