package com.dvopenlabs.freedom.camera;

import java.net.URI;

/** User-chosen HTTPS or literal private-LAN RTSP only. No discovery, redirects or embedded secrets. */
final class CameraPolicy {
    static boolean privateHost(String host){
        if(host==null||!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))return false;
        String[] s=host.split("\\.");int[] a=new int[4];for(int i=0;i<4;i++){if(s[i].length()>1&&s[i].startsWith("0"))return false;a[i]=Integer.parseInt(s[i]);if(a[i]>255)return false;}
        return a[0]==10||a[0]==172&&a[1]>=16&&a[1]<=31||a[0]==192&&a[1]==168;
    }
    static URI url(String raw){
        try{if(raw==null||raw.length()>4096||java.util.regex.Pattern.compile("[\\p{Cntrl}]").matcher(raw).find())throw new IllegalArgumentException();URI u=new URI(raw);String s=u.getScheme();
            if(u.getHost()==null||u.getRawUserInfo()!=null||u.getFragment()!=null||u.getPort()==0||u.getPort()>65535||u.getPort()<-1||!("https".equals(s)||"rtsp".equals(s)&&privateHost(u.getHost())))throw new IllegalArgumentException();
            return u;
        }catch(Exception bad){throw new IllegalArgumentException("Use HTTPS HLS/MP4 or rtsp:// with a private LAN IPv4 address, without credentials or fragments");}
    }
    static boolean sameOrigin(URI chosen,URI requested){return chosen.getScheme().equalsIgnoreCase(requested.getScheme())&&chosen.getHost().equalsIgnoreCase(requested.getHost())&&port(chosen)==port(requested)&&requested.getRawUserInfo()==null;}
    static int port(URI u){return u.getPort()<0?(u.getScheme().equals("https")?443:554):u.getPort();}
    static String credential(String value){if(value==null||value.length()>1024||java.util.regex.Pattern.compile("[\\p{Cntrl}]").matcher(value).find())throw new IllegalArgumentException("Check credentials");return value;}
    static long duration(int minutes){if(minutes!=1&&minutes!=5&&minutes!=15)throw new IllegalArgumentException();return minutes*60000L;}
    private CameraPolicy(){}
}
