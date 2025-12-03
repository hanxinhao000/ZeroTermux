package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_X11_KEYBOARD_HIDE;

import android.content.Context;

public class X11KeyBoardHideConfig implements ZTConfig {
    @Override
    public String getCommand(Context context, String command) {
        return "";
    }

    @Override
    public int getId() {
        return ZT_ID_X11_KEYBOARD_HIDE;
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
