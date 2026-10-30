package com.dvopenlabs.freedom.companions;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import java.io.*;

/** Signature-gated snapshots; no credentials or control endpoint is exported. */
public final class HubProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        Bundle b=new Bundle();if("calendar".equals(method)){
            boolean on=HubStore.prefs(getContext()).getBoolean("calendar",false);if(on)HubStore.refresh(getContext());
            b.putString("events",on?HubStore.events(getContext()).toString():"[]");b.putLong("updated",HubStore.prefs(getContext()).getLong("updated",0));b.putBoolean("failed",HubStore.prefs(getContext()).getBoolean("failed",false));
        }else if("poster".equals(method)&&arg!=null&&arg.length()<=4096)b.putString("key",HubStore.poster(getContext(),arg));else throw new IllegalArgumentException();return b;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        String key=uri.getLastPathSegment();if(!"r".equals(mode)||!HubStore.prefs(getContext()).getBoolean("posters",false)||key==null||!key.matches("[0-9a-f]{64}"))throw new FileNotFoundException();return ParcelFileDescriptor.open(new File(HubStore.folder(getContext()),key+".jpg"),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}public String getType(Uri u){return "image/jpeg";}public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
