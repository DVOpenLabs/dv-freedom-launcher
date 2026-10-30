package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class StudioFramingTest {
    @Test public void portraitFitRetainsWholeImage(){float[] b=StudioFraming.bounds("fit",1100,3840,1920,1080);assertEquals(1080,b[3],.01);assertTrue(b[0]>0);assertEquals(0,b[1],.01);}
    @Test public void fillCropsAtSelectedFocalEdge(){float[] left=StudioFraming.bounds("left",3840,1100,1920,1080),right=StudioFraming.bounds("right",3840,1100,1920,1080);assertEquals(0,left[0],.01);assertEquals(1920-right[2],right[0],.01);assertTrue(right[0]<0);}
    @Test public void landscapeFitHasLetterboxAndKeepsAspect(){float[] b=StudioFraming.bounds("fit",3840,1100,1920,1080);assertEquals(1920,b[2],.01);assertEquals(550,b[3],.01);assertEquals(265,b[1],.01);}
    @Test public void malformedFramingCannotDivideByZero(){for(String mode:new String[]{"bad","fill"})try{StudioFraming.bounds(mode,0,1,1920,1080);fail();}catch(IllegalArgumentException expected){}}
}
