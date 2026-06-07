package com.termux.zerocore.settings.timer

import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object TimerRuntimeState {
    private val running = AtomicBoolean(false)
    private val executionCount = AtomicInteger(0)
    private val nextFireAtMillis = AtomicLong(0L)
    @Volatile
    var statusMessage: String = ""
    private val waitingForScript = AtomicBoolean(false)
    private val executingScript = AtomicBoolean(false)

    fun setWaitingForScript(value: Boolean) {
        waitingForScript.set(value)
    }

    fun isWaitingForScript(): Boolean = waitingForScript.get()

    fun setExecutingScript(value: Boolean) {
        executingScript.set(value)
    }

    fun isExecutingScript(): Boolean = executingScript.get()

    fun setRunning(value: Boolean) {
        running.set(value)
    }

    fun isRunning(): Boolean = running.get()

    fun setExecutionCount(value: Int) {
        executionCount.set(value)
    }

    fun getExecutionCount(): Int = executionCount.get()

    fun incrementExecutionCount(): Int = executionCount.incrementAndGet()

    fun scheduleNext(delayMs: Long) {
        nextFireAtMillis.set(System.currentTimeMillis() + delayMs)
    }

    fun clearSchedule() {
        nextFireAtMillis.set(0L)
    }

    fun remainingMillis(): Long {
        val target = nextFireAtMillis.get()
        if (target <= 0L) return 0L
        return (target - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun formatCountdown(): String {
        if (!isRunning() || isExecutingScript() || isWaitingForScript()) {
            return "--:--"
        }
        val totalSeconds = remainingMillis() / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
