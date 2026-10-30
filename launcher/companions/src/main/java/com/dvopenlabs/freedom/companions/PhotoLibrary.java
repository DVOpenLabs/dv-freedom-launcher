package com.dvopenlabs.freedom.companions;

import android.content.Context;
import android.graphics.*;
import org.json.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Private bounded photo cache. Every fetch is an explicit foreground job; redirects are disabled. */
final class PhotoLibrary {
    static File folder(Context c){File f=new File(c.getCacheDir(),"photo-sources");f.mkdirs();return f;}
    static File[] photos(Context c){File[] a=folder(c).listFiles(f->f.getName().matches("[a-f0-9]{64}\\.jpg"));if(a==null)return new File[0];Arrays.sort(a,Comparator.comparing(File::getName));return a;}
    static JSONObject metadata(File image){File f=new File(image.getParentFile(),image.getName().replace(".jpg",".json"));try{if(f.length()>8192)throw new IOException();return new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));}catch(Exception bad){return new JSONObject();}}
    static void clear(Context c){File[] a=folder(c).listFiles();if(a!=null)for(File f:a)if(f.getName().matches("[a-f0-9]{64}\\.(jpg|json|tmp)"))f.delete();}
    static void active(BooleanSupplier alive)throws IOException{if(Thread.currentThread().isInterrupted()||!alive.getAsBoolean())throw new InterruptedIOException();}
    static Bitmap decode(File f,int maxPixels){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);if(o.outWidth<1||o.outHeight<1||(long)o.outWidth*o.outHeight>32_000_000)return null;o.inSampleSize=1;while((long)(o.outWidth/o.inSampleSize)*(o.outHeight/o.inSampleSize)>maxPixels)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(f.getPath(),o);}
    static void save(Context c,String identity,byte[] bytes,JSONObject meta,BooleanSupplier alive)throws Exception{
        saveInto(folder(c),identity,bytes,meta,alive);
    }
    static void saveInto(File folder,String identity,byte[] bytes,JSONObject meta,BooleanSupplier alive)throws Exception{
        active(alive);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<1||o.outHeight<1||(long)o.outWidth*o.outHeight>32_000_000)throw new IOException("Unsupported image");o.inSampleSize=1;while((long)(o.outWidth/o.inSampleSize)*(o.outHeight/o.inSampleSize)>1920L*1080||Math.max(o.outWidth,o.outHeight)/o.inSampleSize>4096)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(image==null)throw new IOException("Unsupported image");String key=DiscoverStore.hash(identity);File temp=new File(folder,key+".tmp"),target=new File(folder,key+".jpg");try{try(OutputStream out=new FileOutputStream(temp)){if(!image.compress(Bitmap.CompressFormat.JPEG,86,out))throw new IOException();}active(alive);Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);active(alive);String data=meta.toString();if(data.length()>8192)throw new IOException();Files.write(new File(folder,key+".json").toPath(),data.getBytes(StandardCharsets.UTF_8));}finally{image.recycle();temp.delete();}
    }
    static int refresh(Context c,Map<String,?> settings,BooleanSupplier alive)throws Exception{
        String type=value(settings,"type","immich"),url=value(settings,"url",""),secret=value(settings,"secret",""),album=value(settings,"album",""),user=value(settings,"user",""),tag=value(settings,"tag","").trim();
        if(url.isEmpty()||secret.isEmpty())throw new IOException("Configure a source first");
        Map<String,String> headers=new HashMap<>();List<String> urls=new ArrayList<>();List<JSONObject> metadata=new ArrayList<>();active(alive);
        if(type.equals("immich")){
            if(!PhotoSourcePolicy.album(album))throw new IOException();headers.put("x-api-key",secret);String base=url.replaceAll("/+$","");JSONObject data=new JSONObject(new String(Network.requestWith("GET",base+"/api/albums/"+album,headers,null,true,1024*1024),StandardCharsets.UTF_8));JSONArray assets=data.optJSONArray("assets");
            for(int i=0;assets!=null&&i<Math.min(512,assets.length())&&urls.size()<12;i++){JSONObject asset=assets.optJSONObject(i);if(asset==null||!"IMAGE".equals(asset.optString("type"))||!PhotoSourcePolicy.album(asset.optString("id"))||!matchesTag(asset,tag))continue;urls.add(base+"/api/assets/"+asset.getString("id")+"/thumbnail?size=preview");JSONObject exif=asset.optJSONObject("exifInfo"),m=new JSONObject();m.put("date",clean(asset.optString("localDateTime"),32));if(exif!=null)m.put("place",clean(exif.optString("city"),100));StringJoiner people=new StringJoiner(", ");JSONArray a=asset.optJSONArray("people");for(int n=0;a!=null&&n<Math.min(8,a.length());n++){JSONObject person=a.optJSONObject(n);if(person!=null&&!person.optString("name").isEmpty())people.add(clean(person.optString("name"),60));}m.put("people",people.toString());metadata.add(m);}
        }else if(type.equals("dav")){
            headers.put("Authorization",okhttp3.Credentials.basic(user,secret));headers.put("Depth","1");String base=url.endsWith("/")?url:url+"/";byte[] data=Network.requestWith("PROPFIND",base,headers,"<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>",true,1024*1024);DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);NodeList a=f.newDocumentBuilder().parse(new ByteArrayInputStream(data)).getElementsByTagNameNS("DAV:","href");for(int i=0;i<Math.min(128,a.getLength())&&urls.size()<12;i++)try{urls.add(PhotoSourcePolicy.child(base,a.item(i).getTextContent()));metadata.add(new JSONObject());}catch(IllegalArgumentException ignored){}
        }else throw new IOException("Unknown photo source");
        int count=0;Set<String> keep=new HashSet<>();for(int i=0;i<urls.size();i++){active(alive);String photo=urls.get(i);save(c,photo,Network.requestWith("GET",photo,headers,null,true,4*1024*1024),metadata.get(i),alive);keep.add(DiscoverStore.hash(photo));count++;}
        active(alive);File[] files=photos(c);Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());long kept=0;for(File file:files){String key=file.getName().replace(".jpg","");if(!keep.contains(key)||kept+file.length()>16L*1024*1024){file.delete();new File(folder(c),key+".json").delete();}else kept+=file.length();}return count;
    }
    static String clean(String s,int max){return s.replaceAll("[\\p{Cntrl}]", " ").substring(0,Math.min(max,s.length()));}
    static boolean matchesTag(JSONObject asset,String tag){if(tag.isEmpty())return true;JSONArray a=asset.optJSONArray("tags");for(int i=0;a!=null&&i<Math.min(64,a.length());i++){JSONObject x=a.optJSONObject(i);if(x!=null&&tag.equalsIgnoreCase(x.optString("value",x.optString("name"))))return true;}return false;}
    static String value(Map<String,?> p,String key,String fallback){Object v=p.get(key);return v instanceof String?(String)v:fallback;}
    private PhotoLibrary(){}
}
