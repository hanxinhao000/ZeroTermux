package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.arialyy.aria.core.Aria;
import com.example.xh_lib.application.XHApplication;
import com.hjq.permissions.XXPermissions;
import com.hzy.lib7z.Z7Extractor;
import com.lzy.okgo.OkGo;
import com.mallotec.reb.localeplugin.LocaleConstant;
import com.mallotec.reb.localeplugin.LocalePlugin;

import com.termux.shared.errors.Error;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxBootstrap;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.am.TermuxAmSocketServer;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.zerocore.activity.UncaughtExceptionHandlerActivity;
import com.termux.zerocore.bean.SaveDataZeroEngine;
import com.termux.zerocore.bosybox.BusyBoxManager;
import com.termux.zerocore.filetype.MyFileImageListener;
import com.termux.zerocore.ftp.utils.UserSetManage;
import com.termux.zerocore.libsu.LibSuManage;
import com.termux.zerocore.utils.ClipBoardUtil;
import com.termux.zerocore.zero.engine.ZeroCoreManage;
import com.zp.z_file.common.ZFileManageHelp;
import com.zp.z_file.content.ZFileConfiguration;
import com.zp.z_file.util.LogUtils;
import com.zp.z_file.util.ZFileUUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import cn.hotapk.fastandrutils.utils.FUtils;
import okhttp3.OkHttpClient;

// ZeroTermux add {@
//public class TermuxApplication extends Application {
public class TermuxApplication extends XHApplication {
// @}
    private static final String LOG_TAG = "TermuxApplication";


    public void onCreate() {
        super.onCreate();
        // ZeroTermux add {@
        FUtils.init(this);
		// @}


        Context context = getApplicationContext();

        // Set crash handler for the app
        TermuxCrashUtils.setDefaultCrashHandler(this);
		// ZeroTermux add {@
        ZFileUUtils.initUUtils(mContext, mHandler);

        LogUtils.isShow = UserSetManage.Companion.get().getZTUserBean().isOutputLOG();

        ZFileManageHelp.getInstance().init(new MyFileImageListener());
        ZFileConfiguration.Companion.setMApplicationContext(this);
		// @}
        // Set log config for the app
        setLogConfig(context);
        // ZeroTermux add {@
        // Z7Extractor.init();
		// @}

        Logger.logDebug("Starting Application");

        // Set TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER and TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT
        // ZeroTermux add {@
		//TermuxBootstrap.setTermuxPackageManagerAndVariant(BuildConfig.TERMUX_PACKAGE_VARIANT);
		TermuxBootstrap.setTermuxPackageManagerAndVariant("apt-android-7");
		// @}

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties properties = TermuxAppSharedProperties.init(context);

        // Init app wide shell manager
        TermuxShellManager shellManager = TermuxShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create termux files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = TermuxFileUtils.isTermuxFilesDirectoryAccessible(this, true, true);
        boolean isTermuxFilesDirectoryAccessible = error == null;
        if (isTermuxFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "Termux files directory is accessible");

            error = TermuxFileUtils.isAppsTermuxAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/termux-app directory failed\n" + error);
                return;
            }

            // Setup termux-am-socket server
            TermuxAmSocketServer.setupTermuxAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "Termux files directory is not accessible\n" + error);
        }

        // Init TermuxShellEnvironment constants and caches after everything has been setup including termux-am-socket server
        TermuxShellEnvironment.init(this);

        if (isTermuxFilesDirectoryAccessible) {
            TermuxShellEnvironment.writeEnvironmentToFile(this);
        }
		// ZeroTermux add {@
        Aria.init(this);
        Aria.get(this).getDownloadConfig().setMaxSpeed(0);
        Aria.get(this).getDownloadConfig().setConvertSpeed(true);
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
        //初始化定时器
        LibSuManage.getInstall().initTimer();

        new ClipBoardUtil().registerClipEvents();

/*        new Thread(new Runnable() {
            @Override
            public void run() {
                BusyBoxManager.INSTANCE.init();
            }
        }).start();*/
		// @}

    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

   // ZeroTermux add {@
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
	// @}


}

