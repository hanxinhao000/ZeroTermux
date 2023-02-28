package com.termux.zerocore.http_service;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import cn.hotapk.fhttpserver.FHttpManager;

public class HttpServerManager {
    static String TAG = "HttpServerManager";
    static FHttpManager fHttpManager;
    public static void startService(int port, String root) {
        if (fHttpManager == null) {
            LogUtils.d(TAG, "startService: fHttpManager is null, new.");
            fHttpManager = FHttpManager.init(UUtils.getContext(), UserController.class, AppController.class);
        }
        fHttpManager.setPort(port);
        fHttpManager.setResdir(root);
        fHttpManager.setAllowCross(true);
        fHttpManager.startServer();
        LogUtils.d(TAG, "startService: fHttpManager start Ok");
    }

    public static boolean isAlive() {
        if (fHttpManager == null) {
            LogUtils.d(TAG, "startService: fHttpManager is null, return!");
            return false;
        }
        return fHttpManager.isAlive();
    }

    public static void stopService() {
        if (fHttpManager == null) {
            LogUtils.d(TAG, "startService: fHttpManager is null, return!");
            return;
        }
        fHttpManager.stopServer();
    }
}
