package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_X11_COMMAND_HIDE;

import android.content.Context;

public class X11CommandHideConfig extends SimpleConfig {
    @Override
    public String getCommand(Context context, String command) {
        return "";
    }

    @Override
    public int getId() {
        return ZT_ID_X11_COMMAND_HIDE;
    }

    @Override
    public boolean isForWard() {
        return true;
    }
}
