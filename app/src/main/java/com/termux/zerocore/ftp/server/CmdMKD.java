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

import android.util.Log;


import com.example.xh_lib.utils.UUtils;
import com.termux.zerocore.ftp.utils.FileUtil;

import java.io.File;



public class CmdMKD extends FtpCmd implements Runnable {
    private static final String TAG = CmdMKD.class.getSimpleName();

    String input;

    public CmdMKD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Log.d(TAG, "MKD executing");
        String param = getParameter(input);
        File toCreate;
        String errString = null;
        mainblock:
        {
            // If the param is an absolute path, use it as is. If it's a
            // relative path, prepend the current working directory.
            if (param.length() < 1) {
                errString = "550 Invalid name\r\n";
                break mainblock;
            }
            toCreate = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
            if (violatesChroot(toCreate)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break mainblock;
            }
            if (toCreate.exists()) {
                errString = "550 Already exists\r\n";
                break mainblock;
            }

            if (!FileUtil.mkdirs(UUtils.getContext(), toCreate)) {
                errString = "550 Error making directory (permissions?)\r\n";
                break mainblock;
            }
        }
        if (errString != null) {
            sessionThread.writeString(errString);
            Log.i(TAG, "MKD error: " + errString.trim());
        } else {
            sessionThread.writeString("250 Directory created\r\n");
        }
        Log.i(TAG, "MKD complete");
    }

}
