package com.gathin.tvhome;
import java.util.*;
import org.json.*;
/** Bounded child-friendly launcher choices, not account or Android security policy. */
final class FamilyPolicy {
    static final String KEY="family_home_v1";
    final boolean enabled;final Set<String> allowed;
    FamilyPolicy(String raw){try{if(raw==null||raw.length()>16384)throw new IllegalArgumentException();if(raw.isEmpty()){enabled=false;allowed=Collections.emptySet();return;}JSONObject o=new JSONObject(raw);if(o.length()!=2||!o.has("enabled")||!o.has("allowed")||!(o.get("enabled") instanceof Boolean))throw new IllegalArgumentException();enabled=o.getBoolean("enabled");JSONArray a=o.getJSONArray("allowed");if(a.length()>128)throw new IllegalArgumentException();Set<String> list=new LinkedHashSet<>();for(int i=0;i<a.length();i++){String p=a.getString(i);if(!LayoutDraft.packageName(p)||!list.add(p))throw new IllegalArgumentException();}if(enabled&&list.isEmpty())throw new IllegalArgumentException("Choose at least one app");allowed=Collections.unmodifiableSet(list);}catch(Exception bad){throw new IllegalArgumentException("Invalid family Home choices");}}
    static String encode(boolean enabled,Collection<String> allowed){try{String raw=new JSONObject().put("enabled",enabled).put("allowed",new JSONArray(allowed)).toString();new FamilyPolicy(raw);return raw;}catch(Exception bad){throw new IllegalArgumentException("Choose one to 128 distinct apps");}}
    static boolean canSelect(boolean currentRestricted,boolean targetRestricted,boolean sameProfile,boolean parentApproved){return !currentRestricted||targetRestricted||sameProfile||parentApproved;}
    private FamilyPolicy(){enabled=false;allowed=Collections.emptySet();}
}
