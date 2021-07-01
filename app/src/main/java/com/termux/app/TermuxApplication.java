package com.termux.app;

import android.app.Application;
import android.content.Intent;
import android.widget.Toast;

import com.arialyy.aria.core.Aria;
import com.example.xh_lib.application.XHApplication;
import com.hjq.permissions.XXPermissions;
import com.lzy.okgo.OkGo;
import com.mallotec.reb.localeplugin.LocaleConstant;
import com.mallotec.reb.localeplugin.LocalePlugin;
import com.termux.shared.crash.CrashHandler;
import com.termux.shared.crash.TermuxCrashUtils;
import com.termux.shared.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.logger.Logger;
import com.termux.zerocore.activity.UncaughtExceptionHandlerActivity;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


public class TermuxApplication extends XHApplication {
    public void onCreate() {
        super.onCreate();

        // Set crash handler for the app
        TermuxCrashUtils.setCrashHandler(this);
        Aria.init(this);
        Aria.get(this).getDownloadConfig().setMaxSpeed(0);
        Aria.get(this).getDownloadConfig().setConvertSpeed(true);
        // Set log level for the app
        setLogLevel();
        LocalePlugin.INSTANCE.init(this, LocaleConstant.RECREATE_CURRENT_ACTIVITY);
        OkGo.getInstance().init(this);
        OkHttpClient okHttpClient = OkGo.getInstance().getOkHttpClient();

        okHttpClient.newBuilder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .build();
        OkGo.getInstance().setOkHttpClient(okHttpClient);

        XXPermissions.setScopedStorage(true);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {


                Intent intent = new Intent(TermuxApplication.this, UncaughtExceptionHandlerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("error", collectExceptionInfo((Exception) e));
                TermuxApplication.this.startActivity(intent);
                System.exit(1);//关闭已奔溃的app进程

            }
        });

    }

    private void setLogLevel() {
        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(getApplicationContext());
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
        Logger.logDebug("Starting Application");
    }



    private String collectExceptionInfo(Exception extra) {


        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutput);
        extra.printStackTrace(printStream);
        try {
            String s = byteArrayOutput.toString("utf-8");
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
            return s;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return "are.you.kidding.me.NoExceptionFoundException: This is a bug, please contact developers!";
    }
}

