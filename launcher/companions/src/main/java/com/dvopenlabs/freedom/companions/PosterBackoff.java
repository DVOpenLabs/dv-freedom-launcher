package com.dvopenlabs.freedom.companions;
import java.util.*;
/** Failed artwork must not create an observer → request → failure retry loop. */
final class PosterBackoff {
    private final LinkedHashMap<String,Long> retry=new LinkedHashMap<>();
    synchronized boolean ready(String key,long now){Long at=retry.get(key);if(at==null)return true;if(now<at)return false;retry.remove(key);return true;}
    synchronized void failed(String key,long now){retry.remove(key);retry.put(key,now+300000);while(retry.size()>128)retry.remove(retry.keySet().iterator().next());}
    synchronized void clear(){retry.clear();}
}
