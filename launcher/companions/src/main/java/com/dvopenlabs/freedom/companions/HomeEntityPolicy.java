package com.dvopenlabs.freedom.companions;
import org.json.JSONObject;
/** Small HA control allowlist; sensors, climate and covers are deliberately informational. */
final class HomeEntityPolicy {
    static boolean supported(String id){return id!=null&&id.matches("(scene|light|switch|fan|sensor|binary_sensor|climate|cover)\\.[a-z0-9_]{1,100}");}
    static boolean actionable(String id){return id!=null&&id.matches("(scene|light|switch|fan)\\.[a-z0-9_]{1,100}");}
    static String summary(JSONObject e){JSONObject a=e.optJSONObject("attributes");String name=clean(a==null?e.optString("entity_id"):a.optString("friendly_name",e.optString("entity_id")),100),state=clean(e.optString("state"),80),unit=a==null?"":clean(a.optString("unit_of_measurement"),20);String value=name+" · "+state+(unit.isEmpty()?"":" "+unit);if(a!=null&&e.optString("entity_id").startsWith("climate.")){if(a.has("current_temperature"))value+=" · current "+clean(a.optString("current_temperature"),16);if(a.has("temperature"))value+=" · target "+clean(a.optString("temperature"),16);if(!a.optString("temperature_unit").isEmpty())value+=" "+clean(a.optString("temperature_unit"),8);}if(a!=null&&e.optString("entity_id").startsWith("cover.")&&a.has("current_position"))value+=" · "+clean(a.optString("current_position"),8)+"% open";return value;}
    static String clean(String s,int length){String value=s.replaceAll("[\\p{Cntrl}]"," ");return value.substring(0,Math.min(length,value.length()));}
    private HomeEntityPolicy(){}
}
