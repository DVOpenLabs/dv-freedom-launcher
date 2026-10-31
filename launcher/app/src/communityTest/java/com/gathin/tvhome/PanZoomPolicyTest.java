package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class PanZoomPolicyTest {
    @Test public void videoReducedMotionAndHiddenHomeNeverAnimate(){
        assertTrue(PanZoomPolicy.allowed(true,false,false,true));
        assertFalse(PanZoomPolicy.allowed(false,false,false,true));
        assertFalse(PanZoomPolicy.allowed(true,true,false,true));
        assertFalse(PanZoomPolicy.allowed(true,false,true,true));
        assertFalse(PanZoomPolicy.allowed(true,false,false,false));
    }
    @Test public void everyFrameKeepsCoveredEdgesOutsideTheViewport(){
        for(int n=-100;n<=1100;n++){
            float[] f=PanZoomPolicy.frame(n/1000f);
            assertTrue(Math.abs(f[1])<(f[0]-1)/2);
            assertTrue(Math.abs(f[2])<(f[0]-1)/2);
            assertTrue(f[0]<=1.0651f);
        }
        assertArrayEquals(PanZoomPolicy.frame(0),PanZoomPolicy.frame(Float.NaN),0);
    }
}
