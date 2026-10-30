// SPDX-License-Identifier: Apache-2.0
// DVOpenLabs adaptation: paired ADB peer trust is supplied by the foreground setup session.
package io.github.muntashirakon.adb;
import java.security.cert.*;
import javax.net.ssl.X509TrustManager;

public final class PeerTrust {
    public interface Verifier { void verify(Certificate client, X509Certificate[] server)throws CertificateException; }
    private static volatile Verifier verifier;
    public static void use(Verifier value){verifier=value;}
    static X509TrustManager manager(Certificate client){return new X509TrustManager(){
        public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
        public void checkClientTrusted(X509Certificate[] chain,String auth)throws CertificateException{throw new CertificateException("ADB setup is client-only");}
        public void checkServerTrusted(X509Certificate[] chain,String auth)throws CertificateException{Verifier v=verifier;if(v==null||chain==null||chain.length==0||chain.length>8)throw new CertificateException("No authorized ADB session");v.verify(client,chain);}
    };}
    private PeerTrust(){}
}
