// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;
import org.junit.Test;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import static org.junit.Assert.*;
public class BundlePolicyTest {
    @Test public void canonicalMatchesDesktopAsciiContract()throws Exception{JSONObject v=new JSONObject().put("z","é\n").put("a",1);assertEquals("{\"a\":1,\"z\":\"\\u00e9\\n\"}",SetupPolicy.canonical(v));}
    @Test public void tamperedSignedMetadataCannotSelectAnotherPackage()throws Exception{Ed25519PrivateKeyParameters key=new Ed25519PrivateKeyParameters(new java.security.SecureRandom());JSONObject r=new JSONObject().put("schema",1).put("package",SetupPolicy.PACKAGE).put("versionCode",2).put("versionName","test").put("minSdk",26).put("file","launcher.apk").put("bytes",3).put("sha256","a".repeat(64)).put("certificate","b".repeat(64)).put("channel","private-alpha");byte[] data=SetupPolicy.canonical(r).getBytes(StandardCharsets.UTF_8);Ed25519Signer signer=new Ed25519Signer();signer.init(true,key);signer.update(data,0,data.length);String sig=Base64.getEncoder().encodeToString(signer.generateSignature()),pub=Base64.getEncoder().encodeToString(key.generatePublicKey().getEncoded());VerifiedBundle.verify(r,sig,pub);r.put("versionCode",3);try{VerifiedBundle.verify(r,sig,pub);fail();}catch(Exception expected){}r.put("package","org.unknown.target");try{VerifiedBundle.verify(r,sig,pub);fail();}catch(Exception expected){}}
    @Test public void onlyExplicitPrivateNetworkIpv4AndValidPortsAreAccepted(){assertEquals("192.168.1.8",SetupPolicy.host("192.168.1.8"));for(String h:new String[]{"127.0.0.1","8.8.8.8","192.168.1.8;id","192.168.001.1","10.0.0.999"})try{SetupPolicy.host(h);fail(h);}catch(IllegalArgumentException expected){}for(String p:new String[]{"0","65536","5555;id"})try{SetupPolicy.port(p);fail();}catch(IllegalArgumentException expected){}}
    @Test public void setupCannotSelectAFactoryOrResolverAsOriginalHome(){for(String s:new String[]{"android/com.android.internal.app.ResolverActivity","vendor.factory/.Main","org.home/.Main;id"})try{SetupPolicy.component(s);fail(s);}catch(IllegalArgumentException expected){}assertEquals("org.home/.Main",SetupPolicy.component("org.home/.Main"));}
}
