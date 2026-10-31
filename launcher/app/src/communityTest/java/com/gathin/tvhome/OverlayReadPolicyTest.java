package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class OverlayReadPolicyTest {
    @Test public void unreadableEarlyStorageRetainsOnlyAnExistingCopy() {
        assertTrue(OverlayReadPolicy.keepCopy(false,true,30_000));
        assertFalse(OverlayReadPolicy.keepCopy(false,false,30_000));
        assertFalse(OverlayReadPolicy.keepCopy(true,true,30_000));
        assertFalse(OverlayReadPolicy.keepCopy(false,true,OverlayReadPolicy.EARLY_MS));
    }
    @Test public void resetAndUnavailableCopyCannotResurrectOldTheme() {
        assertFalse(OverlayReadPolicy.keepCopy(false,false,0));
        assertFalse(OverlayReadPolicy.keepCopy(false,true,-1));
    }
    @Test public void retriesHaveBothForegroundAndFixedDeadlineOwnership() {
        assertTrue(OverlayReadPolicy.retry(true,false,20,100));
        assertFalse(OverlayReadPolicy.retry(false,false,20,100));
        assertFalse(OverlayReadPolicy.retry(true,true,20,100));
        assertFalse(OverlayReadPolicy.retry(true,false,100,100));
        assertFalse(OverlayReadPolicy.retry(true,false,OverlayReadPolicy.EARLY_MS,Long.MAX_VALUE));
    }
}
