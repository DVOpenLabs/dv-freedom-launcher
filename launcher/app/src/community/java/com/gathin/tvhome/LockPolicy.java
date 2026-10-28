package com.gathin.tvhome;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** A salted local convenience PIN; never an Android/account/child-security boundary. */
final class LockPolicy {
    static void validPin(String pin){if(pin==null||!pin.matches("[0-9]{4,8}"))throw new IllegalArgumentException("Use 4–8 digits");}
    static String create(String pin)throws Exception{validPin(pin);byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);return "v1:"+hex(salt)+":"+hex(derive(pin,salt));}
    static boolean verify(String record,String pin)throws Exception{validPin(pin);if(record==null||!record.matches("v1:[a-f0-9]{32}:[a-f0-9]{64}"))return false;String[] parts=record.split(":");return MessageDigest.isEqual(bytes(parts[2]),derive(pin,bytes(parts[1])));}
    private static byte[] derive(String pin,byte[] salt)throws Exception{PBEKeySpec spec=new PBEKeySpec(pin.toCharArray(),salt,120000,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    private static String hex(byte[] b){StringBuilder out=new StringBuilder();for(byte x:b)out.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return out.toString();}
    private static byte[] bytes(String s){byte[] out=new byte[s.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return out;}
}
