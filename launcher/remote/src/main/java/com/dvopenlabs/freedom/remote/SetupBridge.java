package com.dvopenlabs.freedom.remote;

import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;

/** Fixed same-signer launchers only. The phone cannot choose an arbitrary provider or apply a draft. */
final class SetupBridge {
    static final String[] PACKAGES={"com.dvopenlabs.freedomlauncher","com.dvopenlabs.cytv.usabilitytest"};
    static Uri uri(Context c){for(String pkg:PACKAGES)if(c.getPackageManager().checkSignatures(c.getPackageName(),pkg)==PackageManager.SIGNATURE_MATCH){Uri u=Uri.parse("content://"+pkg+".phone-setup");if(c.getPackageManager().resolveContentProvider(u.getAuthority(),0)!=null)return u;}throw new IllegalStateException("A compatible signed launcher is not installed");}
    static JSONObject call(Context c,String method,String arg)throws Exception{Bundle b=c.getContentResolver().call(uri(c),method,arg,null);return new JSONObject(b==null?"{}":b.getString("json","{}"));}
    static void revoke(Context c){try{call(c,"revoke",null);}catch(Exception ignored){}}
    static JSONObject upload(Context c,File file,String kind,JSONObject inventory,java.util.function.BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new IOException("Remote stopped");MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];for(int n;(n=in.read(b))!=-1;)md.update(b,0,n);}StringBuilder sha=new StringBuilder();for(byte b:md.digest())sha.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        JSONObject metadata=new JSONObject().put("schema",1).put("profile",inventory.getString("profile")).put("revision",inventory.getString("revision")).put("kind",kind).put("bytes",file.length()).put("sha256",sha).put("name","Phone wallpaper."+kind);
        if(!active.getAsBoolean())throw new IOException("Remote stopped");JSONObject receipt=call(c,"media-prepare",metadata.toString());String id=receipt.getString("id");
        Uri destination=uri(c).buildUpon().appendPath("upload").appendPath(id).build();
        ParcelFileDescriptor pipe=c.getContentResolver().openFileDescriptor(destination,"w");
        if(pipe==null)throw new IOException("Transfer unavailable");
        try(OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipe);InputStream in=new FileInputStream(file)){
            byte[] b=new byte[32768];for(int n;(n=in.read(b))!=-1;){if(!active.getAsBoolean()||Thread.currentThread().isInterrupted())throw new IOException("Remote stopped");out.write(b,0,n);}
            if(!active.getAsBoolean())throw new IOException("Remote stopped");
        }catch(Exception bad){try{pipe.closeWithError("Phone transfer cancelled");}catch(IOException ignored){}throw bad;}

        return new JSONObject().put("ok",true).put("reviewRequired",true);
    }
    private SetupBridge(){}
}
