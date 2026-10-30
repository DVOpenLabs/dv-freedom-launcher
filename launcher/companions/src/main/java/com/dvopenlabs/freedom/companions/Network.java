package com.dvopenlabs.freedom.companions;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/** No redirects, proxies, hidden discovery or unbounded responses. Private HA is deliberate. */
final class Network {
    static boolean publicAddress(InetAddress a) {
        byte[] b=a.getAddress();
        return !a.isAnyLocalAddress()&&!a.isLoopbackAddress()&&!a.isLinkLocalAddress()&&!a.isSiteLocalAddress()&&!a.isMulticastAddress()
            &&!(b.length==4&&((b[0]&255)==0||(b[0]&255)==100&&(b[1]&255)>=64&&(b[1]&255)<=127||(b[0]&255)>=224||(b[0]&255)==198&&((b[1]&255)==18||(b[1]&255)==19)))
            &&!(b.length==16&&((b[0]&254)==252));
    }
    static HttpUrl url(String value,boolean home) {
        HttpUrl u=HttpUrl.parse(value);
        if(u==null||!u.username().isEmpty()||!u.password().isEmpty()||u.fragment()!=null||value.length()>4096||(!home&&(!u.isHttps()||u.port()!=443))||home&&!(u.isHttps()||u.scheme().equals("http")))throw new IllegalArgumentException("Use a valid URL without a password or fragment");
        return u;
    }
    static byte[] request(String address,String token,String post,boolean home,int limit)throws IOException {
        Map<String,String> headers=new HashMap<>();if(token!=null&&!token.isEmpty())headers.put("Authorization","Bearer "+token);return requestWith(post==null?"GET":"POST",address,headers,post,home,limit);
    }
    static byte[] requestWith(String method,String address,Map<String,String> headers,String body,boolean home,int limit)throws IOException{
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(!Arrays.asList("GET","POST","PROPFIND").contains(method)||limit<1||limit>8*1024*1024)throw new IllegalArgumentException("Invalid request");HttpUrl u=url(address,home);
        OkHttpClient client=new OkHttpClient.Builder().proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false)
            .connectTimeout(5,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS).callTimeout(10,TimeUnit.SECONDS)
            .dns(host->{List<InetAddress> ips=Dns.SYSTEM.lookup(host);if(!home)for(InetAddress ip:ips)if(!publicAddress(ip))throw new UnknownHostException("Public HTTPS host required");return ips;}).build();
        Request.Builder r=new Request.Builder().url(u);
        for(Map.Entry<String,String> h:headers.entrySet())r.header(h.getKey(),h.getValue());
        if(body!=null)r.method(method,RequestBody.create(body,MediaType.get(method.equals("PROPFIND")?"application/xml":"application/json")));else r.method(method,null);
        try(Response response=client.newCall(r.build()).execute()) {
            if(!response.isSuccessful()||response.body()==null)throw new IOException("Service unavailable");
            if(response.body().contentLength()>limit)throw new IOException("Response too large");
            try(InputStream in=response.body().byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>limit)throw new IOException("Response too large");out.write(b,0,n);}return out.toByteArray();
            }
        }finally{client.connectionPool().evictAll();client.dispatcher().executorService().shutdown();}
    }
}
