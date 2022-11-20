package com.termux.zerocore.data

object CommendShellData {

    public const val SHELL_DATA_WEB_LINUX = "cd ~ && pkg update -y && pkg install wget proot ttyd -y\n"
    public const val SHELL_DATA_RUN_WEB = "ttyd bash&\n"
}
