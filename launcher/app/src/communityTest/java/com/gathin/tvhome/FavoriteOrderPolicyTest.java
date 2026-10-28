package com.gathin.tvhome;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class FavoriteOrderPolicyTest {
 @Test public void movesPreserveMembershipAndDoNotMutateTheOriginal(){List<String> before=Arrays.asList("a","b","c");assertEquals(Arrays.asList("b","a","c"),FavoriteOrderPolicy.move(before,0,1));assertEquals(Arrays.asList("a","c","b"),FavoriteOrderPolicy.move(before,2,-1));assertEquals(Arrays.asList("a","b","c"),before);}
 @Test public void edgesDoNotWrap(){List<String> apps=Arrays.asList("a","b");assertEquals(apps,FavoriteOrderPolicy.move(apps,0,-1));assertEquals(apps,FavoriteOrderPolicy.move(apps,1,1));}
 @Test public void invalidMovesAreRejected(){for(int delta:new int[]{0,2,-2})try{FavoriteOrderPolicy.move(Arrays.asList("a","b"),0,delta);fail();}catch(IllegalArgumentException expected){}try{FavoriteOrderPolicy.move(Arrays.asList("a","a"),0,1);fail();}catch(IllegalArgumentException expected){}}
}
