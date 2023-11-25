package com.termux.zerocore.libsu;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ScrollView;

import androidx.annotation.NonNull;


import com.example.xh_lib.utils.UUtils;
import com.termux.BuildConfig;
import com.termux.R;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;
import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.internal.MainShell;
import com.zp.z_file.util.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibSuManage {
    private static LibSuManage mLibSuManage = null;
    private static final String TAG = "LibSuManage";
    private static final String BASHRC_TERMUX_DIR_PATH = FileUrl.INSTANCE.getTimerTermuxDir();
    private static final String BASHRC_TERMUX_DIR_FILE = FileUrl.INSTANCE.getTimerTermuxFile();

    private static final String BASHRC_SHELL_DIR_PATH = FileUrl.INSTANCE.getTimerShellDir();
    private static final String BASHRC_SHELL_DIR_FILE = FileUrl.INSTANCE.getTimerShellFile();
    private static final String BASHRC_SHELL_DIR_LOG = FileUrl.INSTANCE.getTimerShellLogDir();

    private int cunt = 0;

    public int getCunt() {
        return cunt;
    }

    public void setCunt(int cunt) {
        this.cunt = cunt;
    }

    private TimerListener mTimerListener;
    private final List<String> mConsoleList = new TimerCallbackList();

    private Thread mThread;
    private ShellLogRunnable mShellLogRunnable;

    private boolean isRun = false;

    private LibSuManage(){

    }
    public static LibSuManage getInstall() {
        if (mLibSuManage == null) {
            synchronized (LibSuManage.class) {
                if (mLibSuManage == null) {
                    mLibSuManage = new LibSuManage();
                    return mLibSuManage;
                } else {
                    return mLibSuManage;
                }
            }
        } else {
            return mLibSuManage;
        }
    }

    public void initTimer() {
        if (!writerFile()) {
            LogUtils.e(TAG, "initTimer timer file init fail, bashrc.sh not exists..");
            return;
        }
        LogUtils.e(TAG, "initTimer init.....");
        File file = new File(BASHRC_SHELL_DIR_LOG);
        if (!file.exists()) {
            file.mkdirs();
        }
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setInitializers(ZeroTermuxInitializer.class)
        );
    }

    public void initRunnable() {
        mShellLogRunnable = new ShellLogRunnable(new File(BASHRC_SHELL_DIR_LOG, "/" + getLogName()));
        new Thread(mShellLogRunnable).start();
    }

    public boolean writerFile() {
        return writerTermuxFile() && writerShellFile();
    }
    public void deleteAllFile() {
       new File(BASHRC_TERMUX_DIR_FILE).delete();
       new File(BASHRC_SHELL_DIR_FILE).delete();
    }
    public boolean isFileExists() {
        return new File(BASHRC_TERMUX_DIR_FILE).exists()
            && new File(BASHRC_SHELL_DIR_FILE).exists();
    }
    private boolean writerTermuxFile() {
        File timerDir = new File(BASHRC_TERMUX_DIR_PATH);
        File timerFile = new File(BASHRC_TERMUX_DIR_FILE);
        if (!timerDir.exists()) {
            timerDir.mkdirs();
        }
        LogUtils.i(TAG, "writerTermuxFile TermuxFile is : " + timerFile.exists());
        if (timerFile.exists()) {
            return true;
        }
        UUtils.writerFileRaw(timerFile, R.raw.termux_timer);
        return timerFile.exists();
    }

    private boolean writerShellFile() {
        File timerDir = new File(BASHRC_SHELL_DIR_PATH);
        File timerFile = new File(BASHRC_SHELL_DIR_FILE);
        if (!timerDir.exists()) {
            timerDir.mkdirs();
        }
        LogUtils.d(TAG, "writerShellFile ShellFile is : " + timerFile.exists());
        if (timerFile.exists()) {
            return true;
        }
        UUtils.writerFileRaw(timerFile, R.raw.shell_timer);
        return timerFile.exists();
    }

    public void shellCommandExec(String funName) {
        try {
            mThread = new Thread(new ShellCommandExecRunnable(funName, mConsoleList));
            mThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "shellCommandExec is error: " + e);
        }

    }

    public void stop() {
        try {
            Shell shell = Shell.getCachedShell();
            if (shell != null && shell.isAlive()) {
                shell.close();
            }

        } catch (IOException e) {
            Log.e(TAG, "Error when closing shell", e);
        }
    }

    public void logThreadStop() {
        if (mShellLogRunnable != null) {
            mShellLogRunnable.stop();
            mShellLogRunnable = null;
        }
    }

    public boolean isRun() {
        if (mShellLogRunnable == null) {
            return false;
        }
        return !mShellLogRunnable.isStop;
    }

    public void shellCommandSubmit(String funName) {
        mThread = new Thread(new ShellCommandSubmitRunnable(funName, mConsoleList));
        mThread.start();
    }

    private static class ShellCommandExecRunnable implements Runnable {
        private String mFunName;
        private List<String> mConsoleList;
        public ShellCommandExecRunnable(String funName, List<String> consoleList) {
            this.mFunName = funName;
            this.mConsoleList = consoleList;
        }
        @Override
        public void run() {
           Shell.cmd(mFunName).to(mConsoleList).submit();
            LogUtils.e(TAG, "ShellCommandExecRunnable is end.");
        }
    }

    private static class ShellKillAllCommandExecRunnable implements Runnable {
        private String mFunName;
        private List<String> mConsoleList;
        public ShellKillAllCommandExecRunnable(String funName, List<String> consoleList) {
            this.mFunName = funName;
            this.mConsoleList = consoleList;
        }
        @Override
        public void run() {
            Shell.cmd(mFunName).to(mConsoleList).submit();
            LogUtils.e(TAG, "ShellKillAllCommandExecRunnable is end.");
        }
    }

    private static class ShellCommandSubmitRunnable implements Runnable {
        private String mFunName;
        private List<String> mConsoleList;
        public ShellCommandSubmitRunnable(String funName, List<String> consoleList) {
            this.mFunName = funName;
            this.mConsoleList = consoleList;
        }
        @Override
        public void run() {
            Shell.sh(mFunName).to(mConsoleList).submit();
            LogUtils.e(TAG, "ShellCommandSubmitRunnable is end.");
        }
    }



    static class ZeroTermuxInitializer extends Shell.Initializer {
        @Override
        public boolean onInit(@NonNull Context context, @NonNull Shell shell) {
            // Load our init script
            LogUtils.e(TAG, "ZeroTermuxInitializer init.....");
            InputStream bashrc = context.getResources().openRawResource(R.raw.bashrc);
            shell.newJob().add(bashrc).exec();
            return true;
        }
    }

    class TimerCallbackList extends CallbackList<String> {
        @Override
        public void onAddElement(String s) {
            if (mTimerListener != null) {
                mTimerListener.onAddElement(s);
            }
            //输出LOG到指定目录
            if (mShellLogRunnable != null) {
                mShellLogRunnable.writerString(s);
            }
        }
    }


    private static class ShellLogRunnable implements Runnable {
        private  File mFilePath;
        private  boolean isStop;
        private  String message = "";
        PrintWriter printWriter;
        public ShellLogRunnable(File mFilePath) {
            this.mFilePath = mFilePath;
            isStop = false;
            try {
                if (mFilePath.exists()) {
                    mFilePath.createNewFile();
                }
                printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mFilePath)));
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
        }
        @Override
        public void run() {
            while (true) {
                if (isStop) {
                    LogUtils.e(TAG, "ShellLogRunnable stop...");
                    break;
                }
                try {
                    Thread.sleep(500);
                    if (mFilePath.exists()) {
                        mFilePath.createNewFile();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(printWriter != null && (message != null || !message.isEmpty())) {
                    printWriter.print(message);
                    printWriter.flush();
                    message = "";
                }
            }
        }

        public void stop() {
            isStop = true;
        }

        public void writerString(String msg) {
            message += "\n" + msg;
        }
    }
    public void setTimerListener(TimerListener timerListener) {
        this.mTimerListener = timerListener;
    }

    public interface TimerListener {
        public void onAddElement(String msg);
    }

    private String getLogName() {
        return "ZeroTermuxTimer_" + System.currentTimeMillis() + ".log";
    }
}
