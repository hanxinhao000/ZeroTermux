package com.termux.zerocore.socket.config;

import android.content.Context;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class VersionConfig extends SimpleConfig {
    @Override
    public String getCommand(Context context, String command) {
        return UUtils.getString(R.string.zt_command_version);
    }

    @Override
    public int getId() {
        return ZTKeyConstants.ZT_ID_VERSION;
    }
}
