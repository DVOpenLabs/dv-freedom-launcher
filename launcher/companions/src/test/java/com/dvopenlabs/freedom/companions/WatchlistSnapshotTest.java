package com.dvopenlabs.freedom.companions;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WatchlistSnapshotTest {
    @Test public void sharesOnlyIdentityAndName()throws Exception{JSONObject item=new JSONObject().put("id",12).put("media_type","movie").put("title","A story").put("overview","Private original record").put("key","never export");JSONArray result=WatchlistSnapshot.read(new JSONObject().put("movie/12",item).toString());assertEquals(1,result.length());assertEquals(2,result.getJSONObject(0).length());assertEquals("movie/12",result.getJSONObject(0).getString("token"));assertFalse(result.toString().contains("never export"));}
    @Test public void malformedRecordsCannotPoisonOtherTitles()throws Exception{JSONObject saved=new JSONObject().put("movie/1",new JSONObject().put("media_type","bad").put("id",1)).put("movie/2",new JSONObject().put("media_type","movie").put("id",2).put("title","Valid"));assertEquals(1,WatchlistSnapshot.read(saved.toString()).length());assertEquals(0,WatchlistSnapshot.read("x".repeat(262145)).length());assertEquals(0,WatchlistSnapshot.read("not json").length());}
}
