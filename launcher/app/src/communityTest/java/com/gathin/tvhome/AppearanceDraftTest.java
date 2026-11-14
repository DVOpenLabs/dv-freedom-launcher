package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class AppearanceDraftTest {
    @Test public void browsingAndCancellingNeverGenerateWrites(){AppearanceDraft d=new AppearanceDraft(Collections.emptyMap());assertTrue(d.changes().isEmpty());d.step(0,1);assertEquals(Collections.singletonMap("app_position","middle"),d.changes());d.step(0,-1);assertTrue(d.changes().isEmpty());}
    @Test public void preservesCustomValues(){Map<String,Object> m=new HashMap<>();m.put("tile_gap_choice",12);m.put("wallpaper_dim_choice",35);AppearanceDraft d=new AppearanceDraft(m);assertEquals(12,d.get(2));assertEquals(35,d.get(5));d.step(2,1);d.step(2,-1);assertTrue(d.changes().isEmpty());}
    @Test public void onlyAllowlistedControlsCanChange(){AppearanceDraft d=new AppearanceDraft(Collections.emptyMap());for(int i=0;i<AppearanceDraft.KEYS.length;i++)d.step(i,1);assertTrue(new HashSet<>(Arrays.asList(AppearanceDraft.KEYS)).containsAll(d.changes().keySet()));assertFalse(d.changes().containsKey("theme_pack_id"));assertFalse(d.changes().containsKey("desktop_layout"));}
}
