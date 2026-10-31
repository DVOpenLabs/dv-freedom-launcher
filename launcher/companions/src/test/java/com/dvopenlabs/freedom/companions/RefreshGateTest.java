package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import static org.junit.Assert.*;
public class RefreshGateTest {
    @Test public void overlappingTriggersBecomeOneFollowUp() { RefreshGate g=new RefreshGate();int t=g.begin();assertEquals(-1,g.begin());assertEquals(-1,g.begin());assertTrue(g.finish(t));assertTrue(g.followUp());assertFalse(g.followUp());assertTrue(g.begin()>=0); }
    @Test public void oldConnectionCompletionCannotFinishCurrentRead() { RefreshGate g=new RefreshGate();int old=g.begin();g.cancel();int fresh=g.begin();assertFalse(g.current(old));assertFalse(g.finish(old));assertEquals(-1,g.begin());assertTrue(g.finish(fresh)); }
    @Test public void leavingThePanelDiscardsQueuedFollowUp() { RefreshGate g=new RefreshGate();int t=g.begin();g.begin();g.cancel();assertFalse(g.finish(t));assertFalse(g.followUp()); }
}
