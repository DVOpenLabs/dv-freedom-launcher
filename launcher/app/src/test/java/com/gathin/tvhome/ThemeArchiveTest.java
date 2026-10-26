package com.gathin.tvhome;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class ThemeArchiveTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    static byte[] utf(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    static LinkedHashMap<String,byte[]> valid(String visual) {
        LinkedHashMap<String,byte[]> entries = new LinkedHashMap<>();
        entries.put("manifest.json", utf("{\"schema\":1,\"id\":\"night\",\"name\":\"Night\",\"version\":1}"));
        entries.put("config.json", utf(visual)); entries.put("wallpaper.jpg", new byte[]{1}); return entries;
    }
    static byte[] zip(Map<String,byte[]> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String,byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey())); out.write(entry.getValue()); out.closeEntry();
            }
        } return bytes.toByteArray();
    }
    ThemeArchive.Pack read(Map<String,byte[]> entries) throws Exception {
        return ThemeArchive.extract(new ByteArrayInputStream(zip(entries)), temporary.newFolder());
    }
    void rejected(Map<String,byte[]> entries) throws Exception {
        try { read(entries); fail("Unsafe pack accepted"); } catch (IOException expected) { }
    }
    @Test public void acceptsBoundedVisualPackAndOptionalMedia() throws Exception {
        Map<String,byte[]> entries=valid("{\"home_style\":\"carousel\",\"dock_opacity\":50,\"reduced_motion\":true}");
        entries.put("video.mp4", new byte[]{1,2}); entries.put("wallpapers/sea.webp", new byte[]{1});
        ThemeArchive.Pack pack=read(entries); assertEquals("night",pack.id);
        assertEquals("carousel",pack.visual.get("home_style")); assertEquals(50L,pack.visual.get("dock_opacity"));
    }
    @Test public void rejectsTraversalAbsoluteWindowsAndUnknownAssets() throws Exception {
        for(String path:new String[]{"../outside", "/outside", "C:\\outside", "wallpapers/../../outside", "fonts/test.ttf", "icons/a.png"}) {
            Map<String,byte[]> entries=valid("{}");entries.put(path,new byte[]{1});rejected(entries);
        }
    }
    @Test public void rejectsCaseCollisionsAndDirectoryPayload() throws Exception {
        Map<String,byte[]> entries=valid("{}"); entries.put("wallpapers/a.jpg",new byte[]{1});entries.put("wallpapers/A.jpg",new byte[]{2});rejected(entries);
        entries=valid("{}");entries.put("wallpapers/",new byte[]{1});rejected(entries);
    }
    @Test public void controlsPermissionsAndAppOrderCannotBeImported() throws Exception {
        for(String key:new String[]{"dock_apps","app_keys","local_profiles","stock_home_redirect","live_wallpapers","boot_to_live_tv"})
            rejected(valid("{\""+key+"\":true}"));
    }
    @Test public void rejectsWrongTypesRangesAndDuplicateJsonKeys() throws Exception {
        for(String config:new String[]{"{\"columns\":8}","{\"columns\":4.0}","{\"dock_opacity\":29}","{\"wallpaper\":1}","{\"home_style\":\"unknown\"}","{\"columns\":4,\"columns\":5}","{} trailing","{\"background_color\":\"#00FFFFFF\"}"}) rejected(valid(config));
    }
    @Test public void requiresStillAndExactManifest() throws Exception {
        Map<String,byte[]> entries=valid("{}");entries.remove("wallpaper.jpg");rejected(entries);
        entries=valid("{}");entries.put("manifest.json",utf("{\"schema\":2,\"id\":\"night\",\"name\":\"Night\",\"version\":1}"));rejected(entries);
        entries=valid("{}");entries.put("manifest.json",utf("{\"schema\":1,\"id\":\"night\",\"name\":\"Night\",\"version\":1,\"download\":\"url\"}"));rejected(entries);
    }
    @Test public void focusAndSpacingPacksRemainVisualOnly() throws Exception {
        ThemeArchive.Pack pack=read(valid("{\"glass_style\":\"double\",\"tile_gap\":16}"));
        assertEquals("double",pack.visual.get("glass_style")); assertEquals(16L,pack.visual.get("tile_gap"));
        rejected(valid("{\"tile_gap\":33}")); rejected(valid("{\"glass_style\":\"unknown\"}"));
        rejected(valid("{\"appearance_controls\":true}")); rejected(valid("{\"tv_button_picker\":true}"));
        rejected(valid("{\"hide_inactive_hdmi\":true}"));
    }
    @Test public void boundsZipEntryCountAndExpandedImages() throws Exception {
        Map<String,byte[]> entries=valid("{}");for(int i=0;i<62;i++)entries.put("wallpapers/"+i+".jpg",new byte[]{1});rejected(entries);
        entries=valid("{}");entries.put("wallpaper.jpg",new byte[(int)ThemeArchive.MAX_IMAGE+1]);rejected(entries);
    }
    @Test public void rejectsInvalidUtf8AndOversizedJson() throws Exception {
        Map<String,byte[]> entries=valid("{}");entries.put("config.json",new byte[]{(byte)0xc3,0x28});rejected(entries);
        entries=valid("{}");entries.put("config.json",new byte[ThemeArchive.MAX_JSON+1]);rejected(entries);
    }
    @Test public void corruptEntryChecksumCannotPublish() throws Exception {
        Map<String,byte[]> entries=valid("{}");ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream out=new ZipOutputStream(bytes)) {
            for(Map.Entry<String,byte[]> entry:entries.entrySet()) {
                ZipEntry record=new ZipEntry(entry.getKey());CRC32 crc=new CRC32();crc.update(entry.getValue());
                record.setMethod(ZipEntry.STORED);record.setSize(entry.getValue().length);record.setCrc(crc.getValue());
                out.putNextEntry(record);out.write(entry.getValue());out.closeEntry();
            }
        }
        byte[] corrupt=bytes.toByteArray();int firstPayload=30+"manifest.json".length();corrupt[firstPayload]^=1;
        File root=temporary.newFolder();
        try{ThemeLibrary.importPack(root,new ByteArrayInputStream(corrupt),dir->{});fail();}catch(IOException expected){}
        assertEquals(0,root.list().length);
    }
    @Test public void archiveQuotaIncludesIgnoredTrailingPayload() throws Exception {
        InputStream tail=new InputStream() {
            long remaining=ThemeArchive.MAX_BYTES;
            @Override public int read(){if(remaining==0)return -1;remaining--;return 0;}
            @Override public int read(byte[] b,int off,int len){if(remaining==0)return -1;int count=(int)Math.min(remaining,len);Arrays.fill(b,off,off+count,(byte)0);remaining-=count;return count;}
        };
        File root=temporary.newFolder();
        try{ThemeLibrary.importPack(root,new SequenceInputStream(new ByteArrayInputStream(zip(valid("{}"))),tail),dir->{});fail();}catch(IOException expected){}
        assertEquals(0,root.list().length);
    }
    @Test public void publicationIsImmutableAndFailedValidationLeavesOldPackAlone() throws Exception {
        File root=temporary.newFolder();byte[] archive=zip(valid("{}"));
        ThemeLibrary.Entry first=ThemeLibrary.importPack(root,new ByteArrayInputStream(archive),dir->{});
        byte[] before=Files.readAllBytes(new File(first.directory,"config.json").toPath());
        try { ThemeLibrary.importPack(root,new ByteArrayInputStream(archive),dir->{throw new IOException("Invalid bitmap");});fail(); }catch(IOException expected){}
        assertArrayEquals(before,Files.readAllBytes(new File(first.directory,"config.json").toPath()));
        assertEquals(1,ThemeLibrary.list(root).size());assertEquals(1,root.list().length);
        ThemeLibrary.Entry second=ThemeLibrary.importPack(root,new ByteArrayInputStream(archive),dir->{});
        assertNotEquals(first.token,second.token); assertEquals(2,ThemeLibrary.list(root).size());
    }
    @Test public void cleansInterruptedStagingAndRejectsNinthPack() throws Exception {
        File root=temporary.newFolder();File stale=new File(root,".import-"+UUID.randomUUID());assertTrue(stale.mkdir());
        Files.write(new File(stale,"partial").toPath(),new byte[]{1});byte[] archive=zip(valid("{}"));
        for(int i=0;i<8;i++)ThemeLibrary.importPack(root,new ByteArrayInputStream(archive),dir->{});
        assertFalse(stale.exists());
        try{ThemeLibrary.importPack(root,new ByteArrayInputStream(archive),dir->{});fail();}catch(IOException expected){}
        assertEquals(8,ThemeLibrary.list(root).size()); assertEquals(8,root.list().length);
    }
}
