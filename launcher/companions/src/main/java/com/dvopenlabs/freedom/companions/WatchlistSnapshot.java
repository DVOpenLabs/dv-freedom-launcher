package com.dvopenlabs.freedom.companions;
import org.json.*;
import java.util.Iterator;
/** Bounded local title identities only; never exports original catalogue records or credentials. */
final class WatchlistSnapshot {
    static JSONArray read(String raw){JSONArray result=new JSONArray();if(raw==null||raw.length()>262144)return result;try{JSONObject saved=new JSONObject(raw);Iterator<String> keys=saved.keys();int scanned=0;while(keys.hasNext()&&scanned++<100&&result.length()<100){String key=keys.next();JSONObject item=saved.optJSONObject(key);if(item==null)continue;try{String token=DiscoverStore.token(item),title=DiscoverStore.title(item);if(key.equals(token)&&token.matches("(movie|tv)/[1-9][0-9]{0,9}")&&!title.trim().isEmpty()&&title.length()<=160&&!title.matches("(?s).*[\\p{Cntrl}].*"))result.put(new JSONObject().put("token",token).put("title",title));}catch(RuntimeException invalid){/* Skip one invalid item, retain other bounded records. */}}}catch(JSONException invalid){return new JSONArray();}return result;}
    private WatchlistSnapshot(){}
}
