package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class WatchTonightPolicyTest {
    @Test public void acceptsOnlyBoundedTitleIdentities(){assertTrue(WatchTonightPolicy.valid("movie/123","A story"));assertTrue(WatchTonightPolicy.valid("tv/1","A series"));for(String token:new String[]{"movie/0","https://example.com","movie/1/extra","tv/-1","movie/99999999999"})assertFalse(WatchTonightPolicy.valid(token,"Title"));assertFalse(WatchTonightPolicy.valid("movie/1","\nInjected"));assertFalse(WatchTonightPolicy.valid("movie/1","x".repeat(161)));}
}
