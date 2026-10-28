package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class IconSizePolicyTest {
    @Test public void absentChoiceAndLargeRetainExactOriginalSize(){for(int width:new int[]{80,115,196,391,787}){assertEquals(width,IconSizePolicy.width(width,""));assertEquals(width,IconSizePolicy.width(width,"large"));}}
    @Test public void namedSizesAreDistinctAndCannotGrowOutsideTheSafeCell(){for(int width=80;width<=900;width++){int small=IconSizePolicy.width(width,"small"),medium=IconSizePolicy.width(width,"medium"),large=IconSizePolicy.width(width,"large");assertTrue(small<medium);assertTrue(medium<large);assertTrue(large<=width);}}
    @Test public void persistedChoicesAreAnAllowlist(){assertTrue(IconSizePolicy.valid("small"));assertTrue(IconSizePolicy.valid("medium"));assertTrue(IconSizePolicy.valid("large"));assertTrue(IconSizePolicy.valid(""));assertFalse(IconSizePolicy.valid(null));assertFalse(IconSizePolicy.valid("200"));assertFalse(IconSizePolicy.valid("https://size"));}
    @Test public void allLooksKeepFocusAndNamesInsideEveryScreenPosition(){for(String look:new String[]{"glass","clean","cinema"})for(String position:AppPlacement.IDS)for(String size:IconSizePolicy.IDS)for(int count=1;count<=8;count++)for(int gap:new int[]{0,8,16,24,32}){ExperiencePolicy.Geometry g=new ExperiencePolicy.Geometry(look,position,count,gap,size);assertTrue(g.x(0)-g.cardWidth*.04f-6>=40);assertTrue(g.x(count-1)+g.cardWidth*1.04f+6<=920);assertTrue(g.top-8>=125);assertTrue(g.labelTop+23<=520);for(int i=1;i<count;i++)assertTrue(g.x(i)>g.x(i-1)+g.cardWidth);}}
}
