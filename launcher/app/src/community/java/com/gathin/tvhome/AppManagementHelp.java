package com.gathin.tvhome;
import android.app.Activity;
import android.provider.Settings;
import android.widget.LinearLayout;

/** Visible routes to existing editors, without assuming every installed package is launchable. */
final class AppManagementHelp {
    static void show(Activity a){
        LinearLayout b=FreedomUi.body(a);
        b.addView(FreedomUi.text(a,"Make apps your own",22));
        b.addView(FreedomUi.text(a,"Hold OK on an app for its available actions. Use Arrange apps to move favorites; Customize apps changes a name or artwork. App visibility can hide an app without uninstalling it.",18));
        FreedomUi.action(a,b,"Arrange favorites",()->FavoriteOrderEditor.show(a));
        FreedomUi.action(a,b,"Customize app name or artwork",()->NativeTileEditor.show(a));
        FreedomUi.action(a,b,"App visibility",()->LibraryVisibility.show(a));
        b.addView(FreedomUi.text(a,"Missing an app?",22));
        b.addView(FreedomUi.text(a,"All apps includes TV apps and ordinary apps that advertise a launchable activity. A disabled app, background service or package with no launchable screen cannot appear as a working tile. Check exclusions and the active family profile, then check Android’s Apps page. Installing an app does not automatically add it to your favorites.",18));
        FreedomUi.action(a,b,"Browse All apps",()->AllAppsSurface.show(a,Config.load(a),HomeCategories.catalog(a,Config.load(a))));
        FreedomUi.action(a,b,"Open system Apps",()->CompatibilityCenter.system(a,Settings.ACTION_APPLICATION_SETTINGS));
        FreedomUi.sheet(a,"App management",b,"Close");
    }
    private AppManagementHelp(){}
}
