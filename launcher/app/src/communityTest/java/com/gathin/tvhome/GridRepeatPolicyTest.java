package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class GridRepeatPolicyTest {
    @Test public void firstPressAndReleaseStayImmediate(){GridRepeatPolicy p=new GridRepeatPolicy();assertFalse(p.consume(22,true,0,100,280,true));assertTrue(p.consume(22,true,1,150,280,true));assertFalse(p.consume(22,false,0,160,280,true));assertFalse(p.consume(22,true,0,170,280,true));}
    @Test public void onlyExistingRepeatsAreSlowed(){GridRepeatPolicy p=new GridRepeatPolicy();p.consume(22,true,0,100,180,true);assertFalse(p.consume(22,true,3,280,180,true));assertTrue(p.consume(22,true,4,300,180,true));assertFalse(p.consume(21,true,0,310,180,true));}
    @Test public void disabledMenusAndOtherKeysAreUntouched(){GridRepeatPolicy p=new GridRepeatPolicy();assertFalse(p.consume(22,true,9,100,0,true));assertFalse(p.consume(22,true,9,120,280,false));assertFalse(p.consume(24,true,9,130,280,true));p.reset();assertFalse(p.consume(22,true,1,140,280,true));}
    @Test public void importedIntervalsAreBounded(){assertTrue(GridRepeatPolicy.valid(180));assertTrue(GridRepeatPolicy.valid(0));assertFalse(GridRepeatPolicy.valid(-1));assertFalse(GridRepeatPolicy.valid(181));assertFalse(GridRepeatPolicy.valid("280"));}
}
