package com.gathin.btconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * The tile and module card open this: the paired speakers and headphones, each with its state, and OK on one
 * connects it or, when it is connected, disconnects it. The TV's own Bluetooth settings only offer Connect and Unpair.
 */
public class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private LinearLayout list;
    private TextView status;
    private BluetoothAdapter adapter;
    private BluetoothA2dp a2dp;
    private boolean resumed,registered,connecting;
    /** Addresses asked to change a moment ago, shown as "Working…" until the state change arrives. */
    private final Set<String> pending = new HashSet<>();
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) pending.remove(device.getAddress());
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFF0D1117);
        page.setPadding(Math.round(48 * dp), Math.round(32 * dp), Math.round(48 * dp), Math.round(24 * dp));
        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setTextColor(0xFFFFFFFF);
        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(0xFF8B949E);
        status.setPadding(0, 0, 0, Math.round(16 * dp));
        page.addView(title);
        page.addView(status);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
        BluetoothManager manager=getSystemService(BluetoothManager.class);
        adapter = manager==null?null:manager.getAdapter();
        TextView settings=new TextView(this);settings.setText("Open Bluetooth settings");settings.setTextColor(0xFFFFFFFF);settings.setTextSize(18);settings.setPadding(24,16,24,16);settings.setMinHeight(Math.round(48*dp));settings.setBackgroundResource(R.drawable.row_background);settings.setFocusable(true);settings.setClickable(true);settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));}catch(RuntimeException missing){status.setText("Open Bluetooth & accessories from your TV Settings.");}});page.addView(settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed=true;
        if (Link.needsConnectPermission(Build.VERSION.SDK_INT) && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }
        start();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if(code!=1)return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {if(resumed)start();}
        else status.setText("Nearby devices access is off. You can allow it in this app's Android settings, or use Bluetooth settings below.");
    }

    private void start() {
        if(!resumed)return;
        if (adapter == null || !adapter.isEnabled()) {
            status.setText("Bluetooth is off. Turn it on in the TV's settings.");
            return;
        }
        if(!registered){IntentFilter filter=new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            if(Build.VERSION.SDK_INT>=33)registerReceiver(changed,filter,Context.RECEIVER_EXPORTED);else registerReceiver(changed,filter);registered=true;}
        if (a2dp != null) {
            refresh();
            return;
        }
        if(connecting)return;
        connecting=true;
        if(!adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                connecting=false;
                if(!resumed||isDestroyed()){adapter.closeProfileProxy(BluetoothProfile.A2DP,proxy);return;}
                a2dp = (BluetoothA2dp) proxy;
                refresh();
            }
            @Override public void onServiceDisconnected(int profile) {
                connecting=false;
                a2dp = null;
            }
        }, BluetoothProfile.A2DP)){connecting=false;status.setText("Audio device controls are unavailable. Use Bluetooth settings below.");}
    }

    @Override
    protected void onPause() {
        resumed=false;
        try {
            unregisterReceiver(changed);
        } catch (IllegalArgumentException notRegistered) {
        }
        registered=false;
        if(a2dp!=null){adapter.closeProfileProxy(BluetoothProfile.A2DP,a2dp);a2dp=null;}
        pending.clear();
        main.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (a2dp != null) adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dp);
        super.onDestroy();
    }

    /** Only runs after onResume has checked the Nearby devices permission. */
    @SuppressLint("MissingPermission")
    private void refresh() {
        if (!resumed || a2dp == null) return;
        int focus = 0;
        for (int i = 0; i < list.getChildCount(); i++) if (list.getChildAt(i).isFocused()) focus = i;
        list.removeAllViews();
        Set<String> on = new HashSet<>();
        for (BluetoothDevice d : a2dp.getConnectedDevices()) on.add(d.getAddress());
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (!Link.isAudio(device.getBluetoothClass())) continue;
            String address = device.getAddress();
            boolean connected = on.contains(address);
            Row row = new Row(this, device.getName() == null ? address : device.getName(),
                    pending.contains(address) ? "Working…" : connected ? "Connected" : "Not connected",
                    () -> toggle(device, connected));
            row.setValue(connected ? "Disconnect" : "Connect");
            list.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        status.setText(list.getChildCount() == 0
                ? "No paired speakers. Pair one in the TV's Bluetooth settings first."
                : "OK connects or disconnects a speaker.");
        if (list.getChildCount() > 0) list.getChildAt(Math.min(focus, list.getChildCount() - 1)).requestFocus();
    }

    private void toggle(BluetoothDevice device, boolean connected) {
        if (a2dp == null || pending.contains(device.getAddress())) return;
        if (!Link.set(a2dp, device, !connected)) {
            status.setText("Android refused. Use the TV's Bluetooth settings: "
                    + Settings.ACTION_BLUETOOTH_SETTINGS.substring(Settings.ACTION_BLUETOOTH_SETTINGS.lastIndexOf('.') + 1));
            return;
        }
        pending.add(device.getAddress());
        refresh();
        // A state change normally clears it; if none arrives (a speaker that is off), stop saying Working.
        main.postDelayed(() -> {
            pending.remove(device.getAddress());
            refresh();
        }, 12000);
    }
}
