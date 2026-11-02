package com.gathin.tvhome;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;
public class CinemaWorldTest {
    @Test public void savedShowcaseRequiresExactlyOneEnabledPairedWorld()throws Exception{
        CinemaSpec s=new CinemaSpec(new JSONObject().put("schema",1).put("worldsEnabled",true).put("worlds",new JSONArray().put(world("hogwarts",0)).put(world("hobbit",1))).toString());
        assertEquals("hogwarts",CinemaWorld.showcase(s,new PlaylistPolicy.Spec(true,900,false,List.of(token(0)))).id);
        assertNull(CinemaWorld.showcase(s,new PlaylistPolicy.Spec(true,900,false,List.of(token(0),token(1)))));
        assertNull(CinemaWorld.showcase(s,new PlaylistPolicy.Spec(false,900,false,List.of(token(0)))));
        assertNull(CinemaWorld.showcase(s,new PlaylistPolicy.Spec(true,900,false,List.of(token(9)))));
        assertNull(CinemaWorld.showcase(new CinemaSpec(""),new PlaylistPolicy.Spec(true,900,false,List.of(token(0)))));
    }
    private String token(int n){return String.format(Locale.ROOT,"p-%08x-0000-0000-0000-000000000000",n);}
    private JSONObject world(String id,int n)throws Exception{return new JSONObject().put("id",id).put("wallpaper",token(n));}
    @Test public void upgradeDoesNotEnableWorlds(){CinemaSpec s=new CinemaSpec("");assertFalse(s.worldsEnabled);assertTrue(s.worlds.isEmpty());}
    @Test public void eightWorldsFollowOnlyTheirBoundBackgrounds()throws Exception{JSONArray rows=new JSONArray();for(int i=0;i<8;i++)rows.put(world(CinemaWorld.IDS[i],i));CinemaSpec s=new CinemaSpec(new JSONObject().put("schema",1).put("worldsEnabled",true).put("worlds",rows).toString());for(int i=0;i<8;i++)assertEquals(CinemaWorld.IDS[i],CinemaWorld.find(s,token(i)).id);assertNull(CinemaWorld.find(s,token(10)));assertNull(CinemaWorld.find(new CinemaSpec(new JSONObject(s.json.toString()).put("worldsEnabled",false).toString()),token(0)));}
    @Test public void artIsIncludedInExistingAssetValidation()throws Exception{String hash="a".repeat(64);JSONObject w=world("hogwarts",0).put("idleArt",hash).put("covers",new JSONArray().put(hash));CinemaSpec s=new CinemaSpec(new JSONObject().put("schema",1).put("worlds",new JSONArray().put(w)).toString());assertEquals(Set.of(hash),s.assets());}
    @Test public void duplicateBindingsUnknownWorldsAndExecutableDataAreRejected()throws Exception{for(JSONArray rows:new JSONArray[]{new JSONArray().put(world("hogwarts",0)).put(world("hobbit",0)),new JSONArray().put(world("hogwarts",0)).put(world("hogwarts",1)),new JSONArray().put(world("unknown",0)),new JSONArray().put(world("hogwarts",0).put("script","run")),new JSONArray().put(world("hogwarts",0).put("idleArt","../image"))})try{new CinemaSpec(new JSONObject().put("schema",1).put("worlds",rows).toString());fail();}catch(IllegalArgumentException expected){}}
    @Test public void worldsDoNotChangePlaylistOrHomeFields()throws Exception{CinemaSpec s=new CinemaSpec(new JSONObject().put("schema",1).put("worldsEnabled",true).put("worlds",new JSONArray().put(world("hogwarts",0))).toString());assertFalse(s.json.has(WallpaperPlaylistChoice.KEY));assertFalse(s.sceneBackground);assertFalse(s.autoOpen);}
    @Test public void movieTargetsCannotLeakBetweenWorldsOrGallery()throws Exception{
        CinemaWorld hp=new CinemaWorld(world("hogwarts",0)),hobbit=new CinemaWorld(world("hobbit",1));
        JSONObject title=new JSONObject().put("id",hp.targetId(0,0)).put("name","Owned edition").put("package","org.example.video").put("url","https://example.com/movie/owned");
        CinemaSpec spec=new CinemaSpec(new JSONObject().put("schema",1).put("titles",new JSONArray().put(title)).toString());
        assertNotNull(spec.worldTarget(hp,0,0,"org.example.video"));assertNull(spec.worldTarget(hp,0,1,"org.example.video"));assertNull(spec.worldTarget(hp,1,0,"org.example.video"));assertNull(spec.worldTarget(hobbit,0,0,"org.example.video"));assertNull(spec.worldTarget(hp,0,0,"org.example.other"));
    }
    @Test public void emptyLinksRemainAnExplicitAppOnlyFallback()throws Exception{
        CinemaWorld hp=new CinemaWorld(world("hogwarts",0));JSONObject title=new JSONObject().put("id",hp.targetId(0,0)).put("name","No link yet").put("package","org.example.video");CinemaSpec spec=new CinemaSpec(new JSONObject().put("schema",1).put("titles",new JSONArray().put(title)).toString());assertNull(spec.worldTarget(hp,0,0,"org.example.video"));
    }
    @Test public void nonexistentBookCannotAcquireAnotherTitlesLink()throws Exception{
        CinemaWorld hp=new CinemaWorld(world("hogwarts",0));for(int[] item:new int[][]{{0,-1},{0,8},{1,3},{2,0}})try{hp.targetId(item[0],item[1]);fail();}catch(IllegalArgumentException expected){}
    }
}
