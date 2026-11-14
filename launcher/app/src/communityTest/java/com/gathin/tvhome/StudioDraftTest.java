package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class StudioDraftTest {
    @Test public void customValuesAndUnchangedSettingsSurviveEdits(){Map<String,Object> original=new HashMap<>();original.put("gap",12);original.put("dim",35);original.put("position","bottom");StudioDraft d=new StudioDraft(original);d.set("position","top");assertEquals(Collections.singletonMap("position","top"),d.changes());assertEquals(12,d.get("gap"));assertEquals(35,d.get("dim"));d.undo();assertTrue(d.changes().isEmpty());}
    @Test public void compoundChoiceUndoesAsOneStepAndNeverTouchesInput(){Map<String,Object> original=new HashMap<>();original.put("theme","old");original.put("playlist","rotation");StudioDraft d=new StudioDraft(original);Map<String,Object> next=new HashMap<>();next.put("theme","new");next.put("playlist","single");d.setAll(next);assertEquals(2,d.changes().size());assertEquals("old",original.get("theme"));d.undo();assertTrue(d.changes().isEmpty());}
    @Test public void rejectsUnknownKeysAndResetIsUndoable(){StudioDraft d=new StudioDraft(Collections.singletonMap("size",100));assertThrows(IllegalArgumentException.class,()->d.set("permission",true));d.set("size",120);d.reset();assertTrue(d.changes().isEmpty());d.undo();assertEquals(120,d.get("size"));}
    @Test public void focusUsesIdentityAfterReorderAndIsProfileScoped(){FocusMemory.remember("adult","apps","netflix");assertEquals(1,FocusMemory.index("adult","apps",Arrays.asList("youtube","netflix"),0));assertEquals(0,FocusMemory.index("child","apps",Arrays.asList("youtube","netflix"),0));assertEquals(0,FocusMemory.index("adult","apps",Collections.singletonList("youtube"),8));}
}
