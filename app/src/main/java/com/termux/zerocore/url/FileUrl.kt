package com.termux.zerocore.url

import com.example.xh_lib.utils.UUtils
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.utils.XinhaoStoragePath
import java.io.File


/**
 *
 * 路径管理
 *
 *
 */
object FileUrl {

    //主目录
    public val mainFilesUrl = TermuxConstants.TERMUX_FILES_DIR_PATH
    public val mainAppUrl = TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH
    public val mainHomeUrl = TermuxConstants.TERMUX_FILES_DIR_PATH + "/home"
    public val mainHomeTemp = TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/temp"
    public val mainBinUrl = TermuxConstants.TERMUX_FILES_DIR_PATH + "/usr/bin"
    public val mainConfigUrl = TermuxConstants.TERMUX_FILES_DIR_PATH + "/home/.termux/"
    public val mainConfigImg = TermuxConstants.TERMUX_FILES_DIR_PATH + "/home/.img/"

    public val MAIN_XINHAO_PATH = "/xinhao"
    public val MAIN_XINHAO_DATA_PATH = "$MAIN_XINHAO_PATH/data"
    public val MAIN_XINHAO_APK_PATH = "$MAIN_XINHAO_PATH/apk"
    public val MAIN_XINHAO_WINDOWS_PATH = "$MAIN_XINHAO_PATH/windows"
    public val MAIN_XINHAO_COMMAND_PATH = "$MAIN_XINHAO_PATH/command"
    public val MAIN_XINHAO_FONT_PATH = "$MAIN_XINHAO_PATH/font"
    public val MAIN_XINHAO_ISO_PATH = "$MAIN_XINHAO_PATH/iso"
    public val MAIN_XINHAO_MYSQL_PATH = "$MAIN_XINHAO_PATH/mysql"
    public val MAIN_XINHAO_ONLINE_SYSTEM_PATH = "$MAIN_XINHAO_PATH/online_system"
    public val MAIN_XINHAO_QEMU_PATH = "$MAIN_XINHAO_PATH/qemu"
    public val MAIN_XINHAO_SERVER_PATH = "$MAIN_XINHAO_PATH/server"
    public val MAIN_XINHAO_SHARE_PATH = "$MAIN_XINHAO_PATH/share"
    public val MAIN_XINHAO_SYSTEM_PATH = "$MAIN_XINHAO_PATH/system"
    public val MAIN_XINHAO_WEB_CONFIG_PATH = "$MAIN_XINHAO_PATH/web_config"
    public val MAIN_XINHAO_MODULE_PATH = "$MAIN_XINHAO_PATH/module"
    public val MAIN_XINHAO_MENU_PATH = "$MAIN_XINHAO_PATH/menu"
    public val MAIN_XINHAO_WINDOWS_CONFIG_PATH = "$MAIN_XINHAO_PATH/windows_config"
    public val MAIN_XINHAO_TYPE_ANDROID_PATH = "$MAIN_XINHAO_PATH/sdcard_android"
    public val MAIN_XINHAO_TYPE_PATH = "$MAIN_XINHAO_PATH/sdcard_xinhao"



    // xinhao 目录（根据用户存储设置动态解析，勿缓存为固定路径）
    public val zeroTermuxHome: File get() = XinhaoStoragePath.getRoot(UUtils.getContext())
    public val zeroTermuxData: File get() = XinhaoStoragePath.getDataDir(UUtils.getContext())
    public val zeroTermuxApk: File get() = XinhaoStoragePath.getApkDir(UUtils.getContext())
    public val zeroTermuxWindows: File get() = XinhaoStoragePath.getWindowsDir(UUtils.getContext())
    public val zeroTermuxCommand: File get() = XinhaoStoragePath.getCommandDir(UUtils.getContext())
    public val zeroTermuxFont: File get() = XinhaoStoragePath.getFontDir(UUtils.getContext())
    public val zeroTermuxIso: File get() = XinhaoStoragePath.getIsoDir(UUtils.getContext())
    public val zeroTermuxMysql: File get() = XinhaoStoragePath.getMysqlDir(UUtils.getContext())
    public val zeroTermuxOnlineSystem: File get() = XinhaoStoragePath.getOnlineSystemDir(UUtils.getContext())
    public val zeroTermuxQemu: File get() = XinhaoStoragePath.getQemuDir(UUtils.getContext())
    public val zeroTermuxServer: File get() = XinhaoStoragePath.getServerDir(UUtils.getContext())
    public val zeroTermuxShare: File get() = XinhaoStoragePath.getShareDir(UUtils.getContext())
    public val zeroTermuxSystem: File get() = XinhaoStoragePath.getSystemDir(UUtils.getContext())
    public val zeroTermuxWebConfig: File get() = XinhaoStoragePath.getWebConfigDir(UUtils.getContext())
    public val zeroTermuxModule: File get() = XinhaoStoragePath.getModuleDir(UUtils.getContext())
    public val zeroTermuxMenu: File get() = XinhaoStoragePath.getMenuDir(UUtils.getContext())
    public val zeroTermuxWindowsConfig: File get() = XinhaoStoragePath.getWindowsConfigDir(UUtils.getContext())
    public val zeroTermuxType: File get() = XinhaoStoragePath.getTypeMarkerDir(UUtils.getContext())

    //官方 sources 路径[源路径]
    public val sourcesUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list"
    //官方 science 路径[源路径]
    public val scienceUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list.d/science.list"
    //官方 game 路径[源路径]
    public val gameUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list.d/game.list"
    //短信工具目录
    public val smsUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/smsread"
    //通讯录工具目录
    public val phoneUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/readcontacts"

    //打开左边工具
    public val openLeft = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/openleftwindow"
    //打开右边工具
    public val openRight = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/openrightwindow"
    //zt通用工具
    public val zt = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/zt"
    //通道文件APK
    public val aislePathAPK = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/libexec/termux-x11/loader.apk"
    public val aislePathAPKPath = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/libexec/termux-x11"
    //通道文件执行脚本
    public val aislePathSh = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/termux-x11"
    //通道文件执行脚本
    public val aislePreferencePathSh = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/termux-x11-preference"
    //通道二进制文
    public val aislePathSo = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/lib/libXlorie.so"
    //定时器目录
    public val timerTermuxDir = "${mainHomeUrl}/.timerdir"
    public val timerTermuxFile = "${mainHomeUrl}/.timerdir/termux_timer.sh"

    public val timerShellDir = "${mainHomeUrl}/.timerdir"
    public val timerShellFile = "${mainHomeUrl}/.timerdir/shell_timer.sh"
    public val timerShellLogDir = "${mainHomeUrl}/.timerdir/log"
    public val timerShellExecDir = "${mainFilesUrl}"
    public val timerShellExecFile = "${mainFilesUrl}/execTermuxEnv.sh"


    //获取短信的目录
    public val smsUrlFile = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/home/sms.txt"
    public val phoneUrlFile = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/home/phone.txt"
    //系统启动脚本目录
    ///data/data/com.termux/files/usr/etc/bash.bashrc  .xinhao_history
    public val smsBashrcFile = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/bash.bashrc"
    public val smsMotdFile = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/motd"
    //Zero系统脚本目录
    public val smsZeroBashrcFileD = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/home/.xinhao_history"
    //Zero系统脚本
    public val smsZeroBashrcFile = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/home/.xinhao_history/start_command.sh"
    // 容器JSON
    public val xinhaoSystemPath = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/xinhao_system.infoJson"
    // boxbusy
    public val busyboxStaticPath = "${TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH}/busybox_static"
    public val busyboxPath = "${TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH}/busybox"



}
