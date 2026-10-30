package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.*;

/** One bounded incoming file in app-private cache. No current theme or profile file is overwritten. */
final class MediaInbox {
    private static final Object LOCK=new Object();
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService TIMEOUT=Executors.newSingleThreadScheduledExecutor();
    private static File root(Context c){File root=new File(c.getCacheDir(),"incoming-media");if(!root.isDirectory())root.mkdirs();return root;}
    private static File record(Context c){return new File(root(c),"receipt.json");}
    private static void write(Context c,JSONObject value)throws Exception{
        AtomicFile file=new AtomicFile(record(c));FileOutputStream out=null;
        try{out=file.startWrite();out.write(value.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
        catch(Exception failure){if(out!=null)file.failWrite(out);throw failure;}
    }
    static JSONObject load(Context c)throws Exception{
        File file=record(c);if(!file.isFile()||file.length()>8192)throw new IOException("No media is waiting for review");
        try(InputStream in=new AtomicFile(file).openRead()){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[2048];for(int count;(count=in.read(buffer))!=-1;){if(bytes.size()+count>8192)throw new IOException("Invalid transfer receipt");bytes.write(buffer,0,count);}
            JSONObject data=new JSONObject(bytes.toString("UTF-8"));
            if(!MediaInboxPolicy.id(data.optString("id"))||!MediaInboxPolicy.kind(data.optString("kind"))||!MediaInboxPolicy.hash(data.optString("sha256"))||!MediaInboxPolicy.bytes(data.optString("kind"),data.optLong("bytes"))||!MediaInboxPolicy.fresh(data.optLong("created"),System.currentTimeMillis()))throw new IOException("This media transfer expired. Send it again from the computer.");
            return data;
        }
    }
    static File file(Context c,JSONObject data){return new File(root(c),data.optString("id")+".media");}
    static void bound(Context c,JSONObject data)throws Exception{
        Config.load(c);
        if(!data.getString("profile").equals(ProfileStore.id())||!data.getString("revision").equals(SetupTransfer.revision(c)))throw new IOException("Home changed. Review the transfer again from the computer.");
    }
    static JSONObject prepare(Context c,String raw)throws Exception{
        if(raw==null||raw.length()>8192)throw new IllegalArgumentException("Transfer request too large");
        ThemeArchive.validateJsonObject(raw);JSONObject value=new JSONObject(raw);
        if(!ProfileStore.object(value).keySet().equals(new HashSet<>(Arrays.asList("schema","profile","revision","kind","bytes","sha256","name")))||!Integer.valueOf(1).equals(value.opt("schema"))||!(value.opt("bytes") instanceof Integer||value.opt("bytes") instanceof Long))throw new IllegalArgumentException("Unsupported transfer request");
        for(String key:Arrays.asList("profile","revision","kind","sha256","name"))if(!(value.opt(key) instanceof String)||value.getString(key).length()>128)throw new IllegalArgumentException("Invalid transfer field");
        String kind=value.getString("kind"),sha=value.getString("sha256"),name=MediaInboxPolicy.name(value.getString("name"));
        if(!MediaInboxPolicy.bytes(kind,value.getLong("bytes"))||!MediaInboxPolicy.hash(sha))throw new IllegalArgumentException("Invalid media type, size or checksum");
        synchronized(LOCK){
            bound(c,value);
            JSONObject old=null;try{old=load(c);}catch(Exception expired){}
            if(old!=null&&Arrays.asList("prepared","receiving","ready","importing").contains(old.optString("state")))throw new IOException("Finish or discard the received media on the TV before sending another file");
            if(root(c).getUsableSpace()<value.getLong("bytes")*3+32L*1024*1024)throw new IOException("Not enough free TV storage for a safe media import");
            File[] stale=root(c).listFiles();if(stale!=null)for(File file:stale)if(file.getName().matches("[0-9a-f]{32}\\.(media|part)"))file.delete();
            JSONObject receipt=new JSONObject().put("id",UUID.randomUUID().toString().replace("-","")).put("schema",1).put("profile",value.getString("profile")).put("revision",value.getString("revision"))
                    .put("kind",kind).put("bytes",value.getLong("bytes")).put("sha256",sha).put("name",name).put("created",System.currentTimeMillis()).put("state","prepared");
            write(c,receipt);return receipt;
        }
    }
    static ParcelFileDescriptor open(Context context,String id,String mode)throws Exception{
        Context c=context.getApplicationContext();
        synchronized(LOCK){
            JSONObject receipt=load(c);
            if(!"w".equals(mode)||!MediaInboxPolicy.id(id)||!id.equals(receipt.getString("id"))||!"prepared".equals(receipt.getString("state")))throw new IOException("No matching prepared transfer");
            receipt.put("state","receiving");write(c,receipt);
            ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();
            ScheduledFuture<?> deadline=TIMEOUT.schedule(()->{try{pipe[0].closeWithError("Media transfer timed out");}catch(IOException ignored){}},120,TimeUnit.SECONDS);
            IO.execute(()->{
                File part=new File(root(c),id+".part");
                try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);OutputStream out=new FileOutputStream(part)){
                    MessageDigest hash=MessageDigest.getInstance("SHA-256");long bytes=0;byte[] buffer=new byte[65536];
                    for(int count;(count=in.read(buffer))!=-1;){bytes+=count;if(bytes>receipt.getLong("bytes"))throw new IOException("Media too large");hash.update(buffer,0,count);out.write(buffer,0,count);}
                    out.flush();StringBuilder hex=new StringBuilder();for(byte b:hash.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
                    if(bytes!=receipt.getLong("bytes")||!hex.toString().equals(receipt.getString("sha256")))throw new IOException("Media checksum mismatch");
                    synchronized(LOCK){if(!id.equals(load(c).getString("id"))||!part.renameTo(file(c,receipt)))throw new IOException("Transfer expired");receipt.put("state","ready");write(c,receipt);}
                }catch(Exception failure){synchronized(LOCK){try{if(id.equals(load(c).getString("id"))){receipt.put("state","error");write(c,receipt);}}catch(Exception ignored){}}}
                finally{deadline.cancel(false);if(part.exists())part.delete();}
            });
            return pipe[1];
        }
    }
    static JSONObject status(Context c,String id)throws Exception{
        synchronized(LOCK){JSONObject receipt=load(c);if(!MediaInboxPolicy.id(id)||!id.equals(receipt.getString("id")))throw new IOException("Transfer unavailable");return new JSONObject().put("schema",1).put("id",id).put("state",receipt.getString("state"));}
    }
    static void review(Context c,String id)throws Exception{
        synchronized(LOCK){JSONObject receipt=load(c);if(!MediaInboxPolicy.id(id)||!id.equals(receipt.getString("id"))||!"ready".equals(receipt.getString("state")))throw new IOException("Media is not ready for review");bound(c,receipt);c.startActivity(new Intent(c,MediaReviewActivity.class).putExtra("id",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    }
    static String label(Context c){try{JSONObject receipt=load(c);return "ready".equals(receipt.optString("state"))?receipt.optString("name"):"No file waiting";}catch(Exception none){return "No file waiting";}}
    static void show(android.app.Activity a){try{review(a,load(a).getString("id"));}catch(Exception missing){android.widget.Toast.makeText(a,"Send a file using DV Freedom Setup → Use your phone, then choose Review on TV.",android.widget.Toast.LENGTH_LONG).show();}}
    static void state(Context c,String id,String next)throws Exception{
        synchronized(LOCK){JSONObject receipt=load(c);if(!id.equals(receipt.getString("id")))throw new IOException("Media changed");receipt.put("state",next);write(c,receipt);if("imported".equals(next)||"discarded".equals(next))file(c,receipt).delete();}
    }
    private MediaInbox(){}
}
