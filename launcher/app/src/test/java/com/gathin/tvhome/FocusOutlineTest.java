package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class FocusOutlineTest {
    @Test public void oldSetupBooleansAndNativeStringsMeanTheSameThing(){assertEquals(FocusOutline.read("double"),FocusOutline.read(true));assertEquals(FocusOutline.read("rim"),FocusOutline.read(false));assertEquals("",FocusOutline.read(null));}
    @Test public void malformedTypesFallBackWithoutCrashingHome(){assertEquals("",FocusOutline.read(12));assertEquals("",FocusOutline.read("unsupported"));assertTrue(FocusOutline.valid("double"));assertFalse(FocusOutline.valid("true"));}
}
