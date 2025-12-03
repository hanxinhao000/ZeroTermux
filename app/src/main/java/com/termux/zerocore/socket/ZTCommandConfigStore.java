package com.termux.zerocore.socket;


import android.util.SparseArray;

import com.termux.zerocore.socket.config.AVncConfig;
import com.termux.zerocore.socket.config.ForWardOpenLeftConfig;
import com.termux.zerocore.socket.config.ForWardOpenRightConfig;
import com.termux.zerocore.socket.config.HelpConfig;
import com.termux.zerocore.socket.config.KnowConfig;
import com.termux.zerocore.socket.config.LnConfig;
import com.termux.zerocore.socket.config.RebootConfig;
import com.termux.zerocore.socket.config.ToastConfig;
import com.termux.zerocore.socket.config.VersionConfig;
import com.termux.zerocore.socket.config.X11CommandHideConfig;
import com.termux.zerocore.socket.config.X11CommandShowConfig;
import com.termux.zerocore.socket.config.X11KeyBoardHideConfig;
import com.termux.zerocore.socket.config.X11KeyBoardShowConfig;
import com.termux.zerocore.socket.config.X11StatusConfig;
import com.termux.zerocore.socket.config.ZTConfig;
import com.termux.zerocore.socket.config.ZTKeyConstants;

import java.util.HashMap;
import java.util.Map;

public class ZTCommandConfigStore {
    private static final SparseArray<ZTConfig> sparse_array_config = new SparseArray<>();
    private static final Map<String, Integer> map_array_command = new HashMap<>();
    static {
        // 需要在此添加命令，否则找不到config
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_TOAST, ZTKeyConstants.ZT_ID_TOAST);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_HELP, ZTKeyConstants.ZT_ID_HELP);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_KNOW, ZTKeyConstants.ZT_ID_KNOW);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_VERSION, ZTKeyConstants.ZT_ID_VERSION);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_VERSION_1, ZTKeyConstants.ZT_ID_VERSION);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_LEFT, ZTKeyConstants.ZT_ID_LEFT);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_LEFT_1, ZTKeyConstants.ZT_ID_LEFT);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_RIGHT, ZTKeyConstants.ZT_ID_RIGHT);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_RIGHT_1, ZTKeyConstants.ZT_ID_RIGHT);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_REBOOT, ZTKeyConstants.ZT_ID_REBOOT);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_REBOOT_1, ZTKeyConstants.ZT_ID_REBOOT);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_LN, ZTKeyConstants.ZT_ID_LN);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_VNC, ZTKeyConstants.ZT_ID_VNC);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_COMMAND_SHOW, ZTKeyConstants.ZT_ID_X11_COMMAND_SHOW);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_COMMAND_SHOW_1, ZTKeyConstants.ZT_ID_X11_COMMAND_SHOW);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_COMMAND_HIDE, ZTKeyConstants.ZT_ID_X11_COMMAND_SHOW);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_COMMAND_HIDE_1, ZTKeyConstants.ZT_ID_X11_COMMAND_SHOW);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_STATUS, ZTKeyConstants.ZT_ID_X11_STATUS);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_KEYBOARD_SHOW, ZTKeyConstants.ZT_ID_X11_KEYBOARD_SHOW);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_KEYBOARD_SHOW_1, ZTKeyConstants.ZT_ID_X11_KEYBOARD_SHOW);

        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_KEYBOARD_HIDE, ZTKeyConstants.ZT_ID_X11_KEYBOARD_HIDE);
        map_array_command.put(ZTKeyConstants.ZT_COMMAND_X11_KEYBOARD_HIDE_1, ZTKeyConstants.ZT_ID_X11_KEYBOARD_HIDE);

        // 需要在此添加你的config，否则找不到config
        register(new ToastConfig());
        register(new HelpConfig());
        register(new KnowConfig());
        register(new VersionConfig());
        register(new ForWardOpenRightConfig());
        register(new ForWardOpenLeftConfig());
        register(new RebootConfig());
        register(new LnConfig());
        register(new AVncConfig());
        register(new X11CommandShowConfig());
        register(new X11CommandHideConfig());
        register(new X11StatusConfig());
        register(new X11KeyBoardShowConfig());
        register(new X11KeyBoardHideConfig());
    }
    private static void register(ZTConfig ztConfig) {
        sparse_array_config.append(ztConfig.getId(), ztConfig);
    }

    public static ZTConfig getConfig(String command) {
        Integer id = map_array_command.get(command);
        try {
            ZTConfig ztConfig = sparse_array_config.get(id);
            if (ztConfig == null) {
                return new KnowConfig();
            }
            return ztConfig;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new KnowConfig();
    }

}
