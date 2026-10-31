package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class DeferredHomePolicyTest {
    @Test public void homeWaitsForGraceAndFiresOnlyOnce() {
        DeferredHomePolicy p = new DeferredHomePolicy(); int t = p.window(StockHomeRedirectPolicy.PACKAGE,StockHomeRedirectPolicy.HOME,100);
        assertFalse(p.take(t,319)); assertTrue(p.take(t,320)); assertFalse(p.take(t,321));
    }
    @Test public void profileAndAssistantFollowingHomeCancelIt() {
        for(String next:new String[]{".profile.chooser.ProfileChooserActivity",".gemini.chat.ChatActivity",".entity.EntityActivity"}) {
            DeferredHomePolicy p = new DeferredHomePolicy(); int t = p.schedule(100);
            p.window(StockHomeRedirectPolicy.PACKAGE,StockHomeRedirectPolicy.PACKAGE+next,170);
            assertFalse(p.take(t,1000));
        }
    }
    @Test public void rapidNewHomeInvalidatesOnlyOlderPendingWork() {
        DeferredHomePolicy p = new DeferredHomePolicy(); int old=p.schedule(10), latest=p.schedule(20);
        assertFalse(p.take(old,1000)); assertTrue(p.take(latest,1000)); assertTrue(p.take(p.schedule(1001),1221));
    }
    @Test public void closedSessionCannotRedirect() { DeferredHomePolicy p=new DeferredHomePolicy();int t=p.schedule(0);p.cancel();assertFalse(p.take(t,1000)); }
}
