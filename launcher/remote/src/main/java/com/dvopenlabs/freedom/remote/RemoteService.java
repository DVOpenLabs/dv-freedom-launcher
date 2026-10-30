package com.dvopenlabs.freedom.remote;
import android.app.*;
import android.content.*;
import android.os.*;
import android.media.AudioManager;
import android.view.KeyEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Explicitly started local server. Bounded sockets, strict origins, PIN and RAM-only session. */
public final class RemoteService extends Service {
    static volatile String address="",pin="";ServerSocket server;ExecutorService workers;Pairing pairing;String host;
    volatile boolean running;long lastAction;final Handler main=new Handler(Looper.getMainLooper());
    public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(server!=null)return START_NOT_STICKY;
        try{String ip=intent==null?"":intent.getStringExtra("ip");InetAddress selected=InetAddress.getByName(ip);if(!selected.isSiteLocalAddress()||selected.isLoopbackAddress()||NetworkInterface.getByInetAddress(selected)==null)throw new IOException();
            server=new ServerSocket();server.bind(new InetSocketAddress(selected,0),4);host=ip+":"+server.getLocalPort();pairing=new Pairing(SystemClock.elapsedRealtime());address="http://"+host;pin=pairing.pin;
            NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("remote","Paired phone remote",NotificationManager.IMPORTANCE_LOW));
            startForeground(51,new Notification.Builder(this,"remote").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("DV Freedom phone remote is on").setContentText("Open Phone Remote on TV to stop or revoke pairing").setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,RemoteActivity.class),PendingIntent.FLAG_IMMUTABLE)).build());
            workers=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),new ThreadPoolExecutor.AbortPolicy());
            running=true;new Thread(()->{while(server!=null&&!server.isClosed())try{Socket socket=server.accept();try{workers.execute(()->handle(socket));}catch(RejectedExecutionException busy){socket.close();}}catch(IOException closed){break;}},"dv-remote-listener").start();
        }catch(Exception failed){stopSelf();}return START_NOT_STICKY;
    }
    // All commands are allowlisted; there is no ADB, shell, file browser or arbitrary intent API.
    boolean command(String key,String text)throws Exception{
        if(!running)return false;
        FutureTask<Boolean> task=new FutureTask<>(()->{if(!running)return false;if(key.equals("volumeUp")||key.equals("volumeDown")){getSystemService(AudioManager.class).adjustStreamVolume(AudioManager.STREAM_MUSIC,key.equals("volumeUp")?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);return true;}if(key.equals("play")){AudioManager audio=getSystemService(AudioManager.class);audio.dispatchMediaKeyEvent(new KeyEvent(0,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));audio.dispatchMediaKeyEvent(new KeyEvent(1,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));return true;}return RemoteAccess.current!=null&&RemoteAccess.current.act(key,text);});main.post(task);try{return task.get(2,TimeUnit.SECONDS);}catch(Exception failed){task.cancel(true);throw failed;}
    }
    static String line(InputStream in,int cap)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();int b;while((b=in.read())!=-1){if(out.size()>=cap)throw new IOException();if(b=='\n')return out.toString("US-ASCII").replace("\r","");if(b<32&&b!='\r'||b>126)throw new IOException();out.write(b);}throw new IOException();}
    void handle(Socket socket){try(Socket s=socket){s.setSoTimeout(5000);InputStream in=s.getInputStream();String[] request=line(in,1024).split(" ");if(request.length!=3||!request[2].equals("HTTP/1.1"))return;Map<String,String> h=new HashMap<>();int total=0;String row;while(!(row=line(in,2048)).isEmpty()){total+=row.length();if(total>8192)throw new IOException();int colon=row.indexOf(':');if(colon<1)throw new IOException();String name=row.substring(0,colon).toLowerCase(Locale.ROOT);if(h.put(name,row.substring(colon+1).trim())!=null)throw new IOException();}
            // Closing with unread TCP input can reset even a denied response on slower TVs.
            // Consume only the bounded body; origin checks and pairing still precede every control action.
            byte[] body=null;
            if(request[0].equals("POST")&&!h.containsKey("transfer-encoding")){
                int length=Integer.parseInt(h.getOrDefault("content-length","-1"));if(length<2||length>4096)throw new IOException();body=new byte[length];int count=0,n;while(count<length&&(n=in.read(body,count,length-count))!=-1)count+=n;if(count!=length)throw new IOException();
            }
            if(!running){send(s,503,"application/json","{\"error\":\"Remote is off\"}",null);return;}
            String origin="http://"+host;if(!host.equals(h.get("host"))||h.containsKey("transfer-encoding")||h.get("origin")!=null&&!origin.equals(h.get("origin"))){send(s,403,"text/plain","Forbidden",null);return;}
            String path=request[1];if(request[0].equals("GET")){if(path.equals("/"))send(s,200,"text/html",PAGE,null);else if(path.equals("/remote.js"))send(s,200,"text/javascript",SCRIPT,null);else if(path.equals("/remote.css"))send(s,200,"text/css",STYLE,null);else send(s,404,"text/plain","Not found",null);return;}
            if(!request[0].equals("POST")||!origin.equals(h.get("origin"))||!"application/json".equals(h.get("content-type"))){send(s,403,"text/plain","Forbidden",null);return;}
            JSONObject value=new JSONObject(new String(body,StandardCharsets.UTF_8));long now=SystemClock.elapsedRealtime();
            if(path.equals("/pair")&&value.length()==1){String token=pairing.pair(value.optString("pin"),now);send(s,token.isEmpty()?403:200,"application/json",token.isEmpty()?"{\"error\":\"Wrong PIN, expired window or too many attempts. Check TV.\"}":"{\"ok\":true}",token.isEmpty()?null:"dvremote="+token+"; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800");return;}
            if(!pairing.allowed(h.getOrDefault("cookie",""),now)){send(s,403,"application/json","{\"error\":\"Pair with the TV first\"}",null);return;}
            if(!path.equals("/action")||value.length()>2||!value.has("key")||value.has("text")&&!(value.get("text") instanceof String)){send(s,400,"application/json","{}",null);return;}
            String key=value.optString("key");if(!Arrays.asList("up","down","left","right","ok","home","back","volumeUp","volumeDown","play","text","enter").contains(key)||value.optString("text").length()>1000){send(s,400,"application/json","{}",null);return;}
            synchronized(this){if(now-lastAction<70){send(s,429,"application/json","{\"error\":\"Please slow down\"}",null);return;}lastAction=now;}
            boolean ok=command(key,value.optString("text"));send(s,200,"application/json",ok?"{\"ok\":true}":"{\"error\":\"Action unavailable here. Check TV accessibility or use the physical remote.\"}",null);
        }catch(Exception ignored){/* Never log keyboard text, cookies, PIN or screen information. */}}
    static void send(Socket s,int code,String mime,String body,String cookie)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);String headers="HTTP/1.1 "+code+" Response\r\nContent-Type: "+mime+"; charset=utf-8\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; frame-ancestors 'none'; form-action 'none'; base-uri 'none'\r\n"+(cookie==null?"":"Set-Cookie: "+cookie+"\r\n")+"\r\n";s.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));s.getOutputStream().write(bytes);}
    @Override public void onDestroy(){running=false;if(pairing!=null)pairing.revoke();address="";pin="";try{if(server!=null)server.close();}catch(Exception ignored){}server=null;if(workers!=null)workers.shutdownNow();main.removeCallbacksAndMessages(null);stopForeground(true);super.onDestroy();}
    static final String PAGE="<!doctype html><html lang='en'><meta name='viewport' content='width=device-width,initial-scale=1'><title>DV Freedom Remote</title><link rel='stylesheet' href='/remote.css'><main><p>DV FREEDOM</p><h1>Your phone. Your remote.</h1><section id='pair'><label>Six-digit PIN shown on TV<input id='pin' inputmode='numeric' maxlength='6' autocomplete='off'></label><button id='connect'>Pair phone</button></section><section id='controls' hidden><div class='dpad'><button data-key='up'>↑</button><div><button data-key='left'>←</button><button data-key='ok'>OK</button><button data-key='right'>→</button></div><button data-key='down'>↓</button></div><div class='row'><button data-key='back'>Back</button><button data-key='home'>Home</button><button data-key='play'>Play / pause</button></div><div class='row'><button data-key='volumeDown'>Volume −</button><button data-key='volumeUp'>Volume +</button></div><label>Replace the TV’s focused text field<input id='text' maxlength='1000' autocomplete='off'></label><div class='row'><button id='type'>Send text</button><button data-key='enter'>Enter</button></div></section><p id='status' role='status'>Same trusted network only. Pairing expires after 30 minutes.</p><small>Android 11/12 navigation depends on the focused app. Password fields cannot be edited. Stop the server on your TV to revoke access.</small></main><script src='/remote.js'></script></html>";
    static final String SCRIPT="'use strict';async function send(path,value){try{const r=await fetch(path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(value)});const d=await r.json();document.querySelector('#status').textContent=d.error||'Ready';return r.ok&&d.ok;}catch(e){document.querySelector('#status').textContent='TV unavailable. Check connection and reopen pairing.';return false;}}document.querySelector('#connect').onclick=async()=>{if(await send('/pair',{pin:document.querySelector('#pin').value})){document.querySelector('#pair').hidden=true;document.querySelector('#controls').hidden=false;document.querySelector('#pin').value='';}};for(const b of document.querySelectorAll('[data-key]'))b.onclick=()=>send('/action',{key:b.dataset.key});document.querySelector('#type').onclick=async()=>{if(await send('/action',{key:'text',text:document.querySelector('#text').value}))document.querySelector('#text').value='';};";
    static final String STYLE="*{box-sizing:border-box}body{margin:0;background:#101923;color:#f2f6fc;font:17px system-ui}main{max-width:500px;margin:auto;padding:24px}h1{font-size:30px;letter-spacing:-1px}label{display:block;margin:20px 0 10px}input{display:block;width:100%;padding:14px;border:1px solid #7791aa;background:#192b3e;color:white;border-radius:10px;margin-top:8px;font:inherit}button{min-height:58px;min-width:62px;padding:14px;border:1px solid #7a98ae;border-radius:14px;background:#203c50;color:#fff;font:600 17px system-ui;cursor:pointer}button:focus-visible,input:focus-visible{outline:3px solid #8ddcf0;outline-offset:3px}button:active{background:#46728d}.dpad{text-align:center;margin:24px 0}.dpad div{display:flex;gap:12px;justify-content:center;margin:12px}.row{display:flex;gap:10px;margin:12px 0}.row button{flex:1}small{color:#b6c9db;line-height:1.6}[hidden]{display:none!important}";
}
