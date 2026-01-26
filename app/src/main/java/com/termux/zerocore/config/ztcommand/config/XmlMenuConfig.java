package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_MENU;

import android.content.Context;

import com.example.xh_lib.utils.LogUtils;
import com.termux.zerocore.config.ztcommand.ZTSocketService;

// zt menu update 更新左菜单
// zt menu reset 恢复默认菜单
public class XmlMenuConfig extends BaseOkJsonConfig {
    private static final String TAG = XmlMenuConfig.class.getSimpleName();
    public static final String MENU_UPDATE = "menu_update";
    public static final String MENU_RESET = "menu_reset";
    private String mCommand;

    @Override
    public String getCommand(Context context, String command) {
        this.mCommand = command;
        return null;
    }

    @Override
    public void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context) {
        try {
            String command = mCommand.split(" ")[1];
            clientHandler.sendMessageToActivity("menu_" + command);
            LogUtils.i(TAG, "sendSocketMessage mCommand: " + "menu_" + command);
            clientHandler.sendSocketMessage(getOkJson());
        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendSocketMessage(getJson(1, e.toString(), ""));
        }
    }

    @Override
    public int getId() {
        return ZT_ID_MENU;
    }
}
