package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_QR_CODE_ENCODER_CONFIG;

import android.content.Context;
import android.content.Intent;

import com.example.xh_lib.utils.UUtils;
import com.termux.zerocore.config.ztcommand.ZTSocketService;
import com.termux.zerocore.config.ztcommand.activity.SocketBaseActivity;

public class QRCodeEncoderConfig extends BaseOkJsonConfig {
    private static ZTSocketService.ClientHandler mClientHandler;
    @Override
    public String getCommand(Context context, String command) {
        return null;
    }

    @Override
    public int getId() {
        return ZT_ID_QR_CODE_ENCODER_CONFIG;
    }

    @Override
    public void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context) {
        mClientHandler = clientHandler;
        UUtils.runOnUIThread(() -> {
            Intent intent = new Intent(context, SocketBaseActivity.class);
            intent.putExtra(SocketBaseActivity.OPEN_TYPE, SocketBaseActivity.OPEN_CAMERA_QR_TYPE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    public static void sendMessage(int code, String message) {
        UUtils.runOnThread(() -> {
            mClientHandler.sendSocketMessage(new QRCodeEncoderConfig().getJson(code, message, ""));
            mClientHandler = null;
        });
    }
}
