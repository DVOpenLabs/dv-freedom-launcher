package com.gathin.tvhome;
import java.util.*;
/** Session-only edits. An undo step never writes preferences or touches app/media files. */
final class StudioDraft {
    private final Map<String,Object> original,values;
    private final Deque<Map<String,Object>> undo=new ArrayDeque<>();
    StudioDraft(Map<String,Object> initial){original=new LinkedHashMap<>(initial);values=new LinkedHashMap<>(initial);}
    Object get(String key){return values.get(key);}
    void set(String key,Object value){if(!original.containsKey(key))throw new IllegalArgumentException("Unknown studio setting");if(Objects.equals(values.get(key),value))return;checkpoint();values.put(key,value);}
    void setAll(Map<String,Object> next){if(!original.keySet().containsAll(next.keySet()))throw new IllegalArgumentException();if(next.entrySet().stream().allMatch(e->Objects.equals(values.get(e.getKey()),e.getValue())))return;checkpoint();values.putAll(next);}
    private void checkpoint(){if(undo.size()==32)undo.removeFirst();undo.addLast(new LinkedHashMap<>(values));}
    boolean canUndo(){return !undo.isEmpty();}
    void undo(){if(canUndo()){values.clear();values.putAll(undo.removeLast());}}
    void reset(){if(changes().isEmpty())return;checkpoint();values.clear();values.putAll(original);}
    Map<String,Object> changes(){Map<String,Object> out=new LinkedHashMap<>();for(String key:values.keySet())if(!Objects.equals(values.get(key),original.get(key)))out.put(key,values.get(key));return out;}
}
