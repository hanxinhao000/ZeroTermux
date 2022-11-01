package com.example.xh_lib.utils;

import android.util.Log;

public class LogUtils {
    public static String TAG = "ZeroTermux";

    public static void e(String tag, String msg) {
        Log.e(TAG + "--" + tag, msg);
    }
    public static void d(String tag, String msg) {
        Log.d(TAG + "--" + tag, msg);
    }

}
