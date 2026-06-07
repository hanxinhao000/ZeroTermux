package com.termux.zerocore.settings.timer

import com.termux.zerocore.url.FileUrl
import java.io.File
import java.io.RandomAccessFile

object TimerExecutionLog {
    private val logDir: String
        get() = FileUrl.timerShellLogDir
    private const val SHELL_LOG_NAME = "shell_timer.log"
    private const val TERMUX_LOG_NAME = "termux_timer.log"

    fun logFile(isZeroTermux: Boolean): File {
        ensureLogDir()
        val name = if (isZeroTermux) TERMUX_LOG_NAME else SHELL_LOG_NAME
        return File(logDir, name)
    }

    fun logFilePath(isZeroTermux: Boolean): String = logFile(isZeroTermux).absolutePath

    fun ensureLogDir() {
        File(logDir).mkdirs()
    }

    fun formatRunHeader(count: Int): String {
        return "第${toChineseNumber(count)}次启动"
    }

    fun readLastLines(isZeroTermux: Boolean, maxLines: Int = 100): String {
        val file = logFile(isZeroTermux)
        if (!file.exists() || file.length() == 0L) {
            return ""
        }
        return try {
            val lines = ArrayDeque<String>(maxLines)
            file.bufferedReader().useLines { sequence ->
                sequence.forEach { line ->
                    if (lines.size == maxLines) {
                        lines.removeFirst()
                    }
                    lines.addLast(line)
                }
            }
            lines.asReversed().joinToString("\n")
        } catch (_: Exception) {
            ""
        }
    }

    fun readTail(isZeroTermux: Boolean, maxBytes: Int = 12_288): String {
        val file = logFile(isZeroTermux)
        if (!file.exists() || file.length() == 0L) {
            return ""
        }
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val length = raf.length()
                val readSize = minOf(length, maxBytes.toLong()).toInt()
                raf.seek(length - readSize)
                val bytes = ByteArray(readSize)
                raf.readFully(bytes)
                val text = String(bytes, Charsets.UTF_8)
                if (readSize < length) {
                    val firstNewline = text.indexOf('\n')
                    if (firstNewline >= 0) text.substring(firstNewline + 1) else text
                } else {
                    text
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun toChineseNumber(number: Int): String {
        if (number <= 0) return number.toString()
        val digits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        if (number < 10) return digits[number]
        if (number == 10) return "十"
        if (number < 20) return "十${digits[number - 10]}"
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return if (ones == 0) {
                "${digits[tens]}十"
            } else {
                "${digits[tens]}十${digits[ones]}"
            }
        }
        return number.toString()
    }
}
