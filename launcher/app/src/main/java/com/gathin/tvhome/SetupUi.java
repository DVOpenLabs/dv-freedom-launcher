package com.gathin.tvhome;

import android.app.*;
import android.widget.*;
import org.json.*;
import java.util.function.Consumer;

/** Small native dialogs shared by named looks and on-TV profiles. */
final class SetupUi {
    static void error(Activity a,Exception e){Toast.makeText(a,e.getMessage()==null?a.getString(R.string.style_save_failed):e.getMessage(),Toast.LENGTH_LONG).show();}
    static void name(Activity a,int title,String initial,Consumer<String> save){
        EditText text=new EditText(a);text.setSingleLine(true);text.setHint(R.string.setup_name);text.setText(initial);
        text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});text.setSelectAllOnFocus(true);
        text.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(text).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.setup_save,null).create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{save.accept(SetupValues.name(text.getText().toString()));dialog.dismiss();}catch(Exception e){text.setError(e.getMessage());}});
            text.setOnEditorActionListener((v,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();return true;}return false;});
            text.requestFocus();
        });dialog.show();
    }
    static void confirm(Activity a,int title,int message,Runnable action){
        AlertDialog d=new AlertDialog.Builder(a).setTitle(title).setMessage(message).setNegativeButton(android.R.string.cancel,null)
            .setPositiveButton(android.R.string.ok,(x,w)->{try{action.run();}catch(Exception e){error(a,e);}}).create();
        d.show();d.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    static void show(Activity a){
        try{
            JSONArray rows=SetupStore.read(a);String[] names=new String[rows.length()+1];names[0]=a.getString(R.string.setup_save_current);
            for(int i=0;i<rows.length();i++)names[i+1]=rows.getJSONObject(i).getString("name");
            new AlertDialog.Builder(a).setTitle(R.string.saved_setups).setItems(names,(d,which)->{
                if(which==0){if(rows.length()==SetupStore.LIMIT){Toast.makeText(a,R.string.setup_limit,Toast.LENGTH_LONG).show();return;}
                    name(a,R.string.setup_save_current,"",value->{try{for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("name").equalsIgnoreCase(value))throw new IllegalArgumentException("That name already exists");JSONArray next=new JSONArray(rows.toString());next.put(new JSONObject().put("name",value).put("values",new JSONObject(SetupStore.capture(a))));if(!SetupStore.store(a,next))throw new IllegalStateException(a.getString(R.string.style_save_failed));}catch(Exception e){throw new IllegalArgumentException(e.getMessage(),e);}});
                }else actions(a,rows,which-1);
            }).setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception e){error(a,e);}
    }
    private static void actions(Activity a,JSONArray rows,int index){
        new AlertDialog.Builder(a).setTitle(R.string.saved_setups).setItems(new String[]{a.getString(R.string.setup_restore),a.getString(R.string.setup_rename),a.getString(R.string.setup_delete)},(d,which)->{
            if(which==0)confirm(a,R.string.setup_restore,R.string.setup_restore_help,()->{
                try{if(!SetupStore.restore(a,rows.getJSONObject(index)))throw new IllegalStateException(a.getString(R.string.style_save_failed));a.finish();}catch(Exception e){throw new IllegalArgumentException(e.getMessage(),e);}
            });
            else if(which==1)try{name(a,R.string.setup_rename,rows.getJSONObject(index).getString("name"),value->{try{for(int i=0;i<rows.length();i++)if(i!=index&&rows.getJSONObject(i).getString("name").equalsIgnoreCase(value))throw new IllegalArgumentException("That name already exists");JSONArray next=new JSONArray(rows.toString());next.getJSONObject(index).put("name",value);if(!SetupStore.store(a,next))throw new IllegalStateException(a.getString(R.string.style_save_failed));}catch(Exception e){throw new IllegalArgumentException(e.getMessage(),e);}});}catch(Exception e){error(a,e);}
            else confirm(a,R.string.setup_delete,R.string.setup_delete_help,()->{rows.remove(index);if(!SetupStore.store(a,rows))throw new IllegalStateException(a.getString(R.string.style_save_failed));});
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private SetupUi(){}
}
