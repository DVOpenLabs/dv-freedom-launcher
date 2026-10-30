package com.gathin.tvhome;
import java.util.Locale;
/** Keep the physical port beside a custom name, so two ports named Game stay distinguishable. */
final class InputLabelPolicy {
    static String title(CharSequence custom,CharSequence base,boolean hdmi){String port=InputNames.tidy(base),name=InputNames.tidy(custom);if(port!=null&&hdmi&&!port.toUpperCase(Locale.ROOT).contains("HDMI"))port="HDMI "+port;if(name==null||name.equals(port))return port;return port==null?name:name+" · "+port;}
    private InputLabelPolicy(){}
}
