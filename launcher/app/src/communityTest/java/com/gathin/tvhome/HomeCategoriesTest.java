package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class HomeCategoriesTest {
    private HomeCategories.Row row(int n,String name,List<String> apps){return new HomeCategories.Row(String.format(Locale.ROOT,"row-%08x-0000-0000-0000-000000000000",n),name,true,apps);}
    private void rejects(Runnable action){try{action.run();fail("Expected invalid category data");}catch(IllegalArgumentException expected){}}
    private List<String> apps(int count){List<String> values=new ArrayList<>();for(int i=0;i<count;i++)values.add("example.app"+i);return values;}
    @Test public void categoriesAreOffUntilExplicitlyEnabled(){
        HomeCategories.State state=new HomeCategories.State(false,Collections.singletonList(row(1,"Kids",apps(3))));
        assertFalse(state.enabled);assertEquals(1,state.rows.size());assertTrue(state.rows.get(0).visible);
    }
    @Test public void categoryStateMakesDefensiveImmutableCopies(){
        List<String> apps=new ArrayList<>(Arrays.asList("example.one","example.two"));HomeCategories.Row row=row(1," Cinema ",apps);apps.clear();
        List<HomeCategories.Row> rows=new ArrayList<>(Collections.singletonList(row));HomeCategories.State state=new HomeCategories.State(false,rows);rows.clear();
        assertEquals("Cinema",row.name);assertEquals(2,row.apps.size());assertEquals(1,state.rows.size());
        try{row.apps.add("example.three");fail();}catch(UnsupportedOperationException expected){}
        try{state.rows.clear();fail();}catch(UnsupportedOperationException expected){}
    }
    @Test public void duplicateIdsNamesAndOriginalFavoritesNameAreRejected(){
        rejects(()->new HomeCategories.State(false,Arrays.asList(row(1,"Kids",apps(1)),row(1,"Games",apps(1)))));
        rejects(()->new HomeCategories.State(false,Arrays.asList(row(1,"Kids",apps(1)),row(2," kids ",apps(1)))));
        rejects(()->new HomeCategories.State(false,Collections.singletonList(row(1,"Favorites",apps(1)))));
    }
    @Test public void malformedIdsNamesAndPackagesAreRejected(){
        rejects(()->new HomeCategories.Row("row-------------------------------------","Kids",true,apps(1)));
        rejects(()->row(1," ",apps(1)));rejects(()->row(1,"Kid\ns",apps(1)));rejects(()->row(1,"x".repeat(41),apps(1)));
        rejects(()->row(1,"Kids",Arrays.asList("example.one","example.one")));
        rejects(()->row(1,"Kids",Collections.singletonList("../app")));
    }
    @Test public void rowAndMembershipCapsAreIndependent(){
        List<HomeCategories.Row> eight=new ArrayList<>();for(int i=0;i<8;i++)eight.add(row(i,"Group "+i,apps(16)));
        assertEquals(8,new HomeCategories.State(true,eight).rows.size());
        List<HomeCategories.Row> nine=new ArrayList<>(eight);nine.add(row(9,"Group 9",Collections.emptyList()));rejects(()->new HomeCategories.State(true,nine));
        rejects(()->row(1,"Kids",apps(33)));
        List<HomeCategories.Row> tooMany=new ArrayList<>();for(int i=0;i<5;i++)tooMany.add(row(i,"Group "+i,apps(26)));rejects(()->new HomeCategories.State(true,tooMany));
    }
    @Test public void sameAppMayAppearInDifferentRows(){
        HomeCategories.State state=new HomeCategories.State(true,Arrays.asList(row(1,"Kids",apps(2)),row(2,"Cinema",apps(2))));
        assertEquals(state.rows.get(0).apps,state.rows.get(1).apps);assertNotEquals(state.rows.get(0).id,state.rows.get(1).id);
    }
    @Test public void movingAndEditingRowsNeverMutatesThePreviousState(){
        HomeCategories.Row one=row(1,"Kids",apps(2)),two=row(2,"Games",apps(3));HomeCategories.State original=new HomeCategories.State(true,Arrays.asList(one,two));
        HomeCategories.State moved=original.move(two.id,-1);assertEquals(two.id,moved.rows.get(0).id);assertEquals(one.id,original.rows.get(0).id);
        HomeCategories.State edited=moved.replace(two.id,two.named("Play").shown(false));assertFalse(edited.rows.get(0).visible);assertEquals("Games",two.name);assertEquals(two.apps,edited.rows.get(0).apps);
        assertEquals(1,edited.replace(two.id,null).rows.size());assertTrue(edited.enabled);
    }
    @Test public void edgeMovesAndStaleIdsCannotChangeRows(){
        HomeCategories.Row row=row(1,"Kids",apps(1));HomeCategories.State state=new HomeCategories.State(false,Collections.singletonList(row));
        rejects(()->state.move(row.id,-1));rejects(()->state.move(row.id,1));rejects(()->state.replace("missing",null));assertEquals(row.id,state.rows.get(0).id);
    }
}
