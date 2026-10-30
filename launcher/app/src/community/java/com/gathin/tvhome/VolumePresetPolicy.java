package com.gathin.tvhome;
import org.json.*;
/** Four colour buttons only; navigation, power, Input and held volume are never remapped. */
final class VolumePresetPolicy {
    static final String KEY="volume_presets_v1";
    final int[] values={-1,-1,-1,-1};
    VolumePresetPolicy(String raw){try{if(raw==null||raw.length()>128)throw new IllegalArgumentException();if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);if(a.length()!=4)throw new IllegalArgumentException();for(int i=0;i<4;i++){if(!(a.get(i) instanceof Integer))throw new IllegalArgumentException();values[i]=a.getInt(i);if(values[i]<-1||values[i]>100)throw new IllegalArgumentException();}}catch(Exception bad){throw new IllegalArgumentException("Invalid volume presets");}}
    int percent(int key){return key>=183&&key<=186?values[key-183]:-1;}
    static int level(int percent,int minimum,int maximum){if(percent<0||percent>100||minimum<0||maximum<minimum)throw new IllegalArgumentException();return minimum+Math.round((maximum-minimum)*percent/100f);}
    static String encode(int[] values){if(values.length!=4)throw new IllegalArgumentException();try{String s=new JSONArray(values).toString();new VolumePresetPolicy(s);return s;}catch(Exception bad){throw new IllegalArgumentException("Invalid volume presets");}}
    private VolumePresetPolicy(){ }
}
