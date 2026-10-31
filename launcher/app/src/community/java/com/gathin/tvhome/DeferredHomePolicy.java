package com.gathin.tvhome;

/** Coalesce only a pending Home event; a subsequent Google sub-screen owns the window. */
final class DeferredHomePolicy {
    static final long GRACE_MS = 220;
    private int generation;
    private long due;
    private boolean pending;
    int schedule(long now) { pending = true; due = now + GRACE_MS; return ++generation; }
    int window(CharSequence pkg, CharSequence activity, long now) {
        if (!StockHomeRedirectPolicy.PACKAGE.contentEquals(pkg == null ? "" : pkg)) return -1;
        if (StockHomeRedirectPolicy.isHome(pkg, activity)) return schedule(now);
        if (activity != null) cancel();
        return -1;
    }
    boolean take(int token, long now) {
        if (!pending || token != generation || now < due) return false;
        pending = false; return true;
    }
    void cancel() { pending = false; generation++; }
    DeferredHomePolicy() {}
}
