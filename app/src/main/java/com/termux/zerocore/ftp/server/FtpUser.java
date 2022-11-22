package com.termux.zerocore.ftp.server;

import androidx.annotation.NonNull;


import com.example.xh_lib.utils.LogUtils;
import com.termux.zerocore.ftp.FTPConfig;

import java.io.File;



public class FtpUser {
    public static String TAG = "FtpUser";
    final private String mUsername;
    final private String mPassword;
    final private String mChroot;

    public FtpUser(@NonNull String username, @NonNull String password, @NonNull String chroot) {
        mUsername = username;
        mPassword = password;

        final File rootPath = new File(chroot);
        mChroot = rootPath.isDirectory() ? chroot : FTPConfig.DEF_PATH.getPath();

    }

    public String getUsername() {
        LogUtils.d(TAG, "getUsername:" + mUsername);
        return mUsername;
    }

    public String getPassword() {
        LogUtils.d(TAG, "getPassword:" + mPassword);
        return mPassword;
    }

    public String getChroot() {
        LogUtils.d(TAG, "getChroot:" + mChroot);
        return mChroot;
    }

    @Override
    public String toString() {
        return "FtpUser{" +
            "mUsername='" + mUsername + '\'' +
            ", mPassword='" + mPassword + '\'' +
            ", mChroot='" + mChroot + '\'' +
            '}';
    }
}
