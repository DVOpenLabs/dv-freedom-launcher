package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeCapabilitiesTest {
    @Test public void newHomeIntentReusesOnlySameSavedRenderer(){assertTrue(HomeCapabilities.reuse(true,true,true));assertFalse(HomeCapabilities.reuse(true,false,true));assertFalse(HomeCapabilities.reuse(false,true,true));assertFalse(HomeCapabilities.reuse(true,true,false));}
    @Test public void rendererLimitsAreExplicit(){assertTrue(HomeCapabilities.inlineShelves("glass"));assertFalse(HomeCapabilities.inlineShelves("showcase"));assertFalse(HomeCapabilities.inlineShelves("legacy"));assertTrue(HomeCapabilities.summary("showcase").contains("one pinned world"));}
}
