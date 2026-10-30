package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class HomeEntityPolicyTest {
    @Test public void informationalEntitiesNeverBecomeToggleCommands(){for(String id:new String[]{"sensor.temperature","binary_sensor.door","climate.living_room","cover.blind"}){assertTrue(HomeEntityPolicy.supported(id));assertFalse(HomeEntityPolicy.actionable(id));}assertTrue(HomeEntityPolicy.actionable("scene.evening"));assertFalse(HomeEntityPolicy.supported("script.arbitrary"));assertFalse(HomeEntityPolicy.supported("light.foo/../../services"));}
    @Test public void labelsAreBoundedAndClimateValuesRemainReported()throws Exception{JSONObject e=new JSONObject().put("entity_id","climate.room").put("state","heat").put("attributes",new JSONObject().put("friendly_name","Room\nclimate").put("current_temperature",20).put("temperature",21).put("temperature_unit","°C"));String text=HomeEntityPolicy.summary(e);assertTrue(text.contains("current 20"));assertTrue(text.contains("target 21"));assertTrue(text.contains("°C"));assertFalse(text.contains("\n"));}
}
