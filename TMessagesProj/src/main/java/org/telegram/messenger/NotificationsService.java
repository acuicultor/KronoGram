/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.LaunchActivity;

public class NotificationsService extends Service {

    public static volatile boolean isRunningInForeground = false;
    private static volatile long lastTimeoutElapsedMs = 0L;
    private static final long TIMEOUT_COOLDOWN_MS = 15L * 60L * 1000L;
    private volatile boolean stoppedByTimeout = false;
    public static final int KEEP_ALIVE_NOTIFICATION_ID = 49101;
    private static final String KEEP_ALIVE_CHANNEL_ID = "kronogram_keepalive_service";

    public static boolean isInTimeoutCooldown() {
        long t = lastTimeoutElapsedMs;
        return t > 0L && (SystemClock.elapsedRealtime() - t) < TIMEOUT_COOLDOWN_MS;
    }

    public static void clearTimeoutCooldownIfForeground() {
        if (!ApplicationLoader.mainInterfacePaused) {
            lastTimeoutElapsedMs = 0L;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
        if (KronoConfig.isPersistentForegroundService()) {
            promoteToForeground();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stoppedByTimeout = false;
        boolean requestedForeground = (intent != null && intent.getBooleanExtra("foreground", false))
                || KronoConfig.isPersistentForegroundService();
        if (requestedForeground) {
            promoteToForeground();
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    ConnectionsManager.getInstance(a).setPushConnectionEnabled(true);
                }
            }
        } else if (isRunningInForeground) {
            try {
                if (Build.VERSION.SDK_INT >= 24) {
                    stopForeground(STOP_FOREGROUND_REMOVE);
                } else {
                    stopForeground(true);
                }
            } catch (Throwable ignore) {
            }
            isRunningInForeground = false;
        }
        return START_STICKY;
    }

    private void promoteToForeground() {
        try {
            Notification notification = buildKeepAliveNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(KEEP_ALIVE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(KEEP_ALIVE_NOTIFICATION_ID, notification);
            }
            isRunningInForeground = true;
        } catch (Throwable e) {
            isRunningInForeground = false;
            FileLog.e(e);
        }
    }

    private Notification buildKeepAliveNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = nm.getNotificationChannel(KEEP_ALIVE_CHANNEL_ID);
                    if (channel == null) {
                        channel = new NotificationChannel(
                                KEEP_ALIVE_CHANNEL_ID,
                                "Servicio en segundo plano",
                                NotificationManager.IMPORTANCE_MIN
                        );
                        channel.setDescription("Mantiene KronoGram activo en segundo plano para recibir mensajes sin retrasos");
                        channel.enableLights(false);
                        channel.enableVibration(false);
                        channel.setSound(null, null);
                        channel.setShowBadge(false);
                        nm.createNotificationChannel(channel);
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }

        Intent launchIntent = new Intent(this, LaunchActivity.class);
        launchIntent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent, pendingFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, KEEP_ALIVE_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("KronoGram activo en segundo plano")
                .setContentText("Conexión persistente anti-cierre habilitada")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setContentIntent(contentIntent);
        return builder.build();
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        stoppedByTimeout = true;
        isRunningInForeground = false;
        lastTimeoutElapsedMs = SystemClock.elapsedRealtime();
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Throwable ignore) {
        }
        if (KronoConfig.isPersistentForegroundService() || KronoConfig.isDisableFirebasePush()) {
            KeepAliveJob.startJob();
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        sendRestartBroadcastIfEnabled();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunningInForeground = false;
        if (stoppedByTimeout) {
            if (KronoConfig.isPersistentForegroundService() || KronoConfig.isDisableFirebasePush()) {
                KeepAliveJob.startJob();
            }
            return;
        }
        sendRestartBroadcastIfEnabled();
    }

    private void sendRestartBroadcastIfEnabled() {
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)
                || KronoConfig.isPersistentForegroundService()
                || KronoConfig.isDisableFirebasePush()) {
            try {
                Intent intent = new Intent("org.telegram.start");
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }
}
