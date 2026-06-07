package com.termux.zerocore.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.termux.R;

public class NotificationUtils {
    private static final String CHANNEL_ID = "channel_timer_notification";
    private static final String CHANNEL_NAME = "channel_timer";

    private static void ensureTimerChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }
            NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (existing != null) {
                return;
            }
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification buildTimerNotification(Context context, String title, String message) {
        return buildTimerNotification(context, title, message, null);
    }

    public static Notification buildTimerNotification(Context context, String title, String message, Intent contentIntent) {
        ensureTimerChannel(context);
        Notification.Builder builder = new Notification.Builder(context)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true);
        if (contentIntent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            builder.setContentIntent(PendingIntent.getActivity(context, 1556, contentIntent, flags));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        return builder.build();
    }

    public static void showNotification(Context context, int notificationId, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureTimerChannel(context);
        Notification notification = buildTimerNotification(context, title, message);
        notificationManager.notify(notificationId, notification);
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    public static void updateNotification(Context context, int notificationId, String updatedTitle, String updatedMessage) {
        updateNotification(context, notificationId, updatedTitle, updatedMessage, null);
    }

    public static void updateNotification(Context context, int notificationId, String updatedTitle, String updatedMessage, Intent contentIntent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureTimerChannel(context);
        Notification notification = buildTimerNotification(context, updatedTitle, updatedMessage, contentIntent);
        notificationManager.notify(notificationId, notification);
    }

}
