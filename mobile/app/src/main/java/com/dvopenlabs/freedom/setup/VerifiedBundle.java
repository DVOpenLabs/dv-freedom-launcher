// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;
import android.content.Context;
import android.content.pm.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.json.JSONObject;

/** The same offline Ed25519 release contract used by desktop Setup, checked before any TV mutation. */
final class VerifiedBundle {
    final JSONObject release;final File apk;
    VerifiedBundle(Context c)throws Exception{
        JSONObject envelope=new JSONObject(new String(read(c.getAssets().open("release/catalog.json"),8192),StandardCharsets.UTF_8));
        JSONObject trust=new JSONObject(new String(read(c.getAssets().open("release/trust.json"),2048),StandardCharsets.UTF_8));
        SetupPolicy.fields(envelope,"release","signature");release=envelope.getJSONObject("release");verify(release,envelope.getString("signature"),trust.getString("publicKey"));
        byte[] bytes=read(c.getAssets().open("release/launcher.apk"),SetupPolicy.MAX_APK);
        if(bytes.length!=release.getInt("bytes")||!SetupPolicy.sha(bytes).equals(release.getString("sha256")))throw new IOException("Offline APK checksum mismatch");
        apk=new File(c.getCacheDir(),"verified-launcher.apk");try(FileOutputStream out=new FileOutputStream(apk)){out.write(bytes);out.getFD().sync();}
        Archive actual=archive(c,apk);if(!actual.packageName.equals(SetupPolicy.PACKAGE)||actual.version!=release.getInt("versionCode")||!actual.certificate.equals(release.getString("certificate")))throw new IOException("APK identity does not match signed release");
    }
    static void verify(JSONObject r,String signature,String key)throws Exception{
        SetupPolicy.fields(r,"schema","package","versionCode","versionName","minSdk","file","bytes","sha256","certificate","channel");
        if(r.getInt("schema")!=1||!SetupPolicy.PACKAGE.equals(r.getString("package"))||!"launcher.apk".equals(r.getString("file"))||!"private-alpha".equals(r.getString("channel"))||r.getInt("minSdk")!=26||r.getInt("versionCode")<1||r.getInt("bytes")<1||r.getInt("bytes")>SetupPolicy.MAX_APK||r.getString("versionName").length()>80||!r.getString("sha256").matches("[a-f0-9]{64}")||!r.getString("certificate").matches("[a-f0-9]{64}"))throw new IOException("Unsupported release identity");
        byte[] raw=SetupPolicy.canonical(r).getBytes(StandardCharsets.UTF_8),pub=Base64.getDecoder().decode(key),sig=Base64.getDecoder().decode(signature);if(pub.length!=32||sig.length!=64)throw new IOException("Invalid release signature");Ed25519Signer verifier=new Ed25519Signer();verifier.init(false,new Ed25519PublicKeyParameters(pub,0));verifier.update(raw,0,raw.length);if(!verifier.verifySignature(sig))throw new IOException("Offline release signature failed");
    }
    static final class Archive {final String packageName,certificate;final long version;Archive(String p,long v,String s){packageName=p;version=v;certificate=s;}}
    static Archive archive(Context c,File file)throws Exception{PackageInfo p=c.getPackageManager().getPackageArchiveInfo(file.getPath(),PackageManager.GET_SIGNING_CERTIFICATES);if(p==null||p.signingInfo==null||p.signingInfo.hasMultipleSigners()||p.signingInfo.getApkContentsSigners().length!=1)throw new IOException("APK signer unavailable");return new Archive(p.packageName,p.getLongVersionCode(),SetupPolicy.sha(p.signingInfo.getApkContentsSigners()[0].toByteArray()));}
    static byte[] read(InputStream source,int limit)throws IOException{try(InputStream in=source;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;){if(out.size()+n>limit)throw new IOException("Response exceeds safe limit");out.write(b,0,n);}return out.toByteArray();}}
}
