package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShowcaseLayoutTest {
    @Test public void allPositionsKeepTextAndDockSeparate(){for(String p:AppPlacement.IDS){ShowcaseLayout g=new ShowcaseLayout(p,8,32);assertTrue(g.top>=170);assertTrue(g.top+108<485);assertTrue(g.headline+58<=g.top||g.headline>=g.top+108);assertTrue(g.cell>=80);assertEquals(892,g.width,.01);}}
    @Test public void overflowScrollsWithoutShrinkingOrReorderingVisibleCells(){ShowcaseLayout a=new ShowcaseLayout("bottom",8,16),b=new ShowcaseLayout("bottom",24,16);assertEquals(a.cell,b.cell,.001);assertTrue(b.width>892);for(int i=1;i<24;i++)assertTrue(b.center(i)>b.center(i-1));}
}
