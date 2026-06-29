package com.termux.zerocore.gui

import android.util.Log
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/** 向 Termux 侧 input 守护进程写入鼠标/点击指令（xdotool）。 */
object ZtGuiInputWriter {

    private const val LOG_TAG = "ZtGuiInputWriter"

    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ZtGuiInputWriter").apply { isDaemon = true }
    }

    fun sendClick(x: Int, y: Int) {
        enqueue("click:$x,$y")
    }

    fun sendMove(x: Int, y: Int) {
        enqueue("move:$x,$y")
    }

    private fun enqueue(line: String) {
        ioExecutor.execute { writeCommand(line) }
    }

    private fun writeCommand(line: String) {
        try {
            val dir = ZtGuiPaths.inputDir()
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "${System.nanoTime()}.cmd")
            file.writeBytes((line + "\n").toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            Log.w(LOG_TAG, "input write failed: $line", e)
        }
    }
}
