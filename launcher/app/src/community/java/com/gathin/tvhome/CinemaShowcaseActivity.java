package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;

/** Remote-first cinematic Home and reversible previews; never rewrites app/media geometry. */
public final class CinemaShowcaseActivity extends Activity {
    private CinemaWorld world;
    private ThemeLibrary.Entry entry;
    private Config config;
    private FrameLayout root;
    private LinearLayout dock,info;private HorizontalScrollView dockScroll;private float cellWidth;private ShowcaseLayout layout;private int names;
    private TvButtonController tvButton;private Map<Integer,String> appKeys=Collections.emptyMap();
    private String trial,signature;private boolean savedHome;private final android.os.Handler trialTimer=new android.os.Handler(android.os.Looper.getMainLooper());
    private WeatherClient.Attachment weather;
    private TextView headline,detail,help,clock;
    private final android.os.Handler clockTimer=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable clockTick=new Runnable(){public void run(){if(clock!=null)clock.setText(DateFormat.getTimeFormat(CinemaShowcaseActivity.this).format(new Date()));clockTimer.postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    private FocusLight light;
    private LiveVideo video;
    private float unit;
    private int appAt;
    private CinemaWorldMotion companion;
    private String profile;
    private final List<HomeCategories.App> apps=new ArrayList<>();

    private Bitmap backdrop;

    private int[] colours;

    static void open(Activity a){a.startActivity(new Intent(a,CinemaShowcaseActivity.class).putExtra("preview_only",true));}
    static boolean ready(Context c){try{PlaylistPolicy.Spec p=WallpaperPlaylistChoice.read(c);if(CinemaWorld.showcase(CinemaStore.read(c),p)==null)return false;for(ThemeLibrary.Entry e:ThemeLibrary.list(c))if(e.token.equals(p.tokens.get(0)))return true;}catch(RuntimeException ignored){}return false;}
    private String signature(){return ProfileStore.id()+"|"+ProfileStore.preferences(this).getAll().hashCode();}
    protected void attachBaseContext(Context c){super.attachBaseContext(LauncherLocale.wrap(c));}
    protected void onCreate(Bundle saved){
        super.onCreate(saved);ProductFeatures.init(this);profile=ProfileStore.id();config=Config.load(this);
        trial=getIntent().getStringExtra(ExperienceChoice.EXTRA);savedHome=getIntent().getBooleanExtra("saved_home",false);signature=signature();
        if(trial!=null&&!ExperienceChoice.trialValid(trial)){finish();return;}
        CinemaSpec cinema=CinemaStore.read(this);PlaylistPolicy.Spec playlist=WallpaperPlaylistChoice.read(this);
        for(String token:playlist.tokens){CinemaWorld candidate=CinemaWorld.find(cinema,token);if(candidate!=null){world=candidate;break;}}
        if(world==null){Toast.makeText(this,"Pair a world with a selected wallpaper first",Toast.LENGTH_LONG).show();finish();return;}
        for(ThemeLibrary.Entry item:ThemeLibrary.list(this))if(item.token.equals(world.wallpaper))entry=item;
        if(entry==null){finish();return;}
        Map<String,HomeCategories.App> catalog=new LinkedHashMap<>();for(HomeCategories.App app:HomeCategories.catalog(this,config))catalog.put(app.pkg,app);
        for(String pkg:DesktopLayout.favorites(config,Order.load(Order.file(this))))if(catalog.containsKey(pkg)&&FamilyMode.allowed(this,pkg))apps.add(catalog.get(pkg));
        List<HomeCategories.App> decorated=Folders.decorate(this,new ArrayList<>(apps),new ArrayList<>(catalog.values()));apps.clear();apps.addAll(decorated);if(apps.isEmpty()){finish();return;}
        if(saved!=null){appAt=Math.max(0,Math.min(apps.size()-1,saved.getInt("app")));}
        colours=world.index==0?new int[]{0xffe6c37a,0xffffc78a,0xffa5dccc}:world.index==1?new int[]{0xffc5dc9a,0xffffce96,0xffa8d9e5}:new int[]{world.accent(),0xffe6d5b1,0xffb5dce5};
        tvButton=config.tvButtonPicker?new TvButtonController(this,config):null;appKeys=RemoteKeys.parseAppKeys(config.appKeys);layout=new ShowcaseLayout(PlacementSession.selected(this),apps.size(),config.tileGap);names=ProfileStore.preferences(this).getInt(HomePolish.NAMES,0);unit=getResources().getDisplayMetrics().widthPixels/960f;backdrop=poster();render();if(trial!=null)ExperienceChoice.prompt(this,trial);
    }
    private int px(float n){return Math.round(n*unit);}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));t.setTextColor(0xfff7f0df);t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));t.setShadowLayer(px(2),0,px(1),0xff081016);return t;}
    private void put(View view,float x,float y,float w,float h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(px(w),px(h));p.leftMargin=px(x);p.topMargin=px(y);root.addView(view,p);}
    private Bitmap poster(){try{BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;o.inPreferredConfig=Bitmap.Config.ARGB_8888;return BitmapFactory.decodeFile(new File(entry.directory,Config.WALLPAPER_NAME).getPath(),o);}catch(RuntimeException|OutOfMemoryError absent){return null;}}
    private void render(){
        root=new FrameLayout(this);root.setId(R.id.root);root.setClipChildren(false);root.setBackgroundColor(0xff071018);setContentView(root);
        ImageView image=new ImageView(this);image.setImageBitmap(backdrop);image.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(image,new FrameLayout.LayoutParams(-1,-1));
        File file=new File(entry.directory,"video.mp4");if(file.isFile()&&ThemeLibrary.liveAllowed()&&ProfileStore.preferences(this).getBoolean(ThemeLibrary.LIVE,false)&&LiveVideo.supported(this,config.reducedMotion)){video=new LiveVideo(this,file,0);root.addView(video.view(),new FrameLayout.LayoutParams(-1,-1));}
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xd0071018,0x30071018,0x60071018,0xf0071018}));root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        light=new FocusLight(this);light.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);root.addView(light,new FrameLayout.LayoutParams(-1,-1));
        info=new LinearLayout(this);info.setId(R.id.leftBlock);info.setOrientation(LinearLayout.VERTICAL);clock=text(DateFormat.getTimeFormat(this).format(new Date()),32);clock.setId(R.id.clock);info.addView(clock);info.addView(text(new java.text.SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()),12));ProductFeatures.categoryHeader(this,info,config);put(info,42,27,440,112);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.END);put(controls,savedHome?686:746,29,savedHome?235:175,68);
        utility(controls,"Inputs",0,()->InputPicker.show(this,false));utility(controls,"Settings",1,()->{if(trial!=null){Toast.makeText(this,"Keep or revert before editing Home",Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(this,LauncherSettingsActivity.class).putExtra("experience_context",true));});utility(controls,savedHome?"Home experiences":"Close preview",savedHome?4:2,this::closePreview);if(savedHome)utility(controls,"All apps",3,()->AllAppsSurface.show(this,config,HomeCategories.catalog(this,config),ProductFeatures.layoutEditable(this)));
        TextView brand=text(world.name().toUpperCase(Locale.ROOT)+"  /  SHOWCASE",11);brand.setLetterSpacing(.16f);brand.setTextColor(world.accent());put(brand,44,layout.brand,780,24);
        headline=text("",22);headline.setMaxLines(2);headline.setTypeface(Typeface.create("serif",Typeface.NORMAL));put(headline,42,layout.headline,870,58);detail=text("",16);put(detail,44,327,820,29);detail.setVisibility(View.GONE);
        dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);dock.setClipChildren(false);dock.setClipToPadding(false);dockScroll=new HorizontalScrollView(this);dockScroll.setHorizontalScrollBarEnabled(false);dockScroll.setClipChildren(false);dockScroll.setClipToPadding(false);dockScroll.setFocusable(false);cellWidth=layout.cell;dockScroll.addView(dock,new FrameLayout.LayoutParams(px(layout.width),px(108)));put(dockScroll,34,layout.top,892,108);
        for(int i=0;i<apps.size();i++){final int at=i;HomeCategories.App app=apps.get(i);FrameLayout item=new FrameLayout(this);item.setFocusable(true);item.setClickable(true);item.setContentDescription(app.label);ImageView logo=new ImageView(this);logo.setImageDrawable(CinemaWorldArt.logo(this,app.pkg,app.label,world));logo.setScaleType(ImageView.ScaleType.FIT_CENTER);float logoHeight=63*IconSizePolicy.percent(IconSizes.current(this))/100f;FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,px(logoHeight));p.setMargins(px(8),px(4+(63-logoHeight)/2),px(8),0);item.addView(logo,p);TextView name=text(app.label,13);name.setGravity(Gravity.CENTER);name.setMaxLines(2);FrameLayout.LayoutParams label=new FrameLayout.LayoutParams(-1,px(36));label.topMargin=px(70);item.addView(name,label);LinearLayout.LayoutParams slot=new LinearLayout.LayoutParams(px(cellWidth),px(108));slot.rightMargin=i==apps.size()-1?0:px(layout.gap);dock.addView(item,slot);
            item.setOnFocusChangeListener((v,on)->{name.setVisibility(names==1||names!=3&&on?View.VISIBLE:View.INVISIBLE);v.setBackground(on?new DockFocus(world.accent()):null);logo.animate().cancel();logo.animate().scaleX(on?1.08f:1).scaleY(on?1.08f:1).setDuration(motion()?180:0).start();if(on){appAt=at;appSelection();}});name.setVisibility(names==1?View.VISIBLE:View.INVISIBLE);item.setOnClickListener(v->app.launch(this,config));item.setOnKeyListener((v,key,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_DOWN){openImmersive();return true;}if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){appAt=Math.max(0,Math.min(apps.size()-1,at+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));focusDock();return true;}return false;});
        }
        companion=new CinemaWorldMotion(this,world,motion());if(layout.top<200)companion.companionScale=.65f;root.addView(companion,Math.min(4,root.getChildCount()),new FrameLayout.LayoutParams(-1,-1));
        help=text(savedHome?"↓ Enter the library    ·    ↑ Clock and controls    ·    Settings to personalise":"Showcase preview    ·    ↓ Enter the library    ·    Back returns to your saved Home",13);help.setGravity(Gravity.CENTER);help.setTextColor(0xffdeded5);put(help,90,493,780,30);root.post(()->{focusDock();appSelection();});
    }
    private boolean motion(){return config.animations&&!config.reducedMotion;}
    private void utility(LinearLayout host,String label,int symbol,Runnable action){
        FrameLayout button=new FrameLayout(this);button.setFocusable(true);button.setClickable(true);button.setContentDescription(label);View icon=new View(this){final Paint p=new Paint(3);protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setColor(0xfff7f0df);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(px(1.5f));if(symbol==0){c.drawRoundRect(w*.3f,h*.2f,w*.7f,h*.73f,3,3,p);c.drawLine(w*.42f,h*.33f,w*.58f,h*.33f,p);c.drawLine(w*.42f,h*.43f,w*.58f,h*.43f,p);}else if(symbol==1){c.drawCircle(w/2,h*.46f,h*.18f,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(w/2+(float)Math.cos(a)*h*.23f,h*.46f+(float)Math.sin(a)*h*.23f,w/2+(float)Math.cos(a)*h*.3f,h*.46f+(float)Math.sin(a)*h*.3f,p);}}else if(symbol==2){c.drawLine(w*.35f,h*.27f,w*.65f,h*.64f,p);c.drawLine(w*.65f,h*.27f,w*.35f,h*.64f,p);}else if(symbol==3){for(int row=0;row<2;row++)for(int col=0;col<2;col++)c.drawRect(w*(.3f+col*.23f),h*(.25f+row*.24f),w*(.46f+col*.23f),h*(.41f+row*.24f),p);}else{Path roof=new Path();roof.moveTo(w*.26f,h*.43f);roof.lineTo(w*.5f,h*.21f);roof.lineTo(w*.74f,h*.43f);c.drawPath(roof,p);c.drawRect(w*.34f,h*.43f,w*.66f,h*.72f,p);}}};button.addView(icon,new FrameLayout.LayoutParams(-1,px(44)));TextView caption=text(label.equals("Close preview")?"Close":label,11);caption.setGravity(Gravity.CENTER);caption.setVisibility(View.INVISIBLE);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,px(22));lp.topMargin=px(44);button.addView(caption,lp);host.addView(button,new LinearLayout.LayoutParams(0,px(68),1));button.setOnFocusChangeListener((v,on)->{caption.setVisibility(on?View.VISIBLE:View.INVISIBLE);v.setBackground(on?CinemaWorldArt.focus(world.accent()):null);});button.setOnClickListener(v->action.run());button.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_DOWN&&k==KeyEvent.KEYCODE_DPAD_DOWN){focusDock();return true;}return false;});
    }
    private void tint(int colour){light.move(dockPosition(),colour,motion());}
    private static final class FocusLight extends View {
        final Paint paint=new Paint(3);final android.animation.ArgbEvaluator evaluator=new android.animation.ArgbEvaluator();float position=.5f;int colour=0xffe6c37a;boolean initialized;android.animation.ValueAnimator transition;
        FocusLight(Context context){super(context);}
        void move(float target,int value,boolean animate){
            if(transition!=null)transition.cancel();float from=position;int old=colour;
            if(!animate||!initialized){position=target;colour=value;initialized=true;invalidate();return;}
            transition=android.animation.ValueAnimator.ofFloat(0,1);transition.setDuration(260);
            transition.addUpdateListener(a->{float f=(float)a.getAnimatedValue();position=from+(target-from)*f;colour=(int)evaluator.evaluate(f,old,value);invalidate();});transition.start();
        }
        protected void onDraw(Canvas canvas){float x=getWidth()*position,y=getHeight()*.81f,r=getWidth()*.24f;paint.setShader(new RadialGradient(x,y,r,new int[]{(colour&0xffffff)|0x28000000,colour&0xffffff},null,Shader.TileMode.CLAMP));canvas.drawCircle(x,y,r,paint);paint.setShader(null);}
        void stop(){if(transition!=null)transition.cancel();}
        protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    }
    private float dockPosition(){int offset=Math.max(0,Math.min(px(layout.width-892),px(layout.center(appAt)-446)));dockScroll.scrollTo(offset,0);return (34+layout.center(appAt)-offset/unit)/960f;}
    private void focusDock(){View v=dock.getChildAt(appAt);if(v.isInTouchMode())v.requestFocusFromTouch();else v.requestFocus();}
    private void appSelection(){String pkg=apps.get(appAt).pkg,custom=CinemaStore.read(this).taglines.get(pkg);String fallback=pkg.contains("stb")?"Your channels. Your kind of evening.":pkg.contains("netflix")?"One more chapter?":pkg.contains("youtube")?"Follow your curiosity.":pkg.contains("amazon")?"The next story is waiting.":pkg.contains("crunchy")?"Your next adventure awaits.":world.index==0?"A little magic. Your kind of evening.":"Your next adventure starts at home.";headline.setText(custom==null||custom.trim().isEmpty()?fallback:custom);detail.setText(apps.get(appAt).label+"  ·  ↓ Enter the library");tint(colours[appAt%colours.length]);if(companion!=null)companion.focus(dockPosition(),layout.top/540f);}
    private static final class DockFocus extends Drawable {
        final Paint p=new Paint(3);final int colour;DockFocus(int value){colour=value;}
        public void draw(Canvas c){Rect b=getBounds();p.setColor(colour);p.setStrokeWidth(3);c.drawLine(b.exactCenterX()-b.width()*.15f,b.bottom-3,b.exactCenterX()+b.width()*.15f,b.bottom-3,p);}
        public void setAlpha(int value){p.setAlpha(value);}public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private void openImmersive(){if(trial!=null){Toast.makeText(this,"Keep or revert before opening the library",Toast.LENGTH_SHORT).show();return;}Intent intent=new Intent(this,CinemaWorldActivity.class).putExtra("world",world.id).putExtra("showcase",true);if(world.libraryArt.isEmpty())for(CinemaWorld w:CinemaStore.read(this).worlds)if(w.index==0&&!w.libraryArt.isEmpty())intent.putExtra("study",w.libraryArt);startActivity(intent);overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);}
    private void closePreview(){if(savedHome){ExperienceChoice.show(this);return;}if(trial!=null)ExperienceChoice.cancel();ExperienceChoice.home(this);finish();}
    public void onBackPressed(){if(savedHome)focusDock();else closePreview();}
    protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);recreate();}
    void trialControls(long deadline,Runnable keep,Runnable revert){
        help.setVisibility(View.GONE);LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(0xe8101822);put(bar,215,485,530,48);TextView countdown=text("",13);bar.addView(countdown,new LinearLayout.LayoutParams(0,-1,1));
        for(int i=0;i<2;i++){final boolean accept=i==0;TextView button=text(accept?"Keep":"Revert",14);button.setGravity(Gravity.CENTER);button.setFocusable(true);button.setClickable(true);button.setBackground(CinemaWorldArt.focus(world.accent()));bar.addView(button,new LinearLayout.LayoutParams(px(104),-1));button.setOnClickListener(v->{trialTimer.removeCallbacksAndMessages(null);if(accept)keep.run();else revert.run();});button.setOnKeyListener((v,key,e)->{if(e.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_DPAD_UP){focusDock();return true;}return false;});if(!accept)bar.post(button::requestFocus);}
        trialTimer.post(new Runnable(){public void run(){if(isFinishing()||isDestroyed())return;long left=deadline-android.os.SystemClock.elapsedRealtime();if(left<=0){revert.run();return;}countdown.setText("Try Showcase · "+((left+999)/1000)+" s");trialTimer.postDelayed(this,Math.min(1000,left));}});
    }
    protected void onResume(){super.onResume();if(profile!=null&&!profile.equals(ProfileStore.id())){ExperienceChoice.home(this);finish();return;}if(savedHome&&(!ExperiencePolicy.SHOWCASE.equals(ExperienceChoice.saved(this))||!ready(this))){ExperienceChoice.home(this);finish();return;}if(signature!=null&&!signature.equals(signature())){recreate();return;}if(trial!=null&&!ExperienceChoice.trialValid(trial)){finish();return;}if(savedHome)ProductFeatures.resumed(this);if(video!=null)video.start();if(companion!=null)companion.start();if(info!=null&&!"off".equals(WeatherClient.units(this,WeatherClient.HOME))){weather=new WeatherClient.Attachment(this,info,WeatherClient.HOME,false);weather.start();}clockTimer.removeCallbacks(clockTick);clockTimer.post(clockTick);}
    protected void onPause(){if(savedHome)ProductFeatures.paused(this);if(weather!=null){weather.close();weather=null;}clockTimer.removeCallbacks(clockTick);if(light!=null)light.stop();if(video!=null)video.stop();if(companion!=null)companion.stop();super.onPause();}
    protected void onDestroy(){trialTimer.removeCallbacksAndMessages(null);if(video!=null){video.close();video=null;}if(companion!=null)companion.stop();super.onDestroy();}
    public boolean dispatchKeyEvent(KeyEvent event){
        int code=event.getKeyCode();if(tvButton!=null&&tvButton.handle(event))return true;if(code>=1000&&RemoteKeys.handle(this,event,appKeys.get(code),null))return true;
        if(code==KeyEvent.KEYCODE_TV_INPUT){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)InputPicker.show(this,false);return true;}
        if(config!=null)for(int settingsCode:config.settingsKeycodes)if(code==settingsCode){if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())try{startActivity(config.settingsKeyPanel?new Intent(this,QuickSettingsActivity.class):SystemSettings.intent(this,android.provider.Settings.ACTION_SETTINGS));}catch(Exception ignored){}return true;}
        return super.dispatchKeyEvent(event);
    }
    public void onUserInteraction(){super.onUserInteraction();if(savedHome)ProductFeatures.interaction(this);}
    public void onTrimMemory(int level){super.onTrimMemory(level);ProductFeatures.trimArtwork(level);}
    protected void onSaveInstanceState(Bundle state){state.putInt("app",appAt);super.onSaveInstanceState(state);}
}
