package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/** Categories are navigation state only: browsing never writes launcher preferences. */
final class SettingsNavigation {
    private static final String SAVED="settings_category";
    private static final int[] BUTTONS={R.id.categoryLayout,R.id.categoryApps,R.id.categoryWallpaper,R.id.categoryRemote,R.id.categoryAdvanced};
    private static final int[] GROUPS={R.id.groupLayout,R.id.groupApps,R.id.groupWallpaper,R.id.groupRemote,R.id.groupAdvanced};
    private static final int[] TITLES={R.string.settings_layout,R.string.settings_apps,R.string.settings_wallpaper,R.string.settings_remote,R.string.settings_advanced};
    private static final int[] HINTS={R.string.settings_layout_help,R.string.settings_apps_help,R.string.settings_wallpaper_help,R.string.settings_remote_help,R.string.settings_advanced_help};
    private final Activity activity;
    private final View[] buttons=new View[BUTTONS.length];
    private final ViewGroup[] groups=new ViewGroup[GROUPS.length];
    private final ScrollView scroll;
    private final boolean rtl;
    private int selected;
    SettingsNavigation(Activity activity,Bundle saved) {
        this.activity=activity;scroll=activity.findViewById(R.id.settingsScroll);
        rtl=activity.getResources().getBoolean(R.bool.rtl_menu_bar);
        for(int i=0;i<BUTTONS.length;i++) {
            final int index=i;
            buttons[i]=activity.findViewById(BUTTONS[i]);groups[i]=activity.findViewById(GROUPS[i]);
            buttons[i].setOnFocusChangeListener((v,focused)->{if(focused)select(index);});
            buttons[i].setOnClickListener(v->{select(index);enter();});
            buttons[i].setNextFocusUpId(BUTTONS[Math.max(0,i-1)]);
            buttons[i].setNextFocusDownId(BUTTONS[Math.min(BUTTONS.length-1,i+1)]);
        }
        if(rtl)activity.findViewById(R.id.settingsPanes).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        select(saved==null?0:Math.max(0,Math.min(BUTTONS.length-1,saved.getInt(SAVED,0))));
        buttons[selected].requestFocus();
    }
    private void select(int index) {
        boolean changed=selected!=index;
        selected=index;
        for(int i=0;i<groups.length;i++){groups[i].setVisibility(i==index?View.VISIBLE:View.GONE);buttons[i].setSelected(i==index);}
        ((TextView)activity.findViewById(R.id.settingsSectionTitle)).setText(TITLES[index]);
        ((TextView)activity.findViewById(R.id.settingsSectionHelp)).setText(HINTS[index]);
        if(changed)scroll.scrollTo(0,0);
    }
    private View first(View view) {
        if(view.getVisibility()!=View.VISIBLE||!view.isEnabled())return null;
        if(view.isFocusable())return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View found=first(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    private void enter(){View target=first(groups[selected]);if(target!=null)target.requestFocus();}
    private static boolean inside(View child,View ancestor) {
        while(child!=null){if(child==ancestor)return true;child=child.getParent() instanceof View?(View)child.getParent():null;}return false;
    }
    boolean key(KeyEvent event) {
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
        View focused=activity.getCurrentFocus();int key=event.getKeyCode();
        if(focused==buttons[selected]) {
            if(key==(rtl?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT)){enter();return true;}
            if(key==(rtl?KeyEvent.KEYCODE_DPAD_RIGHT:KeyEvent.KEYCODE_DPAD_LEFT))return true;
            if(key==KeyEvent.KEYCODE_DPAD_UP&&selected==0||key==KeyEvent.KEYCODE_DPAD_DOWN&&selected==buttons.length-1)return true;
        }else if(inside(focused,groups[selected])&&key==(rtl?KeyEvent.KEYCODE_DPAD_RIGHT:KeyEvent.KEYCODE_DPAD_LEFT)) {
            // Keep Left/Right within numeric choice rows; only the edge returns to categories.
            View next=FocusFinder.getInstance().findNextFocus(scroll,focused,rtl?View.FOCUS_RIGHT:View.FOCUS_LEFT);
            if(next==null){buttons[selected].requestFocus();return true;}
        }
        return false;
    }
    boolean back(){if(activity.getCurrentFocus()==buttons[selected])return false;buttons[selected].requestFocus();return true;}
    void save(Bundle out){out.putInt(SAVED,selected);}
}
