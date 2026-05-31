package com.termux.zerocore.url

import android.os.Build
import android.os.Environment
import com.termux.shared.termux.TermuxConstants
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
    public val MAIN_XINHAO_WINDOWS_CONFIG_PATH = "$MAIN_XINHAO_PATH/windows_config"
    public val MAIN_XINHAO_TYPE_ANDROID_PATH = "$MAIN_XINHAO_PATH/sdcard_android"
    public val MAIN_XINHAO_TYPE_PATH = "$MAIN_XINHAO_PATH/sdcard_xinhao"



    //主目录
    public val zeroTermuxHome = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_PATH)
    //恢复目录
    public val zeroTermuxData = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_DATA_PATH)
    //APK目录
    public val zeroTermuxApk = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_APK_PATH)
    //windows目录
    public val zeroTermuxWindows = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_WINDOWS_PATH)
    //命令目录
    public val zeroTermuxCommand = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_COMMAND_PATH)
    //字体目录
    public val zeroTermuxFont = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_FONT_PATH)
    //iso目录
    public val zeroTermuxIso = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_ISO_PATH)
    //mysql目录
    public val zeroTermuxMysql = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_MYSQL_PATH)
    //online_system 目录
    public val zeroTermuxOnlineSystem = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_ONLINE_SYSTEM_PATH)
    //qemu目录
    public val zeroTermuxQemu = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_QEMU_PATH)
    //server目录
    public val zeroTermuxServer = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_SERVER_PATH)
    //share目录
    public val zeroTermuxShare = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_SHARE_PATH)
    //system目录
    public val zeroTermuxSystem = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_SYSTEM_PATH)
    //web_config
    public val zeroTermuxWebConfig = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_WEB_CONFIG_PATH)
    //模块包目录
    public val zeroTermuxModule = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_MODULE_PATH)
    //windows_config目录
    public val zeroTermuxWindowsConfig = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_WINDOWS_CONFIG_PATH)
    public val zeroTermuxType = File(Environment.getExternalStorageDirectory(), MAIN_XINHAO_TYPE_PATH)

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
    /** 根据设备 ABI 返回 assets 中对应的 libXlorie 文件名 */
    public fun getAisleXlorieAssetPath(): String {
        val abi = Build.SUPPORTED_ABIS[0]
        return when {
            abi.startsWith("arm64")   -> "x11/libXlorie-arm64-v8a.so"
            abi.startsWith("armeabi") -> "x11/libXlorie-armeabi-v7a.so"
            abi.startsWith("x86_64")  -> "x11/libXlorie-x86_64.so"
            abi.startsWith("x86")     -> "x11/libXlorie-x86.so"
            else -> "x11/libXlorie-arm64-v8a.so" // fallback
        }
    }
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
