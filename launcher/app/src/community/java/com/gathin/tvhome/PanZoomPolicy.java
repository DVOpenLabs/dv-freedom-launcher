package com.gathin.tvhome;

/** Overscan always exceeds translation, so a covered still cannot expose its edges. */
final class PanZoomPolicy {
    static final String KEY = "still_pan_zoom";
    static final long DURATION_MS = 45_000;
    static boolean allowed(boolean chosen, boolean reduced, boolean video, boolean visible) {
        return chosen && !reduced && !video && visible;
    }
    static float[] frame(float progress) {
        float[] result=new float[3];frame(progress,result);return result;
    }
    static void frame(float progress,float[] result) {
        float p = Float.isNaN(progress) ? 0 : Math.max(0, Math.min(1, progress));
        result[0]=1.04f+.025f*p;result[1]=(2*p-1)*.015f;result[2]=(1-2*p)*.01f;
    }
    private PanZoomPolicy() {}
}
