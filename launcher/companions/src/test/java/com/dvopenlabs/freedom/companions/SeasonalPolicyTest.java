package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import static org.junit.Assert.*;
public class SeasonalPolicyTest {
    @Test public void packExpiresAtTheEndOfTheChosenLocalDate(){var s=new SeasonalPolicy.Spec("winter",20000,20006,true);assertFalse(s.active(19999));assertTrue(s.active(20000));assertTrue(s.active(20006));assertFalse(s.active(20007));}
    @Test public void datesAndSourcesCannotEscapeTheSmallContract(){for(String raw:new String[]{"v1|winter|1|91|1","v1|other|1|2|1","v1|winter|-1|2|1","v1|winter|2|1|1","v1|winter|1|2|yes","https://example.invalid"})assertThrows(IllegalArgumentException.class,()->SeasonalPolicy.parse(raw));}
    @Test public void offAndRoundTripNeverInventASelection(){assertNull(SeasonalPolicy.parse(""));for(String id:SeasonalPolicy.IDS){var s=new SeasonalPolicy.Spec(id,20000,20089,false);assertEquals(s.encode(),SeasonalPolicy.parse(s.encode()).encode());assertFalse(SeasonalPolicy.parse(s.encode()).motion);}}
}
