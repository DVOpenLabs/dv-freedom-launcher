package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class InstallerPolicyTest {
    Map<String,Object> plan(){Map<String,Object> p=new HashMap<>();p.put("style","clean");p.put("position","bottom");p.put("size","medium");p.put("spacing","comfortable");p.put("wallpaper","keep");p.put("apps",Arrays.asList("example.video","example.music"));return p;}
    Set<String> installed(){return new HashSet<>(Arrays.asList("example.video","example.music"));}
    @Test public void mapsOnlyAppearanceChoices(){Map<String,Object> values=InstallerPolicy.choices(plan(),installed());assertEquals("clean",values.get(ExperiencePolicy.KEY));assertEquals(12,values.get(TileSpacing.CHOICE));assertFalse(values.containsKey("wallpaper_choice"));}
    @Test public void allSupportedChoices(){for(String style:Arrays.asList("classic","clean","glass","cinema"))for(String position:Arrays.asList("top","middle","bottom"))for(String size:Arrays.asList("small","medium","large")){Map<String,Object> p=plan();p.put("style",style);p.put("position",position);p.put("size",size);assertEquals(position,InstallerPolicy.choices(p,installed()).get(AppPlacement.KEY));}}
    @Test public void rejectsUnknownAndUnavailable(){for(String key:Arrays.asList("style","position","size","spacing","wallpaper")){Map<String,Object> p=plan();p.put(key,"arbitrary");try{InstallerPolicy.choices(p,installed());fail();}catch(IllegalArgumentException expected){}}Map<String,Object> p=plan();p.put("command","reboot");try{InstallerPolicy.choices(p,installed());fail();}catch(IllegalArgumentException expected){}p=plan();p.put("apps",Arrays.asList("missing.app"));try{InstallerPolicy.choices(p,installed());fail();}catch(IllegalArgumentException expected){}}
    @Test public void rejectsDuplicateEmptyAndTooMany(){for(List<String> apps:Arrays.asList(Collections.<String>emptyList(),Arrays.asList("example.video","example.video"))){Map<String,Object> p=plan();p.put("apps",apps);try{InstallerPolicy.choices(p,installed());fail();}catch(IllegalArgumentException expected){}}}
}
