package com.gathin.tvhome;
import java.util.*;

/** Small access-ordered cache. Eviction drops references; renderers may still own a value. */
final class WeightedCache<K,V> {
    private static final class Entry<V>{final V value;final int bytes;Entry(V v,int b){value=v;bytes=b;}}
    private final LinkedHashMap<K,Entry<V>> entries=new LinkedHashMap<>(16,.75f,true);
    private final int limit,count;private int bytes;
    WeightedCache(int limit,int count){if(limit<1||count<1)throw new IllegalArgumentException();this.limit=limit;this.count=count;}
    synchronized V get(K key){Entry<V> item=entries.get(key);return item==null?null:item.value;}
    synchronized void put(K key,V value,int weight){
        Entry<V> prior=entries.remove(key);if(prior!=null)bytes-=prior.bytes;
        if(value==null||weight<1||weight>limit)return;
        entries.put(key,new Entry<>(value,weight));bytes+=weight;
        Iterator<Entry<V>> it=entries.values().iterator();while(bytes>limit||entries.size()>count){bytes-=it.next().bytes;it.remove();}
    }
    synchronized void clear(){entries.clear();bytes=0;}
    synchronized int bytes(){return bytes;}
    synchronized int size(){return entries.size();}
}
