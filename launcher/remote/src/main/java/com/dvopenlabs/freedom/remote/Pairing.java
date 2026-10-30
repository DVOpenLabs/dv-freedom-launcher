package com.dvopenlabs.freedom.remote;
import java.security.*;
import java.util.*;
/** One phone, short pairing window, hard lockout and no saved secrets. */
final class Pairing {
    final String pin=String.format(Locale.ROOT,"%06d",new SecureRandom().nextInt(1000000));
    final long created;long until,blocked;int attempts;String session="";boolean revoked;
    Pairing(long now){created=now;}
    synchronized String pair(String value,long now){if(revoked||now-created>300000||now<blocked)return "";if(!MessageDigest.isEqual(pin.getBytes(java.nio.charset.StandardCharsets.UTF_8),value.getBytes(java.nio.charset.StandardCharsets.UTF_8))){if(++attempts>=5){blocked=now+60000;attempts=0;}return "";}session=UUID.randomUUID().toString();until=now+1800000;return session;}
    synchronized void revoke(){revoked=true;session="";until=0;}
    synchronized boolean allowed(String cookie,long now){return !session.isEmpty()&&now<until&&MessageDigest.isEqual(("dvremote="+session).getBytes(java.nio.charset.StandardCharsets.UTF_8),cookie.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}
