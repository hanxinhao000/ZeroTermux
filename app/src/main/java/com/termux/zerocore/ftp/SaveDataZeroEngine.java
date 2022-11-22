package com.termux.zerocore.ftp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.zerocore.ftp.server.FtpUser;


public class SaveDataZeroEngine {
    private static String TAG = "SaveDataZeroEngine";

    public static synchronized void putStringData(Context mContext, String key, String values) {
        LogUtils.d(TAG, "putStringData putStringData-----:" + values);
        SharedPreferences mZeroEngineData = mContext.getSharedPreferences("ZeroEngineData", Context.MODE_PRIVATE);
        mZeroEngineData.edit().putString(key, values).apply();
    }

    public static String getStringData(Context mContext, String key) {
        SharedPreferences mZeroEngineData = mContext.getSharedPreferences("ZeroEngineData", Context.MODE_PRIVATE);
        return mZeroEngineData.getString(key, "def");
    }

    public static boolean isEmpty(String key) {
        Context zeroContext = UUtils.getContext();
        if (zeroContext == null) {
            LogUtils.d(TAG, "getFtpUser zeroContext is null return false");
            return false;
        }
        String stringData = SaveDataZeroEngine.getStringData(zeroContext, key);
        return TextUtils.isEmpty(stringData) || "def".equals(stringData);
    }

    public static FtpUser getFtpUser() {
        LogUtils.d(TAG, "getFtpUser context:" + UUtils.getContext());
        Context zeroContext = UUtils.getContext();
        if (zeroContext == null) {
            LogUtils.d(TAG, "getFtpUser zeroContext is null return def FtpUser.");
            return new FtpUser("ftp", "ftp", "def");
        }
        String ftpUserName = SaveDataZeroEngine.getStringData(zeroContext, "ftpUserName");
        String ftpPassWord = SaveDataZeroEngine.getStringData(zeroContext, "ftpPassWord");
        String ftpChroot = SaveDataZeroEngine.getStringData(zeroContext, "ftpChroot");
        return new FtpUser(ftpUserName, ftpPassWord, ftpChroot);
    }

    public static int getPort() {
        Context zeroContext = UUtils.getContext();
        if (zeroContext == null) {
            LogUtils.d(TAG, "getFtpUser zeroContext is null return def FtpUser.");
            return 2121;
        }
        String ftpPort = SaveDataZeroEngine.getStringData(zeroContext, "ftpPort");
        LogUtils.d(TAG,"getPort:" + ftpPort);
        if (isEmpty("ftpPort")) {
            return 2121;
        }
        int port = 2121;
        try {
            port = Integer.parseInt(ftpPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return port;
    }


}
