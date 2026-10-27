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
    private final View categories;
    private boolean editing;
    private int selected;
    private int[] last=new int[BUTTONS.length];
    private final TextView context;
    SettingsNavigation(Activity activity,Bundle saved) {
        this.activity=activity;scroll=activity.findViewById(R.id.settingsScroll);categories=activity.findViewById(R.id.settingsCategories);
        context=activity.findViewById(R.id.settingsContext);
        if(saved!=null){int[] value=saved.getIntArray("settings_focus");if(value!=null&&value.length==last.length)last=value;}
        rtl=activity.getResources().getBoolean(R.bool.rtl_menu_bar);
        for(int i=0;i<BUTTONS.length;i++) {
            final int index=i;
            buttons[i]=activity.findViewById(BUTTONS[i]);groups[i]=activity.findViewById(GROUPS[i]);
            TextView row=(TextView)buttons[i];android.graphics.drawable.Drawable[] icons=row.getCompoundDrawablesRelative();
            float density=activity.getResources().getDisplayMetrics().density;
            for(int n:new int[]{0,2})if(icons[n]!=null){icons[n]=icons[n].mutate();icons[n].setBounds(0,0,Math.round((n==0?24:12)*density),Math.round((n==0?24:20)*density));}
            row.setCompoundDrawablesRelative(icons[0],icons[1],icons[2],icons[3]);
            buttons[i].setOnFocusChangeListener((v,focused)->{if(focused)select(index);});
            buttons[i].setOnClickListener(v->{select(index);enter();});
            buttons[i].setNextFocusUpId(BUTTONS[Math.max(0,i-1)]);
            buttons[i].setNextFocusDownId(BUTTONS[Math.min(BUTTONS.length-1,i+1)]);
        }
        if(rtl)activity.findViewById(R.id.settingsPanes).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        select(saved==null?0:Math.max(0,Math.min(BUTTONS.length-1,saved.getInt(SAVED,0))));
        activity.findViewById(R.id.settingsPanes).getViewTreeObserver().addOnGlobalFocusChangeListener((old,focused)->{
            if(editing&&focused!=null&&inside(focused,groups[selected])){last[selected]=focused.getId();describe(focused);}
        });
        if(saved!=null&&saved.getBoolean("settings_detail",false))enter();else buttons[selected].requestFocus();
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
    private void enter(){editing=true;categories.setVisibility(View.GONE);scroll.setVisibility(View.VISIBLE);View target=activity.findViewById(last[selected]);if(target==null||!inside(target,groups[selected])||!target.isShown()||!target.isEnabled())target=first(groups[selected]);if(target!=null){target.requestFocus();describe(target);}}
    private void describe(View target){
        context.setVisibility(View.VISIBLE);context.setText(target.getContentDescription()!=null?target.getContentDescription():target instanceof TextView?((TextView)target).getText():"");
        int id=target.getId(),help=HINTS[selected];
        int[] ids={R.id.positionChoice,R.id.adaptiveHeaderChoice,R.id.appNamesChoice,R.id.focusOutlineChoice,R.id.tileSpacingChoice,R.id.wallpaperDimChoice,R.id.liveWallpaperChoice,R.id.profilesChoice,R.id.savedSetupsChoice,R.id.inputsShortcutChoice,R.id.cinematicIdleChoice};
        int[] hints={R.string.help_position,R.string.help_text,R.string.help_names,R.string.help_focus,R.string.help_spacing,R.string.wallpaper_dim_help,R.string.help_live,R.string.settings_apps_help,R.string.help_setups,R.string.help_inputs,R.string.help_idle};
        for(int i=0;i<ids.length;i++)if(id==ids[i]){help=hints[i];break;}
        ((TextView)activity.findViewById(R.id.settingsSectionHelp)).setText(help);
    }
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
            if(next==null){back();return true;}
        }
        return false;
    }
    boolean back(){if(!editing)return false;editing=false;context.setVisibility(View.GONE);scroll.setVisibility(View.GONE);categories.setVisibility(View.VISIBLE);select(selected);buttons[selected].requestFocus();return true;}
    void save(Bundle out){out.putInt(SAVED,selected);out.putBoolean("settings_detail",editing);out.putIntArray("settings_focus",last.clone());}
}
