package com.gathin.tvhome;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class IconPackFormatTest {
    private IconPackFormat.Mapping parse(String text)throws Exception{return IconPackFormat.parse(text.getBytes(StandardCharsets.UTF_8));}
    @Test public void exactActivityOverridesExplicitPackageFallback()throws Exception{
        var m=parse("<resources><item component='ComponentInfo{org.example.app/}' drawable='fallback'/><item component='ComponentInfo{org.example.app/.Tv}' drawable='screen'/></resources>");
        assertEquals("screen",m.find("org.example.app","org.example.app.Tv"));assertEquals("fallback",m.find("org.example.app",".Other"));assertNull(m.find("org.example.other",".Tv"));
    }
    @Test public void malformedRowsDoNotHideLaterValidRows()throws Exception{
        var m=parse("<resources><item component='ComponentInfo{org.example.app/.Tv}' drawable=''/><item component='ComponentInfo{org.example.app/.Tv}' drawable='valid_icon'/><item component='ComponentInfo{org.example.app/.Tv}' drawable='later'/><item component='ComponentInfo{/}' drawable='bad'/><item component='ComponentInfo{org.example.bad/.Tv}' drawable='../escape'/></resources>");
        assertEquals("valid_icon",m.find("org.example.app",".Tv"));assertEquals(1,m.icons.size());
    }
    @Test public void noExternalEntityOrDtdIsAccepted(){for(String xml:new String[]{"<!DOCTYPE a SYSTEM 'file:///must-not-open'><resources/>","<!DOCTYPE a [<!ENTITY x 'expanded'>]><resources/>","<!ENTITY x SYSTEM 'https://example.invalid/no-request'>"})assertThrows(Exception.class,()->parse(xml));}
    @Test public void bytesNestingAndItemCountsAreBounded(){assertThrows(Exception.class,()->IconPackFormat.parse(new byte[IconPackFormat.MAX_BYTES+1]));assertThrows(Exception.class,()->parse("<r>".repeat(17)+"</r>".repeat(17)));var b=new IconPackFormat.Builder();for(int i=0;i<IconPackFormat.MAX_ITEMS;i++)b.item(null,null);assertThrows(IllegalArgumentException.class,()->b.item(null,null));}
    @Test public void invalidUtf8AndMalformedXmlFailBeforeImport(){assertThrows(Exception.class,()->IconPackFormat.parse(new byte[]{(byte)0xc3}));assertThrows(Exception.class,()->parse("<resources><item>"));}
}
