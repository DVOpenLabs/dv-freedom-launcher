package com.gathin.tvhome;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;
public class SettingsSearchTest {
    @Test public void aliasesLeadToControlsInsteadOfUnrelatedActions(){
        assertTrue(SettingsSearch.matches("small icons","Icon size","","Appearance"));
        assertTrue(SettingsSearch.matches("gap","App spacing","","Appearance"));
        assertTrue(SettingsSearch.matches("HDMI","Inputs shortcut","","Remotes & inputs"));
        assertTrue(SettingsSearch.matches("font","Text sizes","","Appearance"));
        assertTrue(SettingsSearch.matches("gap","Extra tile spacing","","Appearance"));
        assertTrue(SettingsSearch.matches("highlight","Focus outline","","Appearance"));
        assertTrue(SettingsSearch.matches("bottom","App position","","Layout"));
        assertFalse(SettingsSearch.matches("HDMI","Delete profile","","Profiles"));
    }
    @Test public void everyWordMustMatchAndEmptyAndUnboundedQueriesStayEmpty(){
        assertTrue(SettingsSearch.matches("font sizes","Text sizes","","Appearance"));
        assertFalse(SettingsSearch.matches("font hdmi","Text sizes","","Appearance"));
        assertFalse(SettingsSearch.matches("  ","Text sizes","","Appearance"));
        assertFalse(SettingsSearch.matches("x".repeat(121),"x".repeat(121),"",""));
    }
    @Test public void searchDoesNotDependOnDeviceCasingOrAccents(){
        Locale previous=Locale.getDefault();try{Locale.setDefault(new Locale("tr"));assertTrue(SettingsSearch.matches("INPUTS","Inputs shortcut","",""));assertTrue(SettingsSearch.matches("cafe","Café","",""));}finally{Locale.setDefault(previous);}
    }
    @Test public void scopeDistinguishesSystemAndSharedControlsFromProfileEdits(){
        assertTrue(SettingsSearch.shared("Default Home"));assertTrue(SettingsSearch.shared("Remote helper"));assertTrue(SettingsSearch.shared("Manage offline themes"));assertFalse(SettingsSearch.shared("App position"));assertFalse(SettingsSearch.shared("Layout lock"));
    }
}
