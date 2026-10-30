package com.gathin.tvhome;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Local profile data, intentionally independent of theme files and installed-app availability. */
final class FolderPolicy {
    static final String KEY="freedom_folders_v1";
    static final int MAX=16, MEMBERS=32, BYTES=16384;
    static final class Folder {
        final String id,name; final List<String> apps;
        Folder(String id,String name,List<String> apps){
            if(id==null||!id.matches("[a-f0-9]{32}"))throw new IllegalArgumentException("Invalid folder ID");
            this.name=SetupValues.name(name);this.id=id;
            if(apps==null||apps.isEmpty()||apps.size()>MEMBERS)throw new IllegalArgumentException("Choose 1–32 apps");
            LayoutDraft.validatePackages(apps);this.apps=Collections.unmodifiableList(new ArrayList<>(apps));
        }
    }
    static List<Folder> parse(String raw){
        if(raw==null||raw.isEmpty())return Collections.emptyList();
        if(raw.getBytes(StandardCharsets.UTF_8).length>BYTES)throw new IllegalArgumentException("Folders are too large");
        String[] lines=raw.split("\n",-1);if(!lines[0].equals("v1"))throw new IllegalArgumentException("Unsupported folders");
        List<Folder> result=new ArrayList<>();Set<String> ids=new HashSet<>(),names=new HashSet<>(),members=new HashSet<>();
        for(int i=1;i<lines.length;i++){
            String[] p=lines[i].split("\t",-1);if(p.length!=3||result.size()>=MAX)throw new IllegalArgumentException("Invalid folders");
            String label=new String(Base64.getUrlDecoder().decode(p[1]),StandardCharsets.UTF_8);
            Folder f=new Folder(p[0],label,Arrays.asList(p[2].split(",",-1)));
            if(!ids.add(f.id)||!names.add(f.name.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Folder already exists");
            for(String pkg:f.apps)if(!members.add(pkg))throw new IllegalArgumentException("An app belongs to one folder");
            result.add(f);
        }
        return Collections.unmodifiableList(result);
    }
    static String encode(List<Folder> folders){
        StringBuilder out=new StringBuilder("v1");for(Folder f:folders)out.append('\n').append(f.id).append('\t')
            .append(Base64.getUrlEncoder().withoutPadding().encodeToString(f.name.getBytes(StandardCharsets.UTF_8))).append('\t').append(String.join(",",f.apps));
        String raw=out.toString();parse(raw);return raw;
    }
    private FolderPolicy(){}
}
