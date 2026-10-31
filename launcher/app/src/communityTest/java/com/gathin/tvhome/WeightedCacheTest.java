package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class WeightedCacheTest {
    @Test public void frequentlyReadArtSurvivesAnEviction(){WeightedCache<String,String> c=new WeightedCache<>(10,2);c.put("a","A",4);c.put("b","B",4);assertEquals("A",c.get("a"));c.put("c","C",4);assertNull(c.get("b"));assertEquals("A",c.get("a"));assertEquals(8,c.bytes());}
    @Test public void bothEntryAndByteLimitsApply(){WeightedCache<String,String> c=new WeightedCache<>(10,3);c.put("a","A",6);c.put("b","B",6);assertNull(c.get("a"));assertEquals(1,c.size());c.put("c","C",11);assertNull(c.get("c"));assertEquals(6,c.bytes());}
    @Test public void replacementAndClearReleaseReferencesWithoutDestroyingValues(){WeightedCache<String,StringBuilder> c=new WeightedCache<>(10,3);StringBuilder drawing=new StringBuilder("still used by a view");c.put("a",drawing,8);c.put("a",new StringBuilder("new"),3);assertEquals(3,c.bytes());c.clear();assertEquals(0,c.bytes());assertEquals(0,c.size());assertEquals("still used by a view",drawing.toString());}
}
