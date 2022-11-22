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





import com.termux.zerocore.ftp.utils.Cat;

import java.io.File;

/**
 * CmdRNFR implements RENAME FROM (RNFR)
 * This command specifies the old pathname of the file which is
 * to be renamed. This command must be immediately followed by
 * a RNTO command specifying the new file pathname.
 */
public class CmdRNFR extends FtpCmd implements Runnable {

    protected String input;

    public CmdRNFR(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Cat.d("Executing RNFR");
        String param = getParameter(input);
        String errString = null;
        File file = null;
        mainblock:
        {
            file = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
            if (violatesChroot(file)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break mainblock;
            }
            if (!file.exists()) {
                errString = "450 Cannot rename nonexistent file\r\n";
            }
        }
        if (errString != null) {
            sessionThread.writeString(errString);
            Cat.d("RNFR failed: " + errString.trim());
            sessionThread.setRenameFrom(null);
        } else {
            sessionThread.writeString("350 Filename noted, now send RNTO\r\n");
            sessionThread.setRenameFrom(file);
        }
    }
}
