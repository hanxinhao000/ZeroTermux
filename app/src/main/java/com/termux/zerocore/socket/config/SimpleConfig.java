package com.termux.zerocore.socket.config;

import android.content.Context;

public abstract class SimpleConfig implements ZTConfig {
    @Override
    public boolean isForWard() {
        return false;
    }

    @Override
    public String getCommandForWard(Context context, String command) {
        return "";
    }

}
