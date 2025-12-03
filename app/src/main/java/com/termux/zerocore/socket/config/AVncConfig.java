package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_VNC;

import android.content.Context;
import android.content.Intent;

import com.example.xh_lib.utils.UUtils;
import com.gaurav.avnc.model.ServerProfile;
import com.gaurav.avnc.ui.vnc.VncActivity;
import com.zp.z_file.util.LogUtils;

import java.util.Arrays;

public class AVncConfig extends BaseOkJsonConfig {
    private static final String TAG = AVncConfig.class.getSimpleName();
    @Override
    public String getCommand(Context context, String command) {
        try {
            UUtils.runOnUIThread(() -> {
                Intent intent = new Intent(context, VncActivity.class);
                intent.putExtra("com.gaurav.avnc.server_profile", getServerProfile(command));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
            return getOkJson();
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    private ServerProfile getServerProfile(String command) {
        LogUtils.i(TAG, "getServerProfile command: " + command);
        String[] commandSplit = command.split(" ");
        LogUtils.i(TAG, "getServerProfile commandSplit: " + Arrays.toString(commandSplit));
        String[] split = commandSplit[1].split("_");
        LogUtils.i(TAG, "getServerProfile split: " + Arrays.toString(split));
        String ip = split[0];
        String port = split[1];
        String username = split[2];
        String password = split[3];
        ServerProfile serverProfile = new ServerProfile();
        serverProfile.setHost(ip);
        serverProfile.setPort(Integer.parseInt(port));
        if (!"none".equals(username)) {
            serverProfile.setUsername(username);
        }
        if (!"none".equals(password)) {
            serverProfile.setPassword(password);
        }
        return serverProfile;
    }

    @Override
    public int getId() {
        return ZT_ID_VNC;
    }
}
