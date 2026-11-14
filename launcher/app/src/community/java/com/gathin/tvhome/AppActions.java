package com.gathin.tvhome;
import android.app.*;
/** Identical long-press actions on every modern Home dock. */
final class AppActions {
    static void show(Activity a,HomeCategories.App app){if(!ProductFeatures.allowEdit(a,()->show(a,app)))return;if(app instanceof Folders.FolderApp){Folders.options(a,((Folders.FolderApp)app).folder);return;}new AlertDialog.Builder(a).setTitle(app.label).setItems(new String[]{"Edit app","Create folder"},(d,n)->{if(n==0)NativeTileEditor.show(a,app.pkg);else Folders.edit(a,null);}).setNegativeButton("Back",null).show();}
    private AppActions(){}
}
