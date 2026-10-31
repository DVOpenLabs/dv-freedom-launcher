package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ShelfSelectionTest {
    static final class Row { final String pkg,link;final long at;Row(String p,String l,long t){pkg=p;link=l;at=t;} }
    List<Row> select(Row... rows) { return ShelfSelection.newest(Arrays.asList(rows),r->r.pkg+"\n"+r.link,r->r.at,new HashSet<>()); }
    @Test public void newestWinsRegardlessOfProviderOrder() {
        Row old=new Row("app","show",1), fresh=new Row("app","show",50);
        assertSame(fresh,select(old,fresh).get(0));assertSame(fresh,select(fresh,old).get(0));
    }
    @Test public void differentEpisodesAndPublishingAppsRemainDistinct() {
        assertEquals(3,select(new Row("app","episode1",2),new Row("app","episode2",3),new Row("other","episode1",4)).size());
    }
    @Test public void tiesAreStableAndPriorSourceKeysAreNotReintroduced() {
        Row first=new Row("app","same",10),second=new Row("app","same",10);assertSame(first,select(first,second).get(0));
        Set<String> seen=new HashSet<>(Collections.singleton("app\nsame"));
        assertTrue(ShelfSelection.newest(Arrays.asList(first,second),r->r.pkg+"\n"+r.link,r->r.at,seen).isEmpty());
    }
}
