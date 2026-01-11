package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_RIGHT;

import android.content.Context;

public class ForWardOpenRightConfig extends SimpleConfig {
    @Override
    public String getCommand(Context context, String command) {
        return "";
    }

    @Override
    public int getId() {
        return ZT_ID_RIGHT;
    }

    @Override
    public boolean isForWard() {
        return true;
    }

    @Override
    public String getCommandForWard(Context context, String command) {
        return "";
    }
}
