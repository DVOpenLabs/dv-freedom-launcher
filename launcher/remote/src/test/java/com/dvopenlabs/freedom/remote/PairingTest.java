package com.dvopenlabs.freedom.remote;
import org.junit.Test;
import static org.junit.Assert.*;
public class PairingTest {
    @Test public void wrongPinCannotControl(){Pairing p=new Pairing(0);assertEquals("",p.pair("wrong",1));assertFalse(p.allowed("dvremote=anything",2));}
    @Test public void pinsExpireAndSessionsExpire(){Pairing p=new Pairing(0);String cookie="dvremote="+p.pair(p.pin,100);assertTrue(p.allowed(cookie,101));assertFalse(p.allowed(cookie,1800100));assertEquals("",p.pair(p.pin,300001));}
    @Test public void repeatedFailureBlocksEvenCorrectPinTemporarily(){Pairing p=new Pairing(0);for(int i=0;i<5;i++)p.pair("bad",100);assertEquals("",p.pair(p.pin,101));assertFalse(p.pair(p.pin,60101).isEmpty());}
    @Test public void secondPairingRevokesFirstPhone(){Pairing p=new Pairing(0);String first=p.pair(p.pin,1);String second=p.pair(p.pin,2);assertFalse(p.allowed("dvremote="+first,3));assertTrue(p.allowed("dvremote="+second,3));assertFalse(p.allowed("dvremote="+second+"; attacker=x",3));}
    @Test public void stopRevokesCookiesAndPairingWindow(){Pairing p=new Pairing(0);String token=p.pair(p.pin,1);p.revoke();assertFalse(p.allowed("dvremote="+token,2));assertEquals("",p.pair(p.pin,2));}
}
