package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import static org.junit.Assert.*;
public class DiscoverStoreTest {
    @Test public void fixedApiCannotBeRepointedByATitle(){for(String s:new String[]{"https://evil.test/","movie/1/../../account","account","movie/0"})try{DiscoverStore.endpoint(s,Collections.emptyMap(),"example-key");fail(s);}catch(IllegalArgumentException expected){}}
    @Test public void queryIsEncodedNotParsedAsAnotherParameter(){String u=DiscoverStore.endpoint("search/multi",Collections.singletonMap("query","a&api_key=bad"),"owner-key");assertTrue(u.contains("a%26api_key%3Dbad"));assertTrue(u.endsWith("api_key=owner-key"));}
    @Test public void regionsAndKeysRejectInvalidInput(){assertTrue(DiscoverStore.region("US"));assertFalse(DiscoverStore.region("USA"));assertFalse(DiscoverStore.region("us"));assertFalse(DiscoverStore.key("bad\r\nheader"));assertFalse(DiscoverStore.key("x".repeat(2049)));}
    @Test public void titlesHaveOnlyMovieOrTvIdentifiers()throws Exception{assertEquals("tv/42",DiscoverStore.token(new JSONObject().put("id",42).put("media_type","tv")));try{DiscoverStore.token(new JSONObject().put("id",7).put("media_type","person"));fail();}catch(IllegalArgumentException expected){}}
}
