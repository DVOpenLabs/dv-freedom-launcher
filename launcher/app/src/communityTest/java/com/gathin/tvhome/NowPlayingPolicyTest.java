package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class NowPlayingPolicyTest {
    @Test public void playingAndBufferingWinOverPausedAndStopped(){assertTrue(NowPlayingPolicy.rank(3)>NowPlayingPolicy.rank(6));assertTrue(NowPlayingPolicy.rank(6)>NowPlayingPolicy.rank(2));assertEquals(0,NowPlayingPolicy.rank(1));assertEquals(0,NowPlayingPolicy.rank(99));}
    @Test public void metadataIsBoundedAndControlCharactersCannotInjectLines(){assertEquals("Song Artist",NowPlayingPolicy.text("Song\nArtist",100));assertEquals("Short",NowPlayingPolicy.text("Short title",5));assertEquals("",NowPlayingPolicy.text(null,100));}
    @Test public void consentStaysOutsideAiDrafts(){assertFalse(SetupValues.KEYS.contains(NowPlaying.KEY));assertTrue(LocalProfiles.SCOPED.contains(NowPlaying.KEY));}
}
