package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class StockHomeRedirectPolicyTest {
    @Test public void matchesOnlyExactStockHome() {
        assertTrue(StockHomeRedirectPolicy.isHome(new StringBuilder(StockHomeRedirectPolicy.PACKAGE),
                new StringBuilder(StockHomeRedirectPolicy.HOME)));
    }
    @Test public void rejectsOtherPackagesEvenWithHomeClass() {
        assertFalse(StockHomeRedirectPolicy.isHome("com.netflix.ninja", StockHomeRedirectPolicy.HOME));
    }
    @Test public void rejectsSettingsAndGoogleSubScreens() {
        assertFalse(StockHomeRedirectPolicy.isHome(StockHomeRedirectPolicy.PACKAGE,
                StockHomeRedirectPolicy.PACKAGE + ".settings.SettingsActivity"));
        assertFalse(StockHomeRedirectPolicy.isHome(StockHomeRedirectPolicy.PACKAGE,
                StockHomeRedirectPolicy.PACKAGE + ".coreservices.bootmode.DispatchActivity"));
    }
    @Test public void rejectsMissingNames() {
        assertFalse(StockHomeRedirectPolicy.isHome(null, null));
        assertFalse(StockHomeRedirectPolicy.isHome(StockHomeRedirectPolicy.PACKAGE, null));
    }
    @Test public void firstBindingIsNotBlockedDuringEarlyBoot() {
        assertTrue(new StockHomeRedirectPolicy().allow(0));
    }
    @Test public void duplicatesAreCoalescedWithoutDroppingRapidHomePresses() {
        StockHomeRedirectPolicy policy = new StockHomeRedirectPolicy();
        assertTrue(policy.allow(1000));
        assertFalse(policy.allow(1000));
        assertTrue(policy.allow(1001));
        assertFalse(policy.allow(1001));
        assertTrue(policy.allow(1750));
        assertFalse(policy.allow(1750));
    }
    @Test public void negativeTimeDoesNotConsumeFirstAttempt() {
        StockHomeRedirectPolicy policy = new StockHomeRedirectPolicy();
        assertFalse(policy.allow(-1));
        assertTrue(policy.allow(0));
    }
    @Test public void clockResetDoesNotLockOutRedirect() {
        StockHomeRedirectPolicy policy = new StockHomeRedirectPolicy();
        assertTrue(policy.allow(5000));
        assertTrue(policy.allow(100));
        assertFalse(policy.allow(100));
        assertTrue(policy.allow(101));
    }
}
