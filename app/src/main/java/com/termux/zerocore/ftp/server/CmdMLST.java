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
 * Implements MLST command
 */
public class CmdMLST extends FtpCmd implements Runnable {
    private static final String TAG = CmdMLST.class.getSimpleName();

    private String mInput;

    public CmdMLST(SessionThread sessionThread, String input) {
        super(sessionThread);
        mInput = input;
    }

    @Override
    public void run() {
        Log.d(TAG, "run: LIST executing, input: " + mInput);
        String param = getParameter(mInput);
        
        File fileToFormat = null;
        if(param.equals("")){
            fileToFormat = sessionThread.getWorkingDir();
            param = "/";
        }else{
            fileToFormat = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
        }
        
        if (fileToFormat.exists()) {
            sessionThread.writeString("250- Listing " + param + "\r\n");
            sessionThread.writeString(makeString(fileToFormat) + "\r\n");
            sessionThread.writeString("250 End\r\n");
        } else {
            Log.w(TAG, "run: file does not exist");
            sessionThread.writeString("550 file does not exist\r\n");
        }

        Log.d(TAG, "run: LIST completed");
    }

    public String makeString(File file){
        StringBuilder response = new StringBuilder();
        
        String[] selectedTypes = sessionThread.getFormatTypes();   
        if(selectedTypes != null){
            for (int i = 0; i < selectedTypes.length; ++i) {
                String type = selectedTypes[i];
                if (type.equalsIgnoreCase("size")) {
                    response.append("Size=" + String.valueOf(file.length()) + ';');
                } else if (type.equalsIgnoreCase("modify")) {
                    String timeStr = UUtils.getFtpDate(file.lastModified());
                    response.append("Modify=" + timeStr + ';');
                } else if (type.equalsIgnoreCase("type")) {
                    if (file.isFile()) {
                        response.append("Type=file;");
                    } else if (file.isDirectory()) {
                        response.append("Type=dir;");
                    }
                } else if (type.equalsIgnoreCase("perm")) {
                    response.append("Perm=");
                    if (file.canRead()) {
                        if (file.isFile()) {
                            response.append('r');
                        } else if (file.isDirectory()) {
                            response.append("el");
                        }
                    }
                    if (file.canWrite()) {
                        if (file.isFile()) {
                            response.append("adfw");
                        } else if (file.isDirectory()) {
                            response.append("fpcm");
                        }
                    }
                    response.append(';');
                }
            }
        }

        response.append(' ');    
        response.append(file.getName());
        response.append("\r\n");
        return response.toString();
    }
}

