package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Matrix;
import android.view.TextureView;
import java.io.File;
import java.util.Arrays;

/** Per-library-item framing; separate from Home/profile preferences. */
final class StudioFraming {
    static final String[] MODES={"fill","fit","left","right","top","bottom"};
    static float[] bounds(String mode,int imageWidth,int imageHeight,int width,int height){
        if(!Arrays.asList(MODES).contains(mode)||imageWidth<=0||imageHeight<=0||width<=0||height<=0)throw new IllegalArgumentException("Invalid framing");
        float scale=mode.equals("fit")?Math.min((float)width/imageWidth,(float)height/imageHeight):Math.max((float)width/imageWidth,(float)height/imageHeight);
        float w=imageWidth*scale,h=imageHeight*scale;
        float x=mode.equals("left")?0:mode.equals("right")?width-w:(width-w)/2;
        float y=mode.equals("top")?0:mode.equals("bottom")?height-h:(height-h)/2;
        return new float[]{x,y,w,h};
    }
    static void save(Context c,String token,String mode){if(!ThemeLibrary.validToken(token)||!Arrays.asList(MODES).contains(mode))throw new IllegalArgumentException();if(!c.getSharedPreferences("wallpaper-studio",0).edit().putString(token,mode).commit())throw new IllegalStateException("Framing could not be saved");}
    static boolean video(Context c,File file,TextureView view,int w,int h){
        String mode=c.getSharedPreferences("wallpaper-studio",0).getString(file.getParentFile().getName(),"fill");
        if(mode.equals("fill")||view.getWidth()<=0||view.getHeight()<=0)return false;
        float[] box=bounds(mode,w,h,view.getWidth(),view.getHeight());Matrix matrix=new Matrix();matrix.setScale(box[2]/view.getWidth(),box[3]/view.getHeight());matrix.postTranslate(box[0],box[1]);view.setTransform(matrix);return true;
    }
    private StudioFraming(){}
}
