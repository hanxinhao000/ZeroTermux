package com.termux.zerocore.bean;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.zerocore.utils.FileIOUtils;

public class SaveDataZeroEngine {
    public static int FTP_START_SUCCESS = 8000;
    public static int FTP_START_FAIL = 8001;
    public static int FTP_STARTING = 8000;
    public static int FTP_MIN_PORT = 1001;
    public static int FTP_MAX_PORT = 65534;
    //FTP
    public static String FTP_PASS_WORD = "ftpPassWord";
    public static String FTP_USER_NAME = "ftpUserName";
    public static String FTP_PORT = "ftpPort";
    public static String FTP_CHROOT = "ftpChroot";
    public static String FTP_DEF_USER = "ftp";
    public static String FTP_DEF_PWD = "ftp";
    public static String FTP_DEF_PORT = "2121";
    public static String FTP_SDCARD_ROOT = FileIOUtils.INSTANCE.getSdcardPath();
    public static String FTP_ZERO_TERMUX_FILE = FileIOUtils.INSTANCE.getFilePath();

    public static String TAG = "SaveDataZeroEngine";
    public static void putStringData(Context mContext, String key, String values) {
        LogUtils.d(TAG, "putStringData----- key:" + key + " values:" + values);
        SharedPreferences mZeroEngineData = mContext.getSharedPreferences("ZeroEngineData", Context.MODE_PRIVATE);
        mZeroEngineData.edit().putString(key, values).apply();
    }

    public static String getStringData(Context mContext, String key) {
        SharedPreferences mZeroEngineData = mContext.getSharedPreferences("ZeroEngineData", Context.MODE_PRIVATE);
        return mZeroEngineData.getString(key, "def");
    }

    public static boolean isEmpty(Context mContext, String key) {
        String stringData = getStringData(mContext, key);
        return TextUtils.isEmpty(stringData) || "def".equals(stringData);
    }

}
