package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import static org.junit.Assert.*;
public class PosterBackoffTest {
    @Test public void failureNotificationsCannotImmediatelyRetry(){PosterBackoff p=new PosterBackoff();assertTrue(p.ready("image",0));p.failed("image",0);for(int i=0;i<100;i++)assertFalse(p.ready("image",i));assertTrue(p.ready("image",300000));}
    @Test public void independentImagesAndExplicitClearRemainAvailable(){PosterBackoff p=new PosterBackoff();p.failed("first",0);assertTrue(p.ready("second",1));p.clear();assertTrue(p.ready("first",1));}
    @Test public void failedKeyStorageIsBounded(){PosterBackoff p=new PosterBackoff();for(int i=0;i<129;i++)p.failed("image"+i,0);assertTrue(p.ready("image0",1));assertFalse(p.ready("image128",1));}
}
