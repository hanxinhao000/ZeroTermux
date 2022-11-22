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

import java.io.File;
import java.io.IOException;

public class CmdCDUP extends FtpCmd implements Runnable {
    private static final String TAG = CmdCDUP.class.getSimpleName();
    protected String input;

    public CmdCDUP(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    @Override
    public void run() {
        Log.d(TAG, "CDUP executing");
        File newDir;
        String errString = null;
        mainBlock: {
            File workingDir = sessionThread.getWorkingDir();
            newDir = workingDir.getParentFile();
            if (newDir == null) {
                errString = "550 Current dir cannot find parent\r\n";
                break mainBlock;
            }
            // Ensure the new path does not violate the chroot restriction
            if (violatesChroot(newDir)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break mainBlock;
            }

            try {
                newDir = newDir.getCanonicalFile();
                if (!newDir.isDirectory()) {
                    errString = "550 Can't CWD to invalid directory\r\n";
                    break mainBlock;
                } else if (newDir.canRead()) {
                    sessionThread.setWorkingDir(newDir);
                } else {
                    errString = "550 That path is inaccessible\r\n";
                    break mainBlock;
                }
            } catch (IOException e) {
                errString = "550 Invalid path\r\n";
                break mainBlock;
            }
        }
        if (errString != null) {
            sessionThread.writeString(errString);
            Log.i(TAG, "CDUP error: " + errString);
        } else {
            sessionThread.writeString("200 CDUP successful\r\n");
            Log.d(TAG, "CDUP success");
        }
    }
}
