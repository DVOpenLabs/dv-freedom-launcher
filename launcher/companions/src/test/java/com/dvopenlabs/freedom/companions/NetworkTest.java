package com.dvopenlabs.freedom.companions;
import java.net.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetworkTest {
    @Test public void rejectsLocalPosterTargets()throws Exception{for(String ip:new String[]{"127.0.0.1","10.1.2.3","192.168.1.2","169.254.169.254","100.100.100.200","::1","fc00::1","fe80::1"})assertFalse(ip,Network.publicAddress(InetAddress.getByName(ip)));assertTrue(Network.publicAddress(InetAddress.getByName("8.8.8.8")));}
    @Test public void publicFeedsRequireHttpsAndNoCredentials(){for(String url:new String[]{"http://example.org/x","https://user:secret@example.org/x","https://example.org:8443/x","file:///private","https://example.org/x#secret"}){try{Network.url(url,false);fail(url);}catch(IllegalArgumentException expected){}}assertEquals("https",Network.url("https://example.org/private?key=test",false).scheme());}
    @Test public void explicitHomeAssistantCanUseLocalHttp(){assertEquals(8123,Network.url("http://192.168.1.2:8123",true).port());}
}
