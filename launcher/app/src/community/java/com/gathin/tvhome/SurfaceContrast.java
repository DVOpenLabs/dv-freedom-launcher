package com.gathin.tvhome;
/** Regional contrast with a stable safety floor for changing video frames. */
final class SurfaceContrast {
    static final int LIGHT=0xfff5f7fa,DARK=0xff182334;
    static double channel(int v){double x=v/255d;return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);}
    static double luminance(int c){return .2126*channel(c>>16&255)+.7152*channel(c>>8&255)+.0722*channel(c&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    static int blend(int background,int surface,int alpha){int out=0xff000000;for(int shift:new int[]{16,8,0})out|=(((background>>shift&255)*(255-alpha)+(surface>>shift&255)*alpha)/255)<<shift;return out;}
    static int alpha(int ink,int[] region){int surface=ink==LIGHT?0xff081020:0xffeef3fa;for(int alpha=190;alpha<=245;alpha+=5){boolean ok=true;if(region!=null)for(int c:region)if(contrast(ink,blend(c,surface,alpha))<4.5){ok=false;break;}if(ok)return alpha;}return 245;}
    private SurfaceContrast(){}
}
