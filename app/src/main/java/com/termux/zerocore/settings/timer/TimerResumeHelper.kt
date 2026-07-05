package com.termux.zerocore.settings.timer

import android.content.Context
import android.content.Intent
import android.os.Build
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.services.TimerExeService

object TimerResumeHelper {

    @JvmStatic
    fun tryResumeTimer(context: Context) {
        val bean = TimerSetManage.get().getZTTimerBean()
        if (!bean.isAlwaysAllowTimer) return
        if (TimerRuntimeState.isRunning()) return

        val snapshot = TimerSessionPersist.load() ?: return
        if (!snapshot.active || snapshot.nextFireAtMillis <= 0L) return

        LibSuManage.getInstall().cunt = snapshot.executionCount
        TimerRuntimeState.setExecutionCount(snapshot.executionCount)
        TimerRuntimeState.restoreNextFireAtMillis(snapshot.nextFireAtMillis)

        val intent = Intent(context, TimerExeService::class.java).apply {
            action = TimerExeService.TIMER_EXE_START
            putExtra(TimerExeService.EXTRA_RESUME, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
