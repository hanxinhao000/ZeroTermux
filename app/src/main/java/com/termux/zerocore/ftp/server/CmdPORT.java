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

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CmdPORT extends FtpCmd implements Runnable {
    private static final String TAG = CmdPORT.class.getSimpleName();

    String input;

    public CmdPORT(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Log.d(TAG, "PORT executing");
        String errString = null;
        mainBlock: {
            String param = getParameter(input);
            if (param.contains("|") && param.contains("::")) {
                errString = "550 No IPv6 support, reconfigure your client\r\n";
                break mainBlock;
            }
            String[] substrs = param.split(",");
            if (substrs.length != 6) {
                errString = "550 Malformed PORT argument\r\n";
                break mainBlock;
            }
            for (int i = 0; i < substrs.length; i++) {
                // Check that each IP/port octet is numeric and not too long
                if (!substrs[i].matches("[0-9]+") || substrs[i].length() > 3) {
                    errString = "550 Invalid PORT argument: " + substrs[i] + "\r\n";
                    break mainBlock;
                }
            }
            byte[] ipBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                try {
                    // We have to manually convert unsigned to signed
                    // byte representation.
                    int ipByteAsInt = Integer.parseInt(substrs[i]);
                    if (ipByteAsInt >= 128) {
                        ipByteAsInt -= 256;
                    }
                    ipBytes[i] = (byte) ipByteAsInt;
                } catch (Exception e) {
                    errString = "550 Invalid PORT format: " + substrs[i] + "\r\n";
                    break mainBlock;
                }
            }
            InetAddress inetAddr;
            try {
                inetAddr = InetAddress.getByAddress(ipBytes);
            } catch (UnknownHostException e) {
                errString = "550 Unknown host\r\n";
                break mainBlock;
            }

            int port = Integer.parseInt(substrs[4]) * 256 + Integer.parseInt(substrs[5]);

            sessionThread.onPort(inetAddr, port);
        }
        if (errString == null) {
            sessionThread.writeString("200 PORT OK\r\n");
        } else {
            Log.i(TAG, "PORT error: " + errString);
            sessionThread.writeString(errString);
        }
        Log.d(TAG, "PORT completed");
    }
}
