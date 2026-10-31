package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
/** Opt-in searchable Watch Next metadata. At most 96 rows read once per foreground session. */
final class ContentShelves {
    static final String KEY="private_content_shelf_enabled",PERMISSION="android.permission.READ_TV_LISTINGS";
    static final class Card {final String pkg,title,kind,poster;String description="";long duration,engagement;boolean watchNext;final Intent intent;final HomeCategories.App app;final int progress;Card(String p,String t,String k,Intent i,HomeCategories.App a,int progress,String poster){this.poster=poster;pkg=p;title=t;kind=k;intent=i;app=a;this.progress=progress;}}
    static boolean granted(Context c){return c.checkSelfPermission(PERMISSION)==PackageManager.PERMISSION_GRANTED;}
    static boolean enabled(Context c){return granted(c)&&ProfileStore.preferences(c).getBoolean(KEY,false);}
    static String status(Context c){return ProfileStore.preferences(c).getBoolean(KEY,false)?granted(c)?"On":"Permission unavailable":"Off";}
    static void settings(Activity a){String message="Read only the Watch Next and Preview Programs items that your apps choose to share with TV launchers. This can include titles and viewing progress. No account access, usage-access permission, cloud upload or background polling.\n\nHome reads local artwork. HTTPS posters need a separate opt-in inside DV Freedom Companions; image hosts then receive your IP and poster request. Home itself stays offline. Some streaming apps share nothing. Empty shelves are hidden. Glass, Clean and Cinema show an inline row; the current layout uses this optional browser.\n\nAndroid permission and each profile’s shelf choice are separate. Disabling clears the on-screen shelf; revoke system access in Android App permissions if you want to remove the permission too.";AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle("Content shelves").setMessage(message).setNegativeButton("Close",null);if(!granted(a))b.setPositiveButton("Allow TV listings",(d,w)->{try{a.requestPermissions(new String[]{PERMISSION},4901);}catch(Exception unavailable){Toast.makeText(a,"This device does not offer TV listings access",Toast.LENGTH_LONG).show();}});else b.setPositiveButton(enabled(a)?"Turn shelf off":"Show shelf in this profile",(d,w)->{if(!LauncherLock.require(a,()->settings(a)))return;boolean next=!enabled(a);Map<String,Object> v=new HashMap<>();v.put(KEY,next);v.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);if(SetupValues.commit(ProfileStore.preferences(a),v))a.finish();else Toast.makeText(a,"Could not save shelf choice",Toast.LENGTH_LONG).show();});if(enabled(a))b.setNeutralButton("Browse shared items",(d,w)->a.startActivity(new Intent(a,ContentShelfActivity.class)));b.show();}
    static Intent safeIntent(Context c,String raw,String pkg){try{if(raw==null||raw.length()>8192)return null;Intent parsed=Intent.parseUri(raw,Intent.URI_INTENT_SCHEME|Intent.URI_ANDROID_APP_SCHEME);if(parsed.getPackage()!=null&&!pkg.equals(parsed.getPackage())||parsed.getComponent()!=null&&!pkg.equals(parsed.getComponent().getPackageName()))return null;Uri data=parsed.getData();if(data==null||!ContentShelfPolicy.scheme(data.getScheme()))return null;Intent safe=new Intent(Intent.ACTION_VIEW,data).setPackage(pkg).addCategory(Intent.CATEGORY_BROWSABLE);ResolveInfo target=c.getPackageManager().resolveActivity(safe,0);if(target==null||target.activityInfo==null||!pkg.equals(target.activityInfo.packageName)||!target.activityInfo.exported||!target.activityInfo.enabled)return null;safe.setComponent(new ComponentName(pkg,target.activityInfo.name));return safe;}catch(Exception bad){return null;}}
    static List<Card> read(Context c,CancellationSignal cancel,Map<String,HomeCategories.App> visible,boolean[] unavailable){
        List<Card> result=new ArrayList<>();ShelfOptions.Spec options=ShelfOptions.read(c);
        Map<String,HomeCategories.App> allowed=new HashMap<>(visible);allowed.keySet().removeIf(pkg->!options.accepts(pkg));
        Set<String> seen=new HashSet<>();long now=System.currentTimeMillis();
        String[] columns={"package_name","title","watch_next_type","last_engagement_time_utc_millis","last_playback_position_millis","duration_millis","intent_uri","searchable","poster_art_uri",TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION};
        // Ordinary launchers cannot supply SQL selection/sort clauses to TvProvider. Rank bounded local results instead.
        if(options.watch)try(Cursor rows=c.getContentResolver().query(TvContract.WatchNextPrograms.CONTENT_URI,columns,null,null,null,cancel)){
            if(rows!=null){int scanned=0;while(scanned++<96&&rows.moveToNext()&&!cancel.isCanceled()){
                String pkg=rows.getString(0),title=rows.getString(1);int type=rows.getInt(2);
                if(rows.isNull(2)||rows.isNull(3)||rows.getInt(7)!=1||!ContentShelfPolicy.accepted(pkg,title,type,rows.getLong(3),rows.isNull(4)?-1:rows.getLong(4),rows.isNull(5)?-1:rows.getLong(5),now,allowed.keySet()))continue;
                Intent intent=safeIntent(c,rows.getString(6),pkg);if(intent==null)continue;
                HomeCategories.App app=allowed.get(pkg);Card card=new Card(pkg,title,ContentShelfPolicy.kind(type)+" · "+app.label,intent,app,ContentShelfPolicy.progress(type,rows.isNull(4)?-1:rows.getLong(4),rows.isNull(5)?-1:rows.getLong(5)),rows.getString(8));
                card.description=bounded(rows.getString(9));card.duration=rows.getLong(5);card.engagement=rows.getLong(3);card.watchNext=true;result.add(card);
            }}
        }catch(RuntimeException missing){unavailable[0]=true;result.clear();seen.clear();}
        result=uniqueNewest(result,seen);return readPreviews(c,cancel,allowed,limited(result,options.count),seen,unavailable);
    }
    static List<Card> readPreviews(Context c,CancellationSignal cancel,Map<String,HomeCategories.App> visible,List<Card> result,Set<String> seen,boolean[] unavailable){
        ShelfOptions.Spec options=ShelfOptions.read(c);if(cancel.isCanceled()||result.size()>=12||!options.preview)return result;
        List<Card> previews=new ArrayList<>();String[] columns={"package_name","title","intent_uri","poster_art_uri","browsable",TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,"duration_millis","_id"};
        try(Cursor rows=c.getContentResolver().query(TvContract.PreviewPrograms.CONTENT_URI,columns,null,null,null,cancel)){
            if(rows==null)return result;int scanned=0;
            while(scanned++<96&&rows.moveToNext()&&!cancel.isCanceled()){
                String pkg=rows.getString(0),title=rows.getString(1);
                if(!ContentShelfPolicy.preview(pkg,title,rows.getInt(4)==1,visible.keySet())||!options.accepts(pkg))continue;
                Intent intent=safeIntent(c,rows.getString(2),pkg);if(intent==null)continue;
                HomeCategories.App app=visible.get(pkg);Card card=new Card(pkg,title,"From "+app.label,intent,app,-1,rows.getString(3));card.description=bounded(rows.getString(5));card.duration=rows.getLong(6);card.engagement=rows.getLong(7);previews.add(card);
            }
        }catch(RuntimeException missing){unavailable[0]=true;/* A missing preview source does not remove Watch Next. */}
        result.addAll(uniqueNewest(previews,seen));return limited(result,options.count);
    }
    static List<Card> uniqueNewest(List<Card> cards,Set<String> seen){return ShelfSelection.newest(cards,c->c.pkg+"\n"+c.intent.getDataString(),c->c.engagement,seen);}
    static void newest(List<Card> cards){cards.sort((a,b)->Long.compare(b.engagement,a.engagement));}
    static String bounded(String value){return value==null?"":value.substring(0,Math.min(value.length(),1200));}
    static List<Card> limited(List<Card> cards,int limit){Map<String,Integer> counts=new HashMap<>();List<Card> out=new ArrayList<>();for(Card card:cards){int n=counts.getOrDefault(card.pkg,0);if(n<limit){out.add(card);counts.put(card.pkg,n+1);if(out.size()==12)break;}}return out;}
    static String freshness(Activity a,Card card){return card.watchNext&&card.engagement>0?"\nLast engagement shared by the app: "+android.text.format.DateFormat.getMediumDateFormat(a).format(new java.util.Date(card.engagement)):"\nSuggestion shared by the app; no viewing time supplied.";}
    static void details(Activity a,Card card){new AlertDialog.Builder(a).setTitle(card.title).setMessage(card.kind+freshness(a,card)+(card.duration>0?" · "+Math.round(card.duration/60000d)+" min":"")+(card.progress>=0?" · "+card.progress+"% watched":"")+(card.description.isEmpty()?"":"\n\n"+card.description)+"\n\nShared by "+card.app.label+". Availability and playback are controlled by that app.").setPositiveButton("Open title",(d,w)->{try{a.startActivity(card.intent);}catch(Exception unavailable){offerApp(a,card);}}).setNeutralButton("Open app",(d,w)->card.app.launch(a,Config.load(a))).setNegativeButton("Back",null).show();}
    static void offerApp(Activity a,Card card){new AlertDialog.Builder(a).setTitle("Title unavailable").setMessage("The app no longer opens this shared link. You can open "+card.app.label+" instead.").setNegativeButton("Back",null).setPositiveButton("Open app",(d,w)->card.app.launch(a,Config.load(a))).show();}
    static final class Row {
        final Activity a;
        final LinearLayout host;
        final View returnFocus;
        final String profile;
        final Config config;
        final boolean panel;
        final ExecutorService worker=Executors.newSingleThreadExecutor();
        CancellationSignal cancel=new CancellationSignal();
        volatile boolean active;boolean registered,renderedCards;String rendered="";int readGeneration,paintGeneration;
        final Handler main=new Handler(Looper.getMainLooper());
        final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity x,Bundle b){} public void onActivityStarted(Activity x){}
            public void onActivityStopped(Activity x){} public void onActivitySaveInstanceState(Activity x,Bundle b){}
            public void onActivityResumed(Activity x){if(x==a)resume();} public void onActivityPaused(Activity x){if(x==a)pause();}
            public void onActivityDestroyed(Activity x){if(x==a)close();}
        };
        boolean closed;
        View lastFocused;
        final Map<ImageView,Card> posters=new HashMap<>();
        final android.database.ContentObserver observer=new android.database.ContentObserver(main){public void onChange(boolean self){if(!current())return;for(Map.Entry<ImageView,Card> item:new HashMap<>(posters).entrySet())loadPoster(item.getKey(),item.getValue());}};
        void loadPoster(ImageView art,Card card){if(!current()||worker.isShutdown())return;int token=paintGeneration;worker.execute(()->{if(!current())return;android.graphics.Bitmap bitmap=OptionalPanels.poster(a,card.poster);if(bitmap!=null)main.post(()->{if(!current()||token!=paintGeneration){bitmap.recycle();return;}art.setImageBitmap(bitmap);art.setScaleType(ImageView.ScaleType.CENTER_CROP);posters.remove(art);});});}
        Row(Activity a,LinearLayout host,View returnFocus,boolean emptyMessage){
            this.a=a;this.host=host;this.returnFocus=returnFocus;panel=emptyMessage;
            profile=ProfileStore.id();config=Config.load(a);host.setVisibility(View.GONE);
            Map<String,HomeCategories.App> visible=new HashMap<>();
            for(HomeCategories.App app:HomeCategories.catalog(a,config))visible.put(app.pkg,app);
            a.getApplication().registerActivityLifecycleCallbacks(lifecycle);resume();
        }
        void resume(){if(active||closed||!profile.equals(ProfileStore.id())||!enabled(a))return;active=true;cancel=new CancellationSignal();
            if(!registered)try{a.getContentResolver().registerContentObserver(OptionalPanels.URI,true,observer);registered=true;}catch(Exception absent){}
            int generation=++readGeneration;CancellationSignal signal=cancel;Map<String,HomeCategories.App> visible=new HashMap<>();for(HomeCategories.App app:HomeCategories.catalog(a,config))visible.put(app.pkg,app);
            worker.execute(()->{boolean[] unavailable={false};List<Card> cards=read(a,signal,visible,unavailable);main.post(()->{if(generation!=readGeneration||!current())return;if(unavailable[0]&&cards.isEmpty()&&renderedCards&&host.getChildCount()>0){if(host.getChildAt(0) instanceof TextView)((TextView)host.getChildAt(0)).setText("Saved items · source unavailable; may be out of date");return;}render(cards,unavailable[0]);});});
        }
        void pause(){active=false;readGeneration++;cancel.cancel();main.removeCallbacksAndMessages(null);if(registered)try{a.getContentResolver().unregisterContentObserver(observer);}catch(Exception ignored){}registered=false;}

        boolean current(){return !closed&&active&&profile.equals(ProfileStore.id())&&enabled(a);}
        void render(List<Card> cards,boolean unavailable){
            if(!current())return;StringBuilder fingerprint=new StringBuilder(unavailable?"partial\n":"");for(Card card:cards)fingerprint.append(card.pkg).append("\u0000").append(card.intent.getDataString()).append("\u0000").append(card.title).append("\u0000").append(card.progress).append("\u0000").append(card.engagement).append("\u0000").append(card.description).append("\u0000").append(card.kind).append("\u0000").append(card.duration).append("\u0000").append(card.poster).append("\n");String next=fingerprint.toString();if(next.equals(rendered)&&host.getChildCount()>0)return;
            boolean hadFocus=host.hasFocus();Object focusTag=lastFocused==null?null:lastFocused.getTag();rendered=next;renderedCards=!cards.isEmpty();paintGeneration++;posters.clear();host.removeAllViews();
            if(a instanceof ExperienceHomeActivity)((ExperienceHomeActivity)a).shelfReady(!cards.isEmpty());
            if(cards.isEmpty()){
                if(hadFocus&&returnFocus!=null)returnFocus.requestFocus();host.setVisibility(View.GONE);
                if(panel){host.setVisibility(View.VISIBLE);TextView empty=label(unavailable?"The TV’s shared-content source is unavailable. Your streaming apps still work independently. Return to Home and try again later.":"No shared titles match this profile’s sources. Check Shelf sources & limits, then open an app and watch something. Some apps publish no TV listings. Items older than 30 days are omitted.",18);empty.setSingleLine(false);host.addView(empty);}
                return;
            }
            host.setVisibility(View.VISIBLE);
            if(!panel&&a.getResources().getConfiguration().fontScale>1.2f){
                TextView browse=label("Browse shared titles from your apps",20);browse.setPadding(dp(20),dp(18),dp(20),dp(18));browse.setFocusable(true);browse.setClickable(true);browse.setBackgroundResource(R.drawable.settings_choice_bg);browse.setOnClickListener(v->a.startActivity(new Intent(a,ContentShelfActivity.class)));host.addView(browse);return;
            }
            host.addView(label(unavailable?"From your apps · some sources unavailable":"From your apps",17));
            if(panel)host.addView(label("Read on this visit · hold OK for source, progress and details. Up to 96 entries per provider are scanned; this is not your complete watch history.",15));
            HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);
            LinearLayout line=new LinearLayout(a);line.setPadding(dp(5),dp(4),dp(5),dp(4));scroll.addView(line);host.addView(scroll);
            for(Card card:cards)addCard(line,card);
            if(hadFocus){View restore=null;for(int i=0;i<line.getChildCount();i++)if(line.getChildAt(i).getTag().equals(focusTag))restore=line.getChildAt(i);if(restore==null&&line.getChildCount()>0)restore=line.getChildAt(0);if(restore!=null)restore.requestFocus();}
        }
        void addCard(LinearLayout line,Card card){
            LinearLayout tile=new LinearLayout(a);tile.setOrientation(LinearLayout.VERTICAL);tile.setFocusable(true);tile.setClickable(true);
            tile.setTag(card.pkg+"\n"+card.intent.getDataString());
            tile.setPadding(dp(10),dp(8),dp(10),dp(8));tile.setBackgroundResource(R.drawable.settings_choice_bg);
            ImageView art=new ImageView(a);art.setImageDrawable(card.app.artwork(a,config));art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            tile.addView(art,new LinearLayout.LayoutParams(dp(150),dp(panel?84:42)));
            TextView title=label(card.title,15);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(title,new LinearLayout.LayoutParams(dp(150),panel?-2:dp(36)));
            TextView kind=label(card.kind,11);kind.setTextColor(0xffc5d4e4);kind.setSingleLine(true);kind.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(kind,new LinearLayout.LayoutParams(dp(150),panel?-2:dp(16)));
            if(card.progress>=0){ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgress(card.progress);progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(progress,new LinearLayout.LayoutParams(dp(150),dp(4)));}
            tile.setContentDescription(card.title+", "+card.kind+(card.progress>=0?", "+card.progress+" percent watched":""));
            tile.setOnClickListener(v->{try{a.startActivity(card.intent);}catch(Exception missing){offerApp(a,card);}});
            tile.setOnLongClickListener(v->{details(a,card);return true;});
            tile.setOnFocusChangeListener((v,on)->{if(on)lastFocused=v;});
            tile.setOnKeyListener((v,k,e)->navigate(line,tile,k,e));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(170),panel?-2:dp(118));lp.setMargins(0,dp(3),dp(8),0);line.addView(tile,lp);
            if(card.poster!=null&&card.poster.startsWith("https://"))posters.put(art,card);
            int paintToken=paintGeneration;CancellationSignal pictureCancel=cancel;if(card.poster!=null)worker.execute(()->{
                if(!current()||pictureCancel.isCanceled())return;android.graphics.Bitmap bitmap=ShelfArtwork.read(a,card.poster,card.pkg,pictureCancel);if(bitmap==null&&!pictureCancel.isCanceled())bitmap=OptionalPanels.poster(a,card.poster);final android.graphics.Bitmap loaded=bitmap;
                if(loaded!=null)main.post(()->{if(!current()||paintToken!=paintGeneration||pictureCancel.isCanceled()){loaded.recycle();return;}art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setImageBitmap(loaded);});
            });
        }
        boolean navigate(LinearLayout line,View tile,int key,KeyEvent event){
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){int at=line.indexOfChild(tile);return ExperiencePolicy.next(at,key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1,line.getChildCount(),line.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL)==at;}
            if(returnFocus==null||key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN)return false;
            if(panel&&key==KeyEvent.KEYCODE_DPAD_UP){returnFocus.requestFocus();return true;}
            int[] here=new int[2],back=new int[2];host.getLocationOnScreen(here);returnFocus.getLocationOnScreen(back);boolean below=here[1]>back[1];
            if(key==(below?KeyEvent.KEYCODE_DPAD_UP:KeyEvent.KEYCODE_DPAD_DOWN)){if(a instanceof ExperienceHomeActivity)((ExperienceHomeActivity)a).shelfReturn();else returnFocus.requestFocus();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_UP){View settings=a.findViewById(R.id.btnSettings);if(settings!=null)settings.requestFocus();}return true;
        }
        boolean focus(){if(closed||!host.isShown())return false;if(lastFocused!=null&&lastFocused.isShown())return lastFocused.requestFocus();return first(host);}
        boolean first(View view){if(view.isFocusable()&&view.isClickable())return view.requestFocus();if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(first(((ViewGroup)view).getChildAt(i)))return true;return false;}
        int dp(int value){return Math.round(value*(panel?a.getResources().getDisplayMetrics().density:a.getResources().getDisplayMetrics().widthPixels/960f));}
        TextView label(String value,int size){TextView text=new TextView(a);text.setTypeface(SettingsTypography.regular(a));text.setText(value);text.setTextColor(-1);if(panel)text.setTextSize(size);else text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,dp(size)*a.getResources().getConfiguration().fontScale);return text;}
        void close(){if(closed)return;pause();closed=true;try{a.getContentResolver().unregisterContentObserver(observer);}catch(Exception ignored){}posters.clear();cancel.cancel();worker.shutdownNow();main.removeCallbacksAndMessages(null);host.removeAllViews();host.setVisibility(View.GONE);a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);}
    }
}
