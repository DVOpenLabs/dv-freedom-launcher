package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class ThemePicker {
    static final int IMPORT=4104;
    private static final AtomicBoolean importing=new AtomicBoolean();
    static void show(Activity activity){
        List<ThemeLibrary.Entry> rows=ThemeLibrary.list(activity);
        String[] names=new String[rows.size()+3];names[0]=activity.getString(R.string.theme_original);
        String chosen=ProfileStore.preferences(activity).getString(ThemeLibrary.CHOICE,"");int checked=0;
        for(int i=0;i<rows.size();i++){
            ThemeLibrary.Entry e=rows.get(i);names[i+1]=e.pack.name+" · v"+e.pack.version;
            if(chosen.equals(e.token))checked=i+1;
        }
        names[names.length-2]=activity.getString(R.string.theme_import);
        names[names.length-1]=activity.getString(R.string.theme_remove);
        final int selected=checked;
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(R.string.theme_library)
                .setSingleChoiceItems(names,checked,(d,which)->{
                    d.dismiss();
                    if(which==names.length-2){
                        try{activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}
                        catch(android.content.ActivityNotFoundException e){message(activity,R.string.not_available);}
                    }else if(which==names.length-1)remove(activity,rows);
                    else if(ThemeLibrary.select(activity,which==0?"":rows.get(which-1).token))activity.finish();
                    else message(activity,R.string.theme_save_failed);
                }).setNegativeButton(android.R.string.cancel,null).create();
        dialog.setOnShowListener(d->{dialog.getListView().requestFocus();dialog.getListView().setSelection(selected);});dialog.show();
    }
    static void imported(Activity activity,int result,Intent data){
        if(result!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(R.string.theme_import)
                .setMessage(R.string.theme_import_confirm).setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(R.string.profile_import_action,(d,w)->{
                    if(!importing.compareAndSet(false,true)){message(activity,R.string.theme_import_busy);return;}
                    android.net.Uri uri=data.getData();android.content.Context app=activity.getApplicationContext();
                    message(activity,R.string.theme_import_busy);
                    new Thread(()->{
                        boolean success=false;
                        try(InputStream stream=app.getContentResolver().openInputStream(uri)){ThemeLibrary.importPack(app,stream);success=true;}
                        catch(Exception ignored){}finally{importing.set(false);}
                        final boolean done=success;
                        new Handler(Looper.getMainLooper()).post(()->{
                            if(!activity.isDestroyed()&&!activity.isFinishing()&&activity.hasWindowFocus())
                                message(activity,done?R.string.theme_import_done:R.string.theme_import_failed);
                        });
                    },"theme-import").start();
                }).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus());dialog.show();
    }
    private static void remove(Activity activity,List<ThemeLibrary.Entry> rows){
        String[] names=new String[rows.size()];for(int i=0;i<names.length;i++)names[i]=rows.get(i).pack.name;
        new AlertDialog.Builder(activity).setTitle(R.string.theme_remove).setItems(names,(d,which)->{
            AlertDialog confirm=new AlertDialog.Builder(activity).setTitle(R.string.theme_remove)
                    .setMessage(activity.getString(R.string.theme_remove_confirm,names[which]))
                    .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.theme_remove,(dialog,w)->{
                        try{ThemeLibrary.remove(activity,rows.get(which));message(activity,R.string.theme_removed);}
                        catch(Exception e){message(activity,R.string.theme_remove_used);}
                    }).create();
            confirm.setOnShowListener(dialog->confirm.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus());confirm.show();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private static void message(Activity activity,int text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    private ThemePicker(){}
}
