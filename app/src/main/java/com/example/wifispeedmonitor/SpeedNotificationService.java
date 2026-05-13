package com.example.wifispeedmonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class SpeedNotificationService extends Service {

    private static final String CHANNEL_ID = "speed_channel";
    private static final int NOTIF_ID = 1;

    private long lastRx = 0, lastTx = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("↓ 0 B/s", "↑ 0 B/s"));

        lastRx = TrafficStats.getTotalRxBytes();
        lastTx = TrafficStats.getTotalTxBytes();

        runnable = new Runnable() {
            @Override
            public void run() {
                long currentRx = TrafficStats.getTotalRxBytes();
                long currentTx = TrafficStats.getTotalTxBytes();

                String down = "↓ " + formatSpeed(currentRx - lastRx);
                String up   = "↑ " + formatSpeed(currentTx - lastTx);

                lastRx = currentRx;
                lastTx = currentTx;

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.notify(NOTIF_ID, buildNotification(down, up));

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private Notification buildNotification(String down, String up) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_speed)
                .setContentTitle(down + "   " + up)
                .setContentText("Tap to open WiFi Speed Monitor")
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Speed Monitor", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows live internet speed");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private String formatSpeed(long bytes) {
        if (bytes < 1024) return bytes + " B/s";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB/s", bytes / 1024.0);
        else return String.format("%.2f MB/s", bytes / (1024.0 * 1024));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}
