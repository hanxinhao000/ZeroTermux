/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.termux.zerocore.ftp.server;

import android.text.TextUtils;
import android.util.Log;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.ftp.SaveDataZeroEngine;

import java.io.File;
import java.io.IOException;

public class CmdCWD extends FtpCmd implements Runnable {
    private static final String TAG = CmdCWD.class.getSimpleName();

    protected String input;

    public CmdCWD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Log.d(TAG, "CWD executing");
        String param = getParameter(input);
        File newDir = new File(param);
        String errString = null;
        mainblock: {
            if (TextUtils.isEmpty(param)) {
                 newDir = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
            }
            Log.i(TAG, "run ChrootDir: " + sessionThread.getChrootDir());
            Log.i(TAG, "run WorkingDir: " + sessionThread.getWorkingDir());
            Log.i(TAG, "run newDir: " + newDir);
            Log.i(TAG, "run param: " + param);
            if (!newDir.exists()) {
                try {
                    File chroot = sessionThread.getChrootDir();
                    String canonicalChroot = chroot.getCanonicalPath();
                    String canonicalPath = newDir.getCanonicalPath();
                    LogUtils.d(TAG, "violatesChroot canonicalChroot:" + canonicalChroot);
                    LogUtils.d(TAG, "violatesChroot canonicalPath:" + canonicalPath);
                    newDir = new File(canonicalChroot + canonicalPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (newDir.getAbsolutePath().equals("/")) {
                newDir = new File(SaveDataZeroEngine.getFtpUser().getChroot());
            }
            // Ensure the new path does not violate the chroot restriction
            Log.i(TAG, "newDir is New: " + newDir.getAbsolutePath());
            if (violatesChroot(newDir)) {
                errString = "550 Invalid name or chroot violation\r\n";
                sessionThread.writeString(errString);
                Log.i(TAG, errString);
                break mainblock;
            }


            try {
              //  newDir = newDir.getCanonicalFile();
                Log.i(TAG, "New directory: " + newDir);
                Log.i(TAG, "New is directory: " + newDir.isDirectory());
                Log.i(TAG, "New is isSymbolicLink: " + newDir.getParent());
                if (!newDir.isDirectory()) {
                    sessionThread.writeString(UUtils.getString(R.string.ftp_error) + "\r\n");
                } else if (newDir.canRead()) {
                    sessionThread.setWorkingDir(newDir);
                    sessionThread.writeString("250 CWD successful\r\n");
                } else {
                    sessionThread.writeString("550 That path is inaccessible\r\n");
                }
            } catch (Exception e) {
                sessionThread.writeString("550 Invalid path\r\n");
                break mainblock;
            }
        }
        Log.d(TAG, "CWD complete");
    }
}
