// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.json.*;

/** Fixed operation schema shared with desktop/native Setup. No user-supplied commands or package targets. */
final class SetupPolicy {
    static final String PACKAGE="com.dvopenlabs.freedomlauncher";
    static final int MAX_APK=40*1024*1024;
    static String host(String s){if(s==null||!s.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IllegalArgumentException("Use the IPv4 address shown by the TV");String[] p=s.split("\\.");for(String n:p)if(Integer.parseInt(n)>255||n.length()>1&&n.startsWith("0"))throw new IllegalArgumentException("Invalid TV address");int a=Integer.parseInt(p[0]),b=Integer.parseInt(p[1]);if(!(a==10||a==192&&b==168||a==172&&b>=16&&b<=31))throw new IllegalArgumentException("Use a private home-network TV address");return s;}
    static int port(String value){try{int p=Integer.parseInt(value);if(p<1||p>65535)throw new IllegalArgumentException();return p;}catch(Exception bad){throw new IllegalArgumentException("Port must be 1 to 65535");}}
    static String component(String s){
        if(s==null||!s.matches("[A-Za-z][\\w.]*/[A-Za-z.][\\w.$]*"))throw new IllegalArgumentException("Select a stable original Home on your TV first");
        String pkg=s.substring(0,s.indexOf('/')),activity=s.substring(s.indexOf('/')+1),lower=s.toLowerCase(Locale.ROOT);
        if(Arrays.asList("android","com.google.android.tungsten.setupwraith","com.android.settings","com.android.tv.settings","com.android.provision").contains(pkg)
                ||lower.matches(".*(factory|hkcfac|resolveractivity|chooseractivity|fallbackhome).*")
                ||activity.substring(activity.lastIndexOf('.')+1).equalsIgnoreCase("RecoveryActivity"))throw new IllegalArgumentException("Select a working original Home, not a setup or recovery screen");
        return s;
    }
    static String sha(byte[] bytes)throws Exception{StringBuilder b=new StringBuilder();for(byte x:MessageDigest.getInstance("SHA-256").digest(bytes))b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
    static String quote(String value){return "'"+value.replace("'","'\\''")+"'";}
    static void fields(JSONObject o,String... fields){Set<String> expected=new HashSet<>(Arrays.asList(fields));if(o.length()!=expected.size())throw new IllegalArgumentException("Unexpected record fields");Iterator<String> k=o.keys();while(k.hasNext())if(!expected.contains(k.next()))throw new IllegalArgumentException("Unexpected record fields");}
    static String canonical(Object v)throws Exception{
        if(v==null||v==JSONObject.NULL)return "null";
        if(v instanceof JSONObject){JSONObject o=(JSONObject)v;List<String> keys=new ArrayList<>();o.keys().forEachRemaining(keys::add);Collections.sort(keys);StringJoiner j=new StringJoiner(",","{","}");for(String k:keys)j.add(canonical(k)+":"+canonical(o.get(k)));return j.toString();}
        if(v instanceof JSONArray){JSONArray a=(JSONArray)v;StringJoiner j=new StringJoiner(",","[","]");for(int n=0;n<a.length();n++)j.add(canonical(a.get(n)));return j.toString();}
        if(v instanceof String){StringBuilder b=new StringBuilder("\"");for(char c:((String)v).toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\b':b.append("\\b");break;case '\f':b.append("\\f");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32||c>=127)b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
        if(v instanceof Boolean||v instanceof Integer||v instanceof Long)return v.toString();throw new IllegalArgumentException("Unsupported signed value");
    }
    static JSONObject choices(String style,String position,String size,String spacing,List<String> apps)throws Exception{
        if(!Arrays.asList("classic","clean","glass","cinema").contains(style)||!Arrays.asList("top","middle","bottom").contains(position)||!Arrays.asList("small","medium","large").contains(size)||!Arrays.asList("compact","comfortable","wide").contains(spacing)||apps.isEmpty()||apps.size()>8||new HashSet<>(apps).size()!=apps.size())throw new IllegalArgumentException("Choose one to eight installed apps and supported appearance options");
        for(String p:apps)if(!p.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new IllegalArgumentException("Invalid app");
        return new JSONObject().put("style",style).put("position",position).put("size",size).put("spacing",spacing).put("wallpaper","keep").put("apps",new JSONArray(apps));
    }
    private SetupPolicy(){}
}
