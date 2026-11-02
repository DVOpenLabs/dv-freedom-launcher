package com.gathin.tvhome;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class CinemaMovieSearchTest {
    private CinemaWorld world(String id)throws Exception{return new CinemaWorld(new JSONObject().put("id",id).put("wallpaper","p-00000000-0000-0000-0000-000000000000"));}
    @Test public void searchCannotAutoplayOrSelectAPurchase()throws Exception{String url=CinemaMovieSearch.url(world("hogwarts"),0,1);assertEquals("https://www.youtube.com/results?search_query=Harry%20Potter%20and%20the%20Chamber%20of%20Secrets%20movie",url);assertFalse(url.contains("watch?"));assertFalse(url.contains("autoplay"));assertFalse(url.contains("+"));}
    @Test public void ambiguousBookLabelsIncludeFranchiseAndPart()throws Exception{assertTrue(CinemaMovieSearch.url(world("hogwarts"),0,0).contains("Sorcerer%27s%20Stone"));assertTrue(CinemaMovieSearch.url(world("hogwarts"),0,7).contains("Part%202"));assertTrue(CinemaMovieSearch.url(world("hobbit"),0,0).contains("The%20Hobbit%3A%20An%20Unexpected%20Journey"));}
    @Test public void searchOnlyTargetsTheVerifiedYouTubeTvApp(){assertTrue(CinemaMovieSearch.youtube("com.google.android.youtube.tv"));assertFalse(CinemaMovieSearch.youtube("org.example.youtube"));assertFalse(CinemaMovieSearch.youtube("com.google.android.apps.youtube.tvunplugged"));}
}
