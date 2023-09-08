package com.termux.zerocore.data

import com.example.xh_lib.utils.UUtils
import com.termux.R
import java.io.File

object CommendShellData {

    public const val SHELL_DATA_WEB_LINUX = "cd ~ && pkg update -y && pkg install wget proot ttyd -y\n"
    public const val SHELL_DATA_RUN_WEB = "ttyd bash&\n"

    //备份
    //"cd ~ && cd ~ && tar -zcvf - TemporaryMark | pv -s $(($(du -sk TemporaryMark | awk '{print $1}') * 1024)) | gzip > ./storage/shared/xinhao/data/systemName
   // public var SHELL_BACKUP = "cd ~ && cd ~ && tar -TemporaryMark - /data/data/com.termux/files | pv -s $(($(du -sk /data/data/com.termux/files | awk '{print $1}') * 1024)) | gzip > ./storage/shared/xinhao/data/systemName && echo \"${UUtils.getString(R.string.backup_success)}\" \n"
    public var SHELL_BACKUP = "cd ~ && cd ~ && tar -TemporaryMark ./storage/shared/xinhao/data/systemName /data/data/com.termux/files && echo \"${UUtils.getString(R.string.backup_success)}\" \n"
    public const val SHELL_TAR_GZ = "zcvf"
    public const val SHELL_TAR_BZ2 = "jcvf"
    public const val SHELL_TAR_XZ = "Jcvf"
    public const val SHELL_TAR_Z = "Zcvf"

    //恢复
    public const val SHELL_TAR_RESTORE_GZ = "xzvf"
    public const val SHELL_TAR_RESTORE_BZ2 = "xjf"
    public const val SHELL_TAR_RESTORE_XZ = "xvJf"
    public const val SHELL_TAR_RESTORE_Z = "xZf"

    //欢迎语
    public const val SHELL_WELCOME_MESSAGE =  "echo \"+++++++++++++START+++++++++++++\" \n"
    public fun getShellRestore(command: String, tarFle: File, createFile: File): String {
        return  "cd ~ && cd ~ && tar -v -${command} ./storage/shared/xinhao/data/" + tarFle.getName().replace(" ","") + "  -C ../../" + createFile.getName() + " && mv ../../" + createFile.getName() + "/data/data/com.termux/files/home ../../" + createFile.getName() +" && "+ "mv ../../" + createFile.getName() + "/data/data/com.termux/files/usr ../../" + createFile.getName()+" && rm -rf ../../"+createFile.getName()+"/data && echo \"${UUtils.getString(R.string.system_restore_success)}\" \n"
    }
}
