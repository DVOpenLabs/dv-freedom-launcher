package com.gathin.tvhome;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.JSONObject;
import java.io.FileNotFoundException;

/** Fixed shell-authorized inbox. No general path, command, profile-write or theme-selection API. */
public final class MediaInboxProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        try{
            JSONObject result;
            switch(method){
                case "prepare":result=MediaInbox.prepare(getContext(),arg);break;
                case "status":result=MediaInbox.status(getContext(),arg);break;
                case "review":MediaInbox.review(getContext(),arg);result=new JSONObject().put("schema",1).put("reviewRequired",true);break;
                default:throw new IllegalArgumentException("Unsupported media operation");
            }
            Bundle out=new Bundle();out.putString("json",result.toString());return out;
        }catch(Exception failure){throw new IllegalArgumentException(failure.getMessage());}
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        try{if(uri.getPathSegments().size()!=2||!"upload".equals(uri.getPathSegments().get(0)))throw new Exception();return MediaInbox.open(getContext(),uri.getPathSegments().get(1),mode);}
        catch(Exception invalid){throw new FileNotFoundException("Only a prepared media transfer can be written");}
    }
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
}
