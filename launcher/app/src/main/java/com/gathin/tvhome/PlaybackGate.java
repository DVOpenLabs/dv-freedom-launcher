package com.gathin.tvhome;

/** Generation checks keep late decoder callbacks from resurrecting a hidden or failed background. */
final class PlaybackGate {
    private int generation; private boolean visible,failed;
    int begin(){visible=true;return ++generation;}
    void stop(){visible=false;generation++;}
    boolean accepts(int token){return visible&&!failed&&token==generation;}
    boolean failed(){return failed;}
    boolean fail(int token){if(!accepts(token))return false;failed=true;stop();return true;}
    static boolean supported(long totalBytes,boolean lowRam,boolean reducedMotion){return totalBytes>=2L*1024*1024*1024&&!lowRam&&!reducedMotion;}
}
