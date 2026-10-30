package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class WakeVolumePolicyTest {
    @Test public void volumePresetsNeverMapNavigationOrVolumeKeys(){VolumePresetPolicy p=new VolumePresetPolicy(VolumePresetPolicy.encode(new int[]{10,25,50,-1}));assertEquals(10,p.percent(183));assertEquals(-1,p.percent(186));for(int key:new int[]{3,4,19,20,21,22,23,24,25,26,178,224})assertEquals(-1,p.percent(key));}
    @Test public void presetLevelRespectsFixedMinimumAndMaximum(){assertEquals(4,VolumePresetPolicy.level(0,4,20));assertEquals(20,VolumePresetPolicy.level(100,4,20));assertEquals(12,VolumePresetPolicy.level(50,4,20));assertEquals(4,VolumePresetPolicy.level(50,4,4));}
    @Test public void malformedVolumeOrWakeRulesAreRefused(){for(String raw:new String[]{"[1,2,3]","[0,25,50,101]","[0,25,50,\"100\"]"})try{new VolumePresetPolicy(raw);fail();}catch(IllegalArgumentException expected){}for(String raw:new String[]{"{\"target\":\"shell:grant\",\"delay\":1}","{\"target\":\"org.example.app\",\"delay\":100}","{\"target\":\"org.example.app\",\"delay\":1,\"boot\":true}"})try{new WakeRulePolicy(raw);fail();}catch(IllegalArgumentException expected){}}
    @Test public void wakeRequiresVisibleHomeWithoutPreviewAndAllowedTarget(){assertTrue(WakeRulePolicy.due(true,100,200,true,false,true));assertFalse(WakeRulePolicy.due(true,100,200,false,false,true));assertFalse(WakeRulePolicy.due(true,100,200,true,true,true));assertFalse(WakeRulePolicy.due(true,100,200,true,false,false));assertFalse(WakeRulePolicy.due(true,200,200,true,false,true));assertFalse(WakeRulePolicy.due(false,100,200,true,false,true));}
}
