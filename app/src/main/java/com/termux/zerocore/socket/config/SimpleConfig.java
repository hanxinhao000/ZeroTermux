package com.termux.zerocore.socket.config;

import android.content.Context;

import com.termux.zerocore.socket.ZTSocketService;

public abstract class SimpleConfig implements ZTConfig {
    @Override
    public boolean isForWard() {
        return false;
    }

    @Override
    public String getCommandForWard(Context context, String command) {
        return "";
    }

    @Override
    public void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context) {

    }
}
