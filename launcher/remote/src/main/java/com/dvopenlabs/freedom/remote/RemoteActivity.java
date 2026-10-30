package com.dvopenlabs.freedom.remote;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.net.*;
import java.util.*;

/** Pairing is started on the TV, never at boot or by an exported command. */
public final class RemoteActivity extends Activity {
    final Handler main=new Handler(Looper.getMainLooper());TextView address,pin,state;Spinner interfaces;final List<String> ips=new ArrayList<>();
    final Runnable tick=new Runnable(){public void run(){address.setText(RemoteService.address.isEmpty()?"Remote is off":RemoteService.address);pin.setText(RemoteService.pin.isEmpty()?"Start to get a pairing PIN":"Pairing PIN: "+RemoteService.pin);state.setText(RemoteAccess.current==null?"Phone controls require your separate Android Accessibility choice":"Phone controls available · Android "+Build.VERSION.RELEASE);main.postDelayed(this,1000);}};
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(-1);t.setTextSize(size);t.setTypeface(getResources().getFont(R.font.outfit));t.setFontVariationSettings(size>=26?"'wght' 550":"'wght' 400");t.setPadding(0,dp(8),0,dp(8));return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setPadding(dp(40),dp(24),dp(40),dp(24));root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff101923);root.addView(text("Phone remote & keyboard",28));root.addView(text("Runs on this TV; no PC required. Use a trusted local network. HTTP is unencrypted. PIN lasts five minutes; one phone session lasts 30 minutes. Stopping revokes pairing.",17));
        address=text("",24);pin=text("",23);state=text("",17);root.addView(address);root.addView(pin);root.addView(state);
        try{for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces()))if(ni.isUp()&&!ni.isLoopback())for(InetAddress a:Collections.list(ni.getInetAddresses()))if(a instanceof Inet4Address&&a.isSiteLocalAddress())ips.add(a.getHostAddress());}catch(Exception ignored){}
        interfaces=new Spinner(this);interfaces.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ips));root.addView(interfaces);Button start=new Button(this);start.setText("Start / new pairing PIN");root.addView(start);start.setOnClickListener(v->{if(ips.isEmpty()){Toast.makeText(this,"Connect this TV to a local network first",Toast.LENGTH_LONG).show();return;}stopService(new Intent(this,RemoteService.class));main.postDelayed(()->startForegroundService(new Intent(this,RemoteService.class).putExtra("ip",ips.get(interfaces.getSelectedItemPosition()))),250);});
        Button stop=new Button(this);stop.setText("Stop remote & revoke phone");root.addView(stop);stop.setOnClickListener(v->stopService(new Intent(this,RemoteService.class)));
        root.addView(text("Android 13+ supports system D-pad actions. On Android 11/12, arrows/OK depend on whether the current app exposes focus. Use the physical remote when unavailable. Keyboard replaces the focused editable field; password fields are blocked.",17));
        root.addView(text(getString(com.dvopenlabs.freedom.remote.R.string.access_description),16));Button access=new Button(this);access.setText("Review Android accessibility settings");access.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Review phone control access").setMessage(getString(R.string.access_description)).setPositiveButton("Open Android settings",(d,w)->{try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Exception e){Toast.makeText(this,"Open TV Settings → Accessibility manually",Toast.LENGTH_LONG).show();}}).setNegativeButton("Cancel",null).show());root.addView(access);for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof Button){Button button=(Button)root.getChildAt(i);button.setAllCaps(false);button.setTextSize(18);button.setTypeface(getResources().getFont(R.font.outfit));button.setFontVariationSettings("'wght' 500");button.setBackgroundResource(R.drawable.freedom_action);button.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[0]},new int[]{0xff182334,0xfff3f6fc}));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.bottomMargin=dp(10);button.setLayoutParams(lp);}ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);}
    @Override public void onResume(){super.onResume();main.post(tick);}
    @Override public void onPause(){main.removeCallbacksAndMessages(null);super.onPause();}
}
