package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Remote-first chapters of a world: books, then a themed gallery. No new Home or permissions. */
public final class CinemaWorldActivity extends Activity {
    private CinemaWorld world;private FrameLayout root;private LinearLayout rail;private TextView caption;private CinemaWorldMotion atmosphere;private int page,selected;private float unit;private Config config;private View showcaseFocus;
    static void open(Activity a,CinemaWorld w){a.startActivity(new Intent(a,CinemaWorldActivity.class).putExtra("world",w.id));}
    protected void attachBaseContext(Context c){super.attachBaseContext(LauncherLocale.wrap(c));}
    protected void onCreate(Bundle saved){super.onCreate(saved);ProductFeatures.init(this);config=Config.load(this);String id=getIntent().getStringExtra("world");for(CinemaWorld w:CinemaStore.read(this).worlds)if(w.id.equals(id))world=w;if(world==null){finish();return;}if(saved!=null){page=saved.getInt("page");selected=saved.getInt("selected");}show();}
    int px(float n){return Math.round(n*unit);}private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));v.setTextColor(0xfff6ecda);v.setTypeface(Typeface.create(world.serif()?"serif":"sans-serif",Typeface.NORMAL));v.setShadowLayer(px(2),0,px(1),0xff050910);return v;}
    private void put(View v,float x,float y,float w,float h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(px(w),px(h));p.leftMargin=px(x);p.topMargin=px(y);root.addView(v,p);}
    private void show(){if(world.index<2||getIntent().getBooleanExtra("showcase",false)){showShowcase();return;}if(atmosphere!=null)atmosphere.stop();unit=getResources().getDisplayMetrics().widthPixels/960f;root=new FrameLayout(this);root.setBackgroundColor(0xff080e16);setContentView(root);
        Bitmap image=CinemaAssets.read(this,page==0?world.libraryArt:world.galleryArt);if(image==null&&page==1){for(ThemeLibrary.Entry e:ThemeLibrary.list(this))if(e.token.equals(world.wallpaper)){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;opts.inPreferredConfig=Bitmap.Config.RGB_565;image=BitmapFactory.decodeFile(new java.io.File(e.directory,Config.WALLPAPER_NAME).getPath(),opts);break;}}if(image!=null){ImageView scene=new ImageView(this);scene.setImageBitmap(image);scene.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(scene,new FrameLayout.LayoutParams(-1,-1));}else root.addView(new Room(this,world,page),new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xd9081018,0x17081018,0x17081018,0xe6081018}));root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        TextView name=text(world.name().toUpperCase(Locale.ROOT),12);name.setLetterSpacing(.18f);name.setTextColor(world.accent());put(name,46,28,760,25);
        TextView title=text(page==0?CinemaWorld.ROOMS[world.index]:CinemaWorld.GALLERIES[world.index],31);put(title,44,57,780,48);
        TextView back=text(page==0?"↑ Home":"↑ Library",15);back.setGravity(Gravity.CENTER);back.setFocusable(true);back.setClickable(true);back.setBackground(CinemaWorldArt.focus(world.accent()));put(back,812,38,100,42);back.setOnClickListener(v->up());back.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_DOWN&&k==KeyEvent.KEYCODE_DPAD_DOWN){focus();return true;}return false;});
        boolean gallery=page==1;String[] names=gallery?CinemaWorld.OBJECTS[world.index]:CinemaWorld.TITLES[world.index];selected=Math.max(0,Math.min(names.length-1,selected));
        rail=new LinearLayout(this);rail.setGravity(Gravity.CENTER);rail.setClipChildren(false);rail.setClipToPadding(false);float width=Math.min(gallery?230:142,850f/names.length);float height=gallery?210:world.index==0&&!world.libraryArt.isEmpty()?150:222;
        for(int i=0;i<names.length;i++){final int at=i;LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);cell.setFocusable(true);cell.setClickable(true);cell.setClipChildren(false);cell.setContentDescription(names[i]);CinemaWorldArt art=new CinemaWorldArt(this,world,i,gallery);cell.addView(art,new LinearLayout.LayoutParams(px(width-12),px(height)));TextView label=text(names[i],gallery?15:12);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setVisibility(View.INVISIBLE);cell.addView(label,new LinearLayout.LayoutParams(px(width-5),px(40)));rail.addView(cell,new LinearLayout.LayoutParams(px(width),px(height+48)));
            cell.setOnFocusChangeListener((v,on)->{art.selected=on;art.invalidate();v.animate().cancel();if(config.animations&&!config.reducedMotion)v.animate().scaleX(on?1.06f:1).scaleY(on?1.06f:1).rotationY(gallery?0:on?0:18).setDuration(180).start();else{v.setScaleX(on?1.04f:1);v.setScaleY(on?1.04f:1);v.setRotationY(0);}if(on){selected=at;caption.setText(names[at]);}});
            cell.setOnClickListener(v->{if(gallery&&world.index!=0)detail(names[at]);else watch(names[at],cell);});
            cell.setOnKeyListener((v,k,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT){selected=Math.max(0,Math.min(names.length-1,at+(k==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));focus();return true;}if(k==KeyEvent.KEYCODE_DPAD_DOWN){if(page==0){page=1;selected=0;show();}return true;}if(k==KeyEvent.KEYCODE_DPAD_UP){up();return true;}return false;});
        }put(rail,30,world.index==0&&page==0&&!world.libraryArt.isEmpty()?180:160,900,height+50);
        caption=text(names[selected],20);caption.setTextColor(world.accent());caption.setGravity(Gravity.CENTER);put(caption,70,420,820,34);
        TextView instruction=text(gallery?(world.index==0?"← → Explore   ·   OK Movie options   ·   ↑ Library":"← → Explore   ·   OK Details   ·   ↑ Library"):"← → Choose a story   ·   OK Movie options   ·   ↓ "+CinemaWorld.GALLERIES[world.index],13);instruction.setGravity(Gravity.CENTER);put(instruction,46,484,868,34);
        atmosphere=new CinemaWorldMotion(this,world,config.animations&&!config.reducedMotion);atmosphere.companion=false;atmosphere.setAlpha(.5f);root.addView(atmosphere,new FrameLayout.LayoutParams(-1,-1));atmosphere.start();root.post(this::focus);
    }
    private void showShowcase(){
        if(atmosphere!=null)atmosphere.stop();unit=getResources().getDisplayMetrics().widthPixels/960f;root=new FrameLayout(this);root.setBackgroundColor(0xff080e16);setContentView(root);rail=null;showcaseFocus=null;
        String art=page==0?world.libraryArt:world.galleryArt;if(page==0&&art.isEmpty()){try{art=CinemaSpec.asset(getIntent().getStringExtra("study")==null?"":getIntent().getStringExtra("study"));}catch(Exception ignored){art="";}}
        if(page==0&&art.isEmpty()&&world.index==1)for(CinemaWorld candidate:CinemaStore.read(this).worlds)if(candidate.index==0&&!candidate.libraryArt.isEmpty()){art=candidate.libraryArt;break;}
        Bitmap picture=CinemaAssets.read(this,art);if(picture==null&&page==1)for(ThemeLibrary.Entry entry:ThemeLibrary.list(this))if(entry.token.equals(world.wallpaper)){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;picture=BitmapFactory.decodeFile(new java.io.File(entry.directory,Config.WALLPAPER_NAME).getPath(),options);break;}
        if(picture!=null){ImageView image=new ImageView(this);image.setImageBitmap(picture);image.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(image,new FrameLayout.LayoutParams(-1,-1));}else root.addView(new Room(this,world,page),new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x60081018,0x00081018,0x16081018,0xb5081018}));root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        TextView back=text(page==0?"↑ HOME":"↑ LIBRARY",12);back.setLetterSpacing(.12f);back.setGravity(Gravity.CENTER);back.setFocusable(true);back.setClickable(true);back.setContentDescription(page==0?"Return to Home":"Return to library");put(back,420,20,120,34);back.setOnFocusChangeListener((v,on)->v.setBackground(on?CinemaWorldArt.focus(world.accent()):null));back.setOnClickListener(v->up());back.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_DOWN&&k==KeyEvent.KEYCODE_DPAD_DOWN){focus();return true;}return false;});
        caption=text("",21);caption.setTextColor(world.accent());caption.setGravity(Gravity.CENTER);put(caption,70,415,820,40);
        if(page==0){selected=Math.max(0,Math.min(CinemaWorld.TITLES[world.index].length-1,selected));CinemaShelfView shelf=new CinemaShelfView(this,world,selected,config.animations&&!config.reducedMotion,n->{selected=n;caption.setText(CinemaWorld.TITLES[world.index][n]);},()->watch(CinemaWorld.TITLES[world.index][selected],showcaseFocus),()->{page=1;selected=0;show();},this::up);shelf.setOnLongClickListener(v->{movieOptions(CinemaWorld.TITLES[world.index][selected],shelf);return true;});showcaseFocus=shelf;put(shelf,0,0,960,400);caption.setText(CinemaWorld.TITLES[world.index][selected]);}
        else{Lantern gallery=new Lantern(this);gallery.setFocusable(true);gallery.setClickable(true);gallery.setContentDescription(CinemaWorld.OBJECTS[world.index][selected]);gallery.setOnClickListener(v->{if(world.index==0)watch(CinemaWorld.OBJECTS[world.index][selected],v);else detail(CinemaWorld.OBJECTS[world.index][selected]);});gallery.setOnKeyListener((v,k,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT){selected=Math.max(0,Math.min(CinemaWorld.OBJECTS[world.index].length-1,selected+(k==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));caption.setText(CinemaWorld.OBJECTS[world.index][selected]);v.setContentDescription(CinemaWorld.OBJECTS[world.index][selected]);gallery.move(selected);return true;}if(k==KeyEvent.KEYCODE_DPAD_UP){up();return true;}return false;});showcaseFocus=gallery;put(gallery,0,0,960,410);caption.setText(CinemaWorld.OBJECTS[world.index][selected]);}
        TextView instruction=text(page==0?"← → Choose a book    ·    OK Open    ·    Hold OK Options    ·    ↓ "+CinemaWorld.GALLERIES[world.index]:"← → Move the lantern    ·    OK Explore    ·    ↑ Library",12);instruction.setGravity(Gravity.CENTER);put(instruction,46,489,868,30);
        atmosphere=new CinemaWorldMotion(this,world,config.animations&&!config.reducedMotion);atmosphere.companion=false;atmosphere.setAlpha(.4f);root.addView(atmosphere,new FrameLayout.LayoutParams(-1,-1));atmosphere.start();if(config.animations&&!config.reducedMotion){root.setAlpha(0);root.animate().alpha(1).setDuration(180).start();}root.post(this::focus);
    }
    /** A focus-triggered light move; the supplied creatures themselves remain still. */
    private final class Lantern extends View {
        final Paint paint=new Paint(3);float at=selected;android.animation.ValueAnimator travel;
        Lantern(Context context){super(context);}
        void move(int next){
            if(travel!=null)travel.cancel();float from=at;
            if(!config.animations||config.reducedMotion){at=next;invalidate();return;}
            travel=android.animation.ValueAnimator.ofFloat(from,next);travel.setDuration(280);
            travel.addUpdateListener(a->{at=(float)a.getAnimatedValue();invalidate();});travel.start();
        }
        protected void onDraw(Canvas canvas){
            float w=getWidth(),h=getHeight(),x=w*(.22f+at*.29f),y=h*.58f,r=w*.18f;
            paint.setShader(new RadialGradient(x,y,r,new int[]{0x40ffe0a2,0x0bffe0a2,0x00ffe0a2},new float[]{0,.55f,1},Shader.TileMode.CLAMP));canvas.drawCircle(x,y,r,paint);paint.setShader(null);
            paint.setColor(world.accent());paint.setAlpha(100);paint.setStrokeWidth(px(1));paint.setStyle(Paint.Style.STROKE);canvas.drawArc(x-w*.055f,h*.83f,x+w*.055f,h*.87f,0,180,false,paint);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
            if(world.index!=0)CinemaWorldMotion.drawSymbol(canvas,paint,world.index,x,y,h*.055f,world.accent(),selected);
        }
        void stop(){if(travel!=null)travel.cancel();}
        protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    }
    private void focus(){if(showcaseFocus!=null){if(showcaseFocus.isInTouchMode())showcaseFocus.requestFocusFromTouch();else showcaseFocus.requestFocus();}else if(rail!=null&&rail.getChildCount()>selected){View v=rail.getChildAt(selected);if(v.isInTouchMode())v.requestFocusFromTouch();else v.requestFocus();}}
    private void up(){if(page==1){page=0;selected=0;show();}else finish();}
    private void watch(String title,View book){
        CinemaSpec cinema=CinemaStore.read(this);for(HomeCategories.App app:HomeCategories.catalog(this,config))if(FamilyMode.allowed(this,app.pkg)){CinemaSpec.Title target=cinema.worldTarget(world,page,selected,app.pkg);if(target!=null){openMovieLink(app,target);return;}}
        movieOptions(title,book);
    }
    private void openMovieLink(HomeCategories.App app,CinemaSpec.Title target){
        try{CinemaSpec.link(target.url);startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(target.url)).setPackage(app.pkg));}
        catch(Exception missing){new AlertDialog.Builder(this).setTitle("This app could not open the saved link").setMessage("Your link is still saved. Open the app to find your movie, or hold OK on the book to change its link.").setNegativeButton("Back to library",null).setPositiveButton("Open "+app.label,(dialog,which)->app.launch(this,config)).show();}
    }
    private void movieOptions(String title,View book){
        List<HomeCategories.App> apps=new ArrayList<>();for(HomeCategories.App app:HomeCategories.catalog(this,config)){String p=app.pkg.toLowerCase(Locale.ROOT);if(FamilyMode.allowed(this,app.pkg)&&(p.contains("netflix")||p.contains("amazon")||p.contains("youtube")||p.contains("apple")||p.contains("disney")||p.contains("hotstar")||p.contains("fandango")||p.contains("crunchy")))apps.add(app);}
        if(apps.isEmpty()){Toast.makeText(this,"Add a streaming app to this profile first",Toast.LENGTH_LONG).show();return;}
        CinemaSpec cinema=CinemaStore.read(this);List<String> labels=new ArrayList<>();
        for(HomeCategories.App app:apps)labels.add(cinema.worldTarget(world,page,selected,app.pkg)==null?(CinemaMovieSearch.youtube(app.pkg)?"Find this movie on YouTube":"Open "+app.label+" · find movie in app"):"Open saved movie link · "+app.label);
        labels.add("Add or change this movie's link");
        new AlertDialog.Builder(this).setTitle(title+" · Movie options").setItems(labels.toArray(new String[0]),(d,n)->{
            if(n==apps.size()){new AlertDialog.Builder(this).setTitle("Which app opens your movie link?").setItems(apps.stream().map(a->a.label).toArray(String[]::new),(dialog,at)->editMovieLink(title,apps.get(at))).setNegativeButton("Cancel",null).show();return;}
            HomeCategories.App app=apps.get(n);CinemaSpec.Title target=CinemaStore.read(this).worldTarget(world,page,selected,app.pkg);
            if(target==null){if(CinemaMovieSearch.youtube(app.pkg))try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(CinemaMovieSearch.url(world,page,selected))).setPackage(app.pkg));return;}catch(Exception unsupported){Toast.makeText(this,"YouTube could not open title search; opening the app",Toast.LENGTH_LONG).show();}else Toast.makeText(this,"No movie link saved. Find this title inside "+app.label,Toast.LENGTH_LONG).show();app.launch(this,config);return;}
            openMovieLink(app,target);
        }).setNegativeButton("Back to library",null).setOnDismissListener(d->focus()).show();
    }
    private void editMovieLink(String title,HomeCategories.App app){
        if(!ProductFeatures.allowEdit(this,()->{}))return;CinemaStore.Draft baseline=new CinemaStore.Draft(this);String id=world.targetId(page,selected);CinemaSpec.Title current=baseline.spec.worldTarget(world,page,selected,app.pkg);
        EditText field=new EditText(this);field.setSingleLine(true);field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});field.setText(current==null?"":current.url);field.setHint("https://…");field.setContentDescription("Movie link for "+title);
        LinearLayout body=FreedomUi.body(this);TextView explanation=FreedomUi.text(this,"Paste the movie's Share link from "+app.label+". The app checks your account and ownership. This saves a shortcut; it does not buy or start the movie.",16);body.addView(explanation);body.addView(field);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title+" · "+app.label).setView(body).setNegativeButton("Cancel",null).setPositiveButton("Save movie link",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{
            String url=field.getText().toString().trim();CinemaSpec.link(url);org.json.JSONObject draft=new org.json.JSONObject(baseline.spec.json.toString());org.json.JSONArray titles=draft.optJSONArray("titles");if(titles==null)titles=new org.json.JSONArray();int at=-1;for(int i=0;i<titles.length();i++)if(titles.getJSONObject(i).getString("id").equals(id)){at=i;break;}if(at<0&&titles.length()>=CinemaSpec.MAX_TITLES)throw new IllegalArgumentException("Your 48-title collection is full");org.json.JSONObject target=new org.json.JSONObject().put("id",id).put("name",title).put("package",app.pkg).put("url",url).put("moods",new org.json.JSONArray());if(at<0)titles.put(target);else{org.json.JSONObject old=titles.getJSONObject(at);old.put("package",app.pkg).put("url",url);}draft.put("titles",titles);baseline.save(this,draft.toString());dialog.dismiss();Toast.makeText(this,"Movie link saved",Toast.LENGTH_SHORT).show();
        }catch(Exception bad){field.setError(bad.getMessage());}}));dialog.show();
    }
    private void detail(String object){String text=world.index==0?"A quiet corner of the wizarding greenhouse. Use the arrows to move the lantern focus between its three guardians.":world.index==3?"An atmosphere of lamps, lotus petals and warm sandstone. This world uses scenery and symbolic ornaments, without figures.":"A symbolic object from the "+world.name()+" world. Move left or right to explore this scene.";new AlertDialog.Builder(this).setTitle(object).setMessage(text).setPositiveButton("Back to gallery",null).show();}
    public void onBackPressed(){up();}protected void onResume(){super.onResume();if(atmosphere!=null)atmosphere.start();}protected void onPause(){if(atmosphere!=null)atmosphere.stop();if(showcaseFocus instanceof Lantern)((Lantern)showcaseFocus).stop();super.onPause();}protected void onDestroy(){if(atmosphere!=null)atmosphere.stop();super.onDestroy();}protected void onSaveInstanceState(Bundle b){b.putInt("page",page);b.putInt("selected",selected);super.onSaveInstanceState(b);}
    private static final class Room extends View {
        final CinemaWorld world;final int page;final Paint p=new Paint(3);Room(Context c,CinemaWorld w,int n){super(c);world=w;page=n;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();int a=world.accent();p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff080f18,world.index<4?0xff30271e:0xff102d3c,0xff070c14},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(0xff090f15);
            for(int i=0;i<7;i++){float x=w*(.04f+i*.155f);c.drawRect(x,h*.12f,x+w*.009f,h*.94f,p);}p.setColor(0xff614c32);if(world.index<4){c.drawRect(w*.025f,h*.72f,w*.975f,h*.75f,p);c.drawRect(w*.025f,h*.76f,w*.975f,h*.78f,p);}else{p.setColor(a&0x33ffffff);p.setStrokeWidth(2);for(int i=0;i<8;i++)c.drawLine(w*.02f,h*(.16f+i*.09f),w*.98f,h*(.16f+i*.09f),p);}
            p.setStyle(Paint.Style.STROKE);p.setColor((a&0xffffff)|0x50000000);p.setStrokeWidth(2);if(page==1){for(int i=0;i<3;i++){float x=w*(.23f+i*.27f);c.drawArc(x-w*.09f,h*.14f,x+w*.09f,h*.65f,180,180,false,p);c.drawLine(x-w*.09f,h*.4f,x-w*.09f,h*.8f,p);c.drawLine(x+w*.09f,h*.4f,x+w*.09f,h*.8f,p);}}p.setStyle(Paint.Style.FILL);
        }
    }
}
