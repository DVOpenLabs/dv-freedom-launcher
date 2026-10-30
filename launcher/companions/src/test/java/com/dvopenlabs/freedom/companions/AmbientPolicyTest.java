package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class AmbientPolicyTest {
    @Test public void portraitPairsKeepFullImageInsideEachHalf(){assertTrue(AmbientPolicy.portrait(600,1000));assertFalse(AmbientPolicy.portrait(1920,1080));float[] b=AmbientPolicy.fit(600,1000,40,40,940,1000);assertTrue(b[0]>=40);assertTrue(b[1]>=40);assertTrue(b[2]<=940);assertTrue(b[3]<=1000);assertEquals(.6f,(b[2]-b[0])/(b[3]-b[1]),.0001f);}
    @Test public void refreshNeedsOptInSourceAndElapsedTime(){assertFalse(AmbientPolicy.refresh(false,true,2000000,0));assertFalse(AmbientPolicy.refresh(true,false,2000000,0));assertFalse(AmbientPolicy.refresh(true,true,1000,2000));assertFalse(AmbientPolicy.refresh(true,true,10000,9000));assertTrue(AmbientPolicy.refresh(true,true,2000000,100));}
    @Test public void intervalIsBoundedEvenForCorruptPreferences(){assertEquals(15000,AmbientPolicy.interval(-1));assertEquals(300000,AmbientPolicy.interval(200));}
    @Test public void artMustBePublicDomainAndStayOnMuseumImageHost()throws Exception{JSONObject o=new JSONObject().put("isPublicDomain",true).put("primaryImageSmall","https://images.metmuseum.org/CRDImages/ep/web-large/example.jpg");assertTrue(MuseumArt.image(o).startsWith("https://images.metmuseum.org/"));o.put("isPublicDomain",false);try{MuseumArt.image(o);fail();}catch(IllegalArgumentException expected){}o.put("isPublicDomain",true);for(String u:new String[]{"https://images.metmuseum.org.evil.test/CRDImages/a.jpg","http://images.metmuseum.org/CRDImages/a.jpg","https://user:pass@images.metmuseum.org/CRDImages/a.jpg","https://images.metmuseum.org/CRDImages/a.jpg?token=secret"}){o.put("primaryImageSmall",u);try{MuseumArt.image(o);fail(u);}catch(IllegalArgumentException expected){}}}
    @Test public void tagFilterDoesNotFetchOrMixAlbums()throws Exception{JSONObject x=new JSONObject("{\"tags\":[{\"value\":\"Family\"}]}");assertTrue(PhotoLibrary.matchesTag(x,"family"));assertFalse(PhotoLibrary.matchesTag(x,"Trips"));assertTrue(PhotoLibrary.matchesTag(x,""));}
}
