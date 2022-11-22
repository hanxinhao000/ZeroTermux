/*
Copyright 2014 Pieter Pareit

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

import java.io.File;



/**
 * Implements File Modification Time
 */
public class CmdMDTM extends FtpCmd implements Runnable {
    private static final String TAG = CmdMDTM.class.getSimpleName();

    private String mInput;

    public CmdMDTM(SessionThread sessionThread, String input) {
        super(sessionThread);
        mInput = input;
    }

    @Override
    public void run() {
        Log.d(TAG, "run: MDTM executing, input: " + mInput);
        String param = getParameter(mInput);
        File file = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);

        if (file.exists()) {
            long lastModified = file.lastModified();
            String response = "213 " + UUtils.getFtpDate(lastModified) + "\r\n";
            sessionThread.writeString(response);
        } else {
            Log.w(TAG, "run: file does not exist");
            sessionThread.writeString("550 file does not exist\r\n");
        }

        Log.d(TAG, "run: MDTM completed");
    }

}

