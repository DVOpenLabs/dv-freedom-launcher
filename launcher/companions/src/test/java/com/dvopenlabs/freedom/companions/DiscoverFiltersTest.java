package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DiscoverFiltersTest {
    @Test public void searchDoesNotClaimProviderOrGenreFiltering(){Map<String,String> p=DiscoverFilters.params("movie","US","en-US","8","16",8,1,2,"title","2026-10-30");assertEquals("title",p.get("query"));assertFalse(p.containsKey("with_watch_providers"));assertFalse(p.containsKey("with_genres"));assertFalse(p.containsKey("vote_average.gte"));}
    @Test public void rankingNeedsEnoughVotesAndDoesNotOrderUnreleasedTitlesFirst(){Map<String,String> p=DiscoverFilters.params("tv","US","en-US","8","16",7,1,1,"","2026-10-30");assertEquals("100",p.get("vote_count.gte"));assertEquals("vote_average.desc",p.get("sort_by"));assertEquals("2026-10-30",p.get("first_air_date.lte"));assertEquals("8",p.get("with_watch_providers"));assertEquals("16",p.get("with_genres"));}
    @Test public void paginationAndProviderInputAreBounded(){for(int n:new int[]{0,21})try{DiscoverFilters.params("movie","US","en","","",0,0,n,"","2026-10-30");fail();}catch(IllegalArgumentException expected){}try{DiscoverFilters.params("movie","US","en","8&query=secret","",0,0,1,"","2026-10-30");fail();}catch(IllegalArgumentException expected){}}
    @Test public void titleLinksCannotCrossPackagesOrSmuggleCredentials(){String pkg="com.netflix.ninja",good="https://www.netflix.com/title/12345678";assertEquals(good,ServiceLinks.validate(pkg,good));for(String s:new String[]{"http://www.netflix.com/title/12345678","https://www.netflix.com.evil.test/title/12345678","https://user:pass@www.netflix.com/title/12345678","https://www.netflix.com/title/12345678?token=secret","intent://title/12345678","https://www.netflix.com/title/%2e%2e"})try{ServiceLinks.validate(pkg,s);fail(s);}catch(IllegalArgumentException expected){}try{ServiceLinks.validate("com.disney.disneyplus",good);fail();}catch(IllegalArgumentException expected){}}
}
