package com.dvopenlabs.freedom.companions;

import okhttp3.HttpUrl;

/** A server-supplied WebDAV link cannot move credentials to another host or outside the chosen folder. */
final class PhotoSourcePolicy {
    static String child(String folder,String href){if(href==null||href.toLowerCase(java.util.Locale.ROOT).matches(".*%(2f|5c|2e).*"))throw new IllegalArgumentException("Encoded path separators are not accepted");HttpUrl base=Network.url(folder,true),item=base.resolve(href);if(item==null||!base.scheme().equals(item.scheme())||!base.host().equals(item.host())||base.port()!=item.port()||!item.username().isEmpty()||!item.password().isEmpty()||item.query()!=null||item.fragment()!=null)throw new IllegalArgumentException("Invalid photo address");String path=base.encodedPath();if(!path.endsWith("/"))path+="/";if(!item.encodedPath().startsWith(path)||item.encodedPath().substring(path.length()).contains("/"))throw new IllegalArgumentException("Photo is outside the chosen folder");String p=item.encodedPath().toLowerCase(java.util.Locale.ROOT);if(!(p.endsWith(".jpg")||p.endsWith(".jpeg")||p.endsWith(".png")||p.endsWith(".webp")))throw new IllegalArgumentException("Unsupported photo type");return item.toString();}
    static boolean album(String id){return id!=null&&id.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");}
    private PhotoSourcePolicy(){}
}
