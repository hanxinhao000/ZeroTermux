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

public class CmdOPTS extends FtpCmd implements Runnable {
    private static final String TAG = CmdOPTS.class.getSimpleName();

    private final String input;

    public CmdOPTS(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        String param = getParameter(input);
        String errString = null;
        String responseString = null;

        mainBlock: {
            if (param == null) {
                errString = "550 Need argument to OPTS\r\n";
                Log.w(TAG, "Couldn't understand empty OPTS command");
                break mainBlock;
            }
            String[] splits = param.split(" ");
            String optName = splits[0].toUpperCase();

            // if the first parameter (optName) is HASH, second parameter (optVal) is optional
            if(optName.equals("HASH")) {
                Log.d(TAG, "Got OPTS HASH");
                // if second parameter not passed, respond with currently selected algorithm
                if(splits.length == 1) {
                    Log.d(TAG, "No arguments for OPTS HASH, returning selected algorithm");
                    responseString = "200 " + sessionThread.getHashingAlgorithm() + "\r\n";
                } else if(splits.length == 2) {
                    String newHashingAlgorithm = splits[1].toUpperCase();
                    Log.d(TAG, "Got OPTS HASH: " + newHashingAlgorithm);
                    if(newHashingAlgorithm.equals("MD5") ||
                            newHashingAlgorithm.equals("SHA-1") ||
                            newHashingAlgorithm.equals("SHA-256") ||
                            newHashingAlgorithm.equals("SHA-384") ||
                            newHashingAlgorithm.equals("SHA-512")) {
                        sessionThread.setHashingAlgorithm(newHashingAlgorithm);
                        responseString = "200 " + newHashingAlgorithm + "\r\n";
                    } else {
                        errString = "501 Unknown algorithm, current selection not changed\r\n";
                    }
                } else {
                    errString = "550 Malformed OPTS HASH command\r\n";
                    Log.w(TAG, "Couldn't parse options for OPTS HASH command");
                }
                // break out of mainblock, OPTS command is handled.
                break mainBlock;
            } else if (splits.length != 2) {
                errString = "550 Malformed OPTS command\r\n";
                Log.w(TAG, "Couldn't parse OPTS command");
                break mainBlock;
            }

            String optVal = splits[1].toUpperCase();
            if (optName.equals("UTF8")) {
                // OK, whatever. Don't really know what to do here. We
                // always operate in UTF8 mode.
                if (optVal.equals("ON")) {
                    Log.d(TAG, "Got OPTS UTF8 ON");
                    sessionThread.setEncoding("UTF-8");
                } else {
                    Log.i(TAG, "Ignoring OPTS UTF8 for something besides ON");
                }
                break mainBlock;
            } else if(optName.equals("MLST")) {
                Log.d(TAG, "Got OPTS MLST: " + optVal);
                String[] opts = optVal.split(";");
                boolean hasType = false, hasSize = false, hasModify = false, hasPerm = false;
                for (String opt : opts) {
                    if (opt.equalsIgnoreCase("Type")) {
                        hasType = true;
                    } else if (opt.equalsIgnoreCase("Size")) {
                        hasSize = true;
                    } else if (opt.equalsIgnoreCase("Modify")) {
                        hasModify = true;
                    } else if (opt.equalsIgnoreCase("Perm")) {
                        hasPerm = true;
                    }
                }
                
                int optCount = 0;
                StringBuilder types = new StringBuilder();
                if(hasType){
                    opts[optCount++] = "Type";
                    types.append("Type;");
                }
                if(hasSize){
                    opts[optCount++] = "Size";
                    types.append("Size;");
                }
                if(hasModify){
                    opts[optCount++] = "Modify";
                    types.append("Modify;");
                }
                if(hasPerm){
                    opts[optCount++] = "Perm";
                    types.append("Perm;");
                }
                String[] newOpts = new String[optCount];
                System.arraycopy(opts, 0, newOpts, 0, optCount);
                
                sessionThread.setFormatTypes(newOpts);
                responseString = "200 MLST OPTS " + types.toString() + "\r\n";
            } else {
                Log.d(TAG, "Unrecognized OPTS option: " + optName);
                errString = "502 Unrecognized option\r\n";
                break mainBlock;
            }
        }
        if (errString != null) {
            sessionThread.writeString(errString);
        } else {
            sessionThread.writeString(responseString != null ? responseString : "200 OPTS accepted\r\n");
            Log.d(TAG, "Handled OPTS ok");
        }
    }

}
