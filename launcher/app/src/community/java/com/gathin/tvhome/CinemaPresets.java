package com.gathin.tvhome;
import org.json.*;
/** Film names only; no copyrighted posters, stream availability claims or copied scene assets. */
final class CinemaPresets {
    static String fantasy(String pkg)throws Exception{
        JSONObject o=new JSONObject().put("schema",1).put("collectionName","Middle-earth cinema").put("collectionStyle","books").put("scene","mist").put("glow",true).put("logos",true).put("iconStyle","gold");
        o.put("moods",new JSONArray().put("Lord of the Rings").put("The Hobbit"));
        String[] names={"The Fellowship of the Ring","The Two Towers","The Return of the King","An Unexpected Journey","The Desolation of Smaug","The Battle of the Five Armies"};JSONArray titles=new JSONArray();
        for(int i=0;i<names.length;i++)titles.put(new JSONObject().put("id","middle-earth-"+(i+1)).put("name",names[i]).put("package",pkg).put("accent",i<3?"#C6B276":"#8BC3A1").put("moods",new JSONArray().put(i<3?"Lord of the Rings":"The Hobbit")));
        o.put("titles",titles);return o.toString();
    }
}
