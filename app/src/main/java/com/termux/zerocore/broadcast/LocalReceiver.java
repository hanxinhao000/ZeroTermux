package com.termux.zerocore.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.SaveData;
import com.example.xh_lib.utils.UUtils;
import com.termux.BuildConfig;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.terminal.Logger;
import com.termux.zerocore.bosybox.BusyBoxManager;
import com.termux.zerocore.developer.DeveloperActivity;
import com.termux.zerocore.utils.FileHttpUtils;
import com.termux.zerocore.utils.Z7ExtracatUtils;
import com.zp.z_file.zerotermux.ZTConfig;

import java.io.File;

public class LocalReceiver extends BroadcastReceiver {
    private static String TAG = "LocalReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (intent == null ) {
            return;
        }
        String broadcastString = intent.getStringExtra("broadcastString");
        if (broadcastString != null && !(broadcastString.isEmpty())) {
            TermuxActivity.mTerminalView.sendTextToTerminal(broadcastString + "\n");
            if (ZTConfig.INSTANCE.getCloseListener() != null) {
                ZTConfig.INSTANCE.getCloseListener().close();
            }
            return;
        }
        String broadcastStringTAR = intent.getStringExtra("broadcastStringTar");
        if (broadcastStringTAR != null && !(broadcastStringTAR.isEmpty())) {
            LogUtils.d(TAG, "onReceive broadcastStringTAR:" + broadcastStringTAR);
            try {
                String[] split = broadcastStringTAR.split(",");
                if (split.length != 2) {
                    LogUtils.d(TAG, "onReceive split.length is not 2" );
                    return;
                }
                String name = new File(split[0]).getName();
                String command = "";
                if (name.endsWith("tar.gz")) {
                    command = "tar -xzvf \"" + split[0] + "\" -C " + split[1] + "/ && echo \"解压完成 Decompression complete\"";
                } else if (name.endsWith("tar.bz2")) {
                    command = "tar -xjf \"" + split[0] + "\" -C " + split[1] + "/ && echo \"解压完成 Decompression complete\"";
                } else if (name.endsWith("tar.xz")) {
                    command = "tar -xvJf \"" + split[0] + "\" -C " + split[1] + "/ && echo \"解压完成 Decompression complete\"";
                } else {
                    command = "echo \"不能识别的格式(unrecognized format)\"";
                }

                if (ZTConfig.INSTANCE.getCloseListener() != null) {
                    ZTConfig.INSTANCE.getCloseListener().close();
                }
                TermuxActivity.mTerminalView.sendTextToTerminal( command + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        String broadcastString7Z = intent.getStringExtra("broadcastString7Z");
        if (broadcastString7Z != null && !(broadcastString7Z.isEmpty())) {
            LogUtils.d(TAG, "onReceive broadcastStringTAR:" + broadcastString7Z);

            Z7ExtracatUtils.INSTANCE.setMUnZipCallBack(new Z7ExtracatUtils.UnZipCallBack() {
                @Override
                public void onStart() {

                }

                @Override
                public void onGetFileNum(int fileNum) {

                }

                @Override
                public void onProgress(@Nullable String name, long size) {
                    if (ZTConfig.INSTANCE.getZ7Listener() != null) {
                        UUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                ZTConfig.INSTANCE.getZ7Listener().decompress(name, false, false);
                            }
                        });
                    }
                }

                @Override
                public void onError(int errorCode, @Nullable String message) {
                    if (ZTConfig.INSTANCE.getZ7Listener() != null) {
                       UUtils.runOnUIThread(new Runnable() {
                           @Override
                           public void run() {
                               ZTConfig.INSTANCE.getZ7Listener().decompress("error:" + message, true, true);
                           }
                       });
                    }
                }

                @Override
                public void onSucceed() {
                    if (ZTConfig.INSTANCE.getZ7Listener() != null) {
                       UUtils.runOnUIThread(new Runnable() {
                           @Override
                           public void run() {
                               ZTConfig.INSTANCE.getZ7Listener().decompress("", true, false);
                           }
                       });
                    }
                }
            });
            try {
                String[] split = broadcastString7Z.split(",");
                if (split.length != 2) {
                    LogUtils.d(TAG, "onReceive split.length is not 2" );
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Z7ExtracatUtils.INSTANCE.unZipFile(new File(split[0]), new File(split[1]));
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.d(TAG, "onReceive 7Z Error:" + e.toString() );
            }
            return;
        }

        String broadcastHttp = intent.getStringExtra("broadcastHttp");
        if (broadcastHttp != null && !(broadcastHttp.isEmpty())) {
            if (broadcastHttp.equals("open")) {
                UUtils.showMsg("ok--open");
                FileHttpUtils.Companion.get().startServer();
                FileHttpUtils.Companion.get().setHttpBoot();
            }
            if (broadcastHttp.equals("close")) {
                UUtils.showMsg("ok--close");
                FileHttpUtils.Companion.get().stopServer();
                FileHttpUtils.Companion.get().cancelHttpBoot();
            }

            return;
        }
        String broadcastStartActivity = intent.getStringExtra("broadcastStartActivity");
        if (broadcastStartActivity != null && !(broadcastStartActivity.isEmpty())) {
            if (broadcastStartActivity.equals("DeveloperActivity") && BuildConfig.DEBUG) {
                Intent intent1 = new Intent(context, DeveloperActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            } else {
                UUtils.showMsg(UUtils.getString(R.string.developer_activity));
            }
            return;
        }
    }
}
