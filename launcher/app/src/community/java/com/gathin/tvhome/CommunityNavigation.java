package com.gathin.tvhome;

import android.app.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.Supplier;

/** Native menu/submenu navigation; original controls retain their save/preview contracts. */
final class SettingsNavigation {
    static final class Item {
        final String title,help;final Supplier<String> value;final Runnable action;final Item[] children;
        Item(String title,String help,Supplier<String> value,Runnable action,Item...children){this.title=title;this.help=help;this.value=value;this.action=action;this.children=children;}
    }
    private final Activity a;private final View original;private final LinearLayout menu;private final ScrollView scroll;
    private final TextView title,context,help,breadcrumb;private final Typeface font;private final boolean rtl;
    private final Item root;private final List<Integer> path=new ArrayList<>();private final Map<String,Integer> memory=new HashMap<>();
    private final List<TextView> rows=new ArrayList<>();
    private int focused;
    SettingsNavigation(Activity activity,Bundle saved){
        a=activity;original=a.findViewById(R.id.settingsPanes);font=((TextView)a.findViewById(R.id.categoryLayout)).getTypeface();rtl=a.getResources().getBoolean(R.bool.rtl_menu_bar);
        root=tree();
        if(saved!=null){String key=saved.getString("studio_path","");try{if(!key.isEmpty())for(String p:key.split("/"))path.add(Integer.parseInt(p));current();}catch(Exception invalid){path.clear();}String[] keys=saved.getStringArray("studio_memory_keys");int[] values=saved.getIntArray("studio_memory_values");if(keys!=null&&values!=null&&keys.length==values.length)for(int i=0;i<keys.length;i++)memory.put(keys[i],values[i]);}
        ViewGroup host=(ViewGroup)original.getParent();host.removeView(original);FrameLayout frame=new FrameLayout(a);original.setVisibility(View.GONE);frame.addView(original);
        LinearLayout panes=new LinearLayout(a);panes.setOrientation(LinearLayout.HORIZONTAL);panes.setBackgroundColor(0xff111316);if(rtl)panes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView left=new ScrollView(a);left.setFocusable(false);left.setBackgroundColor(0xff1b1e23);LinearLayout explanation=new LinearLayout(a);explanation.setOrientation(LinearLayout.VERTICAL);explanation.setPadding(dp(56),dp(48),dp(42),dp(24));left.addView(explanation);
        breadcrumb=text("Launcher settings",13,0xffb7c1cf);breadcrumb.setId(R.id.studioBreadcrumb);explanation.addView(breadcrumb);
        title=text("",30,0xfff4f5f7);title.setId(R.id.studioTitle);add(explanation,title,16);
        context=text("",21,0xfff4f5f7);add(explanation,context,24);
        help=text("",17,0xffcbd2dc);help.setId(R.id.studioHelp);help.setLineSpacing(dp(4),1f);add(explanation,help,14);
        TextView tip=text("Up/Down to choose. OK opens. Back returns one level.",13,0xffaeb8c8);add(explanation,tip,32);
        panes.addView(left,new LinearLayout.LayoutParams(0,-1,1));scroll=new ScrollView(a);scroll.setId(R.id.settingsScroll);scroll.setFillViewport(false);scroll.setPadding(dp(22),dp(40),dp(34),dp(24));scroll.setClipToPadding(false);
        menu=new LinearLayout(a);menu.setId(R.id.studioMenu);menu.setOrientation(LinearLayout.VERTICAL);scroll.addView(menu);panes.addView(scroll,new LinearLayout.LayoutParams(dp(450),-1));frame.addView(panes,new FrameLayout.LayoutParams(-1,-1));host.addView(frame,new ViewGroup.LayoutParams(-1,-1));render();
    }
    private int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView v=new TextView(a);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setTypeface(font);return v;}
    private void add(LinearLayout parent,View view,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(view,p);}
    private Item item(String t,String h,Supplier<String> v,Runnable r){return new Item(t,h,v,r);}
    private Item page(String t,String h,String v,Item...children){return new Item(t,h,()->v,null,Arrays.stream(children).filter(Objects::nonNull).toArray(Item[]::new));}
    private String raw(int id){View v=original.findViewById(id);if(!(v instanceof TextView)||v.getVisibility()!=View.VISIBLE)return "Unavailable in this configuration";String s=((TextView)v).getText().toString();int n=s.indexOf(" · ");if(n>=0)return s.substring(n+3);n=s.indexOf(": ");return n>=0?s.substring(n+2):"Open";}
    private boolean categoryContext(){return a.getIntent().getBooleanExtra("category_context",false)||HomeCategories.enabled(a);}
    private boolean categoryAction(int id){
        if(!categoryContext())return false;
        if(id==R.id.reorderChoice){HomeCategories.show(a);return true;}
        if(id==R.id.previewIdleChoice){a.startActivity(new android.content.Intent(a,CategoryHomeActivity.class).putExtra(CategoryHomeActivity.PREVIEW,!HomeCategories.enabled(a)).putExtra(CategoryHomeActivity.IDLE_PREVIEW,true));return true;}
        if(id==R.id.positionChoice||id==R.id.stylesChoice||id==R.id.styleUndoChoice){
            new AlertDialog.Builder(a).setTitle("Existing layout controls").setMessage("This preview applies to the existing app grid. In Apps & rows → Categories, choose Use existing layout before trying it. Your category rows remain saved.").setPositiveButton(android.R.string.ok,null).show();return true;
        }
        return false;
    }
    private Item source(int id,String t,String h){if(categoryContext()&&(id==R.id.advancedChoice||id==R.id.audioIndicatorChoice))return null;return item(t,h,()->raw(id),()->{if(categoryAction(id))return;View v=original.findViewById(id);if(v==null||v.getVisibility()!=View.VISIBLE||!v.isEnabled()||!v.hasOnClickListeners()){Toast.makeText(a,"This option is unavailable in the current configuration",Toast.LENGTH_LONG).show();return;}v.performClick();a.getWindow().getDecorView().post(()->{if(!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus())render();});});}
    private Item numeric(String t,String h,int caption,int[] ids,String[] labels){return item(t,h,()->raw(caption),()->{
        int selected=0;for(int i=0;i<ids.length;i++)if(original.findViewById(ids[i]).isSelected())selected=i;
        new AlertDialog.Builder(a).setTitle(t).setSingleChoiceItems(labels,selected,(d,which)->{original.findViewById(ids[which]).performClick();d.dismiss();render();}).setNegativeButton(android.R.string.cancel,null).show();
    });}
    private Item tree(){
        Item layout=page("Layout","Position, extra rows and dock background control the existing app grid. Category rows are arranged in Apps & rows → Categories.","Position, rows and Home styles",
            source(R.id.positionChoice,"App position",a.getString(R.string.help_position)),
            numeric("Apps per row","More apps per row makes each tile smaller. Your app order stays saved.",R.id.settingsColumnsCaption,new int[]{R.id.columns4,R.id.columns5,R.id.columns6,R.id.columns7,R.id.columns8},new String[]{"4","5","6","7","8"}),
            categoryContext()?null:numeric("Rows below favorites","Choose how many additional grid rows fit below favorites.",R.id.settingsRowsCaption,new int[]{R.id.rows0,R.id.rows1,R.id.rows2,R.id.rows3},new String[]{"0","1","2","3"}),
            categoryContext()?null:numeric("Favorites background","Control the favorites dock background opacity.",R.id.settingsOpacityCaption,new int[]{R.id.dock100,R.id.dock70,R.id.dock50,R.id.dock30},new String[]{"100%","70%","50%","30%"}),
            source(R.id.stylesChoice,"Home style","Preview an existing layout. Keep it or let the countdown revert it."),source(R.id.styleUndoChoice,"Undo Home style","Return to the previous committed style."));
        Item text=page("Text & clock","Make Home readable from your seat.","Text color, names and size preview",
            source(R.id.adaptiveHeaderChoice,"Home text",a.getString(R.string.help_text)),source(R.id.appNamesChoice,"App names",a.getString(R.string.help_names)),
            item("Text sizes","Preview clock, header and app-name sizes independently. Apply saves this profile only.",()->"Preview before applying",()->HomeAppearance.show(a)));
        Item motion=page("Focus & motion","Keep selection clear and motion comfortable.","Outline, spacing and idle view",
            source(R.id.focusOutlineChoice,"Focus outline",a.getString(R.string.help_focus)),source(R.id.tileSpacingChoice,"Extra tile spacing",a.getString(R.string.help_spacing)),source(R.id.refinedMotionChoice,"Focus motion","Use a calmer focus transition; reduced-motion preferences are respected."),source(R.id.animationsChoice,"Animations","Enable or disable the existing Home animations."),source(R.id.cinematicIdleChoice,"Cinematic idle",a.getString(R.string.help_idle)),source(R.id.previewIdleChoice,"Preview idle view","Preview the quiet Home presentation. The first navigation press restores controls."),source(R.id.advancedChoice,"Advanced effects",a.getString(R.string.advanced_effects_warning)));
        return page("Launcher settings","Make Home comfortable to use.","",
            page("Appearance","Choose a layout, readable text and comfortable focus behavior.","Layout, text and motion",layout,text,motion),
            page("Apps & rows","Organize apps without changing their accounts or installed data.","Customize apps and create categories",
                item("Customize apps","Rename an app or change its artwork. Your app layout stays saved.",()->"Name, icon and banner",()->NativeTileEditor.show(a)),
                item("Categories","Create named app rows. Preview first; the existing Home stays saved until you explicitly enable category layout.",()->HomeCategories.enabled(a)?"Category layout on":"Existing layout",()->HomeCategories.show(a)),
                source(R.id.reorderChoice,"Arrange apps",a.getString(R.string.reorder_hint)),source(R.id.findAppsChoice,"Find apps","Search installed app names and packages locally."),source(R.id.recentAppsChoice,"Recently opened apps",a.getString(R.string.recent_apps_help)),source(R.id.clearRecentChoice,"Clear recent apps",a.getString(R.string.recent_clear_help))),
            page("Wallpaper","Browse before selecting. Home video remains silent.","Visual browser, themes and dimming",
                item("Wallpaper playlist","Rotate local background pictures and silent videos without rotating app layout or theme settings. The timer pauses outside Home.",()->{PlaylistPolicy.Spec p=WallpaperPlaylistChoice.read(a);return p.enabled?p.tokens.size()+" wallpapers · "+p.seconds/60+" minutes":"Off";},()->WallpaperPlaylistChoice.show(a)),
                item("Browse wallpapers","Preview a local picture before choosing Apply. Back leaves the current wallpaper unchanged.",()->"Pictures in the current theme",()->WallpaperBrowser.show(a,false)),
                item("Browse themes","See an imported theme’s preview before selecting it.",()->"Offline theme previews",()->WallpaperBrowser.show(a,true)),
                source(R.id.themesChoice,"Manage offline themes","Import or remove local theme packs. Importing does not select them."),source(R.id.liveWallpaperChoice,"Silent live wallpaper",a.getString(R.string.help_live)),source(R.id.wallpaperDimChoice,"Wallpaper dimming",a.getString(R.string.wallpaper_dim_help))),
            page("Profiles","Give each person their own apps and appearance.","Launcher preferences for each person",
                source(R.id.profilesChoice,"Launcher profiles","Create a profile from this Home, switch, rename or delete a locally created profile. Streaming accounts are separate.")),
            page("Remotes & inputs","Use real device controls with clear fallbacks.","HDMI, remote buttons and Bluetooth",
                source(R.id.inputsShortcutChoice,"Inputs shortcut",a.getString(R.string.help_inputs)),source(R.id.inputVisibilityChoice,"Input visibility","Choose whether inactive bare HDMI ports stay visible."),source(R.id.tvButtonChoice,"TV button","Choose what the TV button opens. Physical remote behavior depends on the selected helper and firmware."),source(R.id.tvButtonHelper,"Remote helper","Open the existing helper settings only when you choose to."),source(R.id.bluetoothChoice,"Bluetooth & accessories","Open the TV’s Bluetooth/accessories settings. This does not pair a device automatically."),source(R.id.audioIndicatorChoice,"Audio-playing indicator","An optional generic indicator while Home is visible; it does not identify a track or source.")),
            page("Backup & restore","Recover Home appearance and app choices without changing TV settings.","Saved setups and automatic recent changes",
                source(R.id.savedSetupsChoice,"Saved setups",a.getString(R.string.help_setups)),item("Recent changes","Automatic restore points are kept after real changes, separately from named setups. Media files must still be available.",()->"Up to five per profile",()->HomeHistory.show(a)),source(R.id.followThemeChoice,"Restore theme defaults",a.getString(R.string.settings_reset_help))),
            page("System","Open system pages only when you choose to.","Startup and TV settings",
                source(R.id.openOnStartChoice,"Open on start","Choose a startup app without changing favorites."),source(R.id.quickActionsChoice,"Quick actions","Open existing system shortcuts, recently opened apps and profile access."),source(R.id.defaultHomeChoice,"Default Home",a.getString(R.string.freedom_home_settings_help)),source(R.id.tvSettingsChoice,"TV settings","Open the TV’s own settings. Launcher settings do not change picture, sound or network options."),item(a.getString(R.string.freedom_about),"Version information, project credits and licence.",()->"App version and credits",this::about)));
    }
    private void about(){
        String version="";try{version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;}catch(Exception ignored){}
        new AlertDialog.Builder(a).setTitle(R.string.freedom_app_name).setMessage(a.getString(R.string.freedom_tagline)+"\n\nVersion "+version+"\n\nAn independent DVOpenLabs launcher, based on CleanYourTV.").setNeutralButton(R.string.freedom_credits,(dialog,which)->credits()).setPositiveButton(android.R.string.ok,null).show();
    }
    private void credits(){
        StringBuilder notice=new StringBuilder();
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(a.getResources().openRawResource(R.raw.freedom_notices),java.nio.charset.StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)notice.append(line).append('\n');}
        catch(java.io.IOException failure){notice.append("Licence and source credits: github.com/DVOpenLabs/cleanyourtv");}
        new AlertDialog.Builder(a).setTitle(R.string.freedom_credits).setMessage(notice.toString()).setPositiveButton(android.R.string.ok,null).show();
    }
    private Item current(){Item node=root;for(int i:path){if(i<0||i>=node.children.length)throw new IllegalArgumentException();node=node.children[i];}return node;}
    private String key(){StringBuilder b=new StringBuilder();for(int i:path){if(b.length()>0)b.append('/');b.append(i);}return b.toString();}
    private void describe(Item e){context.setText(e.title);help.setText(e.help);}
    private void render(){
        if(a.isFinishing()||a.isDestroyed())return;Item page=current();title.setText(page.title);StringBuilder crumbs=new StringBuilder(a.getString(R.string.freedom_app_name));Item p=root;for(int i:path){p=p.children[i];crumbs.append("  ›  ").append(p.title);}breadcrumb.setText(crumbs);menu.removeAllViews();rows.clear();
        for(int i=0;i<page.children.length;i++){final int at=i;Item e=page.children[i];TextView row=text("",17,0xfff4f5f7);row.setId(View.generateViewId());row.setFocusable(true);row.setFocusableInTouchMode(true);row.setClickable(true);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(18),dp(10),dp(18),dp(10));row.setBackgroundResource(R.drawable.settings_category_bg);
            String value=e.value.get();SpannableStringBuilder label=new SpannableStringBuilder(e.title);if(value!=null&&!value.isEmpty()){int start=label.length();label.append('\n').append(value);label.setSpan(new RelativeSizeSpan(.78f),start,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setSpan(new ForegroundColorSpan(0xffc2cbd8),start,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}row.setText(label);row.setContentDescription(e.title+(value==null?"":", "+value));
            if(e.children.length>0){android.graphics.drawable.Drawable icon=a.getDrawable(R.drawable.ic_chevron).mutate();icon.setBounds(0,0,dp(12),dp(20));icon.setTint(0xffd6dce6);row.setCompoundDrawablesRelative(null,null,icon,null);}
            row.setOnFocusChangeListener((v,has)->{if(has){focused=at;memory.put(key(),at);describe(e);}});
            row.setOnClickListener(v->{memory.put(key(),at);if(e.children.length>0){path.add(at);render();}else if(e.action!=null)e.action.run();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);row.setMinHeight(dp(62));lp.bottomMargin=dp(4);menu.addView(row,lp);rows.add(row);
        }
        focused=Math.max(0,Math.min(rows.size()-1,memory.getOrDefault(key(),0)));scroll.scrollTo(0,0);if(!rows.isEmpty()){rows.get(focused).requestFocus();rows.get(focused).post(()->rows.get(focused).requestFocus());describe(page.children[focused]);}
    }
    boolean key(KeyEvent event){
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;int key=event.getKeyCode();
        if(key==(rtl?KeyEvent.KEYCODE_DPAD_RIGHT:KeyEvent.KEYCODE_DPAD_LEFT)){if(!path.isEmpty())back();return true;}
        if(key==(rtl?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT)){if(focused>=0&&focused<rows.size())rows.get(focused).performClick();return true;}
        if(key==KeyEvent.KEYCODE_DPAD_UP&&focused==0||key==KeyEvent.KEYCODE_DPAD_DOWN&&focused==rows.size()-1)return true;return false;
    }
    boolean back(){if(path.isEmpty())return false;memory.put(key(),focused);path.remove(path.size()-1);render();return true;}
    void save(Bundle out){out.putString("studio_path",key());out.putStringArray("studio_memory_keys",memory.keySet().toArray(new String[0]));int[] values=new int[memory.size()];int n=0;for(Integer value:memory.values())values[n++]=value;out.putIntArray("studio_memory_values",values);}
}
