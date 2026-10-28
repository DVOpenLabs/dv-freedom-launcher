package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class LockPolicyTest {
 @Test public void aSaltedRecordVerifiesOnlyTheChosenPin()throws Exception{String r=LockPolicy.create("123456");assertFalse(r.contains("123456"));assertTrue(LockPolicy.verify(r,"123456"));assertFalse(LockPolicy.verify(r,"654321"));assertNotEquals(r,LockPolicy.create("123456"));}
 @Test public void malformedRecordsAndPinsCannotUnlock()throws Exception{assertFalse(LockPolicy.verify("v1:broken","1234"));for(String p:new String[]{"","123","123456789","12ab","１２３４"})try{LockPolicy.validPin(p);fail();}catch(IllegalArgumentException expected){}}
}
