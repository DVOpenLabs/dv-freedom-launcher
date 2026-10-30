package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.zip.*;

/** Local media review. Import only adds a new library entry; it never selects or replaces a theme. */
public final class MediaReviewActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private JSONObject receipt;private String id;private Bitmap preview;private TextView status,add,discard;private ImageView image;private boolean busy;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView label(String text,int size){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(getColor(R.color.freedom_text));v.setTypeface(SettingsTypography.regular(this));return v;}
    private TextView button(LinearLayout host,String text,Runnable action){TextView v=label(text,18);v.setPadding(dp(18),dp(12),dp(18),dp(12));v.setMinHeight(dp(50));v.setFocusable(true);v.setFocusableInTouchMode(true);v.setClickable(true);v.setBackgroundResource(R.drawable.settings_choice_bg);v.setOnClickListener(x->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);host.addView(v,p);return v;}
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);ProductFeatures.init(this);id=getIntent().getStringExtra("id");
        try{receipt=MediaInbox.load(this);if(!MediaInboxPolicy.id(id)||!id.equals(receipt.getString("id"))||!"ready".equals(receipt.getString("state")))throw new IOException("This media is unavailable");MediaInbox.bound(this,receipt);}
        catch(Exception invalid){Toast.makeText(this,invalid.getMessage(),Toast.LENGTH_LONG).show();finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(40),dp(30),dp(40),dp(24));root.setBackgroundColor(getColor(R.color.freedom_canvas));
        TextView title=label("A wallpaper from your phone",30);title.setTypeface(SettingsTypography.heading(this));root.addView(title);
        LinearLayout content=new LinearLayout(this);content.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams body=new LinearLayout.LayoutParams(-1,0,1);body.topMargin=dp(20);root.addView(content,body);
        image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Received wallpaper preview");content.addView(image,new LinearLayout.LayoutParams(0,-1,1.2f));
        ScrollView scroll=new ScrollView(this);LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-1,1);right.leftMargin=dp(30);content.addView(scroll,right);LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);scroll.addView(actions);
        actions.addView(label(receipt.optString("name"),22));status=label("Checking the file…",16);status.setTextColor(getColor(R.color.freedom_secondary));status.setPadding(0,dp(14),0,dp(12));actions.addView(status);
        actions.addView(label("Add this file to the wallpaper library. Your current wallpaper, app layout and TV settings stay saved. Select it later from the library when you are ready.",17));
        add=button(actions,"Add to wallpaper library",this::importFile);add.setEnabled(false);add.setAlpha(.5f);
        TextView cancel=button(actions,"Back · keep for later review",this::finish);discard=button(actions,"Discard received TV copy",()->{if(busy)return;try{MediaInbox.state(this,id,"discarded");finish();}catch(Exception failure){message(failure);}});
        setContentView(root);cancel.requestFocus();worker.execute(()->{
            try{Bitmap bitmap=decode();runOnUiThread(()->{if(isDestroyed()){bitmap.recycle();return;}preview=bitmap;image.setImageBitmap(bitmap);status.setText("mp4".equals(receipt.optString("kind"))?"Short video · a still fallback is included. Playback stays silent.":"Picture ready. Large pictures are resized to a safe TV resolution.");add.setEnabled(true);add.setAlpha(1);add.requestFocus();});}
            catch(Exception failure){runOnUiThread(()->{if(!isDestroyed())status.setText("Cannot preview this file. Use a valid JPEG/PNG, or an H.264 MP4 up to 4K and 60 seconds. Nothing was imported.");});}
        });
    }
    private Bitmap decode()throws Exception{
        File file=MediaInbox.file(this,receipt);Bitmap result;
        if("mp4".equals(receipt.getString("kind"))){
            MediaMetadataRetriever media=new MediaMetadataRetriever();try{
                media.setDataSource(file.getAbsolutePath());int width=Integer.parseInt(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)),height=Integer.parseInt(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));long duration=Long.parseLong(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                if(!MediaInboxPolicy.video(width,height,duration))throw new IOException("Unsupported video size or duration");
                result=Build.VERSION.SDK_INT>=27?media.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,1920,1080):media.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            }finally{media.release();}
        }else{
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),options);
            if(!MediaInboxPolicy.picture(options.outWidth,options.outHeight))throw new IOException("Unsupported image dimensions");
            options.inSampleSize=1;while((long)(options.outWidth/options.inSampleSize)*(options.outHeight/options.inSampleSize)>WallpaperSize.MAX_PIXELS)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;result=BitmapFactory.decodeFile(file.getAbsolutePath(),options);
            if(result!=null&&"jpg".equals(receipt.getString("kind"))){int orientation=new ExifInterface(file.getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);Matrix matrix=new Matrix();
                if(orientation==ExifInterface.ORIENTATION_ROTATE_90)matrix.postRotate(90);else if(orientation==ExifInterface.ORIENTATION_ROTATE_180)matrix.postRotate(180);else if(orientation==ExifInterface.ORIENTATION_ROTATE_270)matrix.postRotate(270);else if(orientation==ExifInterface.ORIENTATION_FLIP_HORIZONTAL)matrix.postScale(-1,1);else if(orientation==ExifInterface.ORIENTATION_FLIP_VERTICAL)matrix.postScale(1,-1);else if(orientation==ExifInterface.ORIENTATION_TRANSPOSE){matrix.postRotate(90);matrix.postScale(-1,1);}else if(orientation==ExifInterface.ORIENTATION_TRANSVERSE){matrix.postRotate(270);matrix.postScale(-1,1);}
                if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(result,0,0,result.getWidth(),result.getHeight(),matrix,true);if(rotated!=result)result.recycle();result=rotated;}
            }
        }
        if(result==null||!WallpaperSize.accepted(result.getWidth(),result.getHeight()))throw new IOException("Cannot decode a safe still fallback");return result;
    }
    private void entry(ZipOutputStream zip,String name,byte[] bytes)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();}
    private void importFile(){
        if(busy||preview==null||!LauncherLock.require(this,this::importFile))return;
        try{MediaInbox.bound(this,receipt);MediaInbox.state(this,id,"importing");}catch(Exception failure){message(failure);return;}
        busy=true;add.setEnabled(false);status.setText("Adding to your local library…");worker.execute(()->{
            File archive=new File(getCacheDir(),"incoming-"+id+".zip");
            try{
                ByteArrayOutputStream still=new ByteArrayOutputStream();if(!preview.compress(Bitmap.CompressFormat.JPEG,86,still)||still.size()>MediaInboxPolicy.MAX_IMAGE)throw new IOException("Export a smaller picture and try again");
                try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){zip.setLevel(0);String name=receipt.getString("name");if(name.length()>60)name=name.substring(0,Character.isHighSurrogate(name.charAt(59))?59:60);
                    entry(zip,"manifest.json",new JSONObject().put("schema",1).put("id","phone-"+id).put("name",name).put("version",1).toString().getBytes(StandardCharsets.UTF_8));entry(zip,"config.json","{\"wallpaper\":true}".getBytes(StandardCharsets.UTF_8));entry(zip,"wallpaper.jpg",still.toByteArray());
                    if("mp4".equals(receipt.getString("kind"))){zip.putNextEntry(new ZipEntry("video.mp4"));try(InputStream in=new FileInputStream(MediaInbox.file(this,receipt))){byte[] buffer=new byte[65536];for(int count;(count=in.read(buffer))!=-1;){if(Thread.currentThread().isInterrupted())throw new IOException("Import interrupted");zip.write(buffer,0,count);}}zip.closeEntry();}
                }
                try(InputStream in=new FileInputStream(archive)){ThemeLibrary.importPack(this,in);}MediaInbox.state(this,id,"imported");
                runOnUiThread(()->{busy=false;if(isDestroyed())return;discard.setVisibility(android.view.View.GONE);status.setText("Added to the wallpaper library. Your current Home is unchanged.");add.setText("Browse wallpaper library");add.setEnabled(true);add.setOnClickListener(v->WallpaperBrowser.show(this,true));add.requestFocus();});
            }catch(Exception failure){try{MediaInbox.state(this,id,"ready");}catch(Exception ignored){}runOnUiThread(()->{busy=false;if(!isDestroyed()){add.setEnabled(true);status.setText("Not imported. Check free storage or export a smaller compatible file, then try again.");}});}
            finally{archive.delete();}
        });
    }
    private void message(Exception failure){new AlertDialog.Builder(this).setTitle("Media not changed").setMessage(failure.getMessage()).setPositiveButton("OK",null).show();}
    @Override protected void onDestroy(){worker.shutdownNow();if(image!=null)image.setImageDrawable(null);super.onDestroy();}
}
