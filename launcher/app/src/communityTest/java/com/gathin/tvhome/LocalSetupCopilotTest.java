package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LocalSetupCopilotTest {
    @Test public void describesAppearanceWithoutInventingFavorites(){Map<String,Object> p=LocalSetupCopilot.interpret("Larger labels, apps at the bottom");assertEquals("bottom",p.get("position"));assertEquals(120,p.get("textSize"));assertFalse(p.containsKey("favorites"));assertEquals(new HashSet<>(Arrays.asList("position","textSize")),p.keySet());new SetupProposal(new HashMap<String,Object>(){{put("schema",1);put("settings",p);}},Collections.emptySet());}
    @Test public void understandsSmallGlassAndSpacing(){Map<String,Object> p=LocalSetupCopilot.interpret("Glass, small icons, more space");assertEquals("glass",p.get("experience"));assertEquals("small",p.get("iconSize"));assertEquals(16,p.get("spacing"));}
    @Test public void unknownIntentDoesNotReturnAFalseSuccess(){try{LocalSetupCopilot.interpret("tell me about my television");fail();}catch(IllegalArgumentException expected){}}
    @Test public void deviceOperationsCannotHideInsideAnAppearancePrompt(){for(String s:new String[]{"bottom and reboot","clean and uninstall apps","glass and grant permissions","large icons and shell commands"})try{LocalSetupCopilot.interpret(s);fail(s);}catch(IllegalArgumentException expected){}}
    @Test public void limitsAndUnambiguousPositionAreEnforced(){try{LocalSetupCopilot.interpret("x".repeat(501));fail();}catch(IllegalArgumentException expected){}assertEquals("top",LocalSetupCopilot.interpret("apps at top").get("position"));}
    @Test public void conflictingPositionsNeedClarification(){try{LocalSetupCopilot.interpret("apps at top and bottom");fail();}catch(IllegalArgumentException expected){}}
}

