package com.dvopenlabs.freedom.companions;

import java.util.*;

/** Fixed TMDB browse parameters; search deliberately does not pretend to filter provider availability. */
final class DiscoverFilters {
    static final String[] SORTS={"Popular","Top rated","Recently released"};
    static Map<String,String> params(String kind,String region,String language,String provider,String genre,int rating,int sort,int page,String query,String today){
        if(!("movie".equals(kind)||"tv".equals(kind))||!DiscoverStore.region(region)||rating<0||rating>8||sort<0||sort>2||page<1||page>20||!today.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IllegalArgumentException("Invalid browse options");
        Map<String,String> p=new TreeMap<>();p.put("include_adult","false");p.put("page",String.valueOf(page));p.put("language",language);
        if(!query.isEmpty()){if(query.length()>160)throw new IllegalArgumentException("Search too long");p.put("query",query);return p;}
        p.put("watch_region",region);
        if(!provider.isEmpty()){if(!provider.matches("[1-9][0-9]{0,6}"))throw new IllegalArgumentException("Invalid provider");p.put("with_watch_providers",provider);p.put("with_watch_monetization_types","flatrate");}
        if(!genre.isEmpty()){if(!genre.matches("[1-9][0-9]{0,6}"))throw new IllegalArgumentException("Invalid genre");p.put("with_genres",genre);}
        if(rating>0)p.put("vote_average.gte",String.valueOf(rating));
        if(rating>0||sort==1)p.put("vote_count.gte","100");
        p.put("sort_by",sort==1?"vote_average.desc":sort==2?(kind.equals("movie")?"primary_release_date.desc":"first_air_date.desc"):"popularity.desc");
        p.put(kind.equals("movie")?"primary_release_date.lte":"first_air_date.lte",today);
        return p;
    }
    private DiscoverFilters(){}
}
