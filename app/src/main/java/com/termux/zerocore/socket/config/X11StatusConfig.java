package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_X11_STATUS;

import android.content.Context;
import android.util.Log;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.x11.MainActivity;
import com.termux.zerocore.socket.ZTSocketService;

public class X11StatusConfig extends BaseOkJsonConfig {
    @Override
    public String getCommand(Context context, String command) {
        return null;
    }

    @Override
    public int getId() {
        return ZT_ID_X11_STATUS;
    }

    @Override
    public void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context) {
        UUtils.runOnUIThread(() -> {
            String json = getJson(MainActivity.isConnected() ? 0 : 1,
                MainActivity.isConnected() ? UUtils.getString(R.string.连接) : UUtils.getString(R.string.未连接), "");
            UUtils.runOnThread(() -> {
                clientHandler.sendSocketMessage(json);
            });
        });
    }
}
