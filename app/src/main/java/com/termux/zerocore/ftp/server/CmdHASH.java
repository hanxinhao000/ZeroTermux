package com.termux.zerocore.ftp.server;





import com.example.xh_lib.utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * CmdHASH provides a method to verify the integrity of a transferred file or to compare two files
 * files against each other without transferring them first. See draft-ietf-ftpext2-hash-03 in
 * the documentation.
 */
public class CmdHASH extends FtpCmd implements Runnable {
    private String input;
    public static String TAG = "CmdHASH";

    public CmdHASH(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        LogUtils.d(TAG, "HASH executing");
        String param = getParameter(input);
        File fileToHash;
        String errString = null;

        mainblock:
        {
            fileToHash = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
            if (violatesChroot(fileToHash)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break mainblock;
            } else if (fileToHash.isDirectory()) {
                LogUtils.d(TAG, "Ignoring HASH for directory");
                errString = "553 Can't HASH a directory\r\n";
                break mainblock;
            } else if (!fileToHash.exists()) {
                LogUtils.d(TAG, "Can't HASH nonexistent file: " + fileToHash.getAbsolutePath());
                errString = "550 File does not exist\r\n";
                break mainblock;
            } else if (!fileToHash.canRead()) {
                LogUtils.d(TAG, "Failed HASH permission (canRead() is false)");
                errString = "556 No read permissions\r\n";
                break mainblock;
            }

            FileInputStream in = null;
            try {
                String algorithm = sessionThread.getHashingAlgorithm();
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] buffer = new byte[SessionThread.DATA_CHUNK_SIZE];
                in = new FileInputStream(fileToHash);

                long offset = 0L;
                long endPosition = fileToHash.length() - 1;
                if (sessionThread.offset >= 0) {
                    offset = sessionThread.offset;
                    if (offset <= sessionThread.endPosition
                            && sessionThread.endPosition <= fileToHash.length() - 1) {
                        endPosition = sessionThread.endPosition;
                    }
                }

                // This is not a range but length (Range 0-0 would still read 0th byte), so +1
                long bytesToRead = endPosition - offset + 1;
                int bytesRead;
                in.skip(offset);
                while ((bytesRead = in.read(buffer)) != -1) {
                    if (bytesRead > bytesToRead) {
                        md.update(buffer, 0, (int) bytesToRead);
                        break;
                    }
                    md.update(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                }

                byte[] hash = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    hexString.append(String.format("%02x", b));
                }

                String response = "213 " + algorithm + " " + offset + "-" + endPosition
                        + " " + hexString.toString() + " " + param + "\r\n";
                sessionThread.writeString(response);
            } catch (FileNotFoundException e) {
                errString = "550 File not found\r\n";
                break mainblock;
            } catch (IOException e) {
                errString = "425 Network error\r\n";
                break mainblock;
            } catch (NoSuchAlgorithmException e) {
                errString = "550 Unknown hashing algorithm\r\n";
                break mainblock;
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException ignore) {
                }
            }
        }
        if (errString != null) {
            sessionThread.writeString(errString);
        }
        LogUtils.d(TAG, "HASH done");
    }
}
