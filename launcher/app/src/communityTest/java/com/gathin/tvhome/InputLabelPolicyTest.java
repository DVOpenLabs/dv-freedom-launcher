package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class InputLabelPolicyTest {
    @Test public void customNameKeepsThePort(){assertEquals("Game · HDMI 1",InputLabelPolicy.title("Game","HDMI 1",true));assertEquals("Game · HDMI 3",InputLabelPolicy.title("Game","HDMI 3",true));}
    @Test public void numberedPortsKeepHdmiPrefix(){assertEquals("Console · HDMI 2",InputLabelPolicy.title("Console","2",true));}
    @Test public void noDuplicateNameAndNoInventedPort(){assertEquals("HDMI 4",InputLabelPolicy.title("HDMI 4","HDMI 4",true));assertEquals("Console",InputLabelPolicy.title("Console",null,true));}
    @Test public void philipsSuffixDoesNotLeak(){assertEquals("🎮 Xbox · HDMI 1",InputLabelPolicy.title("Xbox&GAME_CONSOLE","HDMI 1",true));}
}
