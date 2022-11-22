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

public class CmdSYST extends FtpCmd implements Runnable {
    private static final String TAG = CmdSYST.class.getSimpleName();

    // This is considered a safe response to the SYST command, see
    // http://cr.yp.to/ftp/syst.html
    public static final String response = "215 UNIX Type: L8\r\n";

    public CmdSYST(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    @Override
    public void run() {
        Log.d(TAG, "SYST executing");
        sessionThread.writeString(response);
        Log.d(TAG, "SYST finished");
    }
}
