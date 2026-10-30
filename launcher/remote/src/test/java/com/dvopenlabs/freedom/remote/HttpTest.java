package com.dvopenlabs.freedom.remote;
import org.junit.Test;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class HttpTest {
    static final String HOST="192.0.2.20:8000";
    static class Client extends Socket {
        final ByteArrayInputStream input;final ByteArrayOutputStream output=new ByteArrayOutputStream();
        Client(String request){input=new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));}
        @Override public InputStream getInputStream(){return input;}
        @Override public OutputStream getOutputStream(){return output;}
        @Override public void setSoTimeout(int timeout){}
        @Override public void close(){}
        String response(){return new String(output.toByteArray(),StandardCharsets.UTF_8);}
    }
    RemoteService service(){RemoteService s=new RemoteService();s.host=HOST;s.running=true;s.pairing=new Pairing(0);return s;}
    Client post(String path,String origin,String body){return new Client("POST "+path+" HTTP/1.1\r\nHost: "+HOST+"\r\nOrigin: "+origin+"\r\nContent-Type: application/json\r\nContent-Length: "+body.getBytes(StandardCharsets.UTF_8).length+"\r\n\r\n"+body);}
    @Test public void rejectedOriginsConsumeBoundedBodyBeforeSocketClose(){Client c=post("/pair","http://untrusted.example","{\"pin\":\"123456\"}");service().handle(c);assertEquals(0,c.input.available());assertTrue(c.response().startsWith("HTTP/1.1 403"));assertFalse(c.response().contains("Set-Cookie"));}
    @Test public void unpairedActionsCannotReachAControl(){Client c=post("/action","http://"+HOST,"{\"key\":\"home\"}");service().handle(c);assertTrue(c.response().startsWith("HTTP/1.1 403"));assertEquals(0,c.input.available());}
    @Test public void validPairCreatesOnlyHttpOnlySameSiteCookie(){RemoteService service=service();Client c=post("/pair","http://"+HOST,"{\"pin\":\""+service.pairing.pin+"\"}");service.handle(c);assertTrue(c.response().startsWith("HTTP/1.1 200"));assertTrue(c.response().contains("HttpOnly; SameSite=Strict; Max-Age=1800"));assertEquals("{\"ok\":true}",c.response().substring(c.response().indexOf("\r\n\r\n")+4));}
    @Test public void wrongHostIsRejectedWithoutIssuingCookie(){Client c=post("/pair","http://"+HOST,"{\"pin\":\"123456\"}");Client changed=new Client(new String(c.input.readAllBytes(),StandardCharsets.UTF_8).replace("Host: "+HOST,"Host: attacker.example"));service().handle(changed);assertTrue(changed.response().startsWith("HTTP/1.1 403"));assertFalse(changed.response().contains("Set-Cookie"));}
    @Test public void stoppedServerCannotIssuePairingCookie(){RemoteService s=service();s.running=false;Client c=post("/pair","http://"+HOST,"{\"pin\":\""+s.pairing.pin+"\"}");s.handle(c);assertTrue(c.response().startsWith("HTTP/1.1 503"));assertFalse(c.response().contains("Set-Cookie"));}
    @Test public void stoppedServerCannotAcceptAnAuthenticatedAction(){RemoteService s=service();s.pairing.pair(s.pairing.pin,0);s.running=false;Client c=post("/action","http://"+HOST,"{\"key\":\"home\"}");s.handle(c);assertTrue(c.response().startsWith("HTTP/1.1 503"));}
    @Test public void stoppedCommandsDoNotQueueAndroidWork()throws Exception{RemoteService s=service();s.running=false;assertFalse(s.command("home",null));}
}
