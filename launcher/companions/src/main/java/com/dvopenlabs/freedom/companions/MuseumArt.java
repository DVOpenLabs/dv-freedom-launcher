package com.dvopenlabs.freedom.companions;
import android.content.Context;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Optional Met Open Access requests. No image is bundled or accepted without a public-domain flag. */
final class MuseumArt {
    static final int[] OBJECTS={436535,437133,435882};
    static File folder(Context c){File f=new File(c.getCacheDir(),"museum-art");f.mkdirs();return f;}
    static File[] photos(Context c){File[] f=folder(c).listFiles(x->x.getName().matches("[a-f0-9]{64}\\.jpg"));return f==null?new File[0]:f;}
    static JSONObject object(int id)throws Exception{boolean allowed=false;for(int x:OBJECTS)if(x==id)allowed=true;if(!allowed)throw new IllegalArgumentException();JSONObject o=new JSONObject(new String(Network.request("https://collectionapi.metmuseum.org/public/collection/v1/objects/"+id,null,null,false,128*1024),StandardCharsets.UTF_8));image(o);if(o.optInt("objectID")!=id)throw new IOException();return o;}
    static String image(JSONObject o){try{URI u=new URI(o.optString("primaryImageSmall"));if(!o.optBoolean("isPublicDomain")||!"https".equals(u.getScheme())||!"images.metmuseum.org".equals(u.getHost())||u.getPort()!=-1||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getFragment()!=null||!u.getPath().startsWith("/CRDImages/"))throw new IllegalArgumentException();return u.toASCIIString();}catch(Exception bad){throw new IllegalArgumentException("Public-domain museum image unavailable");}}
    static void download(Context c,JSONObject o,BooleanSupplier alive)throws Exception{String url=image(o);JSONObject m=new JSONObject();m.put("title",PhotoLibrary.clean(o.optString("title"),160));m.put("credit",PhotoLibrary.clean(o.optString("artistDisplayName"),100)+" · The Metropolitan Museum of Art · CC0");m.put("source","https://www.metmuseum.org/art/collection/search/"+o.getInt("objectID"));PhotoLibrary.saveInto(folder(c),url,Network.request(url,null,null,false,4*1024*1024),m,alive);}
    private MuseumArt(){}
}
