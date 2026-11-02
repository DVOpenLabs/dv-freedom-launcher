package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class CinemaSpecTest {
    private String title(String id,String pkg){return "{\"id\":\""+id+"\",\"name\":\"A title\",\"package\":\""+pkg+"\",\"moods\":[\"Comfort\"]}";}
    @Test public void defaultsAreOfflineAndUnchanged(){CinemaSpec s=new CinemaSpec("");assertFalse(s.logos);assertFalse(s.autoOpen);assertEquals("off",s.scene);assertTrue(s.assets().isEmpty());}
    @Test public void poolSkipsUnavailableAndAvoidsLastWhenPossible(){CinemaSpec s=new CinemaSpec("{\"schema\":1,\"moods\":[\"Comfort\"],\"titles\":["+title("a","org.example.a")+","+title("b","org.example.b")+","+title("c","org.example.a")+"]}");assertEquals(1,s.pool("Comfort",Set.of("org.example.a"),"a").size());assertEquals("c",s.pool("Anything",Set.of("org.example.a"),"a").get(0).id);assertTrue(s.pool("Unknown",Set.of("org.example.a"),null).isEmpty());}
    @Test public void singleTitleCanRepeat(){CinemaSpec s=new CinemaSpec("{\"schema\":1,\"moods\":[\"Comfort\"],\"titles\":["+title("a","org.example.a")+"]}");assertEquals(1,s.pool("Anything",Set.of("org.example.a"),"a").size());}
    @Test public void rejectsExecutableFieldsAndAssets(){for(String raw:new String[]{"{\"schema\":1,\"script\":\"run\"}","{\"schema\":1,\"scene\":\"javascript:alert(1)\"}","{\"schema\":1,\"appLogos\":{\"org.example.app\":\"../asset\"}}","{\"schema\":1,\"logos\":\"true\"}","{\"schema\":2}"}){try{new CinemaSpec(raw);fail(raw);}catch(IllegalArgumentException expected){}}}
    @Test public void rejectsCredentialAndUnsafeLinks()throws Exception{for(String url:new String[]{"http://example.com","intent://launch","https://user:pass@example.com/a","https://example.com:444/a"}){try{CinemaSpec.link(url);fail(url);}catch(Exception expected){}}CinemaSpec.link("https://example.com/title/123");}
    @Test public void rejectsUnknownMoodAndDuplicateTitles(){for(String raw:new String[]{"{\"schema\":1,\"titles\":["+title("a","org.example.a")+"]}","{\"schema\":1,\"moods\":[\"Comfort\",\"comfort\"]}","{\"schema\":1,\"moods\":[\"Comfort\"],\"titles\":["+title("a","org.example.a")+","+title("a","org.example.a")+"]}"}){try{new CinemaSpec(raw);fail(raw);}catch(IllegalArgumentException expected){}}}
    @Test public void largerCollectionsUseTheBoundedExtendedJsonBudget(){StringBuilder raw=new StringBuilder("{\"schema\":1,\"moods\":[\"Comfort\"],\"titles\":[");for(int i=0;i<48;i++){if(i>0)raw.append(',');raw.append(title("film-"+i,"org.example.a"));}raw.append("]}");assertEquals(48,new CinemaSpec(raw.toString()).titles.size());}
    @Test public void rejectsDuplicateKeysBeforeParsing(){try{new CinemaSpec("{\"schema\":1,\"scene\":\"off\",\"scene\":\"mist\"}");fail();}catch(IllegalArgumentException expected){}}
    @Test public void recreatedDraftCannotRebaseOverAnotherHomeRevision(){assertTrue(CinemaSpec.canRestoreDraft("","abc","","abc"));assertFalse(CinemaSpec.canRestoreDraft("","abc","","changed"));assertFalse(CinemaSpec.canRestoreDraft("old","abc","new","abc"));assertFalse(CinemaSpec.canRestoreDraft("",null,"","abc"));}
}
