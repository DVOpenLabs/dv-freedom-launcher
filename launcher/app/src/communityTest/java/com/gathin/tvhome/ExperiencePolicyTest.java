package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ExperiencePolicyTest {
    @Test public void missingOrUnknownLookCannotSelectAnotherRenderer(){assertFalse(ExperiencePolicy.valid(null));assertFalse(ExperiencePolicy.valid("https://theme"));assertTrue(ExperiencePolicy.valid("legacy"));assertEquals("Current layout",ExperiencePolicy.name("legacy"));}
    @Test public void eightCardsAndTheirFocusRingsFitInsideEveryPosition(){
        for(String look:Arrays.asList("glass","clean","cinema"))for(String position:Arrays.asList("top","middle","bottom")){
            ExperiencePolicy.Geometry g=new ExperiencePolicy.Geometry(look,position,8);
            assertTrue(g.x(0)-g.cardWidth*.04f-6>=40);assertTrue(g.x(7)+g.cardWidth*1.04f+6<=920);
            assertTrue(g.top-8>=125);assertTrue(g.labelTop+23<=520);
            for(int i=1;i<8;i++)assertTrue(g.x(i)>g.x(i-1)+g.cardWidth);
        }
    }
    @Test public void positionsAreDistinctAndShortDocksStayCentered(){
        for(String look:Arrays.asList("glass","clean","cinema")){
            ExperiencePolicy.Geometry top=new ExperiencePolicy.Geometry(look,"top",4),middle=new ExperiencePolicy.Geometry(look,"middle",4),bottom=new ExperiencePolicy.Geometry(look,"bottom",4);
            assertTrue(top.top<middle.top);assertTrue(middle.top<bottom.top);
            assertEquals(960-top.left,top.x(3)+top.cardWidth,.001f);
        }
    }
    @Test public void automaticInkRespondsToTheActualRegionAndManualChoiceWins(){
        assertEquals(0xff182334,ExperiencePolicy.ink(0,new int[]{-1,-1}));assertEquals(0xfff5f7fa,ExperiencePolicy.ink(0,new int[]{0xff000000}));
        assertEquals(0xfff5f7fa,ExperiencePolicy.ink(1,new int[]{-1}));assertEquals(0xff182334,ExperiencePolicy.ink(2,new int[]{0xff000000}));
        assertEquals(0xfff5f7fa,ExperiencePolicy.ink(0,new int[0]));
    }
    @Test public void remoteEdgesDoNotWrapAndRtlReversesOnlyHorizontalMovement(){
        assertEquals(0,ExperiencePolicy.next(0,-1,8,false));assertEquals(7,ExperiencePolicy.next(7,1,8,false));assertEquals(2,ExperiencePolicy.next(3,1,8,true));assertEquals(4,ExperiencePolicy.next(3,-1,8,true));
    }
    @Test public void extraSpacingKeepsAllEightAppsInsideTheSafeArea(){for(String look:Arrays.asList("glass","clean","cinema")){ExperiencePolicy.Geometry a=new ExperiencePolicy.Geometry(look,"middle",8,0),b=new ExperiencePolicy.Geometry(look,"middle",8,32);assertTrue(b.gap>a.gap);assertTrue(b.cardWidth<a.cardWidth);assertTrue(b.left>=40);assertTrue(b.x(7)+b.cardWidth<=920);}}
    @Test public void presentationCannotAcceptUnboundedRowsOrAnUnknownPosition(){
        for(int count:new int[]{0,9,512})try{new ExperiencePolicy.Geometry("glass","bottom",count);fail();}catch(IllegalArgumentException expected){}
        try{new ExperiencePolicy.Geometry("glass","outside",8);fail();}catch(IllegalArgumentException expected){}
    }
}
