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

import com.example.xh_lib.utils.LogUtils;

import java.io.File;
import java.io.IOException;

/**
 * PRINT WORKING DIRECTORY (PWD)
 * Command returns the working directory in the reply.
 */
public class CmdPWD extends FtpCmd implements Runnable {
    private static final String TAG = CmdPWD.class.getSimpleName();

    public CmdPWD(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    @Override
    public void run() {
        Log.d(TAG, "PWD executing");
        // We assume that the chroot restriction has been applied, and that
        // therefore the current directory is located somewhere within the
        // chroot directory. Therefore, we can just slice of the chroot
        // part of the current directory path in order to get the
        // user-visible path (inside the chroot directory).
        try {
            String currentDir = sessionThread.getWorkingDir().getCanonicalPath();
            File chrootDir = sessionThread.getChrootDir();
            LogUtils.d(TAG, "run currentDir:" + currentDir);
            LogUtils.d(TAG, "run chrootDir:" + chrootDir);
         /*   if (chrootDir != null) {
                currentDir = currentDir.substring(chrootDir.getCanonicalPath().length());
            }*/
            // The root directory requires special handling to restore its
            // leading slash
            if (currentDir.length() == 0) {
                currentDir = "/";
            }
            sessionThread.writeString("257 \"" + currentDir + "\"\r\n");
        } catch (IOException e) {
            // This shouldn't happen unless our input validation has failed
            Log.e(TAG, "PWD canonicalize");
            sessionThread.closeSocket(); // should cause thread termination
        }
        Log.d(TAG, "PWD complete");
    }

}
