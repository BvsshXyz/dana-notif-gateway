package com.bayushop.dananotif;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class GatewayForegroundService extends Service {
    private static final String CHANNEL_ID = "dana_notif_gateway_status";
    private static final int NOTIFICATION_ID = 7001;
    private static final long SCAN_INTERVAL_MS = 15000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            DanaNotificationService.scanActiveNotificationsFromService();
            handler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        handler.post(scanTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DanaNotificationService.scanActiveNotificationsFromService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scanTask);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "DANA Gateway aktif",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Menjaga gateway tetap aktif untuk membaca notifikasi pembayaran.");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private android.app.Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(this, CHANNEL_ID)
                : new android.app.Notification.Builder(this);

        return builder
                .setContentTitle("DANA Notif Gateway aktif")
                .setContentText("Memantau notifikasi pembayaran masuk.")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}