package com.gathin.tvhome;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded package-scoped links, never serialized Android intents, shell or grants. */
final class ShortcutPolicy {
    static final String KEY="app_links_v1";
    static String uri(String value){
        if(value==null||value.isEmpty()||value.length()>2048||!value.equals(value.trim()))throw new IllegalArgumentException("Enter an app link up to 2048 characters");
        for(int i=0;i<value.length();i++)if(Character.isWhitespace(value.charAt(i))||Character.isISOControl(value.charAt(i)))throw new IllegalArgumentException("Links cannot contain spaces or control characters");
        try{URI uri=new URI(value);String scheme=uri.getScheme();if(scheme==null||!scheme.matches("[A-Za-z][A-Za-z0-9+.-]{0,40}"))throw new Exception();scheme=scheme.toLowerCase(Locale.ROOT);
            if(Arrays.asList("intent","file","content","javascript","data","about","tel","mailto").contains(scheme)||uri.getRawUserInfo()!=null)throw new Exception();
            if((scheme.equals("https")||scheme.equals("http"))&&(uri.getHost()==null||uri.getHost().isEmpty()))throw new Exception();
            if(uri.getRawSchemeSpecificPart()==null||uri.getRawSchemeSpecificPart().isEmpty())throw new Exception();return value;
        }catch(Exception bad){throw new IllegalArgumentException("Use an https or app-specific link without embedded credentials");}
    }
    static Map<String,String> parse(String raw){
        if(raw==null||raw.length()>16384)throw new IllegalArgumentException("Shortcut list is too large");Map<String,String> out=new LinkedHashMap<>();
        if(!raw.isEmpty())for(String row:raw.split("\n",-1)){String[] fields=row.split("\t",-1);if(fields.length!=2||!LayoutDraft.packageName(fields[0])||out.containsKey(fields[0])||out.size()>=32)throw new IllegalArgumentException("Invalid app shortcuts");out.put(fields[0],uri(new String(Base64.getUrlDecoder().decode(fields[1]),StandardCharsets.UTF_8)));}
        return out;
    }
    static String encode(Map<String,String> values){List<String> rows=new ArrayList<>();for(String pkg:new TreeSet<>(values.keySet()))rows.add(pkg+"\t"+Base64.getUrlEncoder().withoutPadding().encodeToString(uri(values.get(pkg)).getBytes(StandardCharsets.UTF_8)));String raw=String.join("\n",rows);parse(raw);return raw;}
    private ShortcutPolicy(){}
}
