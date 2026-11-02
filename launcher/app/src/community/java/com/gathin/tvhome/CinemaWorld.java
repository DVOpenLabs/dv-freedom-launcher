package com.gathin.tvhome;

import org.json.*;
import java.util.*;

/** Declarative cinematic worlds. Personal images remain local and are never executable. */
final class CinemaWorld {
    static final String[] IDS={"hogwarts","hobbit","rivendell","ayodhya","ironman","avengers","infinity","doomsday"};
    static final String[] NAMES={"Harry Potter","The Hobbit","Lord of the Rings","Ramayana","Iron Man","Avengers","Infinity War","Doomsday"};
    static final int[] ACCENTS={0xffe6c37a,0xffb8d58c,0xffd6e5f2,0xffffc879,0xff8fe7ff,0xff99c9ff,0xffdab5ff,0xffa4e7b1};
    static final String[] ROOMS={"The candlelit library","There and back again","The elven archives","Stories of Ayodhya","The armour archive","The assembly","The cosmic archive","The citadel archives"};
    static final String[] GALLERIES={"The moonlit greenhouse","Treasures of the Shire","Relics of Middle-earth","Lotus courtyard","The workshop","Mission control","The six stones","The emerald sanctum"};
    static final String[][] TITLES={
        {"Philosopher’s Stone","Chamber of Secrets","Prisoner of Azkaban","Goblet of Fire","Order of the Phoenix","Half-Blood Prince","Deathly Hallows I","Deathly Hallows II"},
        {"An Unexpected Journey","The Desolation of Smaug","The Battle of the Five Armies"},
        {"The Fellowship of the Ring","The Two Towers","The Return of the King"},
        {"Ayodhya","The forest journey","Kishkindha","Sundara Kanda","The return"},
        {"Iron Man","Iron Man 2","Iron Man 3"},
        {"The Avengers","Age of Ultron","Infinity War","Endgame"},
        {"The Avengers","Guardians of the Galaxy","Doctor Strange","Thor: Ragnarok","Infinity War","Endgame"},
        {"The Avengers","Age of Ultron","Infinity War","Endgame","Doomsday"}};
    static final String[][] OBJECTS={
        {"Fantastic Beasts and Where to Find Them","The Crimes of Grindelwald","The Secrets of Dumbledore"},
        {"The round door","The traveller’s map","The acorn"},
        {"The evening star","The white tree","The ancient ring"},
        {"The lotus","The bow","The lamps"},
        {"Arc reactor","Flight stabiliser","Armour core"},
        {"The beacon","The shield","The tower"},
        {"Space","Mind","Reality","Power","Time","Soul"},
        {"The mask","The time gate","The emerald seal"}};
    final String id,wallpaper,libraryArt,galleryArt,idleArt,hopArt;final List<String> covers;final int index;
    CinemaWorld(JSONObject o)throws Exception{
        CinemaSpec.fields(o,"id","wallpaper","libraryArt","galleryArt","idleArt","hopArt","covers");
        id=o.getString("id");index=Arrays.asList(IDS).indexOf(id);if(index<0)throw new IllegalArgumentException("Unknown cinematic world");
        wallpaper=o.getString("wallpaper");if(!ThemeLibrary.validToken(wallpaper))throw new IllegalArgumentException("Choose an imported wallpaper");
        libraryArt=art(o,"libraryArt");galleryArt=art(o,"galleryArt");idleArt=art(o,"idleArt");hopArt=art(o,"hopArt");
        covers=new ArrayList<>();if(o.has("covers")){JSONArray a=o.getJSONArray("covers");if(a.length()>8)throw new IllegalArgumentException("Use at most eight covers");for(int i=0;i<a.length();i++)covers.add(CinemaSpec.asset(a.getString(i)));}
    }
    private static String art(JSONObject o,String key)throws Exception{return CinemaSpec.asset(o.has(key)?o.getString(key):"");}
    int accent(){return ACCENTS[index];}String name(){return NAMES[index];}boolean serif(){return index<4;}
    String targetId(int page,int selected){String[] names=page==0?TITLES[index]:page==1?OBJECTS[index]:null;if(names==null||selected<0||selected>=names.length)throw new IllegalArgumentException("Choose an existing world title");return "world-"+id+(page==0?"-book-":"-gallery-")+selected;}
    Set<String> assets(){Set<String> all=new HashSet<>(covers);Collections.addAll(all,libraryArt,galleryArt,idleArt,hopArt);all.remove("");return all;}
    static List<CinemaWorld> parse(JSONArray array)throws Exception{
        List<CinemaWorld> out=new ArrayList<>();if(array==null)return out;if(array.length()>8)throw new IllegalArgumentException("Use at most eight cinematic worlds");Set<String> ids=new HashSet<>(),tokens=new HashSet<>();
        for(int i=0;i<array.length();i++){CinemaWorld w=new CinemaWorld(array.getJSONObject(i));if(!ids.add(w.id)||!tokens.add(w.wallpaper))throw new IllegalArgumentException("Each world needs its own wallpaper");out.add(w);}return out;
    }
    static CinemaWorld find(CinemaSpec s,String token){if(s.worldsEnabled)for(CinemaWorld w:s.worlds)if(w.wallpaper.equals(token))return w;return null;}
    static CinemaWorld showcase(CinemaSpec s,PlaylistPolicy.Spec p){return p.enabled&&p.tokens.size()==1?find(s,p.tokens.get(0)):null;}
}
