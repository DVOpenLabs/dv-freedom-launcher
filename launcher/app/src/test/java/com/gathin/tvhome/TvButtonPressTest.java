package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class TvButtonPressTest {
    private final TvButtonPress gate = new TvButtonPress();
    private TvButtonPress.Result event(boolean down, int repeats, boolean cancel, long time, String choice) {
        return gate.update("device:scan", down, repeats, cancel, time, choice);
    }
    @Test public void completePressLaunchesOnceAcrossThreeReceivers() {
        for (int i=0;i<3;i++) assertNull(event(true,0,false,100,"inputs").launch);
        assertEquals("inputs",event(false,0,false,200,"inputs").launch);
        for (int i=0;i<2;i++) {TvButtonPress.Result duplicate=event(false,0,false,200,"inputs");assertTrue(duplicate.consume);assertNull(duplicate.launch);}
    }
    @Test public void repeatsNeverLaunchOrRearm() {
        event(true,0,false,100,"live");
        for(int i=1;i<10;i++) assertNull(event(true,i,false,100+i*100,"live").launch);
        assertEquals("live",event(false,0,false,1100,"live").launch);
        assertNull(event(false,0,false,1200,"live").launch);
    }
    @Test public void cancellationAtEitherEndSuppressesLaunch() {
        event(true,0,false,100,"inputs");assertNull(event(false,0,true,200,"inputs").launch);
        event(true,0,true,300,"inputs");assertNull(event(false,0,false,400,"inputs").launch);
    }
    @Test public void orphanReleasesDoNotLaunchWhenHelperStartsMidPress() {
        TvButtonPress.Result result=event(false,0,false,200,"inputs");assertTrue(result.consume);assertNull(result.launch);
        event(true,3,false,300,"inputs");assertNull(event(false,0,false,400,"inputs").launch);
    }
    @Test public void followAndInvalidChoicesPassThroughNormally() {
        for(String choice:new String[]{"follow","bad","app:org.example/.Activity"}) {
            long time=choice.length()*1000L;
            TvButtonPress fresh=new TvButtonPress();
            assertFalse(fresh.update("remote",true,0,false,time,choice).consume);
            TvButtonPress.Result up=fresh.update("remote",false,0,false,time+50,choice);
            assertFalse(up.consume);assertNull(up.launch);
        }
    }
    @Test public void choiceChangeMidPressCancelsRatherThanLaunchingNewDestination() {
        event(true,0,false,100,"inputs");assertNull(event(false,0,false,200,"live").launch);
        event(true,0,false,300,"live");TvButtonPress.Result followed=event(false,0,false,400,"follow");
        assertTrue(followed.consume);assertNull(followed.launch);
        assertFalse(event(true,0,false,500,"follow").consume);assertFalse(event(false,0,false,600,"follow").consume);
    }
    @Test public void independentDevicesAndScancodesKeepSeparatePresses() {
        for(String button:new String[]{"1:10","2:10","1:11"}) gate.update(button,true,0,false,100,"live");
        for(String button:new String[]{"1:10","2:10","1:11"}) assertEquals("live",gate.update(button,false,0,false,200,"live").launch);
    }
    @Test public void delayedOldEventsCannotCancelOrRearmNewPress() {
        event(true,0,false,100,"inputs");event(false,0,false,200,"inputs");
        event(true,0,false,300,"inputs");assertNull(event(false,0,false,250,"inputs").launch);
        assertEquals("inputs",event(false,0,false,400,"inputs").launch);
        event(true,0,false,350,"inputs");assertNull(event(false,0,false,500,"inputs").launch);
    }
    @Test public void lostReleaseCanBeReplacedByNewPhysicalPress() {
        event(true,0,false,100,"inputs");event(true,0,false,300,"live");
        assertEquals("live",event(false,0,false,400,"live").launch);
    }
    @Test public void retainedRemoteStateIsBounded() {
        for(int i=0;i<33;i++) gate.update("remote"+i,true,0,false,100,"inputs");
        assertNull(gate.update("remote0",false,0,false,200,"inputs").launch);
        assertEquals("inputs",gate.update("remote32",false,0,false,200,"inputs").launch);
    }
}
