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

    private const val TAG = "TimerResumeHelper"
    private const val RESUME_DELAY_MS = 1_200L

    @JvmStatic
    fun tryResumeTimer(context: Context) {
        val bean = TimerSetManage.get().getZTTimerBean()
        if (!bean.isAlwaysAllowTimer) {
            TimerDiagLog.i(TAG, "tryResume SKIP: alwaysAllow=false")
            return
        }
        if (TimerRuntimeState.isRunning()) {
            TimerDiagLog.i(TAG, "tryResume SKIP: already running")
            return
        }

        val snapshot = TimerSessionPersist.load()
        if (snapshot == null) {
            TimerDiagLog.w(TAG, "tryResume SKIP: no persist snapshot")
            return
        }
        if (!snapshot.active || snapshot.nextFireAtMillis <= 0L) {
            TimerDiagLog.w(
                TAG,
                "tryResume SKIP: snapshot inactive/invalid active=${snapshot.active} next=${snapshot.nextFireAtMillis}"
            )
            return
        }

        val now = System.currentTimeMillis()
        val remain = snapshot.nextFireAtMillis - now
        TimerDiagLog.i(
            TAG,
            "tryResume OK count=${snapshot.executionCount} next=${snapshot.nextFireAtMillis} remainMs=$remain delay=${RESUME_DELAY_MS}ms"
        )

        LibSuManage.getInstall().cunt = snapshot.executionCount
        TimerRuntimeState.setExecutionCount(snapshot.executionCount)
        TimerRuntimeState.restoreNextFireAtMillis(snapshot.nextFireAtMillis)

        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            if (TimerRuntimeState.isRunning()) {
                TimerDiagLog.w(TAG, "tryResume delayed: already running, skip startService")
                return@postDelayed
            }
            val intent = Intent(appContext, TimerExeService::class.java).apply {
                action = TimerExeService.TIMER_EXE_START
                putExtra(TimerExeService.EXTRA_RESUME, true)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                    TimerDiagLog.i(TAG, "tryResume startForegroundService OK")
                } else {
                    appContext.startService(intent)
                    TimerDiagLog.i(TAG, "tryResume startService OK")
                }
            } catch (e: Exception) {
                TimerDiagLog.e(TAG, "tryResume startForegroundService FAIL: ${e.message}, fallback startService")
                try {
                    appContext.startService(intent)
                    TimerDiagLog.i(TAG, "tryResume fallback startService OK")
                } catch (e2: Exception) {
                    TimerDiagLog.e(TAG, "tryResume fallback startService FAIL: ${e2.message}")
                }
            }
        }, RESUME_DELAY_MS)
    }
}
