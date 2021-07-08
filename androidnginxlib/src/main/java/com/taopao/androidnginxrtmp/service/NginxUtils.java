package com.taopao.androidnginxrtmp.service;

import android.content.Context;
import android.content.Intent;

/**
 * @Author：淘跑
 * @Date: 2018/11/19 08:56
 * @Use：
 */
public class NginxUtils {
    /**
     * 开启服务
     *
     * @param context
     */
    public static void startNginx(Context context) {
        Intent intent = new Intent(context, NginxService.class);
        intent.setAction(NginxService.ACTION_START_NGINX);
        context.startService(intent);
    }

    public static void stopNginx(Context context) {
        Intent intent = new Intent(context, NginxService.class);
        intent.setAction(NginxService.ACTION_STOP_NGINX);
        context.startService(intent);
    }
}