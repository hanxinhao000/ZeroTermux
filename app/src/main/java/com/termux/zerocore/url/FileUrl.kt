package com.termux.zerocore.url

import com.termux.shared.termux.TermuxConstants


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


    //官方 sources 路径[源路径]
    public val sourcesUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list"
    //官方 science 路径[源路径]
    public val scienceUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list.d/science.list"
    //官方 game 路径[源路径]
    public val gameUrl = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/usr/etc/apt/sources.list.d/game.list"



}
