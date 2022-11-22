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


import com.termux.zerocore.ftp.FsService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;



public class LocalDataSocket {
    private static final String TAG = LocalDataSocket.class.getSimpleName();

    private static final int SO_TIMEOUT_MS = 30000; // socket timeout millis
    private static final int TCP_CONNECTION_BACKLOG = 5;


    // Listener socket used for PASV mode
    ServerSocket server = null;
    // Remote IP & port information used for PORT mode
    private InetAddress remoteAddress;
    private int remotePort;
    private boolean isPasvMode = true;

    public LocalDataSocket() {
        clearState();
    }

    /**
     * Clears the state of this object, as if no pasv() or port() had occurred. All
     * sockets are closed.
     */
    private void clearState() {
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
            }
        }
        server = null;
        remoteAddress = null;
        remotePort = 0;
        Log.d(TAG, "State cleared");
    }

    public int onPasv() {
        clearState();
        try {
            // Listen on any port (port parameter 0)
            server = new ServerSocket(0, TCP_CONNECTION_BACKLOG);
            Log.d(TAG, "Data socket pasv() listen successful");
            return server.getLocalPort();
        } catch (IOException e) {
            Log.e(TAG, "Data socket creation error");
            clearState();
            return 0;
        }
    }

    public boolean onPort(InetAddress remoteAddress, int remotePort) {
        clearState();
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        return true;
    }

    public Socket onTransfer() {
        if (server == null) {
            // We're in PORT mode (not PASV)
            if (remoteAddress == null || remotePort == 0) {
                Log.i(TAG, "PORT mode but not initialized correctly");
                clearState();
                return null;
            }
            Socket socket;
            try {
                socket = new Socket(remoteAddress, remotePort);
            } catch (IOException e) {
                Log.i(TAG, "Couldn't open PORT data socket to: " + remoteAddress.toString()
                        + ":" + remotePort);
                clearState();
                return null;
            }

            // Kill the socket if nothing happens for X milliseconds
            try {
                socket.setSoTimeout(SO_TIMEOUT_MS);
            } catch (Exception e) {
                Log.e(TAG, "Couldn't set SO_TIMEOUT");
                clearState();
                return null;
            }

            return socket;
        } else {
            // We're in PASV mode (not PORT)
            Socket socket = null;
            try {
                socket = server.accept();
                Log.d(TAG, "onTransfer pasv accept successful");
            } catch (Exception e) {
                Log.i(TAG, "Exception accepting PASV socket");
                socket = null;
            }
            clearState();
            return socket; // will be null if error occurred
        }
    }

    /**
     * Return the port number that the remote client should be informed of (in the body of
     * the PASV response).
     *
     * @return The port number, or -1 if error.
     */
    public int getPortNumber() {
        if (server != null) {
            return server.getLocalPort(); // returns -1 if server socket is unbound
        } else {
            return -1;
        }
    }

    public InetAddress getPasvIp() {
        return FsService.getLocalInetAddress();
    }

    public void reportTraffic(long bytes) {
        // ignore, we don't care about how much traffic goes over wifi.
    }
}
