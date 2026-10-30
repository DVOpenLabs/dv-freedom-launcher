package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class HomePresetsTest {
    @Test public void presetsStayInsideTheValidatedSetupBoundary() {
        for(int i=0;i<HomePresets.NAMES.length;i++) {
            Map<String,Object> draft=new HashMap<>();draft.put("schema",1);draft.put("settings",HomePresets.settings(i));
            SetupProposal proposal=new SetupProposal(draft,Collections.emptySet());
            assertNull(proposal.favorites);
            assertFalse(proposal.values.containsKey(ThemeLibrary.CHOICE));
            assertEquals(true,proposal.values.get(TileSpacing.FOCUS));
        }
    }
    @Test public void presetDraftsDoNotShareMutableState() {
        HomePresets.settings(0).put("text","dark");
        assertEquals("auto",HomePresets.settings(0).get("text"));
    }
    @Test public void invalidSpacingIsRejectedBeforeAnyApply() {
        for(Object spacing:Arrays.asList(-1,33,"16",16.0)) {
            Map<String,Object> root=new HashMap<>();root.put("schema",1);root.put("settings",Collections.singletonMap("spacing",spacing));
            assertThrows(IllegalArgumentException.class,()->new SetupProposal(root,Collections.emptySet()));
        }
    }
}
