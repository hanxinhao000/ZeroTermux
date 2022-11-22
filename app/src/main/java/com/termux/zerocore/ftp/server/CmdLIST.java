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

/* The code that is common to LIST and NLST is implemented in the abstract
 * class CmdAbstractListing, which is inherited here.
 * CmdLIST and CmdNLST just override the
 * makeLsString() function in different ways to provide the different forms
 * of output.
 */

package com.termux.zerocore.ftp.server;





import com.example.xh_lib.utils.LogUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CmdLIST extends CmdAbstractListing implements Runnable {

    // The approximate number of milliseconds in 6 months
    public final static long MS_IN_SIX_MONTHS = 6L * 30L * 24L * 60L * 60L * 1000L;
    private final String input;
    public static String TAG = "CmdLIST";

    public CmdLIST(SessionThread sessionThread, String input) {
        super(sessionThread, input);
        this.input = input;
    }

    @Override
    public void run() {
        String errString = null;

        mainblock: {
            String param = getParameter(input);
            LogUtils.d(TAG, "LIST parameter: " + param);
            while (param.startsWith("-")) {
                // Skip all dashed -args, if present
                LogUtils.d(TAG, "LIST is skipping dashed arg " + param);
                param = getParameter(param);
            }
            File fileToList = null;
            if (param.equals("")) {
                fileToList = sessionThread.getWorkingDir();
            } else {
                if (param.contains("*")) {
                    errString = "550 LIST does not support wildcards\r\n";
                    break mainblock;
                }
                fileToList = new File(sessionThread.getWorkingDir(), param);
                if (violatesChroot(fileToList)) {
                    errString = "450 Listing target violates chroot\r\n";
                    break mainblock;
                }
            }
            String listing;
            if (fileToList.isDirectory()) {
                StringBuilder response = new StringBuilder();
                errString = listDirectory(response, fileToList);
                if (errString != null) {
                    break mainblock;
                }
                listing = response.toString();
            } else {
                listing = makeLsString(fileToList);
                if (listing == null) {
                    errString = "450 Couldn't list that file\r\n";
                    break mainblock;
                }
            }
            errString = sendListing(listing);
            if (errString != null) {
                break mainblock;
            }
        }

        if (errString != null) {
            sessionThread.writeString(errString);
            LogUtils.d(TAG, "LIST failed with: " + errString);
        } else {
            LogUtils.d(TAG, "LIST completed OK");
        }
        // The success or error response over the control connection will
        // have already been handled by sendListing, so we can just quit now.
    }

    // Generates a line of a directory listing in the traditional /bin/ls
    // format.
    @Override
    protected String makeLsString(File file) {
        StringBuilder response = new StringBuilder();

        if (!file.exists()) {
            LogUtils.d(TAG, "makeLsString had nonexistent file");
            return null;
        }

        // See Daniel Bernstein's explanation of /bin/ls format at:
        // http://cr.yp.to/ftp/list/binls.html
        // This stuff is almost entirely based on his recommendations.

        String lastNamePart = file.getName();
        // Many clients can't handle files containing these symbols
        if (lastNamePart.contains("*") || lastNamePart.contains("/")) {
            LogUtils.d(TAG, "Filename omitted due to disallowed character");
            return null;
        } else {
            // The following line generates many calls in large directories
            // staticLog.l(Log.DEBUG, "Filename: " + lastNamePart);
        }

        if (file.isDirectory()) {
            response.append("drwxr-xr-x 1 owner group");
        } else {
            // TODO: think about special files, symlinks, devices
            response.append("-rw-r--r-- 1 owner group");
        }

        // The next field is a 13-byte right-justified space-padded file size
        long fileSize = file.length();
        String sizeString = Long.toString(fileSize);
        int padSpaces = 13 - sizeString.length();
        while (padSpaces-- > 0) {
            response.append(' ');
        }
        response.append(sizeString);

        // The format of the timestamp varies depending on whether the mtime
        // is 6 months old
        long mTime = file.lastModified();
        SimpleDateFormat format;
        // Temporarily commented out.. trying to fix Win7 display bug
        if ((System.currentTimeMillis() - mTime) < MS_IN_SIX_MONTHS) {
            // The mtime is less than 6 months ago
            format = new SimpleDateFormat(" MMM dd HH:mm ", Locale.US);
        } else {
            // The mtime is more than 6 months ago
            format = new SimpleDateFormat(" MMM dd  yyyy ", Locale.US);
        }
        response.append(format.format(new Date(file.lastModified())));
        response.append(lastNamePart);
        response.append("\r\n");
        return response.toString();
    }

}
