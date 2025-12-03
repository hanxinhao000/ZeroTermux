package com.termux.zerocore.socket.config;

public class ZTKeyConstants {
    //未知命令
    public static final int ZT_ID_KNOW = -1;
    public static final String ZT_COMMAND_KNOW = "know";
    // 显示 TOAST
    public static final int ZT_ID_TOAST = 1;
    public static final String ZT_COMMAND_TOAST = "toast";
    // 显示帮助文件
    public static final int ZT_ID_HELP = 2;
    public static final String ZT_COMMAND_HELP = "help";
    // 显示帮助文件
    public static final int ZT_ID_VERSION = 3;
    public static final String ZT_COMMAND_VERSION = "version";
    public static final String ZT_COMMAND_VERSION_1 = "v";
    // 打开 左面板
    public static final int ZT_ID_LEFT = 4;
    public static final String ZT_COMMAND_LEFT = "openleft";
    public static final String ZT_COMMAND_LEFT_1 = "ol";
    // 打开右面板
    public static final int ZT_ID_RIGHT = 5;
    public static final String ZT_COMMAND_RIGHT = "openright";
    public static final String ZT_COMMAND_RIGHT_1 = "or";

    // 重启termux
    public static final int ZT_ID_REBOOT = 6;
    public static final String ZT_COMMAND_REBOOT = "reboot";
    public static final String ZT_COMMAND_REBOOT_1 = "rb";

    // 软连接
    public static final int ZT_ID_LN = 7;
    public static final String ZT_COMMAND_LN = "ln";

    //AVnc
    public static final int ZT_ID_VNC = 8;
    public static final String ZT_COMMAND_VNC = "vnc";

    //显示终端
    public static final int ZT_ID_X11_COMMAND_SHOW = 9;
    public static final String ZT_COMMAND_X11_COMMAND_SHOW = "x11commandshow";
    public static final String ZT_COMMAND_X11_COMMAND_SHOW_1 = "x11xs";

    //隐藏终端
    public static final int ZT_ID_X11_COMMAND_HIDE = 10;
    public static final String ZT_COMMAND_X11_COMMAND_HIDE = "x11commandhide";
    public static final String ZT_COMMAND_X11_COMMAND_HIDE_1 = "x11xh";

    //当前x11状态
    public static final int ZT_ID_X11_STATUS = 11;
    public static final String ZT_COMMAND_X11_STATUS = "x11status";

    // 显示X11键盘
    public static final int ZT_ID_X11_KEYBOARD_SHOW = 12;
    public static final String ZT_COMMAND_X11_KEYBOARD_SHOW = "x11keyboardshow";
    public static final String ZT_COMMAND_X11_KEYBOARD_SHOW_1 = "x11kbs";

    //隐藏X11键盘
    public static final int ZT_ID_X11_KEYBOARD_HIDE = 13;
    public static final String ZT_COMMAND_X11_KEYBOARD_HIDE = "x11keyboardhide";
    public static final String ZT_COMMAND_X11_KEYBOARD_HIDE_1 = "x11kbh";
}
