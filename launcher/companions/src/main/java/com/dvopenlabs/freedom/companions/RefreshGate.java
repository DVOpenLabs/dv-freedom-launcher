package com.dvopenlabs.freedom.companions;
/** One read plus one coalesced follow-up per visible connection generation. */
final class RefreshGate {
    private int generation;
    private boolean running, again;
    synchronized int begin() { if (running) { again = true; return -1; } running = true; return generation; }
    synchronized boolean current(int token) { return token == generation; }
    synchronized boolean finish(int token) { if (token != generation) return false; running = false; return true; }
    synchronized boolean followUp() { boolean value = again; again = false; return value; }
    synchronized void cancel() { generation++; running = false; again = false; }
}
