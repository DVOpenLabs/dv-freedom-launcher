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
        int source;int[] ids;String[] options;
        Item(String title,String help,Supplier<String> value,Runnable action,Item...children){this.title=title;this.help=help;this.value=value;this.action=action;this.children=children;}
    }
    private final Activity a;private final View original;private final LinearLayout menu,rail;private final ScrollView scroll;
    private final TextView title,context,help,breadcrumb,scope,inspectorValue,behavior;private final View previewPanel;private final Typeface font,headingFont,mediumFont;private final boolean rtl;
    private final HomeMiniPreview lightPreview,darkPreview;
    private final Item root;private final List<Integer> path=new ArrayList<>();private final Map<String,Integer> memory=new HashMap<>();
    private final List<View> rows=new ArrayList<>(),railRows=new ArrayList<>();private final List<Item> shown=new ArrayList<>();
    private final Map<Item,Integer> drafts=new IdentityHashMap<>();private final Map<Item,Long> draftRevisions=new IdentityHashMap<>();private final String editingProfile=ProfileStore.id();
    private int focused;private boolean railFocus,searching,rendering;private String query="";private EditText searchField;
    SettingsNavigation(Activity activity,Bundle saved){
        a=activity;original=a.findViewById(R.id.settingsPanes);font=SettingsTypography.regular(a);headingFont=SettingsTypography.heading(a);mediumFont=SettingsTypography.medium(a);rtl=a.getResources().getBoolean(R.bool.rtl_menu_bar);root=tree();
        if(saved!=null&&saved.getInt("studio_version",0)==12){String key=saved.getString("studio_path","");try{if(!key.isEmpty())for(String p:key.split("/"))path.add(Integer.parseInt(p));current();}catch(Exception invalid){path.clear();}String[] keys=saved.getStringArray("studio_memory_keys");int[] values=saved.getIntArray("studio_memory_values");if(keys!=null&&values!=null&&keys.length==values.length)for(int i=0;i<keys.length;i++)memory.put(keys[i],values[i]);query=saved.getString("studio_query","");searching=saved.getBoolean("studio_search",false);}
        if(path.isEmpty())path.add(0); // Appearance is a useful, read-only landing page.
        ViewGroup host=(ViewGroup)original.getParent();host.removeView(original);FrameLayout frame=new FrameLayout(a);original.setVisibility(View.GONE);frame.addView(original);
        LinearLayout panes=new LinearLayout(a);panes.setOrientation(LinearLayout.HORIZONTAL);panes.setBackgroundColor(color(R.color.freedom_canvas));panes.setLayoutDirection(rtl?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        LinearLayout sidebar=new LinearLayout(a);sidebar.setOrientation(LinearLayout.VERTICAL);sidebar.setBackgroundColor(color(R.color.freedom_rail));sidebar.setPadding(dp(16),dp(24),dp(12),dp(16));
        TextView brand=text("DV Freedom",20,color(R.color.freedom_text));brand.setTypeface(headingFont);sidebar.addView(brand);add(sidebar,text("Launcher settings",15,color(R.color.freedom_secondary)),4);
        ScrollView railScroll=new ScrollView(a);railScroll.setFillViewport(false);railScroll.setClipToPadding(false);railScroll.setVerticalScrollBarEnabled(false);railScroll.setPadding(0,dp(16),0,0);rail=new LinearLayout(a);rail.setOrientation(LinearLayout.VERTICAL);railScroll.addView(rail);sidebar.addView(railScroll,new LinearLayout.LayoutParams(-1,0,1));panes.addView(sidebar,new LinearLayout.LayoutParams(dp(216),-1));
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(24),dp(24),dp(24));
        breadcrumb=text("",14,color(R.color.freedom_secondary));breadcrumb.setId(R.id.studioBreadcrumb);body.addView(breadcrumb);
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);title=text("",26,color(R.color.freedom_text));title.setTypeface(headingFont);title.setId(R.id.studioTitle);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));scope=text("",14,color(R.color.freedom_secondary));scope.setMaxLines(2);scope.setEllipsize(TextUtils.TruncateAt.END);scope.setGravity(Gravity.END);heading.addView(scope,new LinearLayout.LayoutParams(dp(198),-2));add(body,heading,4);
        LinearLayout workspace=new LinearLayout(a);workspace.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,0,1);wp.topMargin=dp(20);body.addView(workspace,wp);
        scroll=new ScrollView(a);scroll.setId(R.id.settingsScroll);scroll.setVerticalScrollBarEnabled(true);scroll.setScrollbarFadingEnabled(true);scroll.setFillViewport(false);scroll.setPadding(0,0,dp(8),dp(8));scroll.setClipToPadding(false);menu=new LinearLayout(a);menu.setId(R.id.studioMenu);menu.setOrientation(LinearLayout.VERTICAL);scroll.addView(menu);workspace.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        ScrollView inspector=new ScrollView(a);inspector.setFocusable(false);inspector.setVerticalScrollBarEnabled(false);LinearLayout detail=new LinearLayout(a);detail.setOrientation(LinearLayout.VERTICAL);detail.setPadding(dp(16),dp(16),dp(16),dp(16));inspector.setBackgroundResource(R.drawable.freedom_inspector);inspector.addView(detail);
        context=text("",20,color(R.color.freedom_text));context.setTypeface(mediumFont);detail.addView(context);inspectorValue=text("",16,color(R.color.freedom_accent));inspectorValue.setTypeface(mediumFont);add(detail,inspectorValue,8);help=text("",16,color(R.color.freedom_secondary));help.setId(R.id.studioHelp);help.setLineSpacing(dp(2),1f);help.setMaxLines(3);help.setEllipsize(TextUtils.TruncateAt.END);add(detail,help,12);
        LinearLayout samples=new LinearLayout(a);samples.setOrientation(LinearLayout.VERTICAL);lightPreview=new HomeMiniPreview(a,font);darkPreview=new HomeMiniPreview(a,font);LinearLayout.LayoutParams sample=new LinearLayout.LayoutParams(dp(144),dp(81));sample.topMargin=dp(12);sample.gravity=Gravity.CENTER_HORIZONTAL;samples.addView(darkPreview,sample);add(samples,text("Dark still preview",14,color(R.color.freedom_secondary)),4);LinearLayout.LayoutParams bright=new LinearLayout.LayoutParams(dp(144),dp(81));bright.topMargin=dp(8);bright.gravity=Gravity.CENTER_HORIZONTAL;samples.addView(lightPreview,bright);add(samples,text("Light still preview",14,color(R.color.freedom_secondary)),4);detail.addView(samples);previewPanel=samples;
        behavior=text("",14,color(R.color.freedom_secondary));add(detail,behavior,12);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(220),-1);ip.setMarginStart(dp(16));workspace.addView(inspector,ip);
        panes.addView(body,new LinearLayout.LayoutParams(0,-1,1));frame.addView(panes,new FrameLayout.LayoutParams(-1,-1));host.addView(frame,new ViewGroup.LayoutParams(-1,-1));buildRail();render();frame.getViewTreeObserver().addOnWindowFocusChangeListener(has->{if(has)restoreFocus();});frame.post(this::restoreFocus);
    }
    private void restoreFocus(){if(a.isFinishing()||a.isDestroyed()||a.getCurrentFocus()!=null)return;if(railFocus)railRows.get(searching?0:path.get(0)+1).requestFocusFromTouch();else if(searching&&searchField!=null)searchField.requestFocusFromTouch();else if(!rows.isEmpty())rows.get(Math.max(0,Math.min(rows.size()-1,focused))).requestFocusFromTouch();}
    private int color(int id){return a.getColor(id);}
    private int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView v=new TextView(a);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setTypeface(font);v.setFontFeatureSettings("tnum");return v;}
    private void add(LinearLayout parent,View view,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(view,p);}
    private int icon(String label){
        String name=label.toLowerCase(Locale.ROOT);
        if(name.contains("search")||name.contains("find"))return R.drawable.freedom_search;
        if(name.contains("text")||name.contains("clock")||name.contains("names"))return R.drawable.freedom_text;
        if(name.contains("setup")||name.contains("help"))return R.drawable.freedom_help;
        if(name.contains("avatar")||name.contains("profile"))return R.drawable.freedom_profile;
        if(name.contains("lock"))return R.drawable.freedom_lock;
        if(name.contains("wallpaper")||name.contains("theme"))return R.drawable.freedom_wallpaper;
        if(name.contains("apps")||name.contains("arrange")||name.contains("row"))return R.drawable.freedom_apps;
        if(name.contains("input")||name.contains("tv button"))return R.drawable.freedom_inputs;
        if(name.contains("backup")||name.contains("restore")||name.contains("recent"))return R.drawable.freedom_backup;
        if(name.contains("appearance")||name.contains("experience")||name.contains("layout")||name.contains("position"))return R.drawable.freedom_appearance;
        if(name.contains("focus")||name.contains("motion")||name.contains("spacing"))return R.drawable.freedom_motion;
        return R.drawable.freedom_settings;
    }
    private ImageView iconView(String label){ImageView image=new ImageView(a);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);image.setImageResource(icon(label));image.setColorFilter(0xffc8d9ec);return image;}
    private void buildRail(){
        for(int i=-1;i<root.children.length;i++){final int at=i;String label=i<0?"Search settings":root.children[i].title;LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setClickable(true);row.setPadding(dp(10),dp(8),dp(8),dp(8));row.setBackgroundResource(R.drawable.freedom_settings_row);row.setContentDescription(label);row.addView(iconView(label),new LinearLayout.LayoutParams(dp(22),dp(22)));TextView name=text(label,16,0xfff5f7fa);name.setTypeface(mediumFont);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams n=new LinearLayout.LayoutParams(0,-2,1);n.setMarginStart(dp(10));row.addView(name,n);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);row.setMinimumHeight(dp(48));rail.addView(row,lp);railRows.add(row);
            row.setOnFocusChangeListener((v,has)->{if(has&&!rendering){railFocus=true;if(at<0){searching=true;}else{searching=false;path.clear();path.add(at);}render();}});row.setOnClickListener(v->{if(at<0){searching=true;render();if(searchField!=null){searchField.requestFocus();((android.view.inputmethod.InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(searchField,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}}else{searching=false;path.clear();path.add(at);railFocus=false;render();}});
        }
    }
    private Item item(String t,String h,Supplier<String> v,Runnable r){return new Item(t,h,v,r);}
    private Item page(String t,String h,String v,Item...children){Item[] visible=Arrays.stream(children).filter(Objects::nonNull).toArray(Item[]::new);return visible.length==0?null:new Item(t,h,()->v,null,visible);}
    private String raw(int id){View v=original.findViewById(id);if(!(v instanceof TextView)||v.getVisibility()!=View.VISIBLE)return "Unavailable in this configuration";String s=((TextView)v).getText().toString();int n=s.indexOf(" · ");if(n>=0)return s.substring(n+3);n=s.indexOf(": ");return n>=0?s.substring(n+2):"Open";}
    private boolean categoryContext(){return !ExperienceChoice.enabled(a)&&(a.getIntent().getBooleanExtra("category_context",false)||HomeCategories.enabled(a));}
    private boolean experienceContext(){return ExperienceChoice.enabled(a);}
    private boolean categoryAction(int id){
        if(!categoryContext())return false;
        if(id==R.id.reorderChoice){HomeCategories.show(a);return true;}
        if(id==R.id.previewIdleChoice){a.startActivity(new android.content.Intent(a,CategoryHomeActivity.class).putExtra(CategoryHomeActivity.PREVIEW,!HomeCategories.enabled(a)).putExtra(CategoryHomeActivity.IDLE_PREVIEW,true));return true;}
        if(id==R.id.positionChoice||id==R.id.stylesChoice||id==R.id.styleUndoChoice){
            new AlertDialog.Builder(a).setTitle("Existing layout controls").setMessage("This preview applies to the existing app grid. In Apps & rows → Categories, choose Use existing layout before trying it. Your category rows remain saved.").setPositiveButton(android.R.string.ok,null).show();return true;
        }
        return false;
    }
    private Item source(int id,String t,String h){View nativeRow=original.findViewById(id);if(nativeRow==null||nativeRow.getVisibility()!=View.VISIBLE)return null;if((categoryContext()||experienceContext())&&(id==R.id.advancedChoice||id==R.id.audioIndicatorChoice||id==R.id.openOnStartChoice))return null;Item result=item(t,h,()->raw(id),()->{if(categoryAction(id))return;if(id==R.id.reorderChoice&&experienceContext()){FavoriteOrderEditor.show(a);return;}View v=original.findViewById(id);if(v==null||v.getVisibility()!=View.VISIBLE||!v.isEnabled()||!v.hasOnClickListeners()){Toast.makeText(a,"This option is unavailable in the current configuration",Toast.LENGTH_LONG).show();return;}v.performClick();a.getWindow().getDecorView().post(()->{if(!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus())render();});});result.source=id;return result;}
    private Item numeric(String t,String h,int caption,int[] ids,String[] labels){Item result=item(t,h,()->raw(caption),null);result.ids=ids;result.options=labels;return result;}
    private Item tree(){
        Item layout=page("Layout","Choose where apps sit, their size and the space between them.","Position, icon size and app spacing",
            source(R.id.positionChoice,"App position",a.getString(R.string.help_position)),
            item("Icon size","Choose Small, Medium or Large. Preview before Apply.",()->IconSizePolicy.label(IconSizes.current(a)),()->IconSizes.show(a)),
            source(R.id.tileSpacingChoice,"App spacing","Add space between app tiles without changing their order."),
            numeric("Apps per row","More apps per row makes each tile smaller. Your app order stays saved.",R.id.settingsColumnsCaption,new int[]{R.id.columns4,R.id.columns5,R.id.columns6,R.id.columns7,R.id.columns8},new String[]{"4","5","6","7","8"}),
            categoryContext()||experienceContext()?null:numeric("Rows below favorites","Choose how many additional grid rows fit below favorites.",R.id.settingsRowsCaption,new int[]{R.id.rows0,R.id.rows1,R.id.rows2,R.id.rows3},new String[]{"0","1","2","3"}),
            categoryContext()||experienceContext()?null:numeric("Favorites background","Control the favorites dock background opacity.",R.id.settingsOpacityCaption,new int[]{R.id.dock100,R.id.dock70,R.id.dock50,R.id.dock30},new String[]{"100%","70%","50%","30%"}),
            experienceContext()?null:source(R.id.stylesChoice,"Home style","Preview an existing layout. Keep it or let the countdown revert it."),experienceContext()?null:source(R.id.styleUndoChoice,"Undo Home style","Return to the previous committed style."));
        Item text=page("Text & clock","Make Home readable from your seat.","Text color, names and size preview",
            source(R.id.adaptiveHeaderChoice,"Home text",a.getString(R.string.help_text)),source(R.id.appNamesChoice,"App names",a.getString(R.string.help_names)),
            item("Text sizes","Preview clock, header and app-name sizes independently. Apply saves this profile only.",()->"Preview before applying",()->HomeAppearance.show(a)));
        Item motion=page("Focus & motion","Keep selection clear and motion comfortable.","Outline, spacing and idle view",
            source(R.id.focusOutlineChoice,"Focus outline",a.getString(R.string.help_focus)),source(R.id.refinedMotionChoice,"Focus motion","Use a calmer focus transition; reduced-motion preferences are respected."),experienceContext()?null:source(R.id.animationsChoice,"Animations","Enable or disable the existing Home animations."),source(R.id.cinematicIdleChoice,"Cinematic idle",a.getString(R.string.help_idle)),source(R.id.previewIdleChoice,"Preview idle view","Preview the quiet Home presentation. The first navigation press restores controls."),page("Advanced","Extra effects for capable TVs. Start with the standard appearance.","Optional effects",source(R.id.advancedChoice,"Advanced effects",a.getString(R.string.advanced_effects_warning))));
        return page("Launcher settings","Make Home comfortable to use.","",
            page("Appearance","Choose a Home style, arrange your apps and make text comfortable to read.","Home styles, layout and readability",
                item("Home experience","Choose Classic, Glass, Clean or Cinema. Apps and wallpaper stay saved.",()->ExperiencePolicy.name(ExperienceChoice.saved(a)),()->ExperienceChoice.show(a)),
                layout,text,motion,
                item("Setup help","Walk through Home style, apps and readability, or review an AI setup draft before applying it.",()->"Guided setup and AI drafts",()->a.startActivity(new android.content.Intent(a,GuidedSetupActivity.class)))),
            page("Apps & rows","Organize apps without changing their accounts or installed data.","Customize apps and create categories",
                item("Content shelves","Optional Watch Next items shared by installed apps. Enable TV listings access only if you want this. Empty shelves collapse.",()->ContentShelves.status(a),()->ContentShelves.settings(a)),
                item("Customize apps","Rename an app or change its artwork. Your app layout stays saved.",()->"Name, icon and banner",()->NativeTileEditor.show(a)),
                experienceContext()?null:item("Categories","Create named app rows. Preview first; the existing Home stays saved until you explicitly enable category layout.",()->HomeCategories.enabled(a)?"Category layout on":"Existing layout",()->HomeCategories.show(a)),
                source(R.id.reorderChoice,"Arrange apps",a.getString(R.string.reorder_hint)),source(R.id.findAppsChoice,"Find apps","Search installed app names and packages locally."),source(R.id.recentAppsChoice,"Recently opened apps",a.getString(R.string.recent_apps_help)),source(R.id.clearRecentChoice,"Clear recent apps",a.getString(R.string.recent_clear_help))),
            page("Wallpaper","Browse before selecting. Home video remains silent.","Visual browser, themes and dimming",
                item("Wallpaper playlist","Rotate local background pictures and silent videos without rotating app layout or theme settings. The timer pauses outside Home.",()->{PlaylistPolicy.Spec p=WallpaperPlaylistChoice.read(a);return p.enabled?p.tokens.size()+" wallpapers · "+p.seconds/60+" minutes":"Off";},()->WallpaperPlaylistChoice.show(a)),
                item("Scene schedules","Choose local-time windows for playlist backgrounds. Profiles, app order and TV settings stay unchanged.",()->SceneSchedules.read(a).enabled?SceneSchedules.read(a).rules.size()+" scene windows":"Off",()->SceneSchedules.show(a)),
                item("Browse wallpapers","Preview a local picture before choosing Apply. Back leaves the current wallpaper unchanged.",()->"Pictures in the current theme",()->WallpaperBrowser.show(a,false)),
                item("Browse themes","See an imported theme’s preview before selecting it.",()->"Offline theme previews",()->WallpaperBrowser.show(a,true)),
                source(R.id.themesChoice,"Manage offline themes","Import or remove local theme packs. Importing does not select them."),source(R.id.liveWallpaperChoice,"Silent live wallpaper",a.getString(R.string.help_live)),source(R.id.wallpaperDimChoice,"Wallpaper dimming",a.getString(R.string.wallpaper_dim_help))),
            page("Profiles","Give each person their own apps and appearance.","Launcher preferences for each person",
                item("Switch profile","Choose a local launcher profile with its own app and appearance choices.",()->ProfileAvatars.label(a),()->ProfilePicker.show(a,false)),
                item("Profile avatar","Choose a recognizable color for this launcher's profile.",()->ProfileStore.preferences(a).getString(ProfileAvatars.KEY,"Ocean"),()->ProfileAvatars.settings(a)),
                item("Layout lock","An optional PIN prevents casual edits to this profile. TV settings, streaming accounts and profile switching remain separate.",()->LauncherLock.enabled(a)?"On":"Off",()->LauncherLock.settings(a)),
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
    private void describe(Item e){
        context.setText(e.title);help.setText(e.help);String value=e.value.get();inspectorValue.setText(value==null?"":value);inspectorValue.setVisibility(View.GONE);behavior.setText(segmented(e)?"Left/Right: preview\nOK: "+(e.source==R.id.positionChoice?"try on Home":"save"):toggle(e)?"OK: change\nHold OK for help":"OK: open\nHold OK for help");boolean showPreview=visual(e)&&SettingsLayoutPolicy.previews(a.getResources().getDisplayMetrics().heightPixels/a.getResources().getDisplayMetrics().density,a.getResources().getConfiguration().fontScale);previewPanel.setVisibility(showPreview?View.VISIBLE:View.GONE);help.setMaxLines(showPreview?3:8);
        String position=e.source==R.id.positionChoice?AppPlacement.IDS[draft(e)]:null;
        lightPreview.inspect(e.title,position,segmented(e)?choiceValue(e):null,true);darkPreview.inspect(e.title,position,segmented(e)?choiceValue(e):null,false);
        scope.setText(scope(e));
    }
    private void showHelp(Item e){new AlertDialog.Builder(a).setTitle(e.title).setMessage(e.help+"\n\n"+scope(e)).setPositiveButton(android.R.string.ok,null).show();}
    private boolean visual(Item e){return !searching&&!path.isEmpty()&&(path.get(0)==0||path.get(0)==2)&&!e.title.equals("Setup help");}
    private String scope(Item e){return ProfileAvatars.label(a)+"\n"+(SettingsSearch.shared(e.title)?"Shared across profiles":"Personal to this profile");}
    private boolean segmented(Item e){return e.ids!=null||e.source==R.id.tileSpacingChoice||e.source==R.id.positionChoice&&!categoryContext();}
    private String[] options(Item e){return e.options!=null?e.options:e.source==R.id.tileSpacingChoice?new String[]{"Theme","0 dp","8 dp","16 dp","24 dp"}:new String[]{"Top","Middle","Bottom"};}
    private String choiceValue(Item e){return options(e)[draft(e)];}
    private int selected(Item e){if(e.source==R.id.tileSpacingChoice){android.content.SharedPreferences p=ProfileStore.preferences(a);return SettingsChoicePolicy.spacingIndex(p.contains(TileSpacing.CHOICE),p.getInt(TileSpacing.CHOICE,-1));}if(e.ids!=null){for(int i=0;i<e.ids.length;i++)if(original.findViewById(e.ids[i]).isSelected())return i;return 0;}String saved=PlacementSession.selected(a);return Arrays.asList(AppPlacement.IDS).indexOf(saved);}
    private int draft(Item e){return drafts.getOrDefault(e,Math.max(0,selected(e)));}
    private boolean toggle(Item e){return e.source==R.id.refinedMotionChoice||e.source==R.id.animationsChoice||e.source==R.id.liveWallpaperChoice;}
    private boolean checked(Item e){android.content.SharedPreferences p=ProfileStore.preferences(a);if(e.source==R.id.refinedMotionChoice)return p.getBoolean(HomePolish.MOTION,false);if(e.source==R.id.liveWallpaperChoice)return p.getBoolean(ThemeLibrary.LIVE,false);return Config.load(a).animations;}
    private void activate(Item e,int at){
        if(searching){SearchHit hit=hits.get(at);searching=false;path.clear();path.addAll(hit.parent);if(path.isEmpty()){path.add(hit.index);memory.put(key(),0);}else memory.put(key(),hit.index);railFocus=false;render();return;}
        memory.put(key(),at);if(e.children.length>0){path.add(at);render();}else if(e.ids!=null){original.findViewById(e.ids[draft(e)]).performClick();drafts.remove(e);render();}
        else if(e.source==R.id.tileSpacingChoice){saveSpacing(e);}
        else if(e.source==R.id.positionChoice&&!categoryContext()){PlacementSession.PREVIEW.begin(AppPlacement.IDS[draft(e)],android.os.SystemClock.elapsedRealtime());a.finish();}
        else if(e.action!=null)e.action.run();
    }
    private void saveSpacing(Item e){
        android.content.SharedPreferences p=ProfileStore.preferences(a);long revision=p.getLong(Config.SETTINGS_REVISION_KEY,0);
        if(!editingProfile.equals(ProfileStore.id())||draftRevisions.containsKey(e)&&draftRevisions.get(e)!=revision){Toast.makeText(a,"Home changed. Open spacing again.",Toast.LENGTH_LONG).show();a.finish();return;}
        if(draft(e)==selected(e))return;
        Map<String,Object> values=new HashMap<>();values.put(TileSpacing.CHOICE,draft(e)==0?null:new int[]{0,0,8,16,24}[draft(e)]);values.put(Config.SETTINGS_REVISION_KEY,revision+1);
        ProductFeatures.beforeChange(a,"Change tile spacing");if(SetupValues.commit(p,values)){a.setResult(Activity.RESULT_OK);a.finish();}else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_LONG).show();
    }
    private View makeRow(Item e,int at,String trail){
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setId(View.generateViewId());row.setFocusable(true);row.setClickable(true);row.setPadding(dp(12),dp(12),dp(12),dp(12));row.setBackgroundResource(R.drawable.freedom_settings_row);row.setMinimumHeight(dp(60));
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER_VERTICAL);line.addView(iconView(e.title),new LinearLayout.LayoutParams(dp(24),dp(24)));
        TextView label=text(e.title,18,color(R.color.freedom_text));label.setTypeface(mediumFont);label.setMaxLines(2);LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(0,-2,1);labelParams.setMarginStart(dp(14));line.addView(label,labelParams);
        String value=e.value.get();if(toggle(e)&&!searching){Switch control=new Switch(a);control.setChecked(checked(e));control.setFocusable(false);control.setClickable(false);control.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);line.addView(control);}
        else if((!segmented(e)||searching||selected(e)<0)&&e.children.length==0){TextView state=text(e.children.length>0?"":value,16,0xffc4d0df);state.setMaxLines(2);state.setEllipsize(TextUtils.TruncateAt.END);state.setGravity(Gravity.END);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(120),-2);sp.setMarginStart(dp(12));line.addView(state,sp);}
        if(!toggle(e)&&!segmented(e)||searching){ImageView arrow=new ImageView(a);arrow.setImageResource(R.drawable.ic_chevron);arrow.setColorFilter(0xffa6bbd3);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(10),dp(16));cp.setMarginStart(dp(14));line.addView(arrow,cp);}row.addView(line);
        if(segmented(e)&&!searching){LinearLayout chips=new LinearLayout(a);chips.setGravity(Gravity.CENTER_VERTICAL);chips.setPadding(dp(38),dp(10),0,0);String[] options=options(e);for(int i=0;i<options.length;i++){final int choice=i;TextView chip=text(options[i]+(selected(e)==i?" ✓":""),16,0xfff5f7fa);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(6),dp(6),dp(6),dp(6));chip.setBackgroundResource(R.drawable.freedom_settings_chip);chip.setSelected(draft(e)==i);chip.setClickable(true);chip.setFocusable(false);chip.setOnClickListener(v->{draftRevisions.putIfAbsent(e,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0));drafts.put(e,choice);activate(e,at);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMarginEnd(dp(6));chips.addView(chip,p);}row.addView(chips);}
        if(trail!=null){TextView location=text(trail,15,0xffbecbd9);location.setPadding(dp(38),dp(6),0,0);row.addView(location);}
        row.setOnLongClickListener(v->{showHelp(e);return true;});row.setContentDescription(e.title+(value==null?"":", "+value)+(segmented(e)?", Left or Right to preview, OK to confirm":""));row.setOnFocusChangeListener((v,has)->{if(has){railFocus=false;focused=at;if(!searching)memory.put(key(),at);describe(e);}});row.setOnClickListener(v->activate(e,at));return row;
    }
    private void render(){
        if(a.isFinishing()||a.isDestroyed()||rendering)return;rendering=true;
        Item page=current();title.setText(searching?"Search settings":page.title);StringBuilder crumbs=new StringBuilder("Settings");Item p=root;for(int i:path){p=p.children[i];crumbs.append(" › ").append(p.title);}int last=crumbs.lastIndexOf(" › ");breadcrumb.setText(searching?"Local settings search":last>0?crumbs.substring(0,last):"Launcher settings");menu.removeAllViews();rows.clear();shown.clear();
        for(int i=0;i<railRows.size();i++)railRows.get(i).setSelected(searching?i==0:i==path.get(0)+1);
        if(searching){renderSearch();rendering=false;return;}
        Item[] entries=page.children.length==0?new Item[]{page}:page.children;
        for(int i=0;i<entries.length;i++){Item e=entries[i];View row=makeRow(e,i,null);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);menu.addView(row,lp);rows.add(row);shown.add(e);}
        focused=Math.max(0,Math.min(rows.size()-1,memory.getOrDefault(key(),0)));scroll.scrollTo(0,0);if(!rows.isEmpty()){describe(shown.get(focused));if(!railFocus)rows.get(focused).requestFocusFromTouch();}rendering=false;
    }
    private static final class SearchHit {final Item item;final List<Integer> parent;final int index;final String trail;SearchHit(Item i,List<Integer> p,int n,String t){item=i;parent=p;index=n;trail=t;}}
    private final List<SearchHit> hits=new ArrayList<>();
    private void collect(Item node,List<Integer> parent,String trail,List<SearchHit> all){for(int i=0;i<node.children.length;i++){Item child=node.children[i];all.add(new SearchHit(child,new ArrayList<>(parent),i,trail));List<Integer> next=new ArrayList<>(parent);next.add(i);collect(child,next,trail.isEmpty()?child.title:trail+" › "+child.title,all);}}
    private void renderSearch(){
        searchField=new EditText(a);searchField.setSingleLine(true);searchField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});searchField.setTextSize(18);searchField.setTypeface(font);searchField.setTextColor(0xfff5f7fa);searchField.setHintTextColor(0xffb8c8db);searchField.setHint("Try HDMI, font, wallpaper or profile");searchField.setInputType(android.text.InputType.TYPE_CLASS_TEXT);searchField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);searchField.setText(query);menu.addView(searchField,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout results=new LinearLayout(a);results.setOrientation(LinearLayout.VERTICAL);menu.addView(results);fillResults(results);searchField.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int after){}public void onTextChanged(CharSequence s,int st,int before,int c){query=s.toString();fillResults(results);}public void afterTextChanged(Editable e){}});
        searchField.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){((android.view.inputmethod.InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchField.getWindowToken(),0);if(!rows.isEmpty())rows.get(0).requestFocus();return true;}return false;});
        searchField.setOnFocusChangeListener((v,has)->{if(has)railFocus=false;});context.setText("Find a setting");help.setText("Find a control by name or try words such as HDMI, font or spacing. Choosing a result opens its page without changing the setting.");previewPanel.setVisibility(View.GONE);inspectorValue.setVisibility(View.GONE);behavior.setText("Down: search results\nOK: open a result\nBack: categories");scope.setText("Profile: "+ProfileAvatars.label(a));
    }
    private void fillResults(LinearLayout results){
        results.removeAllViews();rows.clear();shown.clear();hits.clear();List<SearchHit> all=new ArrayList<>();collect(root,new ArrayList<>(),"",all);
        if(!query.trim().isEmpty())for(SearchHit hit:all)if(SettingsSearch.matches(query,hit.item.title,hit.item.help,hit.trail)){hits.add(hit);if(hits.size()==24)break;}
        for(int i=0;i<hits.size();i++){SearchHit hit=hits.get(i);View row=makeRow(hit.item,i,hit.trail);results.addView(row,new LinearLayout.LayoutParams(-1,-2));rows.add(row);shown.add(hit.item);}
        if(hits.isEmpty()){TextView empty=text(query.trim().isEmpty()?"Type a word to find a launcher setting.":"No settings found. Try a shorter word, such as apps or text.",16,0xffc4d0df);empty.setPadding(dp(12),dp(18),dp(12),0);results.addView(empty);}focused=0;
    }
    boolean key(KeyEvent event){
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;int code=event.getKeyCode();View focus=a.getCurrentFocus();
        if(railRows.contains(focus)){if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN){int next=Math.max(0,Math.min(railRows.size()-1,railRows.indexOf(focus)+(code==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));railRows.get(next).requestFocus();return true;}if(code==(rtl?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT)){railFocus=false;if(searching&&searchField!=null)searchField.requestFocus();else if(!rows.isEmpty())rows.get(Math.min(focused,rows.size()-1)).requestFocus();return true;}return false;}
        if(focus==searchField){if(code==KeyEvent.KEYCODE_DPAD_DOWN&&!rows.isEmpty()){rows.get(0).requestFocus();return true;}return false;}
        if(!rows.contains(focus))return false;
        if(code==KeyEvent.KEYCODE_INFO){showHelp(shown.get(focused));return true;}
        if(!searching&&segmented(shown.get(focused))&&(code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT)){
            Item e=shown.get(focused);int delta=(code==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)*(rtl?-1:1);draftRevisions.putIfAbsent(e,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0));drafts.put(e,Math.max(0,Math.min(options(e).length-1,draft(e)+delta)));render();return true;
        }
        if(code==(rtl?KeyEvent.KEYCODE_DPAD_RIGHT:KeyEvent.KEYCODE_DPAD_LEFT)){backToRailOrParent();return true;}
        if(code==(rtl?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT)){if(searching||!toggle(shown.get(focused)))rows.get(focused).performClick();return true;}
        if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN){if(code==KeyEvent.KEYCODE_DPAD_UP&&focused==0&&searching&&searchField!=null)searchField.requestFocus();else rows.get(Math.max(0,Math.min(rows.size()-1,focused+(code==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)))).requestFocus();return true;}return false;
    }
    private void backToRailOrParent(){drafts.clear();draftRevisions.clear();if(!searching&&path.size()>1){memory.put(key(),focused);path.remove(path.size()-1);render();}else{railFocus=true;railRows.get(searching?0:path.get(0)+1).requestFocus();}}
    boolean back(){if(railRows.contains(a.getCurrentFocus()))return false;backToRailOrParent();return true;}
    void save(Bundle out){out.putInt("studio_version",12);out.putString("studio_path",key());out.putString("studio_query",query);out.putBoolean("studio_search",searching);out.putStringArray("studio_memory_keys",memory.keySet().toArray(new String[0]));int[] values=new int[memory.size()];int n=0;for(Integer value:memory.values())values[n++]=value;out.putIntArray("studio_memory_values",values);}
}
