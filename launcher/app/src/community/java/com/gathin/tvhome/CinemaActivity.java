package com.gathin.tvhome;



import android.app.*;

import android.content.*;

import android.graphics.*;

import android.graphics.drawable.*;

import android.net.Uri;

import android.os.*;

import android.text.*;

import android.view.*;

import android.widget.*;

import org.json.*;

import java.io.*;

import java.nio.charset.StandardCharsets;

import java.util.*;

import java.util.concurrent.*;



/** Personal cinema editor, collections and local decision deck. Opening/browsing never saves. */

public final class CinemaActivity extends Activity {

    private static final int IMAGE=6120,IMPORT=6121,EXPORT=6122;

    private CinemaStore.Draft baseline;private JSONObject draft;private String page="studio",assetOwner="",assetRole="";

    private LinearLayout body,footer;private FrameLayout root,canvas;private ImageView collectionWorld;private TextView heading,subheading,status;private CinemaScene scene;

    private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService io=Executors.newSingleThreadExecutor();

    private int generation;private boolean resumed;private String last="",mood="Anything";private CinemaSpec.Title pick;private int countdown;private AlertDialog pendingDialog;

    private final Runnable timer=new Runnable(){public void run(){if(!resumed||!hasWindowFocus()||pick==null)return;if(--countdown<=0){open(pick);return;}status.setText("Opening in "+countdown+"… · Back cancels");main.postDelayed(this,1000);}};

    public static void open(Activity a,String page){a.startActivity(new Intent(a,CinemaActivity.class).putExtra("page",page));}

    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LauncherLocale.wrap(c));}

    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);ProductFeatures.init(this);baseline=new CinemaStore.Draft(this);try{draft=new JSONObject(baseline.spec.json.toString());if(saved!=null&&CinemaSpec.canRestoreDraft(saved.getString("profile"),saved.getString("revision"),baseline.profile,baseline.revision)&&saved.getString("draft")!=null){draft=new JSONObject(saved.getString("draft"));assetOwner=saved.getString("assetOwner","");assetRole=saved.getString("assetRole","");}else if(saved!=null)error(new IllegalStateException("Home changed; the previous draft was discarded."));}catch(Exception bad){draft=new JSONObject();}String token=getIntent().getStringExtra("cinema_token");if(token!=null&&saved==null)try{draft=new JSONObject(CinemaTransfer.take(this,token));getIntent().putExtra("page","preview");}catch(Exception expired){error(expired);finish();return;}page=getIntent().getStringExtra("page");if(page==null)page="studio";if(saved!=null)page=saved.getString("page","studio");show();}

    int dp(int n){return FreedomUi.dp(this,n);}

    private TextView text(String s,int n){return FreedomUi.text(this,s,n);}

    private void show(){stopMotion();root=new FrameLayout(this);root.setBackgroundColor(0xff101822);setContentView(root);LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(36),dp(22),dp(36),dp(18));root.addView(layout,new FrameLayout.LayoutParams(-1,-1));

        TextView brand=text("DV FREEDOM  /  CINEMA STUDIO",13);brand.setTextColor(0xff8ab9ce);brand.setLetterSpacing(.12f);layout.addView(brand);heading=text("",30);heading.setTypeface(SettingsTypography.medium(this));layout.addView(heading);subheading=text("",16);subheading.setTextColor(0xffb8c8d8);subheading.setPadding(0,dp(4),0,dp(14));layout.addView(subheading);

        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));footer=new LinearLayout(this);footer.setPadding(0,dp(12),0,0);layout.addView(footer);

        if(page.equals("studio"))studio();else if(page.equals("collection"))collection();else if(page.equals("moods"))deck();else if(page.equals("preview"))preview();else if(page.equals("titles"))titles();else{page="studio";studio();}root.post(()->{if(!FreedomUi.first(body))FreedomUi.first(footer);});

    }

    private void title(String h,String s){heading.setText(h);subheading.setText(s);}

    private void action(String label,Runnable r){FreedomUi.action(this,body,label,r);}

    private void foot(String label,Runnable r){LinearLayout host=new LinearLayout(this);footer.addView(host,new LinearLayout.LayoutParams(0,-2,1));FreedomUi.action(this,host,label,r);}

    private void go(String target){cancelPick();page=target;show();}

    private CinemaSpec spec(){return new CinemaSpec(draft.toString());}

    private boolean editable(){return ProductFeatures.allowEdit(this,()->go("studio"));}

    private void set(String key,Object value){try{if(!editable())return;draft.put(key,value);new CinemaSpec(draft.toString());show();}catch(Exception e){error(e);}}

    private void toggle(String key,String label,boolean value){action(label+"   ·   "+(value?"On":"Off"),()->set(key,!value));}

    private void studio(){CinemaSpec s=spec();title("Make it feel like yours","Changes stay in this draft until Apply. Your app order and wallpaper playlist stay saved.");

        action("Try Cinematic Showcase · unsaved preview",()->CinemaShowcaseActivity.open(this));
        action("Preview this draft",()->go("preview"));
        action("Preview a saved cinematic world · 20 seconds",()->CinemaWorldChoice.preview(this));
        toggle("worldsEnabled","Cinematic worlds · follow matching wallpapers",s.worldsEnabled);action("Configure eight cinematic worlds",()->CinemaWorldChoice.edit(this,draft,this::show));

        toggle("logos","Floating logos · Clean / Glass / Cinema",s.logos);action("Icon treatment   ·   "+s.iconStyle,()->new AlertDialog.Builder(this).setTitle("Theme emblems").setItems(new String[]{"Original / imported logos","Elven gold","Moonlit silver","Warm ember"},(d,n)->{try{draft.put("iconStyle",new String[]{"original","gold","silver","ember"}[n]);if(n>0)draft.put("logos",true);show();}catch(Exception e){error(e);}}).setNegativeButton("Cancel",null).show());toggle("glow","Selected-app colour glow",s.glow);

        action("App logos & taglines",this::apps);action("Scene atmosphere   ·   "+s.scene,()->new AlertDialog.Builder(this).setTitle("Scene atmosphere").setItems(new String[]{"Off","Aurora","Fireflies","Mist"},(d,n)->set("scene",new String[]{"off","aurora","fireflies","mist"}[n])).setNegativeButton("Cancel",null).show());

        action("Scene artwork   ·   "+(s.sceneArt.isEmpty()?"Use current Home wallpaper":"Local picture"),()->image("scene","sceneArt"));toggle("sceneBackground","Use scene artwork on Home · pauses saved playlist",s.sceneBackground);toggle("mascot","Focus-following starlight companion",s.mascot);

        action("Collections   ·   "+s.titles.size()+" titles",()->go("titles"));action("Collection display   ·   "+s.collectionStyle,()->new AlertDialog.Builder(this).setTitle("Collection display").setItems(new String[]{"Poster cards","Film books","Character cut-outs"},(d,n)->set("collectionStyle",new String[]{"posters","books","characters"}[n])).show());

        action("Mood names   ·   "+s.moods.size()+" moods",()->editMoods());toggle("autoOpen","Automatically open a mood pick after 3 seconds",s.autoOpen);

        action("Lord of the Rings + Hobbit starting point",this::fantasy);action("Import Cinema setup JSON",()->{if(editable())picker(IMPORT,"*/*");});action("Export setup JSON · artwork references only",()->{try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"dv-freedom-cinema.json"),EXPORT);}catch(Exception e){error(e);}});

        action("Open my collection",()->go("collection"));action("Can't decide?",()->go("moods"));action("Play · chess, Skyhop & daily frame",()->startActivity(new Intent(this,FreedomPlayActivity.class)));

        foot("Cancel",this::cancelEditor);foot("Apply this draft",this::apply);

    }

    private void apply(){try{baseline.save(this,draft.toString());Toast.makeText(this,"Cinema choices saved for this profile",Toast.LENGTH_SHORT).show();finish();}catch(Exception e){error(e);}}

    private void cancelEditor(){if(draft.toString().equals(baseline.spec.json.toString())){finish();return;}new AlertDialog.Builder(this).setTitle("Discard this draft?").setMessage("Your saved Home stays as it was. Imported artwork remains available locally.").setNegativeButton("Keep editing",null).setPositiveButton("Discard",(d,w)->finish()).show();}

    private void chooseApp(java.util.function.Consumer<HomeCategories.App> done){List<HomeCategories.App> apps=HomeCategories.catalog(this,Config.load(this));String[] labels=new String[apps.size()];for(int i=0;i<labels.length;i++)labels[i]=apps.get(i).label;new AlertDialog.Builder(this).setTitle("Choose an installed app").setItems(labels,(d,n)->done.accept(apps.get(n))).setNegativeButton("Cancel",null).show();}

    private void apps(){if(!editable())return;chooseApp(app->{new AlertDialog.Builder(this).setTitle(app.label).setItems(new String[]{"Import transparent logo","Edit tagline","Use original artwork"},(d,n)->{if(n==0)image(app.pkg,"appLogos");else if(n==1)input("Tagline",spec().taglines.getOrDefault(app.pkg,""),100,value->{try{JSONObject map=draft.optJSONObject("taglines");if(map==null)map=new JSONObject();if(value.trim().isEmpty())map.remove(app.pkg);else map.put(app.pkg,CinemaSpec.text(value,100));draft.put("taglines",map);show();}catch(Exception e){error(e);}});else{try{JSONObject map=draft.optJSONObject("appLogos");if(map!=null)map.remove(app.pkg);show();}catch(Exception e){error(e);}}}).show();});}

    private void input(String label,String value,int max,java.util.function.Consumer<String> save){EditText field=new EditText(this);field.setSingleLine(true);field.setText(value);field.setTextColor(0xfff5f7fa);field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});LinearLayout box=FreedomUi.body(this);box.addView(field);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(label).setView(box).setNegativeButton("Cancel",null).setPositiveButton("Use in draft",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{save.accept(field.getText().toString());dialog.dismiss();}catch(Exception e){field.setError(e.getMessage());}}));dialog.show();}

    private void editMoods(){if(!editable())return;input("Moods separated by commas",String.join(", ",spec().moods),400,value->{try{JSONArray next=new JSONArray();if(!value.trim().isEmpty())for(String mood:value.split(","))next.put(CinemaSpec.text(mood.trim(),32));JSONObject copy=new JSONObject(draft.toString());copy.put("moods",next);Set<String> names=new HashSet<>();for(int i=0;i<next.length();i++)names.add(next.getString(i));JSONArray titles=copy.optJSONArray("titles");if(titles!=null)for(int i=0;i<titles.length();i++){JSONObject t=titles.getJSONObject(i);JSONArray old=t.optJSONArray("moods"),keep=new JSONArray();if(old!=null)for(int j=0;j<old.length();j++)if(names.contains(old.getString(j)))keep.put(old.getString(j));t.put("moods",keep);}new CinemaSpec(copy.toString());draft=copy;show();}catch(Exception e){throw new IllegalArgumentException(e.getMessage());}});}

    private void fantasy(){if(!editable())return;new AlertDialog.Builder(this).setTitle("Middle-earth starting point").setMessage("Replace this draft's collection with the six Lord of the Rings and Hobbit films, a book shelf and mist. Choose an app you use; film links and artwork can be added later. This does not establish streaming availability or save anything yet.").setNegativeButton("Cancel",null).setPositiveButton("Choose app",(d,w)->chooseApp(app->{try{draft=new JSONObject(CinemaPresets.fantasy(app.pkg));show();}catch(Exception e){error(e);}})).show();}

    private void titles(){CinemaSpec s=spec();title("Your titles",s.collectionName+" · Add a title, its service and optional local art.");action("Add a title",()->{if(editable())chooseApp(app->input("Film or series name","",80,value->{try{JSONObject t=new JSONObject().put("id","title-"+UUID.randomUUID().toString()).put("name",CinemaSpec.text(value,80)).put("package",app.pkg).put("moods",new JSONArray());JSONArray all=draft.optJSONArray("titles");if(all==null)all=new JSONArray();if(all.length()>=CinemaSpec.MAX_TITLES)throw new IllegalArgumentException("Maximum 48 titles");all.put(t);draft.put("titles",all);go("titles");}catch(Exception e){throw new IllegalArgumentException(e.getMessage());}}));});action("Collection name",()->{if(editable())input("Collection name",s.collectionName,48,value->set("collectionName",CinemaSpec.text(value,48)));});for(CinemaSpec.Title t:s.titles)action(t.name,()->editTitle(t));foot("Back to draft",()->go("studio"));}

    private JSONObject titleObject(String id)throws Exception{JSONArray all=draft.optJSONArray("titles");if(all!=null)for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(id))return all.getJSONObject(i);throw new IllegalArgumentException("Title changed. Open it again.");}

    private void editTitle(CinemaSpec.Title t){if(!editable())return;new AlertDialog.Builder(this).setTitle(t.name).setItems(new String[]{"Title link","Choose service app","Choose moods","Poster image","Character cut-out","Backdrop image","Accent colour","Move earlier","Remove from collection"},(d,n)->{try{

        if(n==0)input("HTTPS title link · blank opens the app",t.url,2048,value->{try{if(!value.isEmpty())CinemaSpec.link(value);titleObject(t.id).put("url",value);show();}catch(Exception e){throw new IllegalArgumentException(e.getMessage());}});

        else if(n==1)chooseApp(app->{try{titleObject(t.id).put("package",app.pkg);show();}catch(Exception e){error(e);}});

        else if(n==2){String[] moods=spec().moods.toArray(new String[0]);boolean[] on=new boolean[moods.length];for(int i=0;i<on.length;i++)on[i]=t.moods.contains(moods[i]);FreedomUi.checklist(this,"Choose moods",moods,on,dialog->{try{JSONArray next=new JSONArray();for(int i=0;i<on.length;i++)if(on[i])next.put(moods[i]);titleObject(t.id).put("moods",next);dialog.dismiss();show();}catch(Exception e){error(e);}});}

        else if(n>=3&&n<=5)image(t.id,new String[]{"poster","character","backdrop"}[n-3]);

        else if(n==6)input("Accent · #RRGGBB",t.accent,7,value->{if(!value.matches("#[a-fA-F0-9]{6}"))throw new IllegalArgumentException("Use #RRGGBB");try{titleObject(t.id).put("accent",value);show();}catch(Exception e){error(e);}});

        else if(n==7){JSONArray all=draft.getJSONArray("titles");for(int i=1;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(t.id)){Object previous=all.get(i-1);all.put(i-1,all.get(i));all.put(i,previous);break;}show();}

        else new AlertDialog.Builder(this).setTitle("Remove "+t.name+"?").setNegativeButton("Cancel",null).setPositiveButton("Remove from draft",(x,w)->{try{JSONArray all=draft.getJSONArray("titles");for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(t.id)){all.remove(i);break;}show();}catch(Exception e){error(e);}}).show();

    }catch(Exception e){error(e);}}).setNegativeButton("Cancel",null).show();}

    private void picker(int code,String type){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType(type).addCategory(Intent.CATEGORY_OPENABLE),code);}catch(Exception e){error(new IllegalStateException("This TV has no file picker. Use a compatible document-provider app or desktop Setup."));}}

    private void image(String owner,String role){if(!editable())return;assetOwner=owner;assetRole=role;new AlertDialog.Builder(this).setTitle("Local artwork").setItems(new String[]{"Import a picture","Choose received artwork","Clear this artwork"},(dialog,n)->{if(n==0)picker(IMAGE,"image/*");else if(n==1){Map<String,String> library=CinemaAssets.library(this);if(library.isEmpty()){error(new IllegalStateException("No cinema artwork yet. Import a picture or send one from Setup and choose Add to Cinema artwork on TV."));return;}List<String> ids=new ArrayList<>(library.keySet());new AlertDialog.Builder(this).setTitle("Choose artwork").setItems(library.values().toArray(new String[0]),(d,at)->{try{assignAsset(owner,role,ids.get(at));show();}catch(Exception e){error(e);}}).show();}else try{assignAsset(owner,role,"");show();}catch(Exception e){error(e);}}).show();}

    private void assignAsset(String owner,String role,String id)throws Exception{if(role.equals("sceneArt"))draft.put(role,id);else if(role.equals("appLogos")){JSONObject map=draft.optJSONObject(role);if(map==null)map=new JSONObject();if(id.isEmpty())map.remove(owner);else map.put(owner,id);draft.put(role,map);}else titleObject(owner).put(role,id);}

    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();int run=++generation;String owner=assetOwner,role=assetRole;String profile=baseline.profile;

        io.execute(()->{try{String value;if(request==IMAGE)value=CinemaAssets.importImage(this,uri);else if(request==EXPORT){try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Cannot write setup");out.write(draft.toString(2).getBytes(StandardCharsets.UTF_8));}value="";}else{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot read setup");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];for(int n;(n=in.read(buffer))!=-1;){if(bytes.size()+n>CinemaSpec.MAX_TEXT)throw new IOException("Choose a JSON setup below 60 KB");bytes.write(buffer,0,n);}value=new CinemaSpec(bytes.toString("UTF-8")).json.toString();}CinemaAssets.validate(this,new CinemaSpec(value));}

            final String ready=value;main.post(()->{if(isDestroyed()||run!=generation||!profile.equals(ProfileStore.id()))return;try{if(request==IMAGE){if(role.equals("sceneArt"))draft.put(role,ready);else if(role.equals("appLogos")){JSONObject map=draft.optJSONObject(role);if(map==null)map=new JSONObject();map.put(owner,ready);draft.put(role,map);}else titleObject(owner).put(role,ready);Toast.makeText(this,"Artwork added to draft",Toast.LENGTH_SHORT).show();show();}else if(request==IMPORT){draft=new JSONObject(ready);go("preview");}else Toast.makeText(this,"Setup exported; artwork files remain on this TV",Toast.LENGTH_LONG).show();}catch(Exception e){error(e);}});

        }catch(Exception|OutOfMemoryError failure){main.post(()->{if(!isDestroyed()&&run==generation)error(failure);});}});

    }

    private Set<String> available(){Set<String> result=new HashSet<>();for(HomeCategories.App app:HomeCategories.catalog(this,Config.load(this)))if(FamilyMode.allowed(this,app.pkg))result.add(app.pkg);return result;}

    private List<CinemaSpec.Title> visible(){return spec().pool("Anything",available(),null);}

    private void deck(){title("Can't decide?","Pick a mood. Only titles whose apps are available in this profile are included.");CinemaSpec s=spec();CinemaMoodCard deck=new CinemaMoodCard(this,s,available(),this::spin);body.addView(deck,new LinearLayout.LayoutParams(-1,dp(235)));List<String> names=new ArrayList<>(s.moods);names.add("Anything");for(String m:names){int count=s.pool(m,available(),null).size();action(m+"   ·   "+count+" titles",()->spin(m));}if(s.titles.isEmpty())action("Add your first titles",()->go("titles"));foot("Back",()->go("studio"));}

    private void spin(String chosen){cancelPick();mood=chosen;List<CinemaSpec.Title> pool=spec().pool(mood,available(),last);if(pool.isEmpty()){error(new IllegalStateException("No titles with an available app in this mood. Add titles or choose another mood."));return;}pick=pool.get(new Random().nextInt(pool.size()));last=pick.id;body.removeAllViews();footer.removeAllViews();title(mood,"A little help choosing tonight's film.");Art art=new Art(this,pick,spec().collectionStyle);body.addView(art,new LinearLayout.LayoutParams(-1,dp(178)));status=text("Choosing…",21);status.setPadding(0,dp(10),0,dp(10));body.addView(status);int run=++generation;

        if(!Config.load(this).reducedMotion&&Config.load(this).animations){for(int i=0;i<6;i++){final int step=i;main.postDelayed(()->{if(run==generation&&resumed&&pick!=null){status.setText(pool.get(step%pool.size()).name);}},i*110L);}}

        main.postDelayed(()->{if(run!=generation||!resumed||pick==null)return;status.setText(pick.name);action("Open in its app",()->open(pick));action("Pick again",()->spin(mood));action("Back to moods",()->{cancelPick();show();});FreedomUi.first(body);if(spec().autoOpen){countdown=4;timer.run();}},Config.load(this).reducedMotion||!Config.load(this).animations?0:750);

    }

    private void collection(){CinemaSpec s=spec();Bitmap world=CinemaAssets.read(this,s.sceneArt);{ImageView image=new ImageView(this);collectionWorld=image;image.setImageBitmap(world);image.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(image,0,new FrameLayout.LayoutParams(-1,-1));View shade=new View(this);shade.setBackgroundColor(0xb0101822);root.addView(shade,1,new FrameLayout.LayoutParams(-1,-1));scene=new CinemaScene(this,s.scene,s.mascot,Config.load(this).animations&&!Config.load(this).reducedMotion);root.addView(scene,2,new FrameLayout.LayoutParams(-1,-1));if(resumed)scene.start();}title(s.collectionName,"Left / Right to browse · OK opens the title's app · hold OK for artwork and link choices");List<CinemaSpec.Title> titles=visible();if(titles.isEmpty()){action("Add titles to your collection",()->go("titles"));}else{HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setClipToPadding(false);LinearLayout rail=new LinearLayout(this);rail.setPadding(dp(8),dp(12),dp(8),dp(12));horizontal.addView(rail);body.addView(horizontal);FrameLayout backdrop=new FrameLayout(this);backdrop.setBackgroundColor(0xff192b3b);ImageView sceneImage=new ImageView(this);sceneImage.setScaleType(ImageView.ScaleType.CENTER_CROP);sceneImage.setAlpha(.32f);backdrop.addView(sceneImage,new FrameLayout.LayoutParams(-1,-1));TextView label=text("Choose a film",24);label.setPadding(dp(20),dp(14),dp(20),dp(14));backdrop.addView(label);body.addView(backdrop,new LinearLayout.LayoutParams(-1,dp(88)));

            for(CinemaSpec.Title t:titles){LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setFocusable(true);item.setClickable(true);item.setContentDescription(t.name);item.setBackgroundResource(R.drawable.freedom_settings_row);Art art=new Art(this,t,s.collectionStyle);item.addView(art,new LinearLayout.LayoutParams(dp(s.collectionStyle.equals("characters")?160:132),dp(185)));TextView name=text(t.name,15);name.setMaxLines(2);name.setMinHeight(dp(46));name.setGravity(Gravity.CENTER);item.addView(name);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(s.collectionStyle.equals("characters")?168:140),-2);lp.setMarginEnd(dp(12));rail.addView(item,lp);item.setOnFocusChangeListener((v,on)->{art.selected=on;if(!on){art.animate().cancel();art.setScaleX(1);art.setScaleY(1);}art.invalidate();if(on){label.setText(t.name);Bitmap image=CinemaAssets.read(this,t.backdrop);sceneImage.setImageBitmap(image);if(collectionWorld!=null){collectionWorld.setImageBitmap(image==null?CinemaAssets.read(this,s.sceneArt):image);collectionWorld.setAlpha(.7f);}art.animate().cancel();art.animate().scaleX(1.035f).scaleY(1.035f).setDuration(Config.load(this).reducedMotion?0:160).start();}});item.setOnClickListener(v->open(t));item.setOnLongClickListener(v->{editTitle(t);return true;});}}

        foot("Moods",()->go("moods"));foot("Edit collection",()->go("titles"));foot("Back",()->go("studio"));}

    private void preview(){CinemaSpec s=spec();CinemaWorld world=s.worldsEnabled&&!s.worlds.isEmpty()?s.worlds.get(0):null;title(world==null?"Preview your cinema":"Preview "+s.worlds.size()+" coordinated worlds",world==null?"This is a draft preview. Home wallpaper and app layout are not changed.":"Matching wallpapers will select their complete interface. This draft does not change your app order or rotation timing.");canvas=new FrameLayout(this);canvas.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff142a3a,0xff284356,0xff0a101c}));Bitmap background=CinemaAssets.read(this,s.sceneArt);if(background!=null){ImageView image=new ImageView(this);image.setImageBitmap(background);image.setScaleType(ImageView.ScaleType.CENTER_CROP);canvas.addView(image,new FrameLayout.LayoutParams(-1,-1));}scene=new CinemaScene(this,s.scene,s.mascot,Config.load(this).animations&&!Config.load(this).reducedMotion);canvas.addView(scene,new FrameLayout.LayoutParams(-1,-1));TextView caption=text("Your world. Your way.",25);caption.setPadding(dp(20),dp(16),0,0);canvas.addView(caption);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);FrameLayout.LayoutParams r=new FrameLayout.LayoutParams(-1,dp(82),Gravity.BOTTOM);r.bottomMargin=dp(14);canvas.addView(row,r);List<HomeCategories.App> catalog=HomeCategories.catalog(this,Config.load(this));Map<String,HomeCategories.App> byPackage=new LinkedHashMap<>();for(HomeCategories.App app:catalog)byPackage.put(app.pkg,app);List<HomeCategories.App> apps=new ArrayList<>();for(String pkg:DesktopLayout.favorites(Config.load(this),Order.load(Order.file(this))))if(byPackage.containsKey(pkg))apps.add(byPackage.get(pkg));if(apps.isEmpty())apps.addAll(catalog);int count=Math.min(8,apps.size());for(int i=0;i<count;i++){HomeCategories.App app=apps.get(i);LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setContentDescription(app.label);tile.setPadding(dp(3),dp(5),dp(3),dp(5));tile.setFocusable(true);tile.setClickable(true);tile.setOnClickListener(v->caption.setText(s.taglines.getOrDefault(app.pkg,app.label)));tile.setBackgroundResource(R.drawable.freedom_settings_row);ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);android.graphics.drawable.Drawable art=world==null?CinemaAssets.logo(this,app.pkg,s):CinemaWorldArt.logo(this,app.pkg,app.label,world);icon.setImageDrawable(art==null?app.artwork(this,Config.load(this)):art);tile.addView(icon,new LinearLayout.LayoutParams(-1,dp(46)));TextView label=text(app.label,12);label.setMaxLines(1);label.setEllipsize(TextUtils.TruncateAt.END);tile.addView(label);row.addView(tile,new LinearLayout.LayoutParams(0,-2,1));tile.setOnFocusChangeListener((v,on)->{if(on){caption.setText(s.taglines.getOrDefault(app.pkg,app.label));scene.focus((float)(row.indexOfChild(v)+.5)/Math.max(1,count),0xff75cfff);}});}body.addView(canvas,new LinearLayout.LayoutParams(-1,dp(260)));if(resumed)scene.start();foot("Back to draft",()->go("studio"));foot("Apply",this::apply);}

    private void open(CinemaSpec.Title t){if(t==null)return;cancelPick();if(!available().contains(t.pkg)||!FamilyMode.allowed(this,t.pkg)){error(new IllegalStateException("This app is unavailable in the current profile"));return;}try{if(!t.url.isEmpty()){CinemaSpec.link(t.url);try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(t.url)).setPackage(t.pkg));return;}catch(ActivityNotFoundException|SecurityException ignored){}}Intent fallback=getPackageManager().getLeanbackLaunchIntentForPackage(t.pkg);if(fallback==null)fallback=getPackageManager().getLaunchIntentForPackage(t.pkg);if(fallback==null)throw new IllegalStateException("This app cannot be opened");startActivity(fallback);}catch(Exception e){error(e);}}

    private void cancelPick(){generation++;main.removeCallbacks(timer);pick=null;}

    private void stopMotion(){if(scene!=null){scene.stop();scene=null;}}

    private void error(Throwable e){Toast.makeText(this,e.getMessage()==null?"Could not complete that step":e.getMessage(),Toast.LENGTH_LONG).show();}

    @Override public void onBackPressed(){if(pick!=null){cancelPick();show();}else if(!page.equals("studio"))go("studio");else cancelEditor();}

    @Override protected void onResume(){super.onResume();resumed=true;if(page.equals("moods")&&pick==null)show();else if(scene!=null)scene.start();}

    @Override protected void onPause(){resumed=false;cancelPick();main.removeCallbacksAndMessages(null);if(scene!=null)scene.stop();super.onPause();}
    @Override public void onWindowFocusChanged(boolean has){super.onWindowFocusChanged(has);if(!has&&pick!=null){main.removeCallbacks(timer);if(status!=null)status.setText(pick.name+" · choose Open when ready");}}

    @Override protected void onDestroy(){generation++;main.removeCallbacksAndMessages(null);stopMotion();io.shutdownNow();super.onDestroy();}

    @Override protected void onSaveInstanceState(Bundle out){out.putString("profile",baseline.profile);out.putString("revision",baseline.revision);out.putString("assetOwner",assetOwner);out.putString("assetRole",assetRole);out.putString("draft",draft.toString());out.putString("page",page);super.onSaveInstanceState(out);}

    static final class Art extends View {

        final CinemaSpec.Title title;final String style;final Paint p=new Paint(3);final Bitmap image;boolean selected;

        Art(Context c,CinemaSpec.Title t,String s){super(c);title=t;style=s;image=CinemaAssets.read(c,s.equals("characters")?t.character:t.poster);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}

        protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();if(w==0||h==0)return;int accent=Color.parseColor(title.accent);p.setShader(new LinearGradient(0,0,w,h,new int[]{accent,0xff132030},null,Shader.TileMode.CLAMP));if(style.equals("books")){c.drawRoundRect(w*.12f,h*.04f,w*.88f,h*.96f,5,5,p);p.setShader(null);p.setColor(0xffccb98a);p.setStrokeWidth(2);c.drawLine(w*.2f,h*.08f,w*.2f,h*.92f,p);}else{c.drawRoundRect(w*.04f,h*.04f,w*.96f,h*.96f,10,10,p);p.setShader(null);}if(image!=null){if(style.equals("characters")&&!selected){ColorMatrix mono=new ColorMatrix();mono.setSaturation(0);p.setColorFilter(new ColorMatrixColorFilter(mono));}else p.setColorFilter(null);p.setAlpha(selected?255:160);float k=Math.min(w*.86f/image.getWidth(),h*.88f/image.getHeight()),iw=image.getWidth()*k,ih=image.getHeight()*k;c.drawBitmap(image,null,new RectF((w-iw)/2,(h-ih)/2,(w+iw)/2,(h+ih)/2),p);p.setAlpha(255);p.setColorFilter(null);}else{p.setColor(0xffedf5ff);p.setTextSize(Math.min(w*.16f,h*.16f));p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(style.equals("books")?"FILM":title.name.substring(0,1),w/2,h*.42f,p);p.setTextSize(Math.min(w*.09f,h*.09f));c.drawText("YOUR COLLECTION",w/2,h*.65f,p);}if(selected){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xfff3f8ff);c.drawRoundRect(3,3,w-3,h-3,12,12,p);p.setStyle(Paint.Style.FILL);}}

    }

}
