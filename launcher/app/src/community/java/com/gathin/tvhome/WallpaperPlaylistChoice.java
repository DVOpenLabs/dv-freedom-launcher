package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.view.Gravity;
import android.widget.*;
import java.io.IOException;
import java.util.*;

/** Draft-only editor until Apply; entries are backgrounds from immutable local theme packs. */
final class WallpaperPlaylistChoice {
    static final String KEY="wallpaper_playlist_v1";
    static PlaylistPolicy.Spec read(Context c){try{return PlaylistPolicy.parse(ProfileStore.preferences(c).getString(KEY,""));}catch(RuntimeException invalid){return PlaylistPolicy.parse("");}}
    static boolean references(String raw,String token){try{return PlaylistPolicy.parse(raw).tokens.contains(token);}catch(RuntimeException invalid){return true;}}
    static List<ThemeLibrary.Entry> entries(Context c,PlaylistPolicy.Spec spec){
        Map<String,ThemeLibrary.Entry> all=new HashMap<>();for(ThemeLibrary.Entry e:ThemeLibrary.list(c))all.put(e.token,e);
        List<ThemeLibrary.Entry> result=new ArrayList<>();for(String token:spec.tokens){ThemeLibrary.Entry entry=all.get(token);if(entry!=null)result.add(entry);}return result;
    }
    static boolean enabled(Context c){PlaylistPolicy.Spec spec=read(c);return spec.enabled&&!entries(c,spec).isEmpty();}
    static void validateMedia(Context c,String raw)throws IOException{PlaylistPolicy.Spec spec=PlaylistPolicy.parse(raw);if(entries(c,spec).size()!=spec.tokens.size())throw new IOException("A playlist wallpaper is unavailable");}
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static void show(Activity a){new Editor(a).show();}
    private static final class Editor {
        final Activity a;final String profile;final Map<String,?> initial;final List<ThemeLibrary.Entry> library;final List<String> tokens;
        boolean enabled,shuffle;int seconds;final TextView[] controls=new TextView[5];
        Editor(Activity a){this.a=a;profile=ProfileStore.id();initial=ProfileStore.preferences(a).getAll();library=ThemeLibrary.list(a);PlaylistPolicy.Spec spec=read(a);tokens=new ArrayList<>();for(String token:spec.tokens)for(ThemeLibrary.Entry entry:library)if(token.equals(entry.token))tokens.add(token);enabled=spec.enabled;shuffle=spec.shuffle;seconds=spec.seconds;}
        void show(){
            LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(a,20),0,dp(a,20),dp(a,12));
            TextView hint=new TextView(a);hint.setText("Local backgrounds only. App layout and theme settings stay in place. Video is silent and pauses outside Home. Turn the playlist off to use your single wallpaper.");hint.setTextSize(15);hint.setTextColor(0xffd3d8e0);content.addView(hint);
            for(int i=0;i<controls.length;i++){final int at=i;TextView row=new TextView(a);controls[i]=row;row.setTextSize(17);row.setTextColor(-1);row.setPadding(dp(a,14),0,dp(a,14),0);row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setClickable(true);row.setBackgroundResource(R.drawable.settings_choice_bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(a,48));p.topMargin=dp(a,5);content.addView(row,p);row.setOnClickListener(v->{if(at==0){enabled=!enabled;refresh();}else if(at==1)choose();else if(at==2)interval();else if(at==3)order();else arrange();});}
            refresh();AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Wallpaper playlist").setView(content).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Apply",null).create();
            dialog.setOnShowListener(d->{controls[0].requestFocus();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->save(dialog));});dialog.show();
        }
        void refresh(){controls[0].setText("Playlist · "+(enabled?"On":"Off"));controls[1].setText("Choose wallpapers · "+tokens.size()+" selected");controls[2].setText("Change every · "+seconds/60+(seconds==60?" minute":" minutes"));controls[3].setText("Order · "+(shuffle?"Shuffle":"In sequence"));controls[4].setText("Arrange wallpapers");}
        void choose(){
            if(library.isEmpty()){new AlertDialog.Builder(a).setTitle("Import wallpapers first").setMessage("Use Manage offline themes to import local packs. Each pack provides a still picture and can include a silent video. Importing does not change your selected theme.").setPositiveButton(android.R.string.ok,null).show();return;}
            boolean[] checked=new boolean[library.size()];String[] names=new String[library.size()];for(int i=0;i<library.size();i++){ThemeLibrary.Entry e=library.get(i);checked[i]=tokens.contains(e.token);names[i]=e.pack.name+(new java.io.File(e.directory,"video.mp4").isFile()?" · Video available":" · Still picture");}
            new AlertDialog.Builder(a).setTitle("Choose up to eight wallpapers").setMultiChoiceItems(names,checked,(d,n,on)->checked[n]=on).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Use selected",(d,w)->{List<String> chosen=new ArrayList<>();for(String token:tokens)for(int i=0;i<library.size();i++)if(checked[i]&&token.equals(library.get(i).token))chosen.add(token);for(int i=0;i<library.size();i++)if(checked[i]&&!chosen.contains(library.get(i).token))chosen.add(library.get(i).token);tokens.clear();tokens.addAll(chosen);refresh();}).show();
        }
        void interval(){int[] values={60,300,900,1800,3600};String[] names={"1 minute","5 minutes","15 minutes","30 minutes","60 minutes"};int selected=0;for(int i=0;i<values.length;i++)if(values[i]==seconds)selected=i;new AlertDialog.Builder(a).setTitle("Change wallpaper every").setSingleChoiceItems(names,selected,(d,n)->{seconds=values[n];refresh();d.dismiss();}).setNegativeButton(android.R.string.cancel,null).show();}
        void order(){new AlertDialog.Builder(a).setTitle("Playback order").setSingleChoiceItems(new String[]{"In sequence","Shuffle without immediate repeats"},shuffle?1:0,(d,n)->{shuffle=n==1;refresh();d.dismiss();}).setNegativeButton(android.R.string.cancel,null).show();}
        void arrange(){
            if(tokens.isEmpty()){Toast.makeText(a,"Choose wallpapers first",Toast.LENGTH_SHORT).show();return;}
            String[] names=new String[tokens.size()];for(int i=0;i<names.length;i++){names[i]=tokens.get(i);for(ThemeLibrary.Entry e:library)if(e.token.equals(tokens.get(i)))names[i]=e.pack.name;}
            new AlertDialog.Builder(a).setTitle("Arrange wallpapers").setItems(names,(d,n)->new AlertDialog.Builder(a).setTitle(names[n]).setItems(new String[]{"Move earlier","Move later"},(x,which)->{int target=n+(which==0?-1:1);if(target>=0&&target<tokens.size())Collections.swap(tokens,n,target);arrange();}).setNegativeButton(android.R.string.cancel,null).show()).setNegativeButton("Done",null).show();
        }
        void save(AlertDialog dialog){
            try{
                if(!profile.equals(ProfileStore.id())||!initial.equals(ProfileStore.preferences(a).getAll()))throw new IllegalArgumentException("Home changed. Open the playlist again.");
                PlaylistPolicy.Spec spec=new PlaylistPolicy.Spec(enabled,seconds,shuffle,tokens);validateMedia(a,spec.encode());String value=spec.encode();if(value.equals(ProfileStore.preferences(a).getString(KEY,""))){dialog.dismiss();return;}
                ProductFeatures.beforeChange(a,"Change wallpaper playlist");Map<String,Object> changes=new HashMap<>();changes.put(KEY,value);changes.put(Config.SETTINGS_REVISION_KEY,ProfileStore.preferences(a).getLong(Config.SETTINGS_REVISION_KEY,0)+1);
                if(!SetupValues.commit(ProfileStore.preferences(a),changes))throw new IOException("Could not save wallpaper playlist");dialog.dismiss();a.finish();
            }catch(Exception failed){Toast.makeText(a,failed.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }
    private WallpaperPlaylistChoice(){}
}
