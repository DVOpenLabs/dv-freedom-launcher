package com.gathin.tvhome;
import org.json.*;
/** Per-wake intent, independent of the once-per-boot startup marker. */
final class WakeRulePolicy {
    static final String KEY="wake_app_v1";
    final String target;final int delay;
    WakeRulePolicy(String raw){try{if(raw==null||raw.length()>512)throw new IllegalArgumentException();if(raw.isEmpty()){target="";delay=1;return;}JSONObject o=new JSONObject(raw);if(o.length()!=2||!(o.get("target") instanceof String)||!(o.get("delay") instanceof Integer))throw new IllegalArgumentException();target=o.getString("target");delay=o.getInt("delay");if(!target.isEmpty()&&!LayoutDraft.packageName(target)||delay<0||delay>3)throw new IllegalArgumentException();}catch(Exception bad){throw new IllegalArgumentException("Invalid wake rule");}}
    static String encode(String target,int delay){try{String raw=new JSONObject().put("target",target).put("delay",delay).toString();new WakeRulePolicy(raw);return raw;}catch(Exception bad){throw new IllegalArgumentException("Invalid wake rule");}}
    static boolean due(boolean armed,long now,long expiry,boolean visible,boolean previewing,boolean allowed){return armed&&now<expiry&&visible&&!previewing&&allowed;}
    private WakeRulePolicy(){target="";delay=1;}
}
