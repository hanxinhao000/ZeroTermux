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

/* The code that is common to LIST, NLST and MLSD is implemented in the abstract
 * class CmdAbstractListing, which is inherited here.
 * CmdLIST, CmdNLST and CmdMLSD just override the
 * makeLsString() function in different ways to provide the different forms
 * of output.
 */

package com.termux.zerocore.ftp.server;

import android.util.Log;


import com.example.xh_lib.utils.UUtils;

import java.io.File;



public class CmdMLSD extends CmdAbstractListing implements Runnable {
    static private final String TAG = CmdMLSD.class.getSimpleName();

    private final String input;

    public CmdMLSD(SessionThread sessionThread, String input) {
        super(sessionThread, input);
        this.input = input;
    }

    @Override
    public void run() {
        String errString = null;

        mainblock: {
            String param = getParameter(input);
            Log.d(TAG, "MLSD parameter: " + param);
            File fileToList = null;
            if (param.equals("")) {
                fileToList = sessionThread.getWorkingDir();
            } else {
                if (param.contains("*")) {
                    errString = "550 MLSD does not support wildcards\r\n";
                    break mainblock;
                }
                fileToList = new File(sessionThread.getWorkingDir(), param);
                if (violatesChroot(fileToList)) {
                    errString = "450 MLSD target violates chroot\r\n";
                    break mainblock;
                }
            }
            String listing;
            if (!fileToList.isDirectory()) {
                errString = "501 Not a directory\r\n";
                break mainblock;
            }

            StringBuilder response = new StringBuilder();
            // TBD
            // https://tools.ietf.org/html/rfc3659#page-39
            // MLSD auto need to add [type=cdir] and [type=pdir]
            errString = listDirectory(response, fileToList);
            if (errString != null) {
                break mainblock;
            }
            listing = response.toString();

            errString = sendListing(listing);
            if (errString != null) {
                break mainblock;
            }
        }

        if (errString != null) {
            sessionThread.writeString(errString);
            Log.d(TAG, "MLSD failed with: " + errString);
        } else {
            Log.d(TAG, "MLSD completed OK");
        }
        // The success or error response over the control connection will
        // have already been handled by sendListing, so we can just quit now.
    }

    // Generates a line of a directory listing in the Format of MLSx
    @Override
    protected String makeLsString(File file) {      
        StringBuilder response = new StringBuilder();

        if (!file.exists()) {
            Log.i(TAG, "makeLsString had nonexistent file");
            return null;
        }

        // See Daniel Bernstein's explanation of /bin/ls format at:
        // http://cr.yp.to/ftp/list/binls.html
        // This stuff is almost entirely based on his recommendations.

        String lastNamePart = file.getName();
        // Many clients can't handle files containing these symbols
        if (lastNamePart.contains("*") || lastNamePart.contains("/")) {
            Log.i(TAG, "Filename omitted due to disallowed character");
            return null;
        } else {
            // The following line generates many calls in large directories
            // staticLog.l(Log.DEBUG, "Filename: " + lastNamePart);
        }

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
        response.append(lastNamePart);
        response.append("\r\n");
        return response.toString();
    }

}
