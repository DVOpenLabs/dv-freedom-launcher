package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SurfaceContrastTest {
    @Test public void everySampleMeetsReadableContrastForBothManualChoices(){int[] region={0xff000000,0xffffffff,0xff3377ff,0xffc87513,0xffaaaaaa};for(int ink:new int[]{SurfaceContrast.LIGHT,SurfaceContrast.DARK}){int surface=ink==SurfaceContrast.LIGHT?0xff081020:0xffeef3fa;int alpha=SurfaceContrast.alpha(ink,region);for(int c:region)assertTrue(SurfaceContrast.contrast(ink,SurfaceContrast.blend(c,surface,alpha))>=4.5);}}
    @Test public void oppositeWallpaperFramesHaveStableSafetyFloor(){assertTrue(SurfaceContrast.alpha(SurfaceContrast.LIGHT,new int[]{0xff000000})>=190);assertTrue(SurfaceContrast.alpha(SurfaceContrast.DARK,new int[]{0xffffffff})>=190);}
    @Test public void sourcesRoundTripAndRemainSeparate(){ShelfOptions.Spec s=new ShelfOptions.Spec("v1|4|0|1|org.example.app");assertEquals(4,s.count);assertFalse(s.watch);assertTrue(s.preview);assertFalse(s.accepts("org.example.app"));assertEquals("v1|4|0|1|org.example.app",s.save());}
    @Test(expected=IllegalArgumentException.class)public void arbitrarySourceLimitsAreRejected(){new ShelfOptions.Spec("v1|500|1|1|");}
}
