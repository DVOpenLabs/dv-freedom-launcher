// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;
import android.content.Context;
import java.io.File;
import java.util.List;
import org.json.JSONObject;

final class AndroidDevice implements PhoneInstaller.Device {
    final Context context;final TvConnection connection;
    AndroidDevice(Context c,TvConnection d){context=c;connection=d;}
    public boolean trusted(){return connection.confirmed();}
    public String peer(){return connection.candidate;}
    public String fingerprint()throws Exception{return connection.fingerprint();}
    public String home()throws Exception{return connection.home();}
    public int api()throws Exception{return connection.api();}
    public List<String> apps()throws Exception{return connection.apps();}
    public JSONObject installed(File save)throws Exception{String path=connection.apkPath();if(path==null)return null;connection.pull(path,save);VerifiedBundle.Archive a=VerifiedBundle.archive(context,save);if(!SetupPolicy.PACKAGE.equals(a.packageName))throw new java.io.IOException("Unexpected installed package identity");return new JSONObject().put("versionCode",a.version).put("certificate",a.certificate).put("sha256",SetupPolicy.sha(java.nio.file.Files.readAllBytes(save.toPath())));}
    public void install(File apk,String hash)throws Exception{connection.install(apk,hash);}
    public void preserveHome(String home)throws Exception{connection.preserveHome(home);}
    public void launch()throws Exception{connection.launch();}
    public JSONObject nativeCall(String method,JSONObject value)throws Exception{return connection.provider(method,value);}
}
