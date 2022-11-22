/*
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

public class CmdREST extends FtpCmd implements Runnable {
    protected String input;

    public CmdREST(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        String param = getParameter(input);
        long offset;
        try {
            offset = Long.parseLong(param);
        } catch (NumberFormatException e) {
            sessionThread.writeString("550 No valid restart position\r\n");
            return;
        }
        if (offset < 0) {
            sessionThread.writeString("550 Restart position must be non-negative\r\n");
            return;
        }
        Cat.d("run REST with offset " + offset);
        if (sessionThread.isBinaryMode()) {
            sessionThread.offset = offset;
            sessionThread.writeString("350 Restart position accepted (" + offset + ")\r\n");
        } else {
            // We do not allow a REST in ASCII mode as this this causes problems
            // when server and client use different line ending format
            sessionThread.writeString("550 Restart position not accepted in ASCII mode\r\n");
        }
    }

}
