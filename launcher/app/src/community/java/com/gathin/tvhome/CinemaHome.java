package com.gathin.tvhome;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Optional foreground-only overlays; wallpaper ownership remains in the existing controller. */
final class CinemaHome {
    static boolean ownBackground(Activity a){CinemaSpec s=CinemaStore.read(a);return s.sceneBackground&&!s.sceneArt.isEmpty()&&CinemaAssets.read(a,s.sceneArt)!=null;}
    private static final Map<Activity,Attachment> homes=new WeakHashMap<>();
    static void attach(Activity a){close(a);if(!(a instanceof MainActivity||a instanceof ExperienceHomeActivity||a instanceof CategoryHomeActivity))return;CinemaSpec s=CinemaStore.read(a);if(!s.sceneBackground&&s.scene.equals("off")&&!s.mascot&&!s.glow&&s.taglines.isEmpty())return;View v=a.findViewById(R.id.root);if(!(v instanceof FrameLayout))return;homes.put(a,new Attachment(a,(FrameLayout)v,s));}
    static void close(Activity a){Attachment old=homes.remove(a);if(old!=null)old.close();}
    static void selected(Activity a,String pkg,float x){Attachment host=homes.get(a);if(host!=null)host.selected(pkg,x);}
    static void quiet(Activity a,boolean on){Attachment host=homes.get(a);if(host!=null){host.tagline.setVisibility(on||host.tagline.getText().length()==0?View.GONE:View.VISIBLE);host.glow.setVisibility(on||!host.spec.glow?View.GONE:View.VISIBLE);}}
    private static final class Attachment {
        final Activity a;final FrameLayout root,layer;final CinemaSpec spec;final CinemaScene scene;final TextView tagline;final View glow;
        Attachment(Activity owner,FrameLayout parent,CinemaSpec s){a=owner;root=parent;spec=s;Config c=Config.load(a);layer=new FrameLayout(a);layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);layer.setFocusable(false);root.addView(layer,Math.max(0,root.getChildCount()-1),new FrameLayout.LayoutParams(-1,-1));if(s.sceneBackground){android.graphics.Bitmap image=CinemaAssets.read(a,s.sceneArt);if(image!=null){ImageView background=new ImageView(a);background.setImageBitmap(image);background.setScaleType(ImageView.ScaleType.CENTER_CROP);layer.addView(background,new FrameLayout.LayoutParams(-1,-1));}}scene=new CinemaScene(a,s.scene,s.mascot,c.animations&&!c.reducedMotion);layer.addView(scene,new FrameLayout.LayoutParams(-1,-1));glow=new View(a);glow.setVisibility(s.glow?View.VISIBLE:View.GONE);layer.addView(glow,new FrameLayout.LayoutParams(-1,(int)(a.getResources().getDisplayMetrics().heightPixels*.22f),Gravity.BOTTOM));tagline=FreedomUi.text(a,"",20);tagline.setGravity(Gravity.CENTER);tagline.setShadowLayer(3,0,2,0xff07101b);tagline.setMaxLines(2);tagline.setVisibility(View.GONE);FrameLayout.LayoutParams t=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);t.bottomMargin=(int)(a.getResources().getDisplayMetrics().heightPixels*.27f);layer.addView(tagline,t);scene.start();}
        void selected(String pkg,float x){int colour=0xff75cfff;for(CinemaSpec.Title t:spec.titles)if(t.pkg.equals(pkg)){colour=Color.parseColor(t.accent);break;}scene.focus(x,colour);glow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0,(colour&0xffffff)|0x28000000}));String text=spec.taglines.get(pkg);tagline.setText(text==null?"":text);tagline.setVisibility(text==null?View.GONE:View.VISIBLE);}
        void close(){scene.stop();root.removeView(layer);}
    }
}
