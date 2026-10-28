package com.gathin.tvhome;
import org.junit.Test;import static org.junit.Assert.*;
public class SettingsLayoutPolicyTest {
 @Test public void ordinaryTvHasRoomForContextualPreviews(){assertTrue(SettingsLayoutPolicy.previews(540,1));assertTrue(SettingsLayoutPolicy.previews(720,1));}
 @Test public void largerTextPrioritizesReadableHelp(){assertFalse(SettingsLayoutPolicy.previews(540,1.3f));assertFalse(SettingsLayoutPolicy.previews(540,1.5f));}
 @Test public void shortViewportsOmitTheIllustrations(){assertFalse(SettingsLayoutPolicy.previews(480,1));assertFalse(SettingsLayoutPolicy.previews(360,1));}
}
