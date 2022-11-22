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

public class CmdFEAT extends FtpCmd implements Runnable {
    private static final String TAG = CmdFEAT.class.getSimpleName();

    public CmdFEAT(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    @Override
    public void run() {
        Log.d(TAG, "run: Giving FEAT");
        sessionThread.writeString("211-Features supported by FTP Server\r\n");
        sessionThread.writeString(" UTF8\r\n");
        sessionThread.writeString(" MDTM\r\n");
        sessionThread.writeString(" MFMT\r\n");
        // https://tools.ietf.org/html/rfc3659#page-50
        // Note that there is no distinct FEAT output for MLSD.  The presence of
        // the MLST feature indicates that both MLST and MLSD are supported.
        sessionThread.writeString(" MLST Type*;Size*;Modify*;Perm\r\n");
        // All hashing algorithms supported by all Android versions
        // See https://developer.android.com/reference/java/security/MessageDigest.html
        sessionThread.writeString(" HASH MD5;SHA-1;SHA-256;SHA-384;SHA-512\r\n");
        sessionThread.writeString(" REST STREAM\r\n");
        sessionThread.writeString(" RANG STREAM\r\n");
        sessionThread.writeString("211 End\r\n");
        Log.d(TAG, "run: Gave FEAT");
    }

}
