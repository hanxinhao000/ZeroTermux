package com.termux.zerocore.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.termux.R;

public class NotificationUtils {
    private static final String CHANNEL_ID = "channel_timer_notification";
    private static final String CHANNEL_NAME = "channel_timer";

    public static void showNotification(Context context, int notificationId, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道（仅适用于Android 8.0以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // 创建通知
        Notification.Builder builder = new Notification.Builder(context)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher);

        // 发送通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        notificationManager.notify(notificationId, builder.build());
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    public static void updateNotification(Context context, int notificationId, String updatedTitle, String updatedMessage) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知
        Notification.Builder builder = new Notification.Builder(context)
            .setContentTitle(updatedTitle)
            .setContentText(updatedMessage)
            .setSmallIcon(R.drawable.ic_launcher)
            .setSound(null);

        // 更新通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
            builder.setOnlyAlertOnce(true);
        }
        notificationManager.notify(notificationId, builder.build());
    }

}
