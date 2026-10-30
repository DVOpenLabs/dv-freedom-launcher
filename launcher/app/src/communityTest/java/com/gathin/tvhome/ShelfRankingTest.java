package com.gathin.tvhome;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShelfRankingTest {
    ContentShelves.Card card(String pkg,long time){ContentShelves.Card c=new ContentShelves.Card(pkg,"Example title","From example",null,null,-1,null);c.engagement=time;return c;}
    @Test public void rankBeforeLimitingKeepsNewestItemsPerSource(){List<ContentShelves.Card> rows=new ArrayList<>();for(int i=0;i<96;i++)rows.add(card(i%2==0?"source.a":"source.b",i));ContentShelves.newest(rows);List<ContentShelves.Card> selected=ContentShelves.limited(rows,2);assertEquals(4,selected.size());assertEquals(95,selected.get(0).engagement);assertEquals(94,selected.get(1).engagement);assertEquals(93,selected.get(2).engagement);assertEquals(92,selected.get(3).engagement);}
    @Test public void overallCapAndWatchPrioritySurvivePreviewMerge(){List<ContentShelves.Card> combined=new ArrayList<>();combined.add(card("watch",10));combined.add(card("watch",9));for(int i=0;i<20;i++)combined.add(card("preview"+i,100+i));List<ContentShelves.Card> selected=ContentShelves.limited(combined,2);assertEquals(12,selected.size());assertEquals("watch",selected.get(0).pkg);assertEquals("watch",selected.get(1).pkg);}
}
