package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.timer.TimerBean
import com.termux.zerocore.settings.timer.TimerRuntimeState
import com.termux.zerocore.settings.timer.TimerScheduleHelper
import com.termux.zerocore.settings.timer.TimerSessionPersist
import com.termux.zerocore.settings.timer.TimerTermuxSessionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ZtAiDebugTimerHelper {

    private val gson = Gson()
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun statusJson(context: Context): String {
        val bean = TimerSetManage.get().getZTTimerBean()
        val snapshot = TimerSessionPersist.load()
        val libSu = LibSuManage.getInstall()
        val now = System.currentTimeMillis()
        val nextFire = TimerRuntimeState.getNextFireAtMillis()
        val computedNext = TimerScheduleHelper.computeNextFireAtMillis(bean, now)

        val modeLabel = when (bean.timerMode) {
            TimerBean.MODE_DAILY_TIME -> "daily"
            else -> "interval"
        }

        val intervalPreset = when (bean.timerNumber) {
            TimerBean.TIMER_30_SECOND -> "30s"
            TimerBean.TIMER_1_MINUTE -> "1m"
            TimerBean.TIMER_10_MINUTE -> "10m"
            TimerBean.TIMER_30_MINUTE -> "30m"
            TimerBean.TIMER_OTHER -> "custom"
            else -> "unknown(${bean.timerNumber})"
        }

        return gson.toJson(
            mapOf(
                "ok" to true,
                "nowMillis" to now,
                "now" to formatTime(now),
                "settings" to mapOf(
                    "timerMode" to bean.timerMode,
                    "timerModeLabel" to modeLabel,
                    "timerNumber" to bean.timerNumber,
                    "intervalPreset" to intervalPreset,
                    "timerOtherNumber" to bean.timerOtherNumber,
                    "scheduledHour" to bean.scheduledHour,
                    "scheduledMinute" to bean.scheduledMinute,
                    "scheduledClock" to TimerScheduleHelper.formatClock(
                        bean.scheduledHour,
                        bean.scheduledMinute
                    ),
                    "scheduleLabel" to TimerScheduleHelper.formatScheduleLabel(bean),
                    "isZeroTermux" to bean.isZeroTermux,
                    "alwaysAllowTimer" to bean.isAlwaysAllowTimer
                ),
                "runtime" to mapOf(
                    "running" to TimerRuntimeState.isRunning(),
                    "executionCount" to TimerRuntimeState.getExecutionCount(),
                    "nextFireAtMillis" to nextFire,
                    "nextFireAt" to if (nextFire > 0L) formatTime(nextFire) else null,
                    "remainingMillis" to TimerRuntimeState.remainingMillis(),
                    "countdown" to TimerRuntimeState.formatCountdown(),
                    "executingScript" to TimerRuntimeState.isExecutingScript(),
                    "waitingForScript" to TimerRuntimeState.isWaitingForScript(),
                    "statusMessage" to TimerRuntimeState.statusMessage,
                    "computedNextFireAtMillis" to computedNext,
                    "computedNextFireAt" to formatTime(computedNext)
                ),
                "persistedSnapshot" to snapshot?.let {
                    mapOf(
                        "active" to it.active,
                        "nextFireAtMillis" to it.nextFireAtMillis,
                        "nextFireAt" to formatTime(it.nextFireAtMillis),
                        "executionCount" to it.executionCount
                    )
                },
                "libSu" to mapOf(
                    "isRun" to (libSu?.isRun == true),
                    "cunt" to (libSu?.cunt ?: 0),
                    "shellCommandRunning" to (libSu?.isShellCommandRunning == true)
                ),
                "termuxSession" to mapOf(
                    "sessionReady" to TimerTermuxSessionHelper.isSessionReady(),
                    "scriptRunningInSession" to TimerTermuxSessionHelper.isScriptRunningInSession()
                ),
                "diagnosis" to buildDiagnosis(bean, nextFire, computedNext, snapshot)
            )
        )
    }

    private fun buildDiagnosis(
        bean: TimerBean,
        nextFire: Long,
        computedNext: Long,
        snapshot: TimerSessionPersist.Snapshot?
    ): List<String> {
        val notes = mutableListOf<String>()
        if (TimerRuntimeState.isRunning() && nextFire > 0L &&
            kotlin.math.abs(nextFire - computedNext) > 5_000L
        ) {
            notes.add(
                "nextFireAt differs from settings-based schedule by more than 5s; " +
                    "may be mid-countdown from resume"
            )
        }
        if (snapshot != null && snapshot.active && !TimerRuntimeState.isRunning()) {
            notes.add("persisted snapshot is active but runtime is not running; resume may have failed")
        }
        if (TimerRuntimeState.isExecutingScript() && TimerTermuxSessionHelper.isScriptRunningInSession()) {
            notes.add("executingScript and scriptRunningInSession both true; if no log output, session write likely failed")
        }
        if (bean.timerMode == TimerBean.MODE_INTERVAL && bean.timerNumber == TimerBean.TIMER_10_MINUTE) {
            notes.add("interval preset is 10m; UI should highlight 10-minute button, not 30s")
        }
        return notes
    }

    private fun formatTime(millis: Long): String {
        return timeFmt.format(Date(millis))
    }
}
