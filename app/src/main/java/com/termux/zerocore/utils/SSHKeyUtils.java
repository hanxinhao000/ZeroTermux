package com.termux.zerocore.utils;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.IOException;

public class SSHKeyUtils {
    public static File getSSHDir() {
        return new File(TermuxConstants.TERMUX_HOME_DIR, ".ssh");
    }
    public static File getKeyFile(String alias) {
        String safeName = alias.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return new File(getSSHDir(), safeName + ".key");
    }
    public static boolean isKeyExist(String alias) {
        return getKeyFile(alias).exists();
    }
    public static String getGenerateKeyCommand(String alias) {
        File keyFile = getKeyFile(alias);
        File sshDir = getSSHDir();
        return "mkdir -p " + sshDir.getAbsolutePath() + " && " +
            "rm -f " + keyFile.getAbsolutePath() + "* && " +
            "ssh-keygen -t rsa -b 2048 -f " + keyFile.getAbsolutePath() + " -N ''";
    }
    public static String getCatPublicKeyCommand(String alias) {
        return "cat " + getKeyFile(alias).getAbsolutePath() + ".pub";
    }
}
