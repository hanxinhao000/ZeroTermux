package com.termux.zerocore.libsu;

import android.content.Context;
import android.util.Log;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.BuildConfig;
import com.termux.R;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;
import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibSuManage {
    private static LibSuManage mLibSuManage = null;
    private static final String TAG = "EXAMPLE";
    private static final String BASHRC_DIR_PATH = FileUrl.INSTANCE.getTimerDir();
    private static final String BASHRC_DIR_FILE = FileUrl.INSTANCE.getTimerFile();
    private TimerListener mTimerListener;
    private final List<String> mConsoleList = new TimerCallbackList();

    private ExecutorService mExecutor = Executors.newFixedThreadPool(3);

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
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setInitializers(ZeroTermuxInitializer.class)
        );
    }
    private boolean writerFile() {
        File timerDir = new File(BASHRC_DIR_PATH);
        File timerFile = new File(BASHRC_DIR_FILE);
        if (!timerDir.exists()) {
            timerDir.mkdirs();
        }
        if (timerFile.exists()) {
            return true;
        }
        UUtils.writerFileRaw(timerFile, R.raw.termux_timer);
        return timerFile.exists();
    }

    public boolean writerDebugFile() {
        File timerDir = new File(BASHRC_DIR_PATH);
        File timerFile = new File(BASHRC_DIR_FILE);
        if (!timerDir.exists()) {
            timerDir.mkdirs();
        }
        UUtils.writerFileRaw(timerFile, R.raw.termux_timer);
        return timerFile.exists();
    }

    public void shellCommandExec(String funName) {
        mExecutor.submit(new ShellCommandExecRunnable(funName, mConsoleList));
    }

    public void shellCommandSubmit(String funName) {
        mExecutor.submit(new ShellCommandSubmitRunnable(funName, mConsoleList));
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
            Shell.sh(mFunName).to(mConsoleList).exec();
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
        }
    }

    static class ZeroTermuxInitializer extends Shell.Initializer {
        @Override
        public boolean onInit(@NonNull Context context, @NonNull Shell shell) {
            // Load our init script
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
        }
    }
    public void setTimerListener(TimerListener timerListener) {
        this.mTimerListener = timerListener;
    }

    public interface TimerListener {
        public void onAddElement(String msg);
    }
}
