package com.termux.zerocore.settings.timer

import android.os.Build
import android.util.Log
import com.example.xh_lib.utils.UUtils
import com.termux.BuildConfig
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.XinhaoStoragePath
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 定时任务诊断日志：受开发者选项「输出LOG到存储卡」([ZTUserBean.isOutputLOG]) 控制。
 * 用于排查「偶尔没执行到」：调度、触发、等待脚本、看门狗、恢复等决策都会落盘。
 *
 * 文件位置（开关开启时）：
 * - [XinhaoStoragePath.getLogDir]/timer_diag.log（与 ZeroTermux.log 同目录，方便拷贝）
 * - ~/.timerdir/log/timer_diag.log（Termux 内可读）
 */
object TimerDiagLog {
    private const val TAG = "TimerDiag"
    private const val FILE_NAME = "timer_diag.log"
    private const val MAX_BYTES = 2L * 1024 * 1024
    private val writer = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ZT-TimerDiagLog").apply { isDaemon = true }
    }
    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }
    private val heartbeatCounter = AtomicInteger(0)

    fun isEnabled(): Boolean {
        return try {
            UserSetManage.get().getZTUserBean().isOutputLOG
        } catch (_: Exception) {
            false
        }
    }

    fun i(tag: String, message: String) = write("I", tag, message)

    fun w(tag: String, message: String) = write("W", tag, message)

    fun e(tag: String, message: String) = write("E", tag, message)

    /** 完整运行时快照，便于对照「当时为什么没触发」。 */
    fun snapshot(extra: String = ""): String {
        val bean = try {
            TimerSetManage.get().getZTTimerBean()
        } catch (_: Exception) {
            null
        }
        val now = System.currentTimeMillis()
        val next = TimerRuntimeState.getNextFireAtMillis()
        val mode = when (bean?.timerMode) {
            TimerBean.MODE_DAILY_TIME -> "daily@${bean.scheduledHour}:${bean.scheduledMinute}"
            else -> "interval"
        }
        val persist = try {
            TimerSessionPersist.load()
        } catch (_: Exception) {
            null
        }
        return buildString {
            append("now=").append(now)
            append(" nowFmt=").append(formatTime(now))
            append(" running=").append(TimerRuntimeState.isRunning())
            append(" nextFire=").append(next)
            if (next > 0L) {
                append(" nextFmt=").append(formatTime(next))
                append(" remainMs=").append(TimerRuntimeState.remainingMillis())
            }
            append(" execCount=").append(TimerRuntimeState.getExecutionCount())
            append(" executing=").append(TimerRuntimeState.isExecutingScript())
            append(" waiting=").append(TimerRuntimeState.isWaitingForScript())
            append(" status=").append(TimerRuntimeState.statusMessage.take(80))
            append(" mode=").append(mode)
            append(" zeroTermux=").append(bean?.isZeroTermux)
            append(" alwaysAllow=").append(bean?.isAlwaysAllowTimer)
            append(" persistActive=").append(persist?.active)
            append(" persistNext=").append(persist?.nextFireAtMillis ?: 0L)
            append(" persistCount=").append(persist?.executionCount ?: -1)
            append(" sdk=").append(Build.VERSION.SDK_INT)
            append(" app=").append(BuildConfig.VERSION_NAME)
            if (extra.isNotBlank()) {
                append(" | ").append(extra)
            }
        }
    }

    fun logSnapshot(tag: String, reason: String, extra: String = "") {
        i(tag, "[$reason] ${snapshot(extra)}")
    }

    /**
     * 看门狗心跳：约每 60s（12 * 5s）写一次，避免刷爆日志。
     * @return true 表示本次写了心跳
     */
    fun maybeHeartbeat(tag: String): Boolean {
        if (!isEnabled()) return false
        val n = heartbeatCounter.incrementAndGet()
        if (n % 12 != 0) return false
        logSnapshot(tag, "heartbeat")
        return true
    }

    fun logFilePaths(): String {
        return try {
            val sd = File(XinhaoStoragePath.getLogDir(UUtils.getContext()), FILE_NAME).absolutePath
            val home = File(FileUrl.timerShellLogDir, FILE_NAME).absolutePath
            "sd=$sd home=$home"
        } catch (e: Exception) {
            "path_error=${e.message}"
        }
    }

    private fun write(level: String, tag: String, message: String) {
        if (!isEnabled()) return
        val line = "${timeFormat.get()?.format(Date())} $level/$tag: $message\n"
        Log.println(
            when (level) {
                "E" -> Log.ERROR
                "W" -> Log.WARN
                else -> Log.INFO
            },
            "ZeroTermux--$TAG--$tag",
            message
        )
        writer.execute {
            try {
                appendTo(File(XinhaoStoragePath.getLogDir(UUtils.getContext()), FILE_NAME), line)
            } catch (e: Exception) {
                Log.w(TAG, "write sd log failed: ${e.message}")
            }
            try {
                File(FileUrl.timerShellLogDir).mkdirs()
                appendTo(File(FileUrl.timerShellLogDir, FILE_NAME), line)
            } catch (e: Exception) {
                Log.w(TAG, "write home log failed: ${e.message}")
            }
        }
    }

    private fun appendTo(file: File, line: String) {
        file.parentFile?.mkdirs()
        if (file.exists() && file.length() > MAX_BYTES) {
            rotate(file)
        }
        FileOutputStream(file, true).use { out ->
            out.write(line.toByteArray(Charsets.UTF_8))
            out.flush()
        }
    }

    private fun rotate(file: File) {
        val bak = File(file.parentFile, "$FILE_NAME.1")
        if (bak.exists()) bak.delete()
        file.renameTo(bak)
    }

    private fun formatTime(millis: Long): String {
        if (millis <= 0L) return "-"
        return try {
            timeFormat.get()?.format(Date(millis)) ?: millis.toString()
        } catch (_: Exception) {
            millis.toString()
        }
    }
}
