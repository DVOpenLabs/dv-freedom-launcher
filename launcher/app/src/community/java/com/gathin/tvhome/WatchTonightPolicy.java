package com.gathin.tvhome;
/** No executable URLs or account fields enter the local watchlist bridge. */
final class WatchTonightPolicy {
    static boolean valid(String token,String title){return token!=null&&token.matches("(movie|tv)/[1-9][0-9]{0,9}")&&title!=null&&!title.trim().isEmpty()&&title.length()<=160&&!title.matches("(?s).*[\\p{Cntrl}].*");}
    private WatchTonightPolicy(){}
}
