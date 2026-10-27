package com.gathin.tvhome;

import java.io.FilterInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Bounded offline pack reader. Manifest/parser design adapted from DipsySu's MIT ThemePackage. */
final class ThemeArchive {
    static final long MAX_BYTES = 64L * 1024 * 1024;
    static final int MAX_JSON = 64 * 1024, MAX_ENTRIES = 64;
    static final long MAX_IMAGE = 8L * 1024 * 1024, MAX_VIDEO = 48L * 1024 * 1024;
    private static final Set<String> BOOLEAN = new HashSet<>(Arrays.asList("wallpaper", "dock_row", "dock_title",
            "always_show_names", "reduced_motion", "ambient_glow", "app_glow", "focus_overshoot", "dock_glass", "single_row", "dock_band_all"));
    static final class Pack {
        final String id, name; final int version; final Map<String, Object> visual;
        Pack(String id, String name, int version, Map<String, Object> visual) {
            this.id=id; this.name=name; this.version=version; this.visual=Collections.unmodifiableMap(visual);
        }
    }
    static Pack extract(InputStream input, File staging) throws IOException {
        if (input == null || !staging.isDirectory() || staging.list() == null || staging.list().length != 0) throw fail("Invalid staging directory");
        Set<String> names = new HashSet<>(); long total = 0; int entries = 0;
        Bounded source = new Bounded(input);
        try (ZipInputStream zip = new ZipInputStream(source)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String name=entry.getName();
                if (++entries > MAX_ENTRIES || !allowed(name, entry.isDirectory()) || !names.add(name.toLowerCase(Locale.ROOT))) throw fail("Unsafe or duplicate ZIP entry");
                if (entry.isDirectory()) {
                    if (zip.read() != -1) throw fail("Directory entry contains data");
                    zip.closeEntry(); continue;
                }
                long limit = name.endsWith(".json") ? MAX_JSON : "video.mp4".equals(name) ? MAX_VIDEO : MAX_IMAGE;
                if (entry.getSize() > limit) throw fail("File too large");
                File file = new File(staging, name);
                if (!file.getCanonicalPath().startsWith(staging.getCanonicalPath()+File.separator)) throw fail("Unsafe path");
                File parent=file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw fail("Cannot create asset directory");
                long written=0; byte[] buffer=new byte[8192];
                try (FileOutputStream out=new FileOutputStream(file)) {
                    for(int n;(n=zip.read(buffer))!=-1;) {
                        written+=n;total+=n;
                        if(written>limit||total>MAX_BYTES)throw fail("Theme exceeds storage limits");
                        out.write(buffer,0,n);
                    }
                }
                zip.closeEntry();
            }
            // ZipInputStream stops at the central directory. Count trailing bytes too, so a tiny pack
            // cannot bypass the archive quota by appending a large payload after its last local entry.
            byte[] tail = new byte[8192];
            while (source.read(tail) != -1) { }
        }
        return read(staging);
    }
    static Pack read(File directory) throws IOException {
        Map<String,Object> manifest=new Json(readSmall(new File(directory,"manifest.json"))).object();
        if(!manifest.keySet().equals(new HashSet<>(Arrays.asList("schema","id","name","version"))))throw fail("Unknown manifest fields");
        Object schema=manifest.get("schema"),id=manifest.get("id"),name=manifest.get("name"),version=manifest.get("version");
        if(!(schema instanceof Long)||((Long)schema)!=1||!(id instanceof String)||!((String)id).matches("[a-z0-9][a-z0-9-]{0,47}")
                ||!(name instanceof String)||((String)name).trim().isEmpty()||((String)name).length()>60
                ||!(version instanceof Long)||((Long)version)<1||((Long)version)>Integer.MAX_VALUE)throw fail("Invalid manifest");
        for(char c:((String)name).toCharArray())if(Character.isISOControl(c))throw fail("Invalid theme name");
        if(!new File(directory,"wallpaper.jpg").isFile())throw fail("A still fallback is required");
        Map<String,Object> config=new Json(readSmall(new File(directory,"config.json"))).object();
        for(Map.Entry<String,Object> setting:config.entrySet()) {
            String key=setting.getKey();Object value=setting.getValue();
            if(BOOLEAN.contains(key)){if(!(value instanceof Boolean))throw fail("Boolean required");continue;}
            if("home_style".equals(key)){if(!(value instanceof String)||!HomeStyles.valid((String)value))throw fail("Invalid Home style");continue;}
            if("glass_style".equals(key)){if(!(value instanceof String)||!Arrays.asList("glass","rim","halo","double").contains(value))throw fail("Invalid focus style");continue;}
            if("background_color".equals(key)){if(!(value instanceof String)||!((String)value).matches("#[A-Fa-f0-9]{6}"))throw fail("Invalid color");continue;}
            int min=0,max;
            switch(key){case "columns":min=4;max=7;break;case "rows_below_dock":max=3;break;case "tile_gap":max=32;break;
                case "dock_opacity":min=30;max=100;break;case "wallpaper_dim":max=90;break;
                case "wallpaper_rotation_minutes":max=1440;break;default:throw fail("Unsupported or non-visual setting: "+key);}
            if(!(value instanceof Long)||((Long)value)<min||((Long)value)>max)throw fail("Invalid visual value");
        }
        return new Pack((String)id,(String)name,((Long)version).intValue(),config);
    }
    static boolean allowed(String path, boolean directory) {
        if(path==null||path.contains("..")||path.indexOf('\\')>=0||path.startsWith("/"))return false;
        if(directory)return "wallpapers/".equals(path);
        return Arrays.asList("manifest.json","config.json","wallpaper.jpg","preview.jpg","video.mp4").contains(path)
                ||path.matches("wallpapers/[A-Za-z0-9][A-Za-z0-9_.-]*\\.(?i:jpg|jpeg|png|webp)");
    }
    static String readSmall(File file) throws IOException {
        if(!file.isFile()||file.length()==0||file.length()>MAX_JSON)throw fail("Missing or oversized JSON");
        byte[] bytes=Files.readAllBytes(file.toPath());
        if(bytes.length>MAX_JSON)throw fail("Oversized JSON");
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(java.nio.charset.CharacterCodingException e){throw fail("Invalid UTF-8");}
    }
    private static final class Bounded extends FilterInputStream {
        long read;Bounded(InputStream in){super(in);}
        @Override public int read() throws IOException {int v=in.read();if(v>=0&&++read>MAX_BYTES)throw fail("Archive too large");return v;}
        @Override public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,len);if(n>0&&(read+=n)>MAX_BYTES)throw fail("Archive too large");return n;}
    }
    static IOException fail(String message){return new IOException(message);}
    static void validateJsonObject(String text) throws IOException { new Json(text).object(); }
    private ThemeArchive(){}
    private static final class Json {
        final String s; int p;
        Json(String s) { this.s = s == null ? "" : s; }
        int depth, keys;
        Map<String, Object> object() throws IOException { Object v = value(); ws(); if (p != s.length() || !(v instanceof Map)) throw fail("malformed manifest JSON"); return (Map<String, Object>) v; }
        Object value() throws IOException {
            if (++depth > 32) throw fail("JSON too deep");
            ws(); if (p >= s.length()) throw fail("malformed JSON"); char c = s.charAt(p);
            try {
                if (c == '{') return obj(); if (c == '[') return array(); if (c == '"') return string();
                if (c == '-' || Character.isDigit(c)) return number();
                if (take("true")) return Boolean.TRUE; if (take("false")) return Boolean.FALSE; if (take("null")) return null;
                throw fail("malformed JSON");
            } finally { depth--; }
        }
        Map<String, Object> obj() throws IOException { expect('{'); Map<String,Object> out = new HashMap<>(); ws(); if (eat('}')) return out; while (true) { if (++keys > 128) throw fail("JSON too complex"); ws(); if (p >= s.length() || s.charAt(p) != '"') throw fail("malformed JSON"); String k=string(); ws(); expect(':'); if (out.containsKey(k)) throw fail("duplicate JSON key"); out.put(k,value()); ws(); if (eat('}')) return out; expect(','); } }
        Object array() throws IOException { expect('['); ws(); if (eat(']')) return new Object[0]; int n=0; while (true) { value(); if (++n > 1024) throw fail("JSON too complex"); ws(); if (eat(']')) return new Object[0]; expect(','); } }
        String string() throws IOException { expect('"'); StringBuilder b=new StringBuilder(); while (p<s.length()) { char c=s.charAt(p++); if(c=='"') return b.toString(); if(c<' ') throw fail("bad string"); if(c=='\\') { if(p>=s.length()) throw fail("bad escape"); char e=s.charAt(p++); if(e=='u') { if(p+4>s.length()) throw fail("bad escape"); try { b.append((char)Integer.parseInt(s.substring(p,p+4),16)); } catch(NumberFormatException x){ throw fail("bad escape"); } p+=4; } else if("\"\\/bfnrt".indexOf(e)>=0) b.append(e=='b'?'\b':e=='f'?'\f':e=='n'?'\n':e=='r'?'\r':e=='t'?'\t':e); else throw fail("bad escape"); } else b.append(c); } throw fail("unterminated string"); }
        Long number() throws IOException { int start=p; if(s.charAt(p)=='-')p++; if(p>=s.length()||!Character.isDigit(s.charAt(p)))throw fail("bad number"); if(s.charAt(p)=='0' && p + 1 < s.length() && Character.isDigit(s.charAt(p + 1))) throw fail("bad number"); while(p<s.length()&&Character.isDigit(s.charAt(p)))p++; if(p<s.length() && ".eE".indexOf(s.charAt(p))>=0) throw fail("integer required"); try{return Long.parseLong(s.substring(start,p));}catch(NumberFormatException e){throw fail("bad number");} }
        void ws(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;} boolean eat(char c){if(p<s.length()&&s.charAt(p)==c){p++;return true;}return false;} void expect(char c)throws IOException{ws();if(!eat(c))throw fail("malformed JSON");} boolean take(String v){if(s.startsWith(v,p)){p+=v.length();return true;}return false;}
    }
}
