package com.dvopenlabs.freedom.camera;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** Explicit foreground launch only. Silent, non-interactive overlay; no boot start or Accessibility. */
public final class CameraOverlay extends Service {
    static final String STOP="com.dvopenlabs.freedom.camera.STOP";final Handler handler=new Handler(Looper.getMainLooper());
    WindowManager windows;TextureView view;CameraPlayer player;BroadcastReceiver off;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null||STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)||Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
        try{
            var p=getSharedPreferences("camera",0);String address=p.getString("url","");CameraPolicy.url(address);long timeout=CameraPolicy.duration(p.getInt("minutes",5));
            NotificationManager notifications=getSystemService(NotificationManager.class);notifications.createNotificationChannel(new NotificationChannel("camera","Camera window",NotificationManager.IMPORTANCE_LOW));
            PendingIntent stop=PendingIntent.getService(this,0,new Intent(this,CameraOverlay.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,CameraActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification n=new Notification.Builder(this,"camera").setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("DV Freedom Camera is showing").setContentText("Silent window · automatic timeout enabled").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build();
            if(Build.VERSION.SDK_INT>=29)startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(7,n);
            release();windows=getSystemService(WindowManager.class);view=new TextureView(this);int width=Math.min(640,Math.max(160,getResources().getDisplayMetrics().widthPixels/3));
            WindowManager.LayoutParams box=new WindowManager.LayoutParams(width,width*9/16,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_SECURE,android.graphics.PixelFormat.OPAQUE);box.gravity=Gravity.TOP|Gravity.END;box.x=24;box.y=24;windows.addView(view,box);
            player=new CameraPlayer(this,view,address,p.getString("user",""),p.getString("password",""),this::stopSelf);
            handler.removeCallbacksAndMessages(null);handler.postDelayed(this::stopSelf,timeout);
            if(off==null){off=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){stopSelf();}};if(Build.VERSION.SDK_INT>=33)registerReceiver(off,new IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(off,new IntentFilter(Intent.ACTION_SCREEN_OFF));}
        }catch(RuntimeException unavailable){stopSelf();}
        return START_NOT_STICKY;
    }
    void release(){if(player!=null){player.close();player=null;}if(view!=null&&windows!=null){try{windows.removeView(view);}catch(RuntimeException ignored){}view=null;}}
    @Override public void onTaskRemoved(Intent root){stopSelf();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);release();if(off!=null){try{unregisterReceiver(off);}catch(RuntimeException ignored){}off=null;}stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
