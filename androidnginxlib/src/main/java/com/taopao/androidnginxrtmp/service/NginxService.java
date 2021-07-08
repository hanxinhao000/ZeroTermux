package com.taopao.androidnginxrtmp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.screenshare.rtmp.nginx.Nginx;
import org.screenshare.rtmp.nginx.NginxConfigureTask;

import java.io.File;
import java.util.logging.Logger;

/**
 * Nginx Android Service.
 */
public class NginxService extends Service {
    /**
     * Action: start nginx.
     */
    public static final String ACTION_START_NGINX = "START_NGINX";
    /**
     * Action: stop nginx.
     */
    public static final String ACTION_STOP_NGINX = "STOP_NGINX";
    /**
     * Logger.
     */
    private Logger mLogger = Logger.getLogger("nginx.android");

    @Override
    public void onCreate() {
        super.onCreate();
        File sdcard = Nginx.create().getPrefix();
        (new NginxConfigureTask(this, sdcard)).execute(
                NginxConfigureTask.NGINX_CONF_FILENAME,
                NginxConfigureTask.NGINX_MIMETYPES_FILENAME);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        mLogger.entering(getClass().getName(), "onStartCommand",
                new Object[]{intent, flags, startId});

        super.onStartCommand(intent, flags, startId);


        if (intent == null) {
            mLogger.exiting(getClass().getName(), "onStartCommand", START_STICKY);
            return START_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            mLogger.exiting(getClass().getName(), "onStartCommand", START_STICKY);
            return START_STICKY;
        }

        if (ACTION_START_NGINX.equals(action)) {
            startNginx();
        } else if (ACTION_STOP_NGINX.equals(action)) {
//            stopNginx();
        }

        mLogger.exiting(getClass().getName(), "onStartCommand", START_STICKY);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    /**
     * Start nginx server.
     */
    private void startNginx() {
        mLogger.entering(getClass().getName(), "startNginx");

        Nginx.create().start();

        mLogger.exiting(getClass().getName(), "startNginx");
    }

    /**
     * Stop nginx server.
     */
    private void stopNginx() {
        mLogger.entering(getClass().getName(), "stopNginx");

        Nginx.create().stop(0);

        mLogger.exiting(getClass().getName(), "stopNginx");
    }

}
