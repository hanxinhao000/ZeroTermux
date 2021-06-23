package com.termux.zerocore.url

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
    public val mainHomeUrl = TermuxConstants.TERMUX_FILES_DIR_PATH + "/home"
    public val mainBinUrl = TermuxConstants.TERMUX_FILES_DIR_PATH + "/usr/bin"
    public val mainConfigUrl = TermuxConstants.TERMUX_FILES_DIR_PATH + "/home/.termux/"


    //主目录
    public val zeroTermuxHome = File(Environment.getExternalStorageDirectory(), "/xinhao/")
    //恢复目录
    public val zeroTermuxData = File(Environment.getExternalStorageDirectory(), "/xinhao/data/")
    //APK目录
    public val zeroTermuxApk = File(Environment.getExternalStorageDirectory(), "/xinhao/apk/")
    //windows目录
    public val zeroTermuxWindows = File(Environment.getExternalStorageDirectory(), "/xinhao/windows/")
   //命令目录
    public val zeroTermuxCommand = File(Environment.getExternalStorageDirectory(), "/xinhao/command/")
   //字体目录
    public val zeroTermuxFont = File(Environment.getExternalStorageDirectory(), "/xinhao/font")
   //iso目录
    public val zeroTermuxIso = File(Environment.getExternalStorageDirectory(), "/xinhao/iso")
   //mysql目录
    public val zeroTermuxMysql = File(Environment.getExternalStorageDirectory(), "/xinhao/mysql")
    //online_system 目录
    public val zeroTermuxOnlineSystem = File(Environment.getExternalStorageDirectory(), "/xinhao/online_system")
   //qemu目录
    public val zeroTermuxQemu = File(Environment.getExternalStorageDirectory(), "/xinhao/qemu")
   //server目录
    public val zeroTermuxServer = File(Environment.getExternalStorageDirectory(), "/xinhao/server")
   //share目录
    public val zeroTermuxShare = File(Environment.getExternalStorageDirectory(), "/xinhao/share")
    //system目录
    public val zeroTermuxSystem = File(Environment.getExternalStorageDirectory(), "/xinhao/system")
    //web_config
    public val zeroTermuxWebConfig = File(Environment.getExternalStorageDirectory(), "/xinhao/web_config")

    //官方 sources 路径[源路径]
    public val sourcesUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list"
    //官方 science 路径[源路径]
    public val scienceUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list.d/science.list"
    //官方 game 路径[源路径]
    public val gameUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list.d/game.list"
    //短信工具目录
    public val smsUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/bin/smsread"
    //获取短信的目录
    public val smsUrlFile = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/home/sms.txt"



}