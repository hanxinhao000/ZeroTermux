package com.termux.zerocore.gui

import com.termux.shared.termux.TermuxConstants
import java.io.File

object ZtGuiPaths {

    private fun homeGuiDir(): File =
        File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".zerotermux/gui")

    /** 优先 JPEG（scrot），其次 PPM；兼容旧路径。 */
    fun frameFile(): File {
        val dir = homeGuiDir()
        val jpg = File(dir, "frame.jpg")
        if (jpg.isFile && jpg.length() > 0) return jpg
        val ppm = File(dir, "frame.ppm")
        if (ppm.isFile && ppm.length() > 0) return ppm
        return File(dir, "frame.png")
    }

    /** 每条指令独立文件，避免与守护进程 mv 冲突丢输入。 */
    fun inputDir(): File = File(homeGuiDir(), "input.d")

    fun logFile(): File = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".zerotermux/editor-gui.log")

    fun pidFile(): File = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".zerotermux/editor-gui.pid")
}
