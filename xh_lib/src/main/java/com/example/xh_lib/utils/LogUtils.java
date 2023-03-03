package com.example.xh_lib.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LogUtils {
    public static String TAG = "ZeroTermux";

    public static void e(String tag, String msg) {
        Log.e(TAG + "--" + tag, msg);
    }
    public static void d(String tag, String msg) {
        Log.d(TAG + "--" + tag, msg);
      //  createLogFile(TAG + "--" + tag + msg);
    }
    public static void crawl(String tag, String msg, Exception e) {
        Log.d(TAG + "--" + tag, msg, e);
    }

    public static void createLogFile(String msg) {
        try {
            String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/xinhao/ZeroTermuxLog";
            File logPath = new File(absolutePath);
            if (!logPath.exists()) {
                logPath.mkdirs();
            }
            File logFile = new File(absolutePath, "/ZeroTermux.log");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String fileString = UUtils.getFileString(logFile);
            UUtils.setFileString(logFile, fileString + msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
