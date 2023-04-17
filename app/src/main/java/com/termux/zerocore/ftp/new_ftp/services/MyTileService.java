package com.termux.zerocore.ftp.new_ftp.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.core.app.NotificationCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;


@TargetApi(24)
public class MyTileService extends TileService implements FtpService.OnFTPServiceStatusChangedListener {

    @Override
    public void onStartListening() {
        super.onStartListening();
        FtpService.addOnFtpServiceStatusChangedListener(this);
        refreshTileState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        FtpService.removeOnFtpServiceStatusChangedListener(this);
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        switch (tile.getState()) {
            default:
                break;
            case Tile.STATE_INACTIVE: {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
                if (!FtpService.startService(this)) {
                    showNotification(getResources().getString(R.string.notification_ftp_start_error_title), getResources().getString(R.string.notification_ftp_start_errer_message));
                    tile.setState(Tile.STATE_INACTIVE);
                    tile.updateTile();
                }
            }
            break;
            case Tile.STATE_ACTIVE: {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
                FtpService.stopService();
            }
            break;
        }
    }

    @Override
    public void onFTPServiceStarted() {
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
        try {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFTPServiceStartError(Exception e) {
        showNotification(getResources().getString(R.string.notification_ftp_start_error_title), getResources().getString(R.string.notification_ftp_start_errer_message));
    }

    @Override
    public void onRemainingSeconds(int seconds) {
    }

    @Override
    public void onFTPServiceDestroyed() {
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private void refreshTileState() {
        Tile tile = getQsTile();
        tile.setState(FtpService.isFTPServiceRunning() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private void showNotification(String title, String content) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("attention"
                    , getResources().getString(R.string.notification_channel_attention)
                    , NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, "attention")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(String.valueOf(title))
                .setContentText(String.valueOf(content))
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, TermuxActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        notificationManager.notify(2, notification);
    }

    /*
     * 收起通知栏
     */
    /*public static void collapseStatusBar(Context context) {
        try{
            Object statusBarManager = context.getSystemService("statusbar");
            Method collapse;
            if (Build.VERSION.SDK_INT <= 16) {
                collapse = statusBarManager.getClass().getMethod("collapse");
            } else {
                collapse = statusBarManager.getClass().getMethod("collapsePanels");
            }
            collapse.invoke(statusBarManager);
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }*/
}
