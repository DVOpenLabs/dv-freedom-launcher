package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.CancellationSignal;
import android.system.Os;
import android.system.OsConstants;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Bounded local provider art. Network URLs, arbitrary files and unbounded pipes are not opened. */
final class ShelfArtwork {
    private static final int MAX_BYTES=2*1024*1024;
    static Bitmap read(Context context,String raw,String owner,CancellationSignal cancel){
        if(raw==null||raw.length()>4096||cancel.isCanceled())return null;
        try{
            Uri uri=Uri.parse(raw);
            if("android.resource".equals(uri.getScheme())){
                if(!owner.equals(uri.getAuthority()))return null;
            }else if("content".equals(uri.getScheme())){
                ProviderInfo provider=context.getPackageManager().resolveContentProvider(uri.getAuthority(),0);
                if(provider==null||!owner.equals(provider.packageName)&&!"android.media.tv".equals(uri.getAuthority()))return null;
            }else return null;
            byte[] bytes;
            try(AssetFileDescriptor descriptor=context.getContentResolver().openAssetFileDescriptor(uri,"r",cancel)){
                if(descriptor==null||descriptor.getDeclaredLength()>MAX_BYTES||!OsConstants.S_ISREG(Os.fstat(descriptor.getFileDescriptor()).st_mode))return null;
                try(InputStream input=descriptor.createInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int count;
                    while((count=input.read(buffer))!=-1){if(cancel.isCanceled()||out.size()+count>MAX_BYTES)return null;out.write(buffer,0,count);}
                    bytes=out.toByteArray();
                }
            }
            if(cancel.isCanceled())return null;
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
            if(!ContentShelfPolicy.dimensions(bounds.outWidth,bounds.outHeight))return null;
            BitmapFactory.Options sample=new BitmapFactory.Options();sample.inSampleSize=1;
            while(bounds.outWidth/sample.inSampleSize>384||bounds.outHeight/sample.inSampleSize>216)sample.inSampleSize*=2;
            return cancel.isCanceled()?null:BitmapFactory.decodeByteArray(bytes,0,bytes.length,sample);
        }catch(Exception unavailable){return null;}
    }
}
