package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_X11_KEYBOARD_SHOW;

import android.content.Context;

public class X11KeyBoardShowConfig implements ZTConfig {
    @Override
    public String getCommand(Context context, String command) {
        return "";
    }

    @Override
    public int getId() {
        return ZT_ID_X11_KEYBOARD_SHOW;
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
