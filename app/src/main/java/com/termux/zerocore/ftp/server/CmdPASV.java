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

public class CmdPASV extends FtpCmd implements Runnable {
    private static final String TAG = CmdPASV.class.getSimpleName();

    public CmdPASV(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    @Override
    public void run() {
        String cantOpen = "502 Couldn't open a port\r\n";
        Log.d(TAG, "PASV running");
        int port;
        if ((port = sessionThread.onPasv()) == 0) {
            // There was a problem opening a port
            Log.e(TAG, "Couldn't open a port for PASV");
            sessionThread.writeString(cantOpen);
            return;
        }
        InetAddress address = sessionThread.getDataSocketPasvIp();
        if (address == null) {
            Log.e(TAG, "PASV IP string invalid");
            sessionThread.writeString(cantOpen);
            return;
        }
        Log.d(TAG, "PASV sending IP: " + address.getHostAddress());
        if (port < 1) {
            Log.e(TAG, "PASV port number invalid");
            sessionThread.writeString(cantOpen);
            return;
        }
        StringBuilder response = new StringBuilder("227 Entering Passive Mode (");
        // Output our IP address in the format xxx,xxx,xxx,xxx
        response.append(address.getHostAddress().replace('.', ','));
        response.append(",");
        // Output our port in the format p1,p2 where port=p1*256+p2
        response.append(port / 256);
        response.append(",");
        response.append(port % 256);
        response.append(").\r\n");
        String responseString = response.toString();
        sessionThread.writeString(responseString);
        Log.d(TAG, "PASV completed, sent: " + responseString);
    }
}
