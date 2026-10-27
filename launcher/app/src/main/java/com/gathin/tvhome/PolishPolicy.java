package com.gathin.tvhome;

/** Pure timing/contrast policy. One quiet state and one matched key-up; no background service. */
final class PolishPolicy {
    static int shade(int[] colors){
        if(colors==null||colors.length==0)return 0;long light=0;
        for(int rgb:colors)light+=((rgb>>16&255)*54+(rgb>>8&255)*183+(rgb&255)*19)/256;
        return Math.max(0,Math.min(170,(int)(light/colors.length)-70));
    }
    /** Compare solid ink against linear-light background samples; never return translucent text. */
    static int ink(int[] colors){
        if(colors==null||colors.length==0)return 0xfff5f7fa;
        double light=0;for(int c:colors)light+=luminance(c);light/=colors.length;
        double white=(luminance(0xfff5f7fa)+.05)/(light+.05),dark=(light+.05)/(luminance(0xff182334)+.05);
        return white>=dark?0xfff5f7fa:0xff182334;
    }
    private static double channel(int c){double x=c/255.0;return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);}
    private static double luminance(int c){return .2126*channel(c>>16&255)+.7152*channel(c>>8&255)+.0722*channel(c&255);}
    static boolean validSeconds(int seconds){return seconds==0||seconds==30||seconds==60||seconds==120;}
    static final class Idle {
        private long deadline;private boolean running;boolean quiet;private int swallowed=-1;
        void arm(long now,int seconds){running=seconds>0&&validSeconds(seconds);deadline=now+seconds*1000L;}
        boolean due(long now){if(running&&now>=deadline){running=false;quiet=true;return true;}return false;}
        boolean key(int code,boolean down,boolean navigation){
            if(code==swallowed){if(!down)swallowed=-1;return true;}
            if(!down||!quiet)return false;quiet=false;
            if(navigation){swallowed=code;return true;}return false;
        }
        void stop(){running=false;quiet=false;swallowed=-1;}
    }
    private PolishPolicy(){}
}
