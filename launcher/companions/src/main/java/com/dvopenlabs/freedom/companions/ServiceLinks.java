package com.dvopenlabs.freedom.companions;

import java.net.URI;
import java.util.Locale;

/** User-supplied HTTPS title links, pinned to one installed streaming package. No URL-derived IDs. */
final class ServiceLinks {
    static String pkg(String label){String n=label.toLowerCase(Locale.ROOT);return n.contains("netflix")?"com.netflix.ninja":n.contains("amazon")||n.contains("prime video")?"com.amazon.amazonvideo.livingroom":n.contains("crunchyroll")?"com.crunchyroll.crunchyroid":n.contains("apple")?"com.apple.atve.androidtv.appletv":n.contains("disney")?"com.disney.disneyplus":null;}
    static String validate(String pkg,String value){
        try{
            if(value==null||value.length()>1024||value.matches("(?s).*\\s.*"))throw new IllegalArgumentException();
            URI u=new URI(value);String host=u.getHost(),path=u.getRawPath();
            if(!"https".equals(u.getScheme())||host==null||u.getPort()!=-1||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||path==null||path.contains("%")||path.contains(".."))throw new IllegalArgumentException();
            boolean valid="com.netflix.ninja".equals(pkg)&&("www.netflix.com".equals(host)||"netflix.com".equals(host))&&path.matches("/(?:[a-z]{2}/)?title/[0-9]{6,12}")
                ||"com.amazon.amazonvideo.livingroom".equals(pkg)&&("www.primevideo.com".equals(host)||"primevideo.com".equals(host))&&path.matches("/(?:detail|dp)/[A-Z0-9]{10}")
                ||"com.crunchyroll.crunchyroid".equals(pkg)&&("www.crunchyroll.com".equals(host)||"crunchyroll.com".equals(host))&&path.matches("/(?:[a-z]{2}(?:-[a-z]{2})?/)?(?:series|watch)/[A-Z0-9]+(?:/[A-Za-z0-9-]+)?")
                ||"com.apple.atve.androidtv.appletv".equals(pkg)&&"tv.apple.com".equals(host)&&path.matches("/[a-z]{2}/(?:show|movie)/[A-Za-z0-9-]+/umc\\.cmc\\.[a-z0-9]+")
                ||"com.disney.disneyplus".equals(pkg)&&("www.disneyplus.com".equals(host)||"disneyplus.com".equals(host))&&path.matches("/(?:[a-z]{2}(?:-[a-z]{2})?/)?browse/entity/[a-fA-F0-9-]{36}");
            if(!valid)throw new IllegalArgumentException();return u.toASCIIString();
        }catch(Exception bad){throw new IllegalArgumentException("Use a supported HTTPS title link without tracking parameters");}
    }
    private ServiceLinks(){}
}
