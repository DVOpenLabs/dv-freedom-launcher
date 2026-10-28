package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SettingsChoicePolicyTest {
    @Test public void customImportedSpacingDoesNotFalselyCheckTheme(){assertEquals(0,SettingsChoicePolicy.spacingIndex(false,32));assertEquals(1,SettingsChoicePolicy.spacingIndex(true,0));assertEquals(2,SettingsChoicePolicy.spacingIndex(true,8));assertEquals(3,SettingsChoicePolicy.spacingIndex(true,16));assertEquals(4,SettingsChoicePolicy.spacingIndex(true,24));assertEquals(-1,SettingsChoicePolicy.spacingIndex(true,17));assertEquals(-1,SettingsChoicePolicy.spacingIndex(true,32));}
}
