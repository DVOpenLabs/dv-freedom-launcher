package com.dvopenlabs.freedom.camera;
import org.junit.Test;
import java.net.*;
import static org.junit.Assert.*;
public class CameraPolicyTest {
    @Test public void onlyExplicitSecureOrPrivateRtspSources(){
        assertEquals("https",CameraPolicy.url("https://camera.example/live/main.m3u8").getScheme());
        assertEquals("rtsp",CameraPolicy.url("rtsp://10.2.3.4:8554/main").getScheme());
        for(String url:new String[]{"http://10.2.3.4/live","rtsp://8.8.8.8/main","rtsp://127.0.0.1/main","rtsp://camera.local/main","https://user:pass@example.org/a","file:///a","https://example.org/a#secret","rtsp://010.0.0.1/a"})try{CameraPolicy.url(url);fail(url);}catch(IllegalArgumentException expected){}
    }
    @Test public void hlsCredentialsCannotCrossOrigin(){
        URI base=CameraPolicy.url("https://camera.example/live/main.m3u8");assertTrue(CameraPolicy.sameOrigin(base,URI.create("https://camera.example:443/live/segment.ts")));
        for(String url:new String[]{"http://camera.example/a","https://other.example/a","https://camera.example:444/a","https://user@camera.example/a"})assertFalse(CameraPolicy.sameOrigin(base,URI.create(url)));
    }
    @Test public void boundedSessionAndCredentials(){assertEquals(900000,CameraPolicy.duration(15));try{CameraPolicy.duration(60);fail();}catch(IllegalArgumentException expected){}try{CameraPolicy.credential("x\r\nAuthorization: value");fail();}catch(IllegalArgumentException expected){}}
    @Test public void rtspSocketCannotFollowAnotherHostOrPort()throws Exception{LanSocketFactory f=new LanSocketFactory("10.2.3.4",8554);f.check(new InetSocketAddress(InetAddress.getByName("10.2.3.4"),8554));for(String host:new String[]{"10.2.3.5","8.8.8.8"})try{f.check(new InetSocketAddress(InetAddress.getByName(host),8554));fail();}catch(java.io.IOException expected){}try{f.check(new InetSocketAddress(InetAddress.getByName("10.2.3.4"),554));fail();}catch(java.io.IOException expected){}}
}
