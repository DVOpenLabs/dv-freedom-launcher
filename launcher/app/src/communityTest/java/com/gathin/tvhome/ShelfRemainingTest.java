package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShelfRemainingTest {
    @Test public void remainingTimeUsesPublishedMillis(){assertEquals(2,ContentShelfPolicy.minutesLeft(1000,121000));assertEquals(1,ContentShelfPolicy.minutesLeft(119999,120000));assertEquals(0,ContentShelfPolicy.minutesLeft(120000,120000));}
    @Test public void unknownOrInconsistentProgressIsNeverInvented(){assertEquals(-1,ContentShelfPolicy.minutesLeft(-1,120000));assertEquals(-1,ContentShelfPolicy.minutesLeft(200,0));assertEquals(-1,ContentShelfPolicy.minutesLeft(200,100));}
}
