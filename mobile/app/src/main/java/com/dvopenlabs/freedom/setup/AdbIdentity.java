// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;
import android.content.Context;
import android.security.keystore.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.math.BigInteger;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Per-TV ADB RSA key, encrypted at rest with an Android Keystore AES key; no raw key export. */
final class AdbIdentity {
    final PrivateKey key;final Certificate certificate;
    AdbIdentity(Context c,String host)throws Exception{
        String id=SetupPolicy.sha(host.getBytes(java.nio.charset.StandardCharsets.UTF_8));KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);String alias="dv-adb-"+id;
        if(!store.containsAlias(alias)){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());generator.generateKey();}
        SecretKey wrapping=(SecretKey)store.getKey(alias,null);File dir=new File(c.getFilesDir(),"identities");dir.mkdirs();File file=new File(dir,id+".key"),cert=new File(dir,id+".crt");
        if(file.isFile()&&cert.isFile()){
            if(file.length()>8192||cert.length()>8192)throw new IOException("Identity record damaged");byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());if(bytes.length<29)throw new IOException();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,wrapping,new GCMParameterSpec(128,Arrays.copyOfRange(bytes,0,12)));byte[] raw=cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length));try{key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(raw));}finally{Arrays.fill(raw,(byte)0);}try(InputStream in=new FileInputStream(cert)){certificate=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in);}
        }else{
            if(file.exists()||cert.exists())throw new IOException("Incomplete identity; recover it before creating another key");KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);java.security.KeyPair pair=generator.generateKeyPair();key=pair.getPrivate();X500Name name=new X500Name("CN=DV Freedom Phone Setup");Date now=new Date(System.currentTimeMillis()-60_000),end=new Date(System.currentTimeMillis()+10L*365*24*3600_000);X509v3CertificateBuilder builder=new X509v3CertificateBuilder(name,new BigInteger(128,new SecureRandom()),now,end,name,SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded()));byte[] encoded=builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(key)).getEncoded();certificate=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded));Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,wrapping);byte[] raw=key.getEncoded();try{byte[] encrypted=cipher.doFinal(raw);try(FileOutputStream out=new FileOutputStream(file)){out.write(cipher.getIV());out.write(encrypted);out.getFD().sync();}try(FileOutputStream out=new FileOutputStream(cert)){out.write(encoded);out.getFD().sync();}}finally{Arrays.fill(raw,(byte)0);}
        }
    }
}
