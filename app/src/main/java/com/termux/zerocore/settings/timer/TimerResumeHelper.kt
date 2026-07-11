package com.termux.zerocore.settings.timer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.services.TimerExeService

object TimerResumeHelper {

    private const val RESUME_DELAY_MS = 1_200L

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

        val appContext = context.applicationContext
        // 延后一点，等 Application / Bootstrap 初始化完成，再建会话、起服务
        Handler(Looper.getMainLooper()).postDelayed({
            if (TimerRuntimeState.isRunning()) return@postDelayed
            val intent = Intent(appContext, TimerExeService::class.java).apply {
                action = TimerExeService.TIMER_EXE_START
                putExtra(TimerExeService.EXTRA_RESUME, true)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (e: Exception) {
                // 仍尝试普通 startService，避免恢复彻底失败
                try {
                    appContext.startService(intent)
                } catch (_: Exception) {
                }
            }
        }, RESUME_DELAY_MS)
    }
}
