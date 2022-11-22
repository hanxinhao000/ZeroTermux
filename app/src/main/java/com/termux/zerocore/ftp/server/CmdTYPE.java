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

public class CmdTYPE extends FtpCmd implements Runnable {
    private static final String TAG = CmdTYPE.class.getSimpleName();

    String input;

    public CmdTYPE(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        String output;
        Log.d(TAG, "TYPE executing");
        String param = getParameter(input);
        if (param.equals("I") || param.equals("L 8")) {
            output = "200 Binary type set\r\n";
            sessionThread.setBinaryMode(true);
        } else if (param.equals("A") || param.equals("A N")) {
            output = "200 ASCII type set\r\n";
            sessionThread.setBinaryMode(false);
        } else {
            output = "503 Malformed TYPE command\r\n";
        }
        sessionThread.writeString(output);
        Log.d(TAG, "TYPE complete");
    }

}
