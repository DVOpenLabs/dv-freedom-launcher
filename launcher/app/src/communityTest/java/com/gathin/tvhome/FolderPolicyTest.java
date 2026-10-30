package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class FolderPolicyTest {
    FolderPolicy.Folder f(int n,String name,String...apps){return new FolderPolicy.Folder(String.format(Locale.ROOT,"%032x",n),name,Arrays.asList(apps));}
    void rejects(Runnable r){try{r.run();fail();}catch(IllegalArgumentException expected){}}
    @Test public void roundTripKeepsUnicodeNamesAndOrder(){List<FolderPolicy.Folder> all=Arrays.asList(f(1,"アニメ", "example.one","example.two"),f(2,"Games","example.game"));List<FolderPolicy.Folder> back=FolderPolicy.parse(FolderPolicy.encode(all));assertEquals("アニメ",back.get(0).name);assertEquals(all.get(0).apps,back.get(0).apps);assertEquals(all.get(1).id,back.get(1).id);}
    @Test public void noGroupsIsAValidUnchangedHome(){assertTrue(FolderPolicy.parse("").isEmpty());assertTrue(FolderPolicy.parse("v1").isEmpty());}
    @Test public void damagedOrOversizedStateFailsClosed(){rejects(()->FolderPolicy.parse("v2"));rejects(()->FolderPolicy.parse("v1\nbad"));rejects(()->FolderPolicy.parse("x".repeat(16385)));}
    @Test public void duplicatesDoNotMoveAppsBetweenFolders(){rejects(()->FolderPolicy.encode(Arrays.asList(f(1,"Anime","example.one"),f(2,"Games","example.one"))));rejects(()->FolderPolicy.encode(Arrays.asList(f(1,"Anime","example.one"),f(2,"anime","example.two"))));}
    @Test public void invalidMembershipAndNamesAreRejected(){rejects(()->f(1,"Bad\nName","example.one"));rejects(()->f(1,"Empty"));rejects(()->f(1,"App","../bad"));rejects(()->f(1,"App","example.one","example.one"));}
    @Test public void unavailablePackagesRemainInData(){FolderPolicy.Folder f=f(1,"Offline USB apps","example.external");assertEquals(f.apps,FolderPolicy.parse(FolderPolicy.encode(Collections.singletonList(f))).get(0).apps);}
    @Test public void dataIsDefensivelyCopied(){List<String> apps=new ArrayList<>(Arrays.asList("example.one"));FolderPolicy.Folder f=new FolderPolicy.Folder("0".repeat(32),"Apps",apps);apps.clear();assertEquals(1,f.apps.size());try{f.apps.clear();fail();}catch(UnsupportedOperationException expected){}}
    @Test public void folderCountIsBounded(){List<FolderPolicy.Folder> many=new ArrayList<>();for(int i=0;i<17;i++)many.add(f(i,"Folder "+i,"example.app"+i));rejects(()->FolderPolicy.encode(many));}
}
