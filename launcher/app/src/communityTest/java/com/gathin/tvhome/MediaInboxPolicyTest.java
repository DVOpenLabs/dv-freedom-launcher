package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class MediaInboxPolicyTest {
    @Test public void fileBoundsAndOpaqueNamesPreventPathsOrUnboundedMedia(){assertTrue(MediaInboxPolicy.id("a".repeat(32)));assertFalse(MediaInboxPolicy.id("../private"));assertTrue(MediaInboxPolicy.bytes("mp4",MediaInboxPolicy.MAX_VIDEO));assertFalse(MediaInboxPolicy.bytes("mp4",MediaInboxPolicy.MAX_VIDEO+1));assertFalse(MediaInboxPolicy.bytes("zip",100));assertFalse(MediaInboxPolicy.bytes("jpg",MediaInboxPolicy.MAX_IMAGE+1));assertThrows(IllegalArgumentException.class,()->MediaInboxPolicy.name("../art.png"));assertThrows(IllegalArgumentException.class,()->MediaInboxPolicy.name("bad\nname"));}
    @Test public void expiredOrFutureReceiptsCannotBeReviewed(){long now=2_000_000_000_000L;assertTrue(MediaInboxPolicy.fresh(now-1000,now));assertFalse(MediaInboxPolicy.fresh(now-MediaInboxPolicy.TTL-1,now));assertFalse(MediaInboxPolicy.fresh(now+60_001,now));}
    @Test public void imageAndVideoLimitsAreIndependent(){assertTrue(MediaInboxPolicy.picture(6000,4000));assertFalse(MediaInboxPolicy.picture(16000,16000));assertTrue(MediaInboxPolicy.video(3840,2160,8000));assertFalse(MediaInboxPolicy.video(3840,2160,60_001));assertFalse(MediaInboxPolicy.video(7680,4320,8000));assertFalse(MediaInboxPolicy.video(1920,1080,0));}
}
