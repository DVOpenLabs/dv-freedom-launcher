package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ArtworkSpecTest {
    @Test public void emptyMeansOriginalArtwork(){assertTrue(ArtworkSpec.parse("").isEmpty());assertEquals("",ArtworkSpec.encode(Collections.emptyMap()));}
    @Test public void choicesAreDeterministicAndRoundTrip(){Map<String,String> values=new LinkedHashMap<>();values.put("com.example.z","icon");values.put("com.example.a","image:"+"a".repeat(64));String text=ArtworkSpec.encode(values);assertTrue(text.startsWith("v1\ncom.example.a="));assertEquals(values,ArtworkSpec.parse(text));}
    @Test public void refusesExecutablePathsAndMalformedMaps(){
        for(String text:new String[]{"v2\ncom.example.a=icon\n","v1\ncom.example.a=image:../../secret\n","v1\ncom.example.a=file:/private\n","v1\ncom.example.a=banner_tv\ncom.example.a=icon\n","v1\ncom.example.a=icon\n\ncom.example.b=icon\n","v1\n../app=icon\n","v1\ncom.example.a="+"x".repeat(17000)})try{ArtworkSpec.parse(text);fail(text.substring(0,Math.min(40,text.length())));}catch(IllegalArgumentException expected){}
    }
    @Test public void boundedMapDoesNotSilentlyDropAnOldChoice(){Map<String,String> values=new LinkedHashMap<>();for(int i=0;i<64;i++)values.put("com.example.app"+i,"icon");assertEquals(64,ArtworkSpec.parse(ArtworkSpec.encode(values)).size());values.put("com.example.extra","icon");try{ArtworkSpec.encode(values);fail();}catch(IllegalArgumentException expected){}}
    @Test public void namesAllowSixtyCharactersButNeverControls(){assertEquals("My app",ArtworkSpec.name(" My app "));assertEquals(60,ArtworkSpec.name("a".repeat(60)).length());for(String value:new String[]{" ","x".repeat(61),"Name\ncommand","Name\u0000"})try{ArtworkSpec.name(value);fail();}catch(IllegalArgumentException expected){}}
    @Test public void onlyLocalImmutableAssetTokensAreValid(){assertTrue(ArtworkSpec.token("icon"));assertTrue(ArtworkSpec.token("banner_tv"));assertTrue(ArtworkSpec.token("image:"+"0".repeat(64)));for(String value:new String[]{"https://remote/image.jpg","image:"+"A".repeat(64),"banner_../tv","image:123"})assertFalse(ArtworkSpec.token(value));}
    @Test public void originalNameRemovesTheOverrideAndRetainsOtherApps(){Map<String,String> values=new LinkedHashMap<>();values.put("com.example.a","Old name");values.put("com.example.b","Other name");Map<String,String> reset=ArtworkSpec.named(values,"com.example.a",null);assertFalse(reset.containsKey("com.example.a"));assertEquals("Other name",reset.get("com.example.b"));assertEquals("Old name",values.get("com.example.a"));assertEquals(reset,ArtworkSpec.named(reset,"com.example.a",null));}
}
