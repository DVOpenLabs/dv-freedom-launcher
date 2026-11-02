package com.gathin.tvhome;

import org.json.*;
import java.net.URI;
import java.util.*;

/** Versioned, non-executable personal cinema data. No remote assets or account information. */
final class CinemaSpec {
    static final String KEY="cinema_studio_v1";
    static final int MAX_TEXT=60000,MAX_TITLES=48,MAX_MOODS=12;
    static boolean canRestoreDraft(String savedProfile,String savedRevision,String profile,String revision){return savedProfile!=null&&savedRevision!=null&&!savedRevision.isEmpty()&&savedProfile.equals(profile)&&savedRevision.equals(revision);}
    final JSONObject json;
    final boolean logos,glow,mascot,autoOpen,sceneBackground;
    final String scene,sceneArt,collectionStyle,collectionName,iconStyle;
    final List<Title> titles=new ArrayList<>();
    final List<String> moods=new ArrayList<>();
    final Map<String,String> appLogos=new LinkedHashMap<>(),taglines=new LinkedHashMap<>();
    static final class Title {
        final String id,name,pkg,url,poster,character,backdrop,accent;final List<String> moods;
        Title(JSONObject o)throws Exception{
            fields(o,"id","name","package","url","poster","character","backdrop","accent","moods");
            for(String key:new String[]{"id","name","package","url","poster","character","backdrop","accent"})if(o.has(key)&&!(o.get(key) instanceof String))throw new IllegalArgumentException("Invalid title "+key);
            if(o.has("moods")&&!(o.get("moods") instanceof JSONArray))throw new IllegalArgumentException("Invalid title moods");
            id=token(o.getString("id"));name=text(o.getString("name"),80);pkg=o.getString("package");
            if(!LayoutDraft.packageName(pkg))throw new IllegalArgumentException("Choose a valid app package");
            url=o.optString("url","");if(!url.isEmpty())link(url);
            poster=asset(o.optString("poster",""));character=asset(o.optString("character",""));backdrop=asset(o.optString("backdrop",""));
            accent=o.optString("accent","#75CFFF");if(!accent.matches("#[a-fA-F0-9]{6}"))throw new IllegalArgumentException("Use a six-digit colour");
            moods=strings(o.optJSONArray("moods"),MAX_MOODS,32);
        }
    }
    CinemaSpec(String raw){
        try{
            if(raw==null||raw.length()>MAX_TEXT)throw new IllegalArgumentException("Cinema setup is too large");
            String safe=raw.isEmpty()?"{\"schema\":1}":raw;ThemeArchive.validateJsonObject(safe,768);json=new JSONObject(safe);
            fields(json,"schema","logos","glow","mascot","autoOpen","sceneBackground","scene","sceneArt","collectionStyle","collectionName","iconStyle","appLogos","taglines","moods","titles");
            for(String key:new String[]{"scene","sceneArt","collectionStyle","collectionName","iconStyle"})if(json.has(key)&&!(json.get(key) instanceof String))throw new IllegalArgumentException("Invalid "+key);
            if(!(json.get("schema") instanceof Integer)||json.getInt("schema")!=1)throw new IllegalArgumentException("Unsupported cinema setup");
            logos=bool(json,"logos");glow=bool(json,"glow");mascot=bool(json,"mascot");autoOpen=bool(json,"autoOpen");sceneBackground=bool(json,"sceneBackground");
            scene=json.optString("scene","off");if(!Arrays.asList("off","aurora","fireflies","mist").contains(scene))throw new IllegalArgumentException("Unknown scene");
            sceneArt=asset(json.optString("sceneArt",""));collectionName=text(json.optString("collectionName","My cinema"),48);collectionStyle=json.optString("collectionStyle","posters");if(!Arrays.asList("posters","books","characters").contains(collectionStyle))throw new IllegalArgumentException("Unknown collection style");
            iconStyle=json.optString("iconStyle","original");if(!Arrays.asList("original","gold","silver","ember").contains(iconStyle))throw new IllegalArgumentException("Unknown icon treatment");
            for(String key:new String[]{"moods","titles"})if(json.has(key)&&!(json.get(key) instanceof JSONArray))throw new IllegalArgumentException("Invalid "+key);
            for(String key:new String[]{"appLogos","taglines"})if(json.has(key)&&!(json.get(key) instanceof JSONObject))throw new IllegalArgumentException("Invalid "+key);
            moods.addAll(strings(json.optJSONArray("moods"),MAX_MOODS,32));
            for(String mood:moods)if(mood.equalsIgnoreCase("Anything"))throw new IllegalArgumentException("Anything is provided automatically");
            map(json.optJSONObject("appLogos"),appLogos,true);map(json.optJSONObject("taglines"),taglines,false);
            JSONArray rows=json.optJSONArray("titles");Set<String> ids=new HashSet<>();
            if(rows!=null){if(rows.length()>MAX_TITLES)throw new IllegalArgumentException("Use at most 48 titles");for(int i=0;i<rows.length();i++){
                Title t=new Title(rows.getJSONObject(i));if(!ids.add(t.id)||!moods.containsAll(t.moods))throw new IllegalArgumentException("Duplicate title or unknown mood");titles.add(t);
            }}
        }catch(Exception e){throw new IllegalArgumentException(e.getMessage()==null?"Invalid cinema setup":e.getMessage(),e);}
    }
    private static boolean bool(JSONObject o,String k)throws Exception{if(!o.has(k))return false;if(!(o.get(k) instanceof Boolean))throw new IllegalArgumentException("Invalid "+k);return o.getBoolean(k);}
    static void fields(JSONObject o,String...names){Set<String> allowed=new HashSet<>(Arrays.asList(names));for(Iterator<String> i=o.keys();i.hasNext();)if(!allowed.contains(i.next()))throw new IllegalArgumentException("Unknown cinema field");}
    static String text(String s,int max){if(s==null||s.trim().isEmpty()||s.length()>max)throw new IllegalArgumentException("Enter 1–"+max+" characters");for(int i=0;i<s.length();i++)if(Character.isISOControl(s.charAt(i)))throw new IllegalArgumentException("Remove control characters");return s.trim();}
    static String token(String s){if(s==null||!s.matches("[a-z0-9][a-z0-9-]{0,47}"))throw new IllegalArgumentException("Invalid title identifier");return s;}
    static String asset(String s){if(!s.isEmpty()&&!s.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid local artwork");return s;}
    static void link(String s)throws Exception{if(s.length()>2048)throw new IllegalArgumentException("Link is too long");URI u=new URI(s);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getPort()!=-1&&u.getPort()!=443)throw new IllegalArgumentException("Use an HTTPS title link without credentials");}
    static List<String> strings(JSONArray a,int max,int length)throws Exception{List<String> result=new ArrayList<>();Set<String> seen=new HashSet<>();if(a==null)return result;if(a.length()>max)throw new IllegalArgumentException("Too many choices");for(int i=0;i<a.length();i++){if(!(a.get(i) instanceof String))throw new IllegalArgumentException("Invalid choice");String s=text(a.getString(i),length);if(!seen.add(s.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Duplicate choice");result.add(s);}return result;}
    private static void map(JSONObject o,Map<String,String> result,boolean assets)throws Exception{if(o==null)return;if(o.length()>64)throw new IllegalArgumentException("Too many apps");for(Iterator<String> i=o.keys();i.hasNext();){String pkg=i.next();if(!LayoutDraft.packageName(pkg))throw new IllegalArgumentException("Invalid app");if(!(o.get(pkg) instanceof String))throw new IllegalArgumentException("Invalid app customization");String v=o.getString(pkg);result.put(pkg,assets?asset(v):text(v,100));}}
    List<Title> pool(String mood,Set<String> available,String last){List<Title> result=new ArrayList<>();for(Title t:titles)if(available.contains(t.pkg)&&(mood.equals("Anything")||t.moods.contains(mood)))result.add(t);if(result.size()>1&&last!=null)result.removeIf(t->t.id.equals(last));return result;}
    Set<String> assets(){Set<String> out=new HashSet<>(appLogos.values());out.add(sceneArt);for(Title t:titles){out.add(t.poster);out.add(t.character);out.add(t.backdrop);}out.remove("");return out;}
}
