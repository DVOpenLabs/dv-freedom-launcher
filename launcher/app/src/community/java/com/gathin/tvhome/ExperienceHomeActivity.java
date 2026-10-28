package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.tv.*;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/** Three native presentations using the same profile/app/media stores and existing system routes. */
public final class ExperienceHomeActivity extends Activity {
    private static final java.lang.ref.WeakReference<ExperienceHomeActivity> EMPTY=new java.lang.ref.WeakReference<>(null);
    private static java.lang.ref.WeakReference<ExperienceHomeActivity> current=EMPTY;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root,chrome;private LinearLayout info,controls;private RecyclerView list;private LinearLayoutManager listLayout;
    private TextView clock,date,hero,settings,trialRevert;private View topShade,bottomShade,dock,labelShade;private LinearLayout trialBar;
    private ContentShelves.Row shelf;private WallpaperPlayer wallpaper;private LiveVideo video;private Config config;private Typeface font;
    private List<HomeCategories.App> catalog=Collections.emptyList(),favorites=Collections.emptyList();
    private String look,signature,preview,selectedPackage="";private float unit=1;private int selected,ink=0xfff5f7fa,labelInk=0xfff5f7fa;private boolean resumed,quiet;
    private TvButtonController tvButton;private Map<Integer,String> appKeys=Collections.emptyMap();
    private ExperiencePolicy.Geometry geometry;private final List<View> quietViews=new ArrayList<>();
    private final PolishPolicy.Idle idle=new PolishPolicy.Idle();private boolean touchWake;private int idleSeconds;
    private final Runnable idleTimeout=()->{if(resumed&&hasWindowFocus()&&idle.due(SystemClock.elapsedRealtime()))setQuiet(true);};
    private final Runnable tick=new Runnable(){public void run(){if(!resumed)return;updateClock();if(wallpaper!=null)wallpaper.update();handler.postDelayed(this,60000);}};
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LauncherLocale.wrap(c));}
    @Override protected void onCreate(Bundle saved){super.onCreate(saved);ProductFeatures.init(this);root=new FrameLayout(this);root.setId(R.id.root);root.setBackgroundColor(0xff101820);setContentView(root);if(saved!=null)selectedPackage=saved.getString("selected","");preview=getIntent().getStringExtra(ExperienceChoice.EXTRA);current=new java.lang.ref.WeakReference<>(this);}
    private int px(float n){return Math.round(n*unit);}
    private TextView text(String label,float size){TextView v=new TextView(this);v.setText(label);v.setTypeface(font);v.setTextSize(TypedValue.COMPLEX_UNIT_PX,px(size)*getResources().getConfiguration().fontScale);v.setTextColor(ink);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);return v;}
    private void put(View v,float x,float y,float w,float h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w<0?-1:px(w),h<0?-2:px(h));p.leftMargin=px(x);p.topMargin=px(y);chrome.addView(v,p);}
    private GradientDrawable round(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(px(radius));return d;}
    private void focus(View v,boolean on){v.animate().cancel();v.setBackground(round(on?0xffe5f3ff:0xb31b2a3c,19));if(v instanceof TextView)((TextView)v).setTextColor(on?0xff182334:0xfff5f7fa);}
    private TextView button(String label,int id,Runnable action){TextView v=text(label,14);if(id!=0)v.setId(id);v.setGravity(Gravity.CENTER);v.setPadding(px(13),0,px(13),0);v.setFocusable(true);v.setClickable(true);focus(v,false);v.setOnFocusChangeListener((x,on)->focus(x,on));v.setOnClickListener(x->action.run());v.setOnKeyListener((x,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_DOWN){focusApp(selected);return true;}return false;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,px(38));p.setMarginStart(px(8));controls.addView(v,p);return v;}
    private String currentSignature(){return ProfileStore.id()+"|"+ProfileStore.preferences(this).getAll().hashCode()+"|"+getResources().getDisplayMetrics().widthPixels;}
    private void release(){if(wallpaper!=null){wallpaper.close();wallpaper=null;}if(video!=null){video.close();video=null;}}
    private void render(){
        config=Config.load(this);look=preview==null?ExperienceChoice.saved(this):preview;
        if(ExperiencePolicy.LEGACY.equals(look)||preview!=null&&!ExperienceChoice.trialValid(preview)){finish();return;}
        if(shelf!=null){shelf.close();shelf=null;}unit=getResources().getDisplayMetrics().widthPixels/960f;release();handler.removeCallbacks(idleTimeout);idle.stop();quiet=false;quietViews.clear();root.removeAllViews();root.setBackgroundColor(config.backgroundColor);
        View template=getLayoutInflater().inflate(R.layout.item_app,root,false);font=((TextView)template.findViewById(R.id.name)).getTypeface();
        wallpaper=new WallpaperPlayer(this,root,config);
        ThemeLibrary.Entry theme=ThemeLibrary.active();if(!ProductFeatures.playlistEnabled(this)&&config.wallpaperEnabled&&theme!=null&&ThemeLibrary.liveAllowed()&&ProfileStore.preferences(this).getBoolean(ThemeLibrary.LIVE,false)&&LiveVideo.supported(this,config.reducedMotion)){
            File file=new File(theme.directory,"video.mp4");if(file.isFile()){video=new LiveVideo(this,file,config.wallpaperDim);root.addView(video.view(),Math.min(1,root.getChildCount()),new FrameLayout.LayoutParams(-1,-1));}
        }
        chrome=new FrameLayout(this);chrome.setClipChildren(false);root.addView(chrome,new FrameLayout.LayoutParams(-1,-1));
        topShade=new View(this);topShade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xb3081020,0x00081020}));put(topShade,0,0,960,175);
        bottomShade=new View(this);bottomShade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00081020,0xb3081020}));put(bottomShade,0,350,960,190);
        info=new LinearLayout(this);info.setId(R.id.leftBlock);info.setOrientation(LinearLayout.VERTICAL);clock=text("",ExperiencePolicy.CLEAN.equals(look)?44:34);clock.setId(R.id.clock);date=text("",13);info.addView(clock);info.addView(date);put(info,40,27,390,98);
        controls=new LinearLayout(this);controls.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);put(controls,450,30,470,42);
        button("Inputs",R.id.btnInputs,this::inputs);button("Apps",0,this::allApps);settings=button("Launcher settings",R.id.btnSettings,()->{if(canEdit())startActivity(new Intent(this,LauncherSettingsActivity.class).putExtra("experience_context",true));});button("Profiles",0,()->{if(canEdit())ProfilePicker.show(this,false);});
        ProductFeatures.categoryHeader(this,info,config);
        catalog=HomeCategories.catalog(this,config);Map<String,HomeCategories.App> by=new LinkedHashMap<>();for(HomeCategories.App app:catalog)by.put(app.pkg,app);List<HomeCategories.App> apps=new ArrayList<>();
        for(String pkg:DesktopLayout.favorites(config,Order.load(Order.file(this)))){HomeCategories.App app=by.get(pkg);if(app!=null)apps.add(app);}
        if(apps.isEmpty())apps.addAll(catalog.subList(0,Math.min(8,catalog.size())));favorites=apps;
        String position=PlacementSession.selected(this);if(!AppPlacement.valid(position))position=ExperiencePolicy.defaultPosition(look);
        geometry=new ExperiencePolicy.Geometry(look,position,Math.max(1,Math.min(Math.min(8,config.columns),favorites.size())),config.tileGap);
        labelShade=new View(this);labelShade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);put(labelShade,0,geometry.labelTop-28,960,80);quietViews.add(labelShade);
        if(!ExperiencePolicy.CLEAN.equals(look)){dock=new View(this);dock.setBackground(round(ExperiencePolicy.CINEMA.equals(look)?0xa61b2a3c:0xb34a6078,ExperiencePolicy.CINEMA.equals(look)?16:20));put(dock,geometry.left-14,geometry.top-16,960-2*geometry.left+28,geometry.cardHeight+(ExperiencePolicy.CINEMA.equals(look)?34:55));quietViews.add(dock);}
        list=new RecyclerView(this);list.setId(R.id.grid);list.setClipChildren(false);list.setClipToPadding(false);list.setItemAnimator(null);list.setOverScrollMode(View.OVER_SCROLL_NEVER);list.setItemViewCacheSize(2);list.setPadding(px(7),px(7),px(7),0);
        list.setLayoutDirection(getResources().getBoolean(R.bool.rtl_app_grid)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);listLayout=new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false);list.setLayoutManager(listLayout);list.setAdapter(new Tiles());
        put(list,geometry.left-7,geometry.top-7,960-2*geometry.left+14,geometry.cardHeight+44);quietViews.add(list);quietViews.add(controls);
        hero=text("",32);hero.setShadowLayer(px(2),0,px(1),0xcc081020);if(ExperiencePolicy.CINEMA.equals(look)){float y="middle".equals(position)?150:"top".equals(position)?365:geometry.top-72;put(hero,46,y,820,50);quietViews.add(hero);}
        if(preview==null&&ContentShelves.enabled(this)){LinearLayout shelfHost=new LinearLayout(this);shelfHost.setOrientation(LinearLayout.VERTICAL);shelfHost.setPadding(px(12),px(8),px(12),px(6));shelfHost.setBackground(round(0xde111b2a,14));put(shelfHost,46,geometry.top<260?geometry.top+geometry.cardHeight+60:142,868,154);quietViews.add(shelfHost);shelf=new ContentShelves.Row(this,shelfHost,list,false);}
        selected=0;for(int i=0;i<favorites.size();i++)if(favorites.get(i).pkg.equals(selectedPackage))selected=i;updateSelection();
        idleSeconds=ProfileStore.preferences(this).getInt(HomePolish.IDLE,0);tvButton=config.tvButtonPicker?new TvButtonController(this,config):null;appKeys=RemoteKeys.parseAppKeys(config.appKeys);signature=currentSignature();
        applyInk(new int[]{config.backgroundColor},new int[]{config.backgroundColor});if(wallpaper!=null){wallpaper.headerSample(this::sample);wallpaper.start();}if(video!=null)video.start();ProductFeatures.resumed(this);handler.removeCallbacks(tick);handler.post(tick);list.post(()->focusApp(selected));armIdle();
    }
    void shelfReturn(){focusApp(selected);}
    void shelfReady(boolean shown){if(hero!=null&&ExperiencePolicy.CINEMA.equals(look)&&geometry.top<200)hero.setVisibility(shown?View.GONE:View.VISIBLE);}
    private void updateClock(){if(clock!=null)clock.setText(DateFormat.getTimeFormat(this).format(new Date()));if(date!=null)date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()));}
    private void updateSelection(){if(!favorites.isEmpty()){selected=Math.max(0,Math.min(favorites.size()-1,selected));selectedPackage=favorites.get(selected).pkg;if(hero!=null)hero.setText(favorites.get(selected).label);}}
    private void focusApp(int index){if(favorites.isEmpty()){settings.requestFocus();return;}selected=Math.max(0,Math.min(favorites.size()-1,index));updateSelection();if(list.findViewHolderForAdapterPosition(selected)==null)listLayout.scrollToPosition(selected);focusReady(selected,0);}
    private void focusReady(int at,int attempt){if(!resumed)return;RecyclerView.ViewHolder h=list.findViewHolderForAdapterPosition(at);if(h!=null)h.itemView.requestFocus();else if(attempt<16)list.postOnAnimation(()->focusReady(at,attempt+1));}
    private final class Tiles extends RecyclerView.Adapter<Tile> {
        @Override public int getItemCount(){return favorites.size();}
        @Override public Tile onCreateViewHolder(ViewGroup parent,int type){
            FrameLayout cell=new FrameLayout(ExperienceHomeActivity.this);cell.setClipChildren(false);cell.setClipToPadding(false);cell.setFocusable(true);cell.setClickable(true);cell.setLayoutParams(new RecyclerView.LayoutParams(px(geometry.cardWidth+geometry.gap),px(geometry.cardHeight+38)));
            ImageView art=new ImageView(ExperienceHomeActivity.this);FrameLayout.LayoutParams picture=new FrameLayout.LayoutParams(px(geometry.cardWidth),px(geometry.cardHeight));picture.topMargin=px(1);art.setBackground(round(0xff28435f,ExperiencePolicy.CLEAN.equals(look)?9:12));art.setClipToOutline(true);art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);cell.addView(art,picture);
            View ring=new View(ExperienceHomeActivity.this);ring.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);ring.setFocusable(false);ring.setVisibility(View.INVISIBLE);boolean strong=ProfileStore.preferences(ExperienceHomeActivity.this).getBoolean(TileSpacing.FOCUS,false);GradientDrawable dark=round(Color.TRANSPARENT,16);dark.setStroke(px(strong?6:5),0xff101820);GradientDrawable light=round(Color.TRANSPARENT,14);light.setStroke(px(strong?3:2),0xfff5f7fa);LayerDrawable outline=new LayerDrawable(new Drawable[]{dark,light});outline.setLayerInset(1,px(2),px(2),px(2),px(2));ring.setBackground(outline);FrameLayout.LayoutParams edge=new FrameLayout.LayoutParams(px(geometry.cardWidth+12),px(geometry.cardHeight+12));edge.leftMargin=-px(6);edge.topMargin=-px(5);cell.addView(ring,edge);
            TextView name=text("",14);name.setTextColor(labelInk);name.setGravity(Gravity.CENTER);name.setShadowLayer(px(1),0,px(1),0xc0000000);FrameLayout.LayoutParams title=new FrameLayout.LayoutParams(px(geometry.cardWidth),px(24));title.topMargin=px(geometry.cardHeight+7);cell.addView(name,title);HomeAppearance.name(name);return new Tile(cell,art,name,ring);
        }
        @Override public void onBindViewHolder(Tile h,int at){HomeCategories.App app=favorites.get(at);h.app=app;h.itemView.setContentDescription(app.label);h.name.setText(app.label);Drawable art=app.artwork(ExperienceHomeActivity.this,config);h.art.setImageDrawable(art);boolean banner=art!=null&&art.getIntrinsicWidth()>art.getIntrinsicHeight()*1.3f;h.art.setScaleType(banner?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_INSIDE);h.art.setPadding(banner?0:px(12),banner?0:px(8),banner?0:px(12),banner?0:px(8));h.paint(h.itemView.isFocused());}
        @Override public void onViewRecycled(Tile h){h.art.setImageDrawable(null);h.app=null;}
    }
    private final class Tile extends RecyclerView.ViewHolder {
        final ImageView art;final TextView name;final View ring;HomeCategories.App app;
        Tile(FrameLayout cell,ImageView art,TextView name,View ring){super(cell);this.art=art;this.name=name;this.ring=ring;
            cell.setOnFocusChangeListener((v,on)->{paint(on);int at=getBindingAdapterPosition();if(on&&at>=0){selected=at;updateSelection();}});
            cell.setOnClickListener(v->{if(app!=null){if(preview!=null){ExperienceChoice.cancel();finish();}app.launch(ExperienceHomeActivity.this,config);}});
            cell.setOnLongClickListener(v->{if(app!=null&&canEdit())NativeTileEditor.show(ExperienceHomeActivity.this,app.pkg);return true;});
            cell.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;int at=getBindingAdapterPosition();if(at<0)return false;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){focusApp(ExperiencePolicy.next(at,key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1,favorites.size(),list.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL));return true;}
                if(key==KeyEvent.KEYCODE_DPAD_DOWN&&shelf!=null&&geometry.top<260&&shelf.focus())return true;
                if(key==KeyEvent.KEYCODE_DPAD_UP){if(shelf!=null&&geometry.top>=260&&shelf.focus())return true;if(trialRevert!=null)trialRevert.requestFocus();else settings.requestFocus();return true;}return key==KeyEvent.KEYCODE_DPAD_DOWN;
            });
        }
        void paint(boolean on){
            art.animate().cancel();float scale=on?1.07f:1;art.animate().scaleX(scale).scaleY(scale).setDuration(config.animations&&!config.reducedMotion?150:0).start();
            ring.setVisibility(on?View.VISIBLE:View.INVISIBLE);
            int mode=ProfileStore.preferences(ExperienceHomeActivity.this).getInt(HomePolish.NAMES,0);name.setVisibility(ExperiencePolicy.CINEMA.equals(look)||mode==3?View.INVISIBLE:View.VISIBLE);name.setAlpha(on?1:PolishPolicy.nameAlpha(mode,config.alwaysShowNames?1:0));
        }
    }
    void sample(Bitmap bitmap){
        if(bitmap==null||bitmap.isRecycled()||bitmap.getConfig()==Bitmap.Config.HARDWARE)return;
        int[] head=new int[32],labels=new int[32];float cropScale=Math.max(960f/bitmap.getWidth(),540f/bitmap.getHeight());float cropX=(bitmap.getWidth()-960/cropScale)/2,cropY=(bitmap.getHeight()-540/cropScale)/2;
        for(int i=0;i<32;i++){int x=Math.max(0,Math.min(bitmap.getWidth()-1,(int)(cropX+(i+.5f)*960/32/cropScale)));head[i]=bitmap.getPixel(x,Math.min(bitmap.getHeight()-1,(int)(cropY+55/cropScale)));labels[i]=bitmap.getPixel(x,Math.min(bitmap.getHeight()-1,(int)(cropY+(geometry==null?480:geometry.labelTop)/cropScale)));}
        applyInk(head,labels);
    }
    private void applyInk(int[] head,int[] labels){
        android.content.SharedPreferences p=ProfileStore.preferences(this);int choice=p.getBoolean(HomePolish.HEADER,false)?p.getInt(HomePolish.INK,0):0;
        ink=ExperiencePolicy.ink(choice,head);labelInk=ExperiencePolicy.ink(choice,labels);if(!ExperiencePolicy.CLEAN.equals(look)){labelInk=choice==2?0xff182334:0xfff5f7fa;if(dock!=null)dock.setBackground(round(choice==2?0xe6eef3fa:0xe6192637,20));}headerInk(info,ink);hero.setTextColor(labelInk);
        if(labelShade!=null)labelShade.setBackground(ExperiencePolicy.CLEAN.equals(look)?new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,labelInk==0xfff5f7fa?0xb3081020:0xcceef3fa,0x00000000}):null);
        boolean light=ink==0xfff5f7fa;topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{light?0xa6081020:0x99f5f7fa,0x00000000}));
        boolean lightLabels=labelInk==0xfff5f7fa;bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,lightLabels?0xb3081020:0x99f5f7fa}));
        for(int i=0;i<list.getChildCount();i++){RecyclerView.ViewHolder h=list.getChildViewHolder(list.getChildAt(i));if(h instanceof Tile){((Tile)h).name.setTextColor(labelInk);((Tile)h).name.setShadowLayer(labelInk==0xfff5f7fa?px(1):0,0,px(1),0xb3000000);}}
    }
    private void headerInk(View v,int color){if(v instanceof TextView){TextView t=(TextView)v;if(t.isFocusable())t.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[0]},new int[]{0xff182334,color}));else t.setTextColor(color);}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)headerInk(((ViewGroup)v).getChildAt(i),color);}
    void trialControls(long deadline,Runnable keep,Runnable revert){
        if(trialBar!=null&&trialBar.getParent()==chrome)chrome.removeView(trialBar);
        trialBar=new LinearLayout(this);trialBar.setGravity(Gravity.CENTER_VERTICAL);trialBar.setBackground(round(0xf0111b2a,12));trialBar.setPadding(px(14),px(5),px(8),px(5));
        TextView notice=text("",13);trialBar.addView(notice,new LinearLayout.LayoutParams(0,-2,1));
        TextView back=text("Revert",14),save=text("Keep this look",14);trialRevert=back;
        for(TextView v:new TextView[]{back,save}){v.setGravity(Gravity.CENTER);v.setPadding(px(12),0,px(12),0);v.setFocusable(true);v.setClickable(true);focus(v,false);v.setOnFocusChangeListener((x,on)->focus(x,on));v.setOnKeyListener((x,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_DOWN){focusApp(selected);return true;}return false;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,px(32));p.leftMargin=px(8);trialBar.addView(v,p);}
        back.setOnClickListener(v->revert.run());save.setOnClickListener(v->keep.run());put(trialBar,160,104,640,44);back.requestFocus();
        handler.post(new Runnable(){public void run(){if(!resumed||isFinishing())return;long remaining=deadline-SystemClock.elapsedRealtime();if(remaining<=0){revert.run();return;}notice.setText("Preview · reverts in "+(remaining+999)/1000+"s · Saved Home unchanged");handler.postDelayed(this,250);}});
    }
    private boolean canEdit(){if(preview==null)return true;Toast.makeText(this,"Keep or revert this preview before editing Home",Toast.LENGTH_SHORT).show();return false;}
    private void allApps(){AllAppsSurface.show(this,config,catalog,preview==null);}
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
    private void armIdle(){handler.removeCallbacks(idleTimeout);if(resumed&&!quiet&&idleSeconds>0){idle.arm(SystemClock.elapsedRealtime(),idleSeconds);handler.postDelayed(idleTimeout,idleSeconds*1000L);}}
    private void setQuiet(boolean value){quiet=value;for(View view:quietViews){view.animate().cancel();view.animate().alpha(value?0:1).setDuration(config.animations&&!config.reducedMotion?180:0).start();}}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int code=event.getKeyCode();boolean nav=code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_BACK;
        boolean was=idle.quiet,consume=idle.key(code,event.getAction()==KeyEvent.ACTION_DOWN,nav);if(was&&!idle.quiet)setQuiet(false);if(event.getAction()==KeyEvent.ACTION_DOWN)armIdle();if(consume)return true;
        if(tvButton!=null&&tvButton.handle(event))return true;if(code>=1000&&RemoteKeys.handle(this,event,appKeys.get(code),null))return true;
        if(code==KeyEvent.KEYCODE_TV_INPUT){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)inputs();return true;}
        if(config!=null)for(int settingsCode:config.settingsKeycodes)if(code==settingsCode){if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())try{startActivity(config.settingsKeyPanel?new Intent(this,QuickSettingsActivity.class):SystemSettings.intent(this,Settings.ACTION_SETTINGS));}catch(Exception ignored){}return true;}
        return super.dispatchKeyEvent(event);
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN&&quiet){touchWake=true;idle.quiet=false;setQuiet(false);armIdle();}if(touchWake){if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP||event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)touchWake=false;return true;}if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)armIdle();return super.dispatchTouchEvent(event);}
    private void action(){String action=getIntent().getStringExtra("home_action");getIntent().removeExtra("home_action");if(MainActivity.ACTION_VIEW_INPUTS.equals(action))handler.post(this::inputs);else if(MainActivity.ACTION_LIBRARY.equals(action))handler.post(this::allApps);else if(MainActivity.ACTION_IDLE_PREVIEW.equals(action)){idle.quiet=true;setQuiet(true);}}
    @Override protected void onResume(){super.onResume();resumed=true;render();action();if(preview!=null&&!isFinishing())handler.postDelayed(()->ExperienceChoice.prompt(this,preview),180);else if(PlacementSession.preview()!=null)handler.postDelayed(()->PlacementSession.prompt(this),180);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(tick);handler.removeCallbacks(idleTimeout);idle.stop();if(wallpaper!=null)wallpaper.stop();if(video!=null)video.stop();if(preview!=null){ExperienceChoice.cancel();finish();}super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);release();if(current.get()==this)current=EMPTY;super.onDestroy();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);preview=intent.getStringExtra(ExperienceChoice.EXTRA);if(resumed){render();action();}}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)handler.removeCallbacks(idleTimeout);else if(resumed){Config.load(this);if(signature!=null&&!signature.equals(currentSignature()))render();armIdle();}}
    @Override public void onBackPressed(){if(preview!=null){ExperienceChoice.cancel();finish();}else focusApp(0);}
    @Override protected void onSaveInstanceState(Bundle saved){saved.putString("selected",selectedPackage);super.onSaveInstanceState(saved);}
    static Map<String,Object> status(){Map<String,Object> out=new LinkedHashMap<>();ExperienceHomeActivity a=current.get();out.put("active",a!=null&&a.resumed);if(a!=null){out.put("look",a.look);out.put("preview",a.preview!=null);out.put("selected",a.selectedPackage);out.put("profile",ProfileStore.id());out.put("quiet",a.quiet);out.put("ink",a.ink);out.put("labelInk",a.labelInk);out.put("apps",a.favorites.stream().map(x->x.pkg).toArray(String[]::new));}return out;}
}
