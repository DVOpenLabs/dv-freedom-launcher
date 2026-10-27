package com.gathin.tvhome;

import android.app.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Launcher-only quiet view and a still/poster-sampled header gradient. Never reads another app. */
final class HomePolish {
    static final String IDLE="cinematic_idle_seconds",HEADER="adaptive_header",MOTION="refined_motion",INK="text_ink_choice",NAMES="app_names_choice";
    private final Activity activity;private final Config config;private final BooleanSupplier ready;
    private final Handler handler=new Handler(Looper.getMainLooper());private final PolishPolicy.Idle idle=new PolishPolicy.Idle();
    private final Map<View,Float> faded=new HashMap<>();private final int seconds;private final View shade;
    private boolean visible;private int shadeAlpha;private int[] poster;
    int shadeOpacity(){return shadeAlpha;}
    private final Runnable timeout=this::onTimeout;
    private void onTimeout(){
        if(!visible)return;
        android.view.accessibility.AccessibilityManager access=activity.getSystemService(android.view.accessibility.AccessibilityManager.class);
        if(!activity.hasWindowFocus()||!ready.getAsBoolean()||access!=null&&access.isTouchExplorationEnabled()){interact();return;}
        if(idle.due(SystemClock.elapsedRealtime()))fade();
    }
    HomePolish(Activity activity,Config config,BooleanSupplier ready){
        this.activity=activity;this.config=config;this.ready=ready;
        seconds=ProfileStore.preferences(activity).getInt(IDLE,0);
        FrameLayout root=activity.findViewById(R.id.root);shade=new View(activity);shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);shade.setFocusable(false);
        View grid=activity.findViewById(R.id.grid);View content=(View)grid.getParent().getParent();
        root.addView(shade,root.indexOfChild(content),new FrameLayout.LayoutParams(-1,1));
        shade.setVisibility(View.GONE);
        grid.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->grid.post(this::refreshText));
        if(ProfileStore.preferences(activity).getBoolean(HEADER,false)){
            shadow(activity.findViewById(R.id.leftBlock));
        }
    }
    private void text(TextView view,int color){
        if(view.isFocusable())view.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[0]},new int[]{0xff182334,color}));
        else view.setTextColor(color);
        view.setShadowLayer(!view.isFocusable()&&color==0xfff5f7fa?1.5f*activity.getResources().getDisplayMetrics().density:0,0,1,0xc0000000);
    }
    // One palette per theme, not independent colours for neighbouring app names.
    // Theme background is authoritative; the poster may contain transient bright particles/water.
    private int ink(View view){return PolishPolicy.textInk(ProfileStore.preferences(activity).getInt(INK,0),config.backgroundColor);}
    private void header(View view,int color){
        if(view instanceof TextView)text((TextView)view,color);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)header(((ViewGroup)view).getChildAt(i),color);
    }
    void name(TextView view){if(ProfileStore.preferences(activity).getBoolean(HEADER,false))text(view,ink(view));}
    private void names(View view){
        if(view.getId()==R.id.name&&view instanceof TextView)name((TextView)view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)names(((ViewGroup)view).getChildAt(i));
    }
    private void shadow(View view){if(view instanceof TextView)text((TextView)view,0xfff5f7fa);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)shadow(((ViewGroup)view).getChildAt(i));}
    void refreshText(){
        if(!ProfileStore.preferences(activity).getBoolean(HEADER,false))return;
        View left=activity.findViewById(R.id.leftBlock);int color=ink(left);header(left,color);
        if(activity instanceof MainActivity)((MainActivity)activity).greetingInk(color);
        names(activity.findViewById(R.id.grid));
    }
    void sample(Bitmap bitmap){
        if(!ProfileStore.preferences(activity).getBoolean(HEADER,false)||bitmap==null||bitmap.isRecycled()||bitmap.getConfig()==Bitmap.Config.HARDWARE)return;
        android.util.DisplayMetrics screen=activity.getResources().getDisplayMetrics();
        float scale=Math.max((float)screen.widthPixels/bitmap.getWidth(),(float)screen.heightPixels/bitmap.getHeight());
        float left=(bitmap.getWidth()-screen.widthPixels/scale)/2,top=(bitmap.getHeight()-screen.heightPixels/scale)/2;
        poster=new int[32*18];
        for(int i=0;i<poster.length;i++)poster[i]=bitmap.getPixel(Math.min(bitmap.getWidth()-1,(int)(left+(i%32+.5f)*screen.widthPixels/32/scale)),Math.min(bitmap.getHeight()-1,(int)(top+(i/32+.5f)*screen.heightPixels/18/scale)));
        int[] colors=Arrays.copyOf(poster,32*4);int alpha=PolishPolicy.ink(colors)==0xfff5f7fa?PolishPolicy.shade(colors):0;
        shadeAlpha=alpha;shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{alpha<<24,alpha<<24,0}));shade.setVisibility(alpha==0?View.GONE:View.VISIBLE);
        shade.post(()->{View header=(View)activity.findViewById(R.id.leftBlock).getParent();int[] p=new int[2];header.getLocationOnScreen(p);
            FrameLayout.LayoutParams layout=(FrameLayout.LayoutParams)shade.getLayoutParams();layout.height=Math.round((p[1]+header.getHeight())*1.7f);shade.setLayoutParams(layout);refreshText();});
    }
    void start(){visible=true;activity.findViewById(R.id.grid).post(this::refreshText);interact();}
    void stop(){visible=false;handler.removeCallbacks(timeout);idle.stop();restore(false);}
    boolean quiet(){return idle.quiet;}
    void preview(){if(!visible)return;handler.removeCallbacks(timeout);idle.quiet=true;fade();}
    void interact(){
        if(!visible)return;if(idle.quiet){idle.quiet=false;restore(true);}
        handler.removeCallbacks(timeout);idle.arm(SystemClock.elapsedRealtime(),seconds);
        if(seconds>0&&PolishPolicy.validSeconds(seconds))handler.postDelayed(timeout,seconds*1000L);
    }
    boolean key(KeyEvent event){
        int code=event.getKeyCode();boolean was=idle.quiet;
        boolean navigation=code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_BUTTON_A;
        boolean consume=idle.key(code,event.getAction()==KeyEvent.ACTION_DOWN,navigation);
        if(was&&!idle.quiet){restore(true);interact();}return consume;
    }
    private void fade(){
        faded.clear();View grid=activity.findViewById(R.id.grid);
        List<View> views=Arrays.asList((View)grid.getParent(),activity.findViewById(R.id.glass),activity.findViewById(R.id.topButtons),activity.findViewById(R.id.greeting),activity.findViewById(R.id.hero));
        for(View view:views)if(view!=null){faded.put(view,view.getAlpha());view.animate().cancel();view.animate().alpha(0).setDuration(duration()).start();}
    }
    private long duration(){return config.animations&&!config.reducedMotion?180:0;}
    private void restore(boolean animate){boolean changed=!faded.isEmpty();for(Map.Entry<View,Float> entry:faded.entrySet()){View view=entry.getKey();view.animate().cancel();if(animate)view.animate().alpha(entry.getValue()).setDuration(duration()).start();else view.setAlpha(entry.getValue());}faded.clear();if(changed&&activity instanceof MainActivity)activity.findViewById(R.id.grid).post(()->((MainActivity)activity).refitGlass());}
    static void settings(Activity a){
        android.content.SharedPreferences p=ProfileStore.preferences(a);
        TextView quiet=a.findViewById(R.id.cinematicIdleChoice);
        quiet.setText(a.getString(R.string.cinematic_idle_state,p.getInt(IDLE,0)==0?a.getString(R.string.live_off):p.getInt(IDLE,0)+" s"));
        a.findViewById(R.id.previewIdleChoice).setOnClickListener(v->a.startActivity(new android.content.Intent(a,MainActivity.class).setAction(MainActivity.ACTION_IDLE_PREVIEW)));
        quiet.setOnClickListener(v->{int[] values={0,30,60,120};String[] names={a.getString(R.string.live_off),"30 s","60 s","120 s"};
            new AlertDialog.Builder(a).setTitle(R.string.cinematic_idle).setSingleChoiceItems(names,Arrays.binarySearch(values,p.getInt(IDLE,0)),(d,index)->{
                Map<String,Object> changes=new HashMap<>();changes.put(IDLE,values[index]);changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
                if(SetupValues.commit(p,changes)){d.dismiss();a.finish();}else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_SHORT).show();
            }).setNegativeButton(android.R.string.cancel,null).show();});
        TextView motion=a.findViewById(R.id.refinedMotionChoice);motion.setText(a.getString(R.string.refined_motion)+" · "+a.getString(p.getBoolean(MOTION,false)?R.string.live_on:R.string.live_off));
        motion.setOnClickListener(v->{if(CompanionControls.saveBoolean(p,MOTION,!p.getBoolean(MOTION,false)))a.finish();else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_SHORT).show();});
        choice(a,R.id.adaptiveHeaderChoice,R.string.home_text,R.array.home_text_options,p.getBoolean(HEADER,false)?Math.max(0,Math.min(2,p.getInt(INK,0)))+1:0,true);
        choice(a,R.id.appNamesChoice,R.string.app_names,R.array.app_name_options,Math.max(0,Math.min(3,p.getInt(NAMES,0))),false);
    }
    private static void choice(Activity a,int id,int title,int options,int current,boolean ink){
        String[] names=a.getResources().getStringArray(options);TextView row=a.findViewById(id);row.setText(a.getString(title)+" · "+names[current]);
        row.setOnClickListener(v->AppearanceSettings.choose(a,title,names,current,which->{
            android.content.SharedPreferences p=ProfileStore.preferences(a);Map<String,Object> changes=new HashMap<>();
            if(ink){changes.put(HEADER,which!=0);changes.put(INK,which<2?null:which-1);}else changes.put(NAMES,which==0?null:which);
            changes.put(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1);
            if(SetupValues.commit(p,changes))a.finish();else Toast.makeText(a,R.string.style_save_failed,Toast.LENGTH_SHORT).show();
        }));
    }
}
