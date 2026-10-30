package com.gathin.btconnect;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LinkTest {
    @Test public void android11MustNotRequestAnAndroid12Permission(){org.junit.Assert.assertFalse(Link.needsConnectPermission(30));assertTrue(Link.needsConnectPermission(31));assertTrue(Link.needsConnectPermission(34));}
    @Test public void unknownClassIsShown() {
        assertTrue(Link.isAudio(null));
    }
}
