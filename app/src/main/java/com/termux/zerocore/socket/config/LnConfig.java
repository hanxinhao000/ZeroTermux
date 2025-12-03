package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_LN;

import android.content.Context;
import android.system.Os;

public class LnConfig extends BaseOkJsonConfig {
    @Override
    public String getCommand(Context context, String command) {
        try {
            String[] lns = command.split(" ");
            String path1 = lns[1];
            String path2 = lns[2];
            Os.symlink(path2, path1);
        } catch (Exception e) {
            return e.toString();
        }
        return getOkJson();
    }

    @Override
    public int getId() {
        return ZT_ID_LN;
    }
}
