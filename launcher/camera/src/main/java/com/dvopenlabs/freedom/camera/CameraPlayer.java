package com.dvopenlabs.freedom.camera;

import android.content.Context;
import android.net.Uri;
import android.view.TextureView;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.source.*;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

/** Silent player. Authentication never follows a redirect or a foreign HLS segment origin. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class CameraPlayer implements AutoCloseable {
    final ExoPlayer player;OkHttpClient client;
    CameraPlayer(Context c,TextureView view,String address,String user,String password,Runnable failed){
        androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF);
        URI chosen=CameraPolicy.url(address);CameraPolicy.credential(user);CameraPolicy.credential(password);
        player=new ExoPlayer.Builder(c).setLoadControl(new DefaultLoadControl.Builder().setBufferDurationsMs(1000,5000,500,1000).build()).build();
        try{
        player.setVolume(0);player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).build(),false);
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO,true).build());
        player.setVideoTextureView(view);player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){failed.run();}@Override public void onVideoSizeChanged(VideoSize size){if(size.width<1||size.height<1||view.getWidth()<1)return;float ratio=(size.width*size.pixelWidthHeightRatio)/size.height,box=(float)view.getWidth()/view.getHeight();android.graphics.Matrix m=new android.graphics.Matrix();if(ratio>box)m.setScale(1,box/ratio,view.getWidth()/2f,view.getHeight()/2f);else m.setScale(ratio/box,1,view.getWidth()/2f,view.getHeight()/2f);view.setTransform(m);}});
        MediaSource source;
        if(chosen.getScheme().equals("rtsp")){
            Uri target=Uri.parse(address);if(!user.isEmpty())target=target.buildUpon().encodedAuthority(Uri.encode(user)+":"+Uri.encode(password)+"@"+chosen.getHost()+(chosen.getPort()<0?"":":"+chosen.getPort())).build();
            source=new RtspMediaSource.Factory().setForceUseRtpTcp(true).setDebugLoggingEnabled(false).setSocketFactory(new LanSocketFactory(chosen.getHost(),CameraPolicy.port(chosen))).createMediaSource(MediaItem.fromUri(target));
        }else{
            client=new OkHttpClient.Builder().proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false).connectTimeout(8,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).addInterceptor(chain->{Request request=chain.request();if(!CameraPolicy.sameOrigin(chosen,request.url().uri()))throw new java.io.IOException("Foreign camera resource refused");Request.Builder next=request.newBuilder().removeHeader("Authorization");if(!user.isEmpty())next.header("Authorization",Credentials.basic(user,password));return chain.proceed(next.build());}).build();
            source=new DefaultMediaSourceFactory(new OkHttpDataSource.Factory(client)).createMediaSource(MediaItem.fromUri(address));
        }
        player.setMediaSource(source);player.prepare();player.play();
        }catch(RuntimeException bad){close();throw bad;}
    }
    @Override public void close(){player.clearVideoSurface();player.release();if(client!=null){client.dispatcher().cancelAll();client.connectionPool().evictAll();client.dispatcher().executorService().shutdown();}}
}
