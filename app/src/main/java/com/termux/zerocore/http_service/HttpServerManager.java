package com.termux.zerocore.http_service;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import cn.hotapk.fhttpserver.FHttpManager;

public class HttpServerManager {
    static String TAG = "HttpServerManager";
    static FHttpManager fHttpManager;
    public static void startService(int port, String root) {
        LogUtils.d(TAG, "startService: fHttpManager root is:" + root);
        if (fHttpManager == null) {
            LogUtils.d(TAG, "startService: fHttpManager is null, new.");
            fHttpManager = FHttpManager.init(UUtils.getContext(), UserController.class, AppController.class);
        }
        try {
            fHttpManager.setPort(port);
            fHttpManager.setResdir(root);
            fHttpManager.setAllowCross(true);
            fHttpManager.startServer();
            LogUtils.d(TAG, "startService: fHttpManager start Ok");
        } catch (Exception e){
            LogUtils.e(TAG, "startService: error " + e.toString());
        }
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
        try {
            fHttpManager.stopServer();
            LogUtils.d(TAG, "startService: fHttpManager stopServer OK.");
        } catch (Exception e) {
            LogUtils.e(TAG, "startService: fHttpManager stopServer ERROR." + e.toString());
        }

    }
}
