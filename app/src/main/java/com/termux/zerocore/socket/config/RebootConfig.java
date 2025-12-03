package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_REBOOT;

import android.content.Context;
import android.content.Intent;

import com.termux.app.TermuxService;

public class RebootConfig extends BaseOkJsonConfig {
    @Override
    public String getCommand(Context context, String command) {
        Intent intent = new Intent(context, TermuxService.class);
        intent.setAction("com.termux.service_stop");
        context.startService(intent);
        return getOkJson();
    }

    @Override
    public int getId() {
        return ZT_ID_REBOOT;
    }
}
