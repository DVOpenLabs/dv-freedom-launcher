package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.tv.*;
import android.os.*;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/** Optional named-row renderer in the same installed product. The legacy Home stays unchanged when off. */
public final class CategoryHomeActivity extends Activity {
    static final String PREVIEW="category_preview";
    static final String IDLE_PREVIEW="category_idle_preview";
    private FrameLayout root;private LinearLayout body,header;private ScrollView scroll;
    private WallpaperPlayer wallpaper;private LiveVideo video;private Config config;
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final PolishPolicy.Idle idle=new PolishPolicy.Idle();private int idleSeconds;private boolean touchWake;
    private final List<View> quietChrome=new ArrayList<>();private final Map<View,Float> faded=new HashMap<>();
    private TvButtonController tvButton;
    private Map<Integer,String> appKeys=Collections.emptyMap();
    private final List<RowView> rows=new ArrayList<>();private final Map<String,String> selections=new HashMap<>();
    private List<HomeCategories.App> catalog=Collections.emptyList();private String lastRow="favorites",rendered="";private boolean preview,resumed;
    private Typeface typeface;private int ink;private TextView date,settings;
    private final Runnable idleTimeout=()->{if(!resumed||!hasWindowFocus())return;android.view.accessibility.AccessibilityManager access=getSystemService(android.view.accessibility.AccessibilityManager.class);if(access!=null&&access.isTouchExplorationEnabled()){armIdle();return;}if(idle.due(SystemClock.elapsedRealtime()))fadeChrome();};
    private final Runnable tick=new Runnable(){public void run(){if(!resumed)return;if(date!=null)date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()));if(wallpaper!=null)wallpaper.update();timer.postDelayed(this,60000);}};
    static void openIfEnabled(MainActivity activity,Config config){
        if(HomeCategories.enabled(activity)&&!activity.isFinishing())activity.startActivity(new Intent(activity,CategoryHomeActivity.class));
    }
    @Override protected void attachBaseContext(Context context){super.attachBaseContext(LauncherLocale.wrap(context));}
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);ProductFeatures.init(this);preview=getIntent().getBooleanExtra(PREVIEW,false);
        if(saved!=null){lastRow=saved.getString("row","favorites");String[] ids=saved.getStringArray("row_ids"),apps=saved.getStringArray("row_apps");if(ids!=null&&apps!=null&&ids.length==apps.length&&ids.length<=9)for(int i=0;i<ids.length;i++)selections.put(ids[i],apps[i]);}
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,float size){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(ink);view.setTypeface(typeface);view.setShadowLayer(ink==0xfff5f7fa?dp(1):0,0,1,0xc0000000);return view;}
    private TextView button(String label,Runnable action){TextView view=text(label,16);view.setGravity(Gravity.CENTER);view.setPadding(dp(18),dp(10),dp(18),dp(10));view.setTextColor(Color.WHITE);view.setBackgroundResource(R.drawable.settings_choice_bg);view.setFocusable(true);view.setClickable(true);view.setOnClickListener(v->action.run());view.setOnKeyListener((v,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_DOWN&&!rows.isEmpty()){rows.get(0).focus(selections.get(rows.get(0).id),0);return true;}return event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_UP;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.setMarginStart(dp(8));header.addView(view,p);quietChrome.add(view);return view;}
    private void release(){if(wallpaper!=null){wallpaper.stop();wallpaper.close();wallpaper=null;}if(video!=null){video.close();video=null;}}
    private void render(){
        config=Config.load(this);HomeCategories.State state=HomeCategories.read(this);if(!preview&&!state.enabled){finish();return;}
        timer.removeCallbacks(idleTimeout);idle.stop();restoreChrome(false);quietChrome.clear();touchWake=false;
        tvButton=config.tvButtonPicker?new TvButtonController(this,config):null;
        appKeys=RemoteKeys.parseAppKeys(config.appKeys);
        idleSeconds=ProfileStore.preferences(this).getInt(HomePolish.IDLE,0);if(!PolishPolicy.validSeconds(idleSeconds))idleSeconds=0;
        release();root.removeAllViews();rows.clear();root.setBackgroundColor(config.backgroundColor);
        View probe=getLayoutInflater().inflate(R.layout.item_app,root,false);typeface=((TextView)probe.findViewById(R.id.name)).getTypeface();
        ink=ProfileStore.preferences(this).getBoolean(HomePolish.HEADER,false)?PolishPolicy.textInk(ProfileStore.preferences(this).getInt(HomePolish.INK,0),config.backgroundColor):0xfff5f7fa;
        wallpaper=new WallpaperPlayer(this,root,config);
        ThemeLibrary.Entry theme=ThemeLibrary.active();
        if(config.wallpaperEnabled&&theme!=null&&ThemeLibrary.liveAllowed()&&ProfileStore.preferences(this).getBoolean(ThemeLibrary.LIVE,false)&&LiveVideo.supported(this,config.reducedMotion)){
            File file=new File(theme.directory,"video.mp4");if(file.isFile()){video=new LiveVideo(this,file,config.wallpaperDim);root.addView(video.view(),Math.min(1,root.getChildCount()),new FrameLayout.LayoutParams(-1,-1));}
        }
        View scrim=new View(this);scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x90000000,0x00000000,0x70000000}));root.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(40),dp(22),dp(40),dp(12));content.setLayoutDirection(getResources().getBoolean(R.bool.rtl_menu_bar)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);root.addView(content,new FrameLayout.LayoutParams(-1,-1));
        header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);content.addView(header,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout clockBox=new LinearLayout(this);clockBox.setId(R.id.leftBlock);clockBox.setOrientation(LinearLayout.VERTICAL);header.addView(clockBox,new LinearLayout.LayoutParams(0,-2,1));
        TextClock clock=new TextClock(this);clock.setId(R.id.clock);clock.setFormat12Hour("h:mm a");clock.setFormat24Hour("HH:mm");clock.setTextSize(36);clock.setTypeface(typeface);clock.setTextColor(ink);clock.setShadowLayer(dp(1),0,1,0xc0000000);clockBox.addView(clock);
        date=text("",15);date.setId(R.id.date);clockBox.addView(date);
        if(config.showInputsPill)button(getString(R.string.inputs),this::inputs).setId(R.id.btnInputs);
        button("Apps",this::allApps);settings=button("Settings",()->startActivity(new Intent(this,LauncherSettingsActivity.class).putExtra("category_context",true)));settings.setId(R.id.btnSettings);
        ProductFeatures.categoryHeader(this,clockBox,config);
        if(preview){LinearLayout notice=new LinearLayout(this);notice.setGravity(Gravity.CENTER_VERTICAL);TextView label=text("Category preview · Your saved Home is unchanged",14);notice.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView exit=text("Back to saved Home",14);exit.setPadding(dp(16),dp(8),dp(16),dp(8));exit.setBackgroundResource(R.drawable.settings_choice_bg);exit.setFocusable(true);exit.setClickable(true);exit.setOnClickListener(v->finish());notice.addView(exit);content.addView(notice,new LinearLayout.LayoutParams(-1,-2));quietChrome.add(notice);}
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setFocusable(false);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));quietChrome.add(scroll);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setClipChildren(false);body.setClipToPadding(false);body.setPadding(0,dp(18),0,dp(18));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        catalog=HomeCategories.catalog(this,config);Map<String,HomeCategories.App> byPackage=new HashMap<>();for(HomeCategories.App app:catalog)byPackage.put(app.pkg,app);
        addRow("favorites","Favorites",DesktopLayout.favorites(config,Order.load(Order.file(this))),byPackage);
        for(HomeCategories.Row row:state.rows)if(row.visible)addRow(row.id,row.name,row.apps,byPackage);
        if(rows.isEmpty()){TextView empty=text("No apps are available in these rows. Open Settings → Apps & rows → Categories to choose apps, or return to your existing layout.",18);empty.setPadding(0,dp(24),0,dp(24));body.addView(empty);settings.requestFocus();}
        else if(preview&&state.rows.isEmpty()){TextView hint=text("Create named rows in Settings → Apps & rows → Categories. Favorites above are your existing apps.",16);hint.setPadding(0,dp(12),0,dp(12));body.addView(hint);}
        rendered=ProfileStore.id()+"\n"+ProfileStore.preferences(this).getString(HomeCategories.KEY,"");
        if(wallpaper!=null)wallpaper.start();if(video!=null)video.start();timer.removeCallbacks(tick);timer.post(tick);
        body.post(()->{RowView desired=null;for(RowView row:rows)if(row.id.equals(lastRow))desired=row;if(desired==null&&!rows.isEmpty())desired=rows.get(0);if(desired!=null)desired.focus(selections.get(desired.id),0);armIdle();if(getIntent().getBooleanExtra(IDLE_PREVIEW,false)){getIntent().removeExtra(IDLE_PREVIEW);timer.removeCallbacks(idleTimeout);idle.quiet=true;fadeChrome();}});
    }
    private void addRow(String id,String label,List<String> packages,Map<String,HomeCategories.App> catalog){
        List<HomeCategories.App> apps=new ArrayList<>();for(String pkg:packages){HomeCategories.App app=catalog.get(pkg);if(app!=null)apps.add(app);}if(apps.isEmpty())return;
        TextView title=text(label,19);LinearLayout.LayoutParams titleLayout=new LinearLayout.LayoutParams(-1,-2);titleLayout.topMargin=dp(12);titleLayout.bottomMargin=dp(4);body.addView(title,titleLayout);
        RowView row=new RowView(id,apps);rows.add(row);body.addView(row.list,new LinearLayout.LayoutParams(-1,row.height));
    }
    private final class RowView {
        final String id;final List<HomeCategories.App> apps;final RecyclerView list;final LinearLayoutManager layout;final int cell,width,height;
        RowView(String id,List<HomeCategories.App> apps){
            this.id=id;this.apps=apps;int available=getResources().getDisplayMetrics().widthPixels-dp(80);int columns=Math.max(2,Math.min(8,config.columns));cell=available/columns;
            width=TileSpacing.width(AppAdapter.tileWidthPx(available,columns,getResources().getDisplayMetrics().density),config.tileGap,getResources().getDisplayMetrics().density);height=AppAdapter.tileHeightPx(width)+dp(58);
            list=new RecyclerView(CategoryHomeActivity.this);list.setLayoutDirection(getResources().getBoolean(R.bool.rtl_app_grid)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);list.setClipToPadding(false);list.setClipChildren(false);list.setItemAnimator(null);list.setPadding(0,dp(6),0,dp(6));list.setNestedScrollingEnabled(false);list.setItemViewCacheSize(2);
            layout=new LinearLayoutManager(CategoryHomeActivity.this,RecyclerView.HORIZONTAL,false);list.setLayoutManager(layout);list.setAdapter(new Tiles(this));
        }
        void focus(String pkg,int fallback){int at=-1;if(pkg!=null)for(int i=0;i<apps.size();i++)if(apps.get(i).pkg.equals(pkg))at=i;focus(at<0?Math.max(0,Math.min(apps.size()-1,fallback)):at);}
        void focus(int at){if(at<0||at>=apps.size())return;RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(at);if(holder==null)layout.scrollToPosition(at);focusReady(at,0);}
        private void focusReady(int at,int attempt){if(!resumed)return;RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(at);if(holder!=null){View card=holder.itemView.findViewById(R.id.card);if(card.isInTouchMode())card.requestFocusFromTouch();else card.requestFocus();}else if(attempt<16)list.postOnAnimation(()->focusReady(at,attempt+1));}
        boolean key(int at,int code){
            if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN){int index=rows.indexOf(this),next=index+(code==KeyEvent.KEYCODE_DPAD_UP?-1:1);if(next<0)settings.requestFocus();else if(next<rows.size())rows.get(next).focus(selections.get(rows.get(next).id),at);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT){boolean rtl=getResources().getBoolean(R.bool.rtl_app_grid);int next=at+((code==KeyEvent.KEYCODE_DPAD_RIGHT)^rtl?1:-1);if(next>=0&&next<apps.size())focus(next);return true;}return false;
        }
    }
    private final class Tiles extends RecyclerView.Adapter<Tile> {
        final RowView row;Tiles(RowView row){this.row=row;setHasStableIds(true);}
        @Override public int getItemCount(){return row.apps.size();}
        @Override public long getItemId(int at){long hash=0xcbf29ce484222325L;String key=row.id+"\n"+row.apps.get(at).component.flattenToString();for(int i=0;i<key.length();i++){hash^=key.charAt(i);hash*=0x100000001b3L;}return hash;}
        @Override public Tile onCreateViewHolder(ViewGroup parent,int type){
            View view=getLayoutInflater().inflate(R.layout.item_app,parent,false);view.setLayoutParams(new RecyclerView.LayoutParams(row.cell,row.height));
            View card=view.findViewById(R.id.card);ViewGroup.LayoutParams p=card.getLayoutParams();p.width=row.width;p.height=AppAdapter.tileHeightPx(row.width);card.setLayoutParams(p);card.setClipToOutline(true);
            card.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,Outline outline){outline.setRoundRect(0,0,v.getWidth(),v.getHeight(),AppAdapter.cornerRadius(row.width,getResources().getDisplayMetrics().density));}});
            TextView name=view.findViewById(R.id.name);FrameLayout.LayoutParams names=(FrameLayout.LayoutParams)name.getLayoutParams();names.width=row.width;names.topMargin=p.height+dp(11);names.height=dp(28);name.setLayoutParams(names);name.setTranslationY(0);name.setTextColor(ink);name.setShadowLayer(ink==0xfff5f7fa?dp(1):0,0,1,0xc0000000);ProductFeatures.styleName(name);
            return new Tile(view,row);
        }
        @Override public void onBindViewHolder(Tile holder,int at){
            HomeCategories.App app=row.apps.get(at);holder.app=app;holder.name.setText(app.label);holder.card.setContentDescription(app.label);holder.card.setOnClickListener(v->{lastRow=row.id;selections.put(row.id,app.pkg);app.launch(CategoryHomeActivity.this,config);});
            Drawable art=app.artwork(CategoryHomeActivity.this,config);holder.banner.setImageDrawable(art);boolean wide=art!=null&&art.getIntrinsicWidth()>art.getIntrinsicHeight()*1.3f;holder.banner.setScaleType(wide?ImageView.ScaleType.CENTER_CROP:ImageView.ScaleType.CENTER_INSIDE);holder.banner.setPadding(wide?0:dp(14),wide?0:dp(10),wide?0:dp(14),wide?0:dp(10));holder.paint(holder.card.isFocused());
        }
        @Override public void onViewRecycled(Tile holder){holder.banner.setImageDrawable(null);holder.app=null;}
    }
    private final class Tile extends RecyclerView.ViewHolder {
        final View card;final TextView name;final ImageView banner;final RowView row;final GlassView focusFrame;HomeCategories.App app;
        Tile(View view,RowView row){
            super(view);this.row=row;card=view.findViewById(R.id.card);name=view.findViewById(R.id.name);banner=view.findViewById(R.id.banner);view.findViewById(R.id.icon).setVisibility(View.GONE);view.findViewById(R.id.label).setVisibility(View.GONE);
            focusFrame=new GlassView(CategoryHomeActivity.this,null);focusFrame.setStyle(config.glassStyle);focusFrame.setRadius(AppAdapter.cornerRadius(row.width,getResources().getDisplayMetrics().density));focusFrame.setFocusable(false);focusFrame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);focusFrame.setVisibility(View.INVISIBLE);focusFrame.setLayerType(View.LAYER_TYPE_NONE,null);
            FrameLayout.LayoutParams frame=new FrameLayout.LayoutParams(row.width+dp(12),AppAdapter.tileHeightPx(row.width)+dp(12),Gravity.CENTER_HORIZONTAL|Gravity.TOP);frame.topMargin=-dp(6);((FrameLayout)view).addView(focusFrame,frame);
            card.setOnFocusChangeListener((v,on)->{paint(on);if(on&&app!=null){lastRow=row.id;selections.put(row.id,app.pkg);scroll.requestChildFocus(body,row.list);}});
            card.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;int at=getBindingAdapterPosition();return at!=RecyclerView.NO_POSITION&&row.key(at,key);});
        }
        void paint(boolean focused){
            card.animate().cancel();focusFrame.animate().cancel();boolean refined=ProfileStore.preferences(CategoryHomeActivity.this).getBoolean(HomePolish.MOTION,false);float scale=focused&&config.animations?AppAdapter.FOCUS_SCALE:1;
            long duration=!config.animations?0:config.reducedMotion?(focused?Motion.REDUCED_MS:60):refined?(focused?170:130):(focused?260:180);
            android.animation.TimeInterpolator motion=config.reducedMotion||refined||focused&&!config.focusOvershoot?new android.view.animation.DecelerateInterpolator(1.5f):new android.view.animation.OvershootInterpolator(focused?1.6f:0);
            if(duration==0){card.setScaleX(scale);card.setScaleY(scale);}else card.animate().scaleX(scale).scaleY(scale).setDuration(duration).setInterpolator(motion).start();
            focusFrame.setVisibility(focused?View.VISIBLE:View.INVISIBLE);focusFrame.setLayerType(focused?View.LAYER_TYPE_HARDWARE:View.LAYER_TYPE_NONE,null);
            if(focused&&duration>0)focusFrame.animate().scaleX(scale).scaleY(scale).setDuration(duration).setInterpolator(motion).start();else{focusFrame.setScaleX(scale);focusFrame.setScaleY(scale);}
            int mode=ProfileStore.preferences(CategoryHomeActivity.this).getInt(HomePolish.NAMES,0);name.setVisibility(mode==3?View.INVISIBLE:View.VISIBLE);name.setAlpha(focused?1f:PolishPolicy.nameAlpha(mode,config.alwaysShowNames?1f:0f));
        }
    }
    private void allApps(){String[] labels=new String[catalog.size()];for(int i=0;i<labels.length;i++)labels[i]=catalog.get(i).label;new AlertDialog.Builder(this).setTitle("Apps").setItems(labels,(d,which)->catalog.get(which).launch(this,config)).setNegativeButton(android.R.string.cancel,null).show();}
    private void inputs(){inputs(false);}
    private void inputs(boolean showAll){
        List<TvInputInfo> available=new ArrayList<>();List<String> labels=new ArrayList<>();TvInputManager manager=getSystemService(TvInputManager.class);int hidden=0;
        if(manager!=null)try{
            List<TvInputInfo> inputs=new ArrayList<>(manager.getTvInputList());Set<String> parents=new HashSet<>();for(TvInputInfo info:inputs)if(info.getParentId()!=null)parents.add(info.getParentId());
            inputs.sort((a,b)->String.valueOf(a.loadLabel(this)).compareToIgnoreCase(String.valueOf(b.loadLabel(this))));
            for(TvInputInfo info:inputs)if(info.isPassthroughInput()&&!parents.contains(info.getId())&&available.size()<32){
                boolean bare=info.getType()==TvInputInfo.TYPE_HDMI&&info.getParentId()==null;if(bare&&!HdmiVisibility.visible(!showAll&&config.hideInactiveHdmi,manager.getInputState(info.getId()))){hidden++;continue;}
                Intent intent=new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(info.getId()));if(getPackageManager().resolveActivity(intent,0)==null)continue;
                available.add(info);CharSequence custom=info.loadCustomLabel(this);labels.add(String.valueOf(custom==null?info.loadLabel(this):custom));
            }
        }catch(RuntimeException ignored){}
        if(available.isEmpty()&&hidden==0){InputShortcut.help(this);return;}
        if(hidden>0)labels.add("Show all inputs");
        new AlertDialog.Builder(this).setTitle(R.string.inputs).setItems(labels.toArray(new String[0]),(d,which)->{if(which==available.size()){inputs(true);return;}try{startActivity(new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(available.get(which).getId())));}catch(Exception e){Toast.makeText(this,R.string.not_available,Toast.LENGTH_LONG).show();}}).setNegativeButton(android.R.string.cancel,null).show();
    }
    private void armIdle(){timer.removeCallbacks(idleTimeout);if(!resumed||idle.quiet)return;idle.arm(SystemClock.elapsedRealtime(),idleSeconds);if(idleSeconds>0)timer.postDelayed(idleTimeout,idleSeconds*1000L);}
    private void fadeChrome(){faded.clear();for(View view:quietChrome){faded.put(view,view.getAlpha());view.animate().cancel();view.animate().alpha(0).setDuration(config.animations&&!config.reducedMotion?180:0).start();}}
    private void restoreChrome(boolean animate){for(Map.Entry<View,Float> item:faded.entrySet()){View view=item.getKey();view.animate().cancel();if(animate)view.animate().alpha(item.getValue()).setDuration(config!=null&&config.animations&&!config.reducedMotion?180:0).start();else view.setAlpha(item.getValue());}faded.clear();}
    private boolean navigation(int code){return code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_BUTTON_A;}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN||event.getAction()==KeyEvent.ACTION_UP){boolean was=idle.quiet;boolean consume=idle.key(event.getKeyCode(),event.getAction()==KeyEvent.ACTION_DOWN,navigation(event.getKeyCode()));if(was&&!idle.quiet)restoreChrome(true);if(event.getAction()==KeyEvent.ACTION_DOWN)armIdle();if(consume)return true;}
        if(tvButton!=null&&tvButton.handle(event))return true;
        if(event.getKeyCode()>=1000&&RemoteKeys.handle(this,event,appKeys.get(event.getKeyCode()),null))return true;
        if(config!=null)for(int code:config.settingsKeycodes)if(code==event.getKeyCode()){
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())try{startActivity(config.settingsKeyPanel?new Intent(this,QuickSettingsActivity.class).putExtra("category_context",true):SystemSettings.intent(this,Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,R.string.not_available,Toast.LENGTH_LONG).show();}return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
        int action=event.getActionMasked();if(action==android.view.MotionEvent.ACTION_DOWN&&idle.quiet){touchWake=true;idle.quiet=false;restoreChrome(true);armIdle();}
        if(touchWake){if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL)touchWake=false;return true;}
        if(action==android.view.MotionEvent.ACTION_DOWN)armIdle();return super.dispatchTouchEvent(event);
    }
    @Override protected void onResume(){super.onResume();resumed=true;render();}
    @Override protected void onPause(){resumed=false;timer.removeCallbacks(tick);timer.removeCallbacks(idleTimeout);idle.stop();touchWake=false;restoreChrome(false);if(wallpaper!=null)wallpaper.stop();if(video!=null)video.stop();super.onPause();}
    @Override protected void onDestroy(){release();timer.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)timer.removeCallbacks(idleTimeout);else if(resumed){armIdle();String current=ProfileStore.id()+"\n"+ProfileStore.preferences(this).getString(HomeCategories.KEY,"");if(!current.equals(rendered))root.post(()->{if(resumed&&hasWindowFocus())render();});}}
    @Override public boolean onKeyDown(int code,KeyEvent event){if(code==KeyEvent.KEYCODE_TV_INPUT){if(event.getRepeatCount()==0)inputs();return true;}return super.onKeyDown(code,event);}
    @Override public void onBackPressed(){if(preview)finish();else if(!rows.isEmpty())rows.get(0).focus(0);}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("row",lastRow);state.putStringArray("row_ids",selections.keySet().toArray(new String[0]));state.putStringArray("row_apps",selections.values().toArray(new String[0]));super.onSaveInstanceState(state);}
}
