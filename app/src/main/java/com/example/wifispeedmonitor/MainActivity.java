package com.example.wifispeedmonitor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvDownloadSpeed, tvUploadSpeed, tvWifiName, tvSignal, tvDeviceCount;
    private RecyclerView rvDevices;
    private DeviceAdapter deviceAdapter;

    private long lastRxBytes = 0, lastTxBytes = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable speedRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View bindings
        tvDownloadSpeed = findViewById(R.id.tvDownloadSpeed);
        tvUploadSpeed   = findViewById(R.id.tvUploadSpeed);
        tvWifiName      = findViewById(R.id.tvWifiName);
        tvSignal        = findViewById(R.id.tvSignal);
        tvDeviceCount   = findViewById(R.id.tvDeviceCount);
        rvDevices       = findViewById(R.id.rvDevices);

        deviceAdapter = new DeviceAdapter(new ArrayList<>());
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(deviceAdapter);

        // Start notification service
        Intent serviceIntent = new Intent(this, SpeedNotificationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Request permissions
        checkPermissions();

        startSpeedMonitor();
    }

    private void checkPermissions() {
        String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        };
        List<String> needed = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), 100);
        }
    }

    private void startSpeedMonitor() {
        lastRxBytes = TrafficStats.getTotalRxBytes();
        lastTxBytes = TrafficStats.getTotalTxBytes();

        speedRunnable = new Runnable() {
            @Override
            public void run() {
                updateSpeed();
                updateWifiInfo();
                updateConnectedDevices();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(speedRunnable);
    }

    private void updateSpeed() {
        long currentRx = TrafficStats.getTotalRxBytes();
        long currentTx = TrafficStats.getTotalTxBytes();

        long rxSpeed = currentRx - lastRxBytes; // bytes per second
        long txSpeed = currentTx - lastTxBytes;

        lastRxBytes = currentRx;
        lastTxBytes = currentTx;

        tvDownloadSpeed.setText(formatSpeed(rxSpeed));
        tvUploadSpeed.setText(formatSpeed(txSpeed));
    }

    private String formatSpeed(long bytesPerSec) {
        if (bytesPerSec < 1024) return bytesPerSec + " B/s";
        else if (bytesPerSec < 1024 * 1024) return String.format("%.1f KB/s", bytesPerSec / 1024.0);
        else return String.format("%.2f MB/s", bytesPerSec / (1024.0 * 1024));
    }

    private void updateWifiInfo() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID().replace("\"", "");
            tvWifiName.setText("WiFi: " + ssid);
            int rssi = wifiInfo.getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, 5);
            tvSignal.setText("Signal: " + getSignalLabel(level) + " (" + rssi + " dBm)");
        }
    }

    private String getSignalLabel(int level) {
        switch (level) {
            case 4: return "Excellent ████";
            case 3: return "Good ███░";
            case 2: return "Fair ██░░";
            case 1: return "Weak █░░░";
            default: return "Poor ░░░░";
        }
    }

    private void updateConnectedDevices() {
        new Thread(() -> {
            List<DeviceInfo> devices = getConnectedDevices();
            runOnUiThread(() -> {
                tvDeviceCount.setText("Connected Devices: " + devices.size());
                deviceAdapter.updateDevices(devices);
            });
        }).start();
    }

    private List<DeviceInfo> getConnectedDevices() {
        List<DeviceInfo> devices = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4) {
                    String ip  = parts[0];
                    String mac = parts[3];
                    if (!mac.equals("00:00:00:00:00:00")) {
                        devices.add(new DeviceInfo(ip, mac, getDeviceName(mac)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return devices;
    }

    private String getDeviceName(String mac) {
        // Simple OUI lookup for common vendors
        String prefix = mac.toUpperCase().substring(0, 8);
        switch (prefix) {
            case "00:50:56": return "VMware Device";
            case "B8:27:EB": return "Raspberry Pi";
            case "DC:A6:32": return "Raspberry Pi 4";
            case "00:1A:11": return "Google Device";
            case "F4:F5:D8": return "Google/Nest";
            case "18:65:90": return "Apple Device";
            case "A4:C3:F0": return "Apple iPhone";
            case "00:23:14": return "Samsung Device";
            case "8C:BE:BE": return "Samsung TV";
            default: return "Unknown Device";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(speedRunnable);
    }
}
