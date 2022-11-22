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




import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.zerocore.ftp.FTPConfig;
import com.termux.zerocore.ftp.SaveDataZeroEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;


public class SessionThread extends Thread {

    private static final int MAX_AUTH_FAILS = 3;
    public static final int DATA_CHUNK_SIZE = 65536;  // do file I/O in 64k chunks
    public static String TAG = "SessionThread";
    private Socket cmdSocket;
    private boolean pasvMode = false;
    private boolean binaryMode = false;
    private String userName = null;  // username that the client sends
    private boolean userAuthenticated = false;
    private File workingDir = new File(SaveDataZeroEngine.getFtpUser().getChroot());
    private File chrootDir = workingDir;
    private Socket dataSocket = null;
    private File renameFrom = null;
    private LocalDataSocket localDataSocket;
    private InputStream dataInputStream = null;
    private OutputStream dataOutputStream = null;
    private boolean sendWelcomeBanner;
    // FTP control sessions should start out in ASCII, according to the RFC. However, many clients
    // don't turn on UTF-8 even though they support it, so we just turn it on by default.
    protected String encoding = "UTF-8";
    long offset = -1; // where to start append when using REST/RANG
    long endPosition = -1; // where to stop append when using RANG
    private String[] formatTypes = {"Size", "Modify", "Type", "Perm"}; // types option of MLST/MLSD
    private int authFails = 0;
    private String hashingAlgorithm = "SHA-1";

    public SessionThread(Socket socket, LocalDataSocket dataSocket) {
        cmdSocket = socket;
        localDataSocket = dataSocket;
        sendWelcomeBanner = true;
    }

    /**
     * Sends a string over the already-established data socket
     *
     * @param string string to send
     * @return Whether the send completed successfully
     */
    public boolean sendViaDataSocket(String string) {
        try {
            byte[] bytes = string.getBytes(encoding);
            LogUtils.d(TAG, "Using data connection encoding: " + encoding);
            return sendViaDataSocket(bytes, 0, bytes.length);
        } catch (UnsupportedEncodingException e) {
            LogUtils.e(TAG,"Unsupported encoding for data socket send");
            return false;
        }
    }

    /**
     * Sends a byte array over the already-established data socket
     *
     * @param bytes bytes to send
     * @param start start offset of data
     * @param len   number of bytes to write
     * @return true if success
     */
    public boolean sendViaDataSocket(byte[] bytes, int start, int len) {

        if (dataOutputStream == null) {
            LogUtils.d(TAG,"Can't send via null dataOutputStream");
            return false;
        }
        if (len == 0) {
            return true; // this isn't an "error"
        }
        try {
            dataOutputStream.write(bytes, start, len);
        } catch (IOException e) {
            LogUtils.e(TAG,"Couldn't write output stream for data socket, error:" + e.toString());
            return false;
        }
        localDataSocket.reportTraffic(len);
        return true;
    }

    /**
     * Received some bytes from the data socket, which is assumed to already be connected.
     * The bytes are placed in the given array buf, and the number of bytes successfully read
     * is returned.
     *
     * @param buf Where to place the input bytes
     * @return >0 if successful which is the number of bytes read
     *         -1 if no bytes remain to be read
     *         -2 if the data socket was not connected
     *         0 if there was a read  error
     */
    public int receiveFromDataSocket(byte[] buf) {
        int bytesRead;

        if (dataSocket == null) {
            LogUtils.d(TAG,"Can't receive from null dataSocket");
            return -2;
        }
        if (!dataSocket.isConnected()) {
            LogUtils.d(TAG,"Can't receive from unconnected socket");
            return -2;
        }

        try {
            do {
                bytesRead = dataInputStream.read(buf, 0, buf.length);
            } while (bytesRead == 0);
        } catch (IOException e) {
            LogUtils.d(TAG,"Error reading data socket");
            return 0;
        }
        return bytesRead;
    }

    /**
     * Called when we receive a PASV command.
     *
     * @return Whether the necessary initialization was successful.
     */
    public int onPasv() {
        return localDataSocket.onPasv();
    }

    /**
     * Called when we receive a PORT command.
     *
     * @return Whether the necessary initialization was successful.
     */
    public boolean onPort(InetAddress dest, int port) {
        return localDataSocket.onPort(dest, port);
    }

    public InetAddress getDataSocketPasvIp() {
        // When the client sends PASV, our reply will contain the address and port
        // of the data connection that the client should connect to. For this purpose
        // we always use the same IP address that the command socket is using.
        return cmdSocket.getLocalAddress();
    }

    /**
     * Will be called by (e.g.) CmdSTOR, CmdRETR, CmdLIST, etc. when they are about to
     * start actually doing IO over the data socket.
     *
     * Must call closeDataSocket() when done
     *
     * @return true if successful
     */
    public boolean openDataSocket() {
        try {
            dataSocket = localDataSocket.onTransfer();
            if (dataSocket == null) {
                LogUtils.d(TAG,"dataSocketFactory.onTransfer() returned null");
                return false;
            }
            dataInputStream = dataSocket.getInputStream();
            dataOutputStream = dataSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            LogUtils.d(TAG,"IOException getting OutputStream for data socket");
            dataSocket = null;
            return false;
        }
    }

    /**
     * Call when done doing IO over the data socket
     */
    public void closeDataSocket() {
        LogUtils.d(TAG,"Closing data socket");
        if (dataInputStream != null) {
            try {
                dataInputStream.close();
            } catch (IOException ignore) {
            }
            dataInputStream = null;
        }
        if (dataOutputStream != null) {
            try {
                dataOutputStream.close();
            } catch (IOException ignore) {
            }
            dataOutputStream = null;
        }
        if (dataSocket != null) {
            try {
                dataSocket.close();
            } catch (IOException ignore) {
            }
        }
        dataSocket = null;
    }

    public void quit() {
        LogUtils.d(TAG,"SessionThread told to quit");
        closeSocket();
    }

    @Override
    public void run() {
        LogUtils.d(TAG,"SessionThread started");
        // Give client a welcome
        if (sendWelcomeBanner) {
            writeString("220 SwiFTP " + UUtils.getVersionName(UUtils.getContext()) + " ready\r\n");
        }
        // Main loop: read an incoming line and process it
        try {
            final Reader reader = new InputStreamReader(cmdSocket.getInputStream());
            final BufferedReader in = new BufferedReader(reader, 8192); // use 8k buffer
            while (true) {
                String line;
                line = in.readLine(); // will accept \r\n or \n for terminator
                if (line != null) {
                    LogUtils.d(TAG,"Received line from client: " + line);
                    FtpCmd.dispatchCommand(this, line);
                } else {
                    LogUtils.d(TAG,"readLine gave null, quitting");
                    break;
                }
            }
        } catch (IOException e) {
            LogUtils.d(TAG,"Connection was dropped");
        }
        closeSocket();
    }

    public void closeSocket() {
        if (cmdSocket == null) {
            return;
        }
        try {
            cmdSocket.close();
        } catch (IOException ignore) {
        }
    }

    public void writeBytes(byte[] bytes) {
        try {
            OutputStream outputStream = cmdSocket.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            localDataSocket.reportTraffic(bytes.length);
        } catch (IOException e) {
            LogUtils.d(TAG,"Exception writing socket");
            closeSocket();
        }
    }

    public void writeString(String str) {
        LogUtils.d(TAG,"writeString: " + str);
        byte[] strBytes;
        try {
            strBytes = str.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            LogUtils.e(TAG,"Unsupported encoding: " + encoding);
            strBytes = str.getBytes();
        }
        writeBytes(strBytes);
    }

    protected Socket getSocket() {
        return cmdSocket;
    }

    public boolean isPasvMode() {
        return pasvMode;
    }

    static public ByteBuffer stringToBB(String s) {
        return ByteBuffer.wrap(s.getBytes());
    }

    public boolean isBinaryMode() {
        return binaryMode;
    }

    public void setBinaryMode(boolean binaryMode) {
        this.binaryMode = binaryMode;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    /**
     * @return true if we should allow FTP operations
     */
    public boolean isAuthenticated() {
        return true;
    }

    /**
     * @return true only when we are anonymously logged in
     */
    public boolean isAnonymouslyLoggedIn() {
        return true;
       // return !userAuthenticated && FsSettings.allowAnonymous();
    }

    /**
     * @return true if a valid user has logged in
     */
    public boolean isUserLoggedIn() {
        return userAuthenticated;
    }

    public void authAttempt(boolean authenticated) {
        if (authenticated) {
            LogUtils.d(TAG,"Authentication complete");
            userAuthenticated = true;
        } else {
            authFails++;
            LogUtils.d(TAG,"Auth failed: " + authFails + "/" + MAX_AUTH_FAILS);
            if (authFails > MAX_AUTH_FAILS) {
                LogUtils.d(TAG,"Too many auth fails, quitting session");
                quit();
            }
        }
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(File workingDir) {
        try {
            this.workingDir = workingDir.getCanonicalFile().getAbsoluteFile();
        } catch (IOException e) {
            LogUtils.d(TAG,"SessionThread canonical error");
        }
    }

    public Socket getDataSocket() {
        return dataSocket;
    }

    public void setDataSocket(Socket dataSocket) {
        this.dataSocket = dataSocket;
    }

    public File getRenameFrom() {
        return renameFrom;
    }

    public void setRenameFrom(File renameFrom) {
        this.renameFrom = renameFrom;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String[] getFormatTypes() {
        return formatTypes;
    }

    public void setFormatTypes(String[] formatTypes) {
        this.formatTypes = formatTypes;
    }

    public String getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    public void setHashingAlgorithm(String algorithm) {
        this.hashingAlgorithm = algorithm;
    }

    public File getChrootDir() {
        File file = chrootDir.isDirectory() ? chrootDir : FTPConfig.DEF_PATH;
        LogUtils.d(TAG, "chrootDir File Path:" + file.getAbsolutePath());
        return file;
    }

    public void setChrootDir(String chrootPath) {
        if (chrootPath == null)
            return;
        File chrootDir = new File(chrootPath);
        if (chrootDir.isDirectory()) {
            this.chrootDir = chrootDir;
            this.workingDir = chrootDir;
        }
    }
}
