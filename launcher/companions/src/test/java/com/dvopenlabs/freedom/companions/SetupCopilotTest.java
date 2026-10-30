package com.dvopenlabs.freedom.companions;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class SetupCopilotTest {
    @Test public void modelIdCannotBecomeAUrlOrPath(){for(String s:new String[]{"https://evil.test","gemini-test/../account","gemini-x?key=bad","../models"})assertFalse(SetupCopilotActivity.valid("k".repeat(30),s,"larger text"));assertTrue(SetupCopilotActivity.valid("k".repeat(30),"gemini-example","larger text"));}
    @Test public void requestHasNoToolsOrInventoryAndOutputIsCapped()throws Exception{JSONObject p=SetupCopilotActivity.payload("larger text");assertFalse(p.has("tools"));assertFalse(p.toString().contains("example.netflix"));assertEquals(1024,p.getJSONObject("generationConfig").getInt("maxOutputTokens"));assertEquals("application/json",p.getJSONObject("generationConfig").getString("responseMimeType"));}
}
