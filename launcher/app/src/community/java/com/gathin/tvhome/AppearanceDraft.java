package com.gathin.tvhome;
import java.util.*;
/** Small editable allowlist. Only changed controls become writes. */
final class AppearanceDraft {
    static final String[] KEYS={"app_position","icon_size_v1","tile_gap_choice","app_names_choice","animations","wallpaper_dim_choice","reduced_motion_choice"};
    static final Object[][] OPTIONS={{"top","middle","bottom"},{"","small","medium","large"},{0,4,8,16,24,32},{0,1,2,3},{true,false},{0,10,20,30,40,50,60,70,80,90},{false,true}};
    private final Map<String,Object> baseline=new HashMap<>(),values=new HashMap<>();
    AppearanceDraft(Map<String,Object> initial){for(int i=0;i<KEYS.length;i++){Object v=initial.get(KEYS[i]);boolean custom=(i==2||i==5)&&v instanceof Integer&&(Integer)v>=0&&(Integer)v<=(i==2?64:90);if(!custom&&!Arrays.asList(OPTIONS[i]).contains(v))v=OPTIONS[i][0];baseline.put(KEYS[i],v);values.put(KEYS[i],v);}}
    Object get(int row){return values.get(KEYS[row]);}
    void step(int row,int direction){if(row<0||row>=KEYS.length||Math.abs(direction)!=1)throw new IllegalArgumentException();List<Object> options=new ArrayList<>(Arrays.asList(OPTIONS[row]));Object original=baseline.get(KEYS[row]);if(!options.contains(original)){options.add(original);options.sort((a,b)->Integer.compare((Integer)a,(Integer)b));}values.put(KEYS[row],options.get(Math.floorMod(options.indexOf(get(row))+direction,options.size())));}
    Map<String,Object> changes(){Map<String,Object> out=new LinkedHashMap<>();for(String k:KEYS)if(!Objects.equals(values.get(k),baseline.get(k)))out.put(k,values.get(k));return out;}
    Map<String,Object> preview(String look){Map<String,Object> out=HomePresets.settings(0);out.put("experience",ExperiencePolicy.SHOWCASE.equals(look)?ExperiencePolicy.CINEMA:look);out.put("position",get(0));out.put("iconSize",get(1));out.put("spacing",get(2));out.put("names",new String[]{"selected","all","selected","hidden"}[(Integer)get(3)]);out.put("dim",get(5));return out;}
}
