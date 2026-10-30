package com.dvopenlabs.freedom.camera;
import javax.net.SocketFactory;
import java.net.*;
import java.io.IOException;

/** RTSP-over-TCP sockets are confined to the exact private IPv4 host selected by the user. */
final class LanSocketFactory extends SocketFactory {
    final String host;final int port;
    LanSocketFactory(String host,int port){if(!CameraPolicy.privateHost(host)||port<1||port>65535)throw new IllegalArgumentException();this.host=host;this.port=port;}
    void check(SocketAddress endpoint)throws IOException{if(!(endpoint instanceof InetSocketAddress))throw new IOException("Camera destination refused");InetSocketAddress a=(InetSocketAddress)endpoint;if(a.getAddress()==null||!host.equals(a.getAddress().getHostAddress())||a.getPort()!=port)throw new IOException("Camera destination refused");}
    public Socket createSocket(){return new Socket(){@Override public void connect(SocketAddress target)throws IOException{connect(target,10000);}@Override public void connect(SocketAddress target,int timeout)throws IOException{check(target);super.connect(target,Math.min(10000,timeout<=0?10000:timeout));}};}
    public Socket createSocket(String h,int p)throws IOException{Socket s=createSocket();s.connect(new InetSocketAddress(h,p));return s;}
    public Socket createSocket(InetAddress h,int p)throws IOException{return createSocket(h.getHostAddress(),p);}
    public Socket createSocket(String h,int p,InetAddress local,int lp)throws IOException{Socket s=createSocket();s.bind(new InetSocketAddress(local,lp));s.connect(new InetSocketAddress(h,p));return s;}
    public Socket createSocket(InetAddress h,int p,InetAddress local,int lp)throws IOException{return createSocket(h.getHostAddress(),p,local,lp);}
}
