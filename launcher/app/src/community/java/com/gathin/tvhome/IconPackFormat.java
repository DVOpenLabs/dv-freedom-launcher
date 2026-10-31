package com.gathin.tvhome;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Bounded appfilter data, not executable theme code. Unrecognised/invalid rows are ignored. */
final class IconPackFormat {
    static final int MAX_BYTES=2*1024*1024,MAX_ITEMS=8192,MAX_EVENTS=50_000;
    static boolean resource(String name){return name!=null&&name.matches("[a-z][a-z0-9_]{0,119}");}
    static String component(String pkg,String activity){
        if(!LayoutDraft.packageName(pkg)||activity==null)return null;
        if(activity.isEmpty())return pkg+"/";
        String full=activity.startsWith(".")?pkg+activity:activity;
        return full.matches("[A-Za-z_][A-Za-z0-9_.$]{0,255}")?pkg+"/"+full:null;
    }
    static final class Mapping {
        final Map<String,String> icons;
        Mapping(Map<String,String> icons){this.icons=Collections.unmodifiableMap(new LinkedHashMap<>(icons));}
        String find(String pkg,String activity){String exact=component(pkg,activity);return exact!=null&&icons.containsKey(exact)?icons.get(exact):icons.get(pkg+"/");}
        List<String> names(){return new ArrayList<>(new TreeSet<>(icons.values()));}
    }
    static final class Builder {
        final Map<String,String> icons=new LinkedHashMap<>();int rows;
        void item(String raw,String drawable){
            if(++rows>MAX_ITEMS)throw new IllegalArgumentException("This icon mapping is too large");
            if(raw==null||raw.length()>512||!raw.startsWith("ComponentInfo{")||!raw.endsWith("}")||!resource(drawable))return;
            String[] parts=raw.substring(14,raw.length()-1).split("/",-1);if(parts.length!=2)return;
            String key=component(parts[0],parts[1]);if(key!=null)icons.putIfAbsent(key,drawable);
        }
        Mapping build(){return new Mapping(icons);}
    }
    static Mapping parse(byte[] bytes)throws Exception {
        if(bytes.length==0||bytes.length>MAX_BYTES)throw new IllegalArgumentException("Choose a supported appfilter under 2 MB");
        String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        if(text.startsWith("\ufeff"))text=text.substring(1);
        if(java.util.regex.Pattern.compile("<!\\s*(DOCTYPE|ENTITY)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text).find())
            throw new IllegalArgumentException("Icon mappings cannot contain document types or entities");
        Builder builder=new Builder();SAXParserFactory factory=SAXParserFactory.newInstance();factory.setValidating(false);factory.setNamespaceAware(false);
        XMLReader reader=factory.newSAXParser().getXMLReader();
        reader.setEntityResolver((publicId,systemId)->{throw new SAXException("External icon mapping data is not supported");});
        reader.setErrorHandler(new DefaultHandler(){@Override public void error(SAXParseException e)throws SAXException{throw e;}@Override public void fatalError(SAXParseException e)throws SAXException{throw e;}});
        reader.setContentHandler(new DefaultHandler(){int depth,events;
            @Override public void startElement(String uri,String local,String name,Attributes attributes)throws SAXException{
                if(Thread.currentThread().isInterrupted()||++depth>16||++events>MAX_EVENTS)throw new SAXException("Icon mapping is too complex");
                if("item".equals(name))try{builder.item(attributes.getValue("component"),attributes.getValue("drawable"));}catch(IllegalArgumentException tooMany){throw new SAXException(tooMany);}
            }
            @Override public void endElement(String uri,String local,String name){depth--;}
        });
        reader.parse(new InputSource(new StringReader(text)));return builder.build();
    }
    private IconPackFormat(){}
}
