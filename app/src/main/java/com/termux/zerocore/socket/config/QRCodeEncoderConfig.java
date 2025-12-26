package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_QR_CODE_ENCODER_CONFIG;

import android.content.Context;
import android.content.Intent;

import com.example.xh_lib.activity.ScanActivity;
import com.example.xh_lib.utils.UUtils;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.socket.ZTSocketService;
import com.termux.zerocore.socket.activity.SocketBaseActivity;

import org.checkerframework.checker.units.qual.C;

import java.util.List;

import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder;

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
