package com.termux.zerocore.data

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.utils.XinhaoStoragePath
import java.io.File

object CommendShellData {

    public const val SHELL_DATA_WEB_LINUX = "cd ~ && pkg update -y && pkg install wget proot ttyd -y\n"
    public const val SHELL_DATA_RUN_WEB = "ttyd bash&\n"

    public const val SHELL_TAR_GZ = "zcvf"
    public const val SHELL_TAR_BZ2 = "jcvf"
    public const val SHELL_TAR_XZ = "Jcvf"
    public const val SHELL_TAR_Z = "Zcvf"

    public const val SHELL_TAR_RESTORE_GZ = "xzvf"
    public const val SHELL_TAR_RESTORE_BZ2 = "xjf"
    public const val SHELL_TAR_RESTORE_XZ = "xvJf"
    public const val SHELL_TAR_RESTORE_Z = "xZf"

    public const val SHELL_WELCOME_MESSAGE = "echo \"+++++++++++++START+++++++++++++\" \n"

    @JvmStatic
    fun getShellBackup(systemName: String, tarOption: String): String {
        return XinhaoStoragePath.getShellBackup(systemName, tarOption)
    }

    @JvmStatic
    fun getShellRestore(command: String, tarFle: File, createFile: File): String {
        return XinhaoStoragePath.getShellRestore(
            command,
            tarFle.name,
            createFile.name
        )
    }
}
