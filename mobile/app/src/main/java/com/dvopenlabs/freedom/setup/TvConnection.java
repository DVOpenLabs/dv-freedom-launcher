// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;

import android.content.Context;
import android.content.SharedPreferences;
import io.github.muntashirakon.adb.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Paired TLS only, one foreground connection, fixed operation methods, bounded streams. */
final class TvConnection implements AutoCloseable {
    final Context context;final String host;final int port;final Manager manager;final SharedPreferences peers;
    String candidate="";boolean pairing;
    static final class Manager extends AbsAdbConnectionManager {
        final AdbIdentity identity;
        Manager(AdbIdentity id){identity=id;setApi(34);setTimeout(20,TimeUnit.SECONDS);}
        protected PrivateKey getPrivateKey(){return identity.key;}
        protected java.security.cert.Certificate getCertificate(){return identity.certificate;}
        protected String getDeviceName(){return "DVFreedomPhone";}
    }
    TvConnection(Context c,String address,int port)throws Exception{context=c.getApplicationContext();host=SetupPolicy.host(address);if(port<1||port>65535)throw new IllegalArgumentException();this.port=port;peers=c.getSharedPreferences("paired-peers",0);manager=new Manager(new AdbIdentity(c,host));PeerTrust.use((client,chain)->{
        try{String seen=SetupPolicy.sha(chain[0].getEncoded());if(pairing)return;String pin=peers.getString("peer/"+host,"");if(!pin.isEmpty()&&!pin.equals(seen))throw new CertificateException("TV identity changed; pair again using the code on the TV");candidate=seen;}catch(CertificateException e){throw e;}catch(Exception e){throw new CertificateException("TV identity unavailable");}
    });}
    void pair(int pairingPort,String code)throws Exception{if(!code.matches("[0-9]{6}"))throw new IllegalArgumentException("Use the six-digit code currently shown by the TV");pairing=true;try{if(pairingPort<1||pairingPort>65535||!manager.pair(host,pairingPort,code))throw new IOException("Pairing was not accepted");if(!peers.edit().putBoolean("paired/"+host,true).remove("peer/"+host).commit())throw new IOException("Could not save pairing");}finally{pairing=false;}}
    void connect()throws Exception{if(!peers.getBoolean("paired/"+host,false))throw new IOException("Pair this phone using the TV’s code first");candidate="";if(!manager.connect(host,port)||candidate.isEmpty()){close();throw new IOException("Paired TLS connection unavailable. Check the connection port, not the pairing port.");}}
    boolean confirmed(){return !candidate.isEmpty()&&candidate.equals(peers.getString("peer/"+host,""));}
    void confirmPeer(){if(candidate.isEmpty())throw new IllegalStateException();if(!peers.edit().putString("peer/"+host,candidate).commit())throw new IllegalStateException("Could not save this TV identity");}
    void trusted(){if(!confirmed())throw new IllegalStateException("Confirm this TV before an operation");}
    private String shell(String command,int limit)throws Exception{try(AdbStream stream=manager.openStream("shell:"+command)){return new String(VerifiedBundle.read(stream.openInputStream(),limit),StandardCharsets.UTF_8).trim();}}
    String model()throws Exception{return shell("getprop ro.product.model",1024);}
    int api()throws Exception{return Integer.parseInt(shell("getprop ro.build.version.sdk",32));}
    String fingerprint()throws Exception{String id=shell("settings get secure android_id",256);if(!id.matches("[a-fA-F0-9]{8,64}")||id.matches("0+")||id.equalsIgnoreCase("9774d56d682e549c"))throw new IOException("Stable TV identifier unavailable");return SetupPolicy.sha(("dv-freedom-device:"+id).getBytes(StandardCharsets.UTF_8));}
    String homeRaw()throws Exception{String[] lines=shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME",4096).split("\n");return lines[lines.length-1].trim();}
    String home()throws Exception{return SetupPolicy.component(homeRaw());}
    void preserveHome(String before)throws Exception{SetupPolicy.component(before);String after=homeRaw();if(after.equals(before))return;if(!after.equals("android/com.android.internal.app.ResolverActivity"))throw new IOException("Home changed during installation. Your new selection was not overwritten.");String[] candidates=shell("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME",16384).split("\n");boolean found=false;for(String c:candidates)if(c.trim().equals(before))found=true;if(!found)throw new IOException("Original Home is no longer available");trusted();shell("cmd package set-home-activity --user 0 "+SetupPolicy.quote(before),4096);if(!home().equals(before))throw new IOException("Choose your original Home on the TV before continuing");}
    List<String> apps()throws Exception{String data=shell("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER",128*1024);Set<String> p=new TreeSet<>();for(String line:data.split("\n"))if(line.matches("[A-Za-z][\\w.]*/[^\\s]+")){String pkg=line.substring(0,line.indexOf('/'));if(!pkg.equals(SetupPolicy.PACKAGE))p.add(pkg);}return new ArrayList<>(p);}
    String apkPath()throws Exception{String data=shell("pm path "+SetupPolicy.PACKAGE,8192);if(data.isEmpty())return null;if(!data.matches("package:/data/app/[A-Za-z0-9_./+=~-]+/base\\.apk")||data.contains(".."))throw new IOException("Split or unverified APK installation needs desktop review");return data.substring(8);}
    org.json.JSONObject provider(String method,org.json.JSONObject value)throws Exception{if(!Arrays.asList("status","preview","revert","restore").contains(method))throw new IllegalArgumentException();if(!method.equals("status"))trusted();String command="content call --uri content://"+SetupPolicy.PACKAGE+".theme --method product_installer_"+method;if(value!=null){String data=value.toString();if(data.getBytes(StandardCharsets.UTF_8).length>60000)throw new IOException("Setup request exceeds its limit");command+=" --arg "+SetupPolicy.quote(data);}String output=shell(command,256*1024);int start=output.indexOf("json=");if(start<0)throw new IOException("Unlock launcher editing on the TV and use a compatible Setup build");org.json.JSONTokener tokener=new org.json.JSONTokener(output.substring(start+5));Object parsed=tokener.nextValue();if(!(parsed instanceof org.json.JSONObject))throw new IOException();org.json.JSONObject result=(org.json.JSONObject)parsed;if(result.optInt("schema")!=1)throw new IOException("Unsupported launcher interface");return result;}
    void pull(String path,File target)throws Exception{if(!path.matches("/data/app/[A-Za-z0-9_./+=~-]+/base\\.apk")||path.contains(".."))throw new IOException();try(AdbStream stream=manager.openStream("sync:");OutputStream file=new FileOutputStream(target)){DataInputStream in=new DataInputStream(stream.openInputStream());OutputStream out=stream.openOutputStream();frame(out,"RECV",path.getBytes(StandardCharsets.UTF_8));long size=0;while(true){String id=tag(in);int n=little(in);if(id.equals("DONE"))break;if(!id.equals("DATA")||n<1||n>65536||size+n>SetupPolicy.MAX_APK)throw new IOException("APK transfer refused");byte[] b=new byte[n];in.readFully(b);file.write(b);size+=n;}frame(out,"QUIT",new byte[0]);}}
    void install(File apk,String hash)throws Exception{trusted();if(!hash.matches("[a-f0-9]{64}")||!SetupPolicy.sha(java.nio.file.Files.readAllBytes(apk.toPath())).equals(hash))throw new IOException("Reviewed APK changed");String path="/data/local/tmp/dv-freedom-"+hash.substring(0,16)+".apk";
        try{try(AdbStream stream=manager.openStream("sync:");InputStream file=new FileInputStream(apk)){DataInputStream in=new DataInputStream(stream.openInputStream());OutputStream out=stream.openOutputStream();frame(out,"SEND",(path+",33206").getBytes(StandardCharsets.UTF_8));byte[] b=new byte[65536];for(int n;(n=file.read(b))!=-1;){out.write("DATA".getBytes(StandardCharsets.US_ASCII));writeLittle(out,n);out.write(b,0,n);out.flush();}out.write("DONE".getBytes(StandardCharsets.US_ASCII));writeLittle(out,(int)(System.currentTimeMillis()/1000));out.flush();if(!tag(in).equals("OKAY")||little(in)!=0)throw new IOException("APK transfer failed");frame(out,"QUIT",new byte[0]);}
            if(!shell("sha256sum "+path,1024).startsWith(hash+" "))throw new IOException("Transferred APK checksum mismatch");if(!shell("pm install -r "+path,16384).equals("Success"))throw new IOException("Android refused installation. No protection was bypassed.");
        }finally{try{shell("rm -f "+path,1024);}catch(Exception ignored){}}
    }
    void launch()throws Exception{trusted();shell("am start -n "+SetupPolicy.PACKAGE+"/com.gathin.tvhome.MainActivity",8192);}
    static void frame(OutputStream out,String id,byte[] b)throws IOException{out.write(id.getBytes(StandardCharsets.US_ASCII));writeLittle(out,b.length);out.write(b);out.flush();}
    static void writeLittle(OutputStream out,int v)throws IOException{for(int n=0;n<4;n++)out.write(v>>>(8*n)&255);}
    static int little(DataInputStream in)throws IOException{return Integer.reverseBytes(in.readInt());}
    static String tag(DataInputStream in)throws IOException{byte[] b=new byte[4];in.readFully(b);return new String(b,StandardCharsets.US_ASCII);}
    @Override public void close(){try{manager.disconnect();}catch(Exception ignored){}PeerTrust.use(null);}
}
