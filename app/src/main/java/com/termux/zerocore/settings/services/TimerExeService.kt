package com.termux.zerocore.settings.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.timer.TimerBean
import com.termux.zerocore.utils.NotificationUtils
import com.topjohnwu.superuser.Shell
import com.zp.z_file.util.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

class TimerExeService : Service(), LibSuManage.TimerListener {

    companion object {
        const val TIMER_EXE_START = "timer_exe_start"
        const val TIMER_EXE_END = "timer_exe_end"
        private const val TAG = "TimerExeService"
        private const val NOTIFICATION_ID = 1556
        /** Shell 仍存活时的重试间隔，避免 5 秒高频唤醒 */
        private const val SHELL_IDLE_RETRY_MS = 30_000L
        private const val MAX_SHELL_IDLE_RETRIES = 6
    }

    private var mTimerBean: TimerBean? = null
    private var mLibSuManage: LibSuManage? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isActive = AtomicBoolean(false)
    private var shellIdleRetries = 0

    private val scheduleRunnable = Runnable { onIntervalElapsed() }
    private val shellRetryRunnable = Runnable { ensureShellIdleAndRun() }

    class TimerExeLocalBinder : Binder {
        val service: TimerExeService
        constructor(timerExeService: TimerExeService) {
            service = timerExeService
        }
    }

    override fun onCreate() {
        super.onCreate()
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        mLibSuManage = LibSuManage.getInstall()
        mLibSuManage?.setTimerListener(this)
        mLibSuManage?.cunt = 0
        if (!mLibSuManage!!.isFileExists) {
            mLibSuManage?.writerFile()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            TIMER_EXE_START -> {
                LogUtils.e(TAG, "onStartCommand TIMER_EXE_START")
                startTimer()
                return START_STICKY
            }
            TIMER_EXE_END -> {
                LogUtils.e(TAG, "onStartCommand TIMER_EXE_END")
                endTime()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        if (!isActive.compareAndSet(false, true)) {
            return
        }
        shellIdleRetries = 0
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        mLibSuManage?.setTimerListener(this)
        startForeground(
            NOTIFICATION_ID,
            NotificationUtils.buildTimerNotification(
                applicationContext,
                UUtils.getString(R.string.zt_timer_notification_timer_title),
                buildWaitingMessage()
            )
        )
        scheduleNextExecution()
    }

    private fun scheduleNextExecution() {
        if (!isActive.get()) return
        mainHandler.removeCallbacks(scheduleRunnable)
        val delay = getIntervalMs()
        updateWaitingNotification()
        mainHandler.postDelayed(scheduleRunnable, delay)
    }

    private fun onIntervalElapsed() {
        if (!isActive.get()) return
        shellIdleRetries = 0
        ensureShellIdleAndRun()
    }

    private fun ensureShellIdleAndRun() {
        if (!isActive.get()) return
        val cachedShell = Shell.getCachedShell()
        if (cachedShell != null && cachedShell.isAlive) {
            if (shellIdleRetries >= MAX_SHELL_IDLE_RETRIES) {
                mLibSuManage?.stop()
                shellIdleRetries = 0
                mainHandler.postDelayed(shellRetryRunnable, SHELL_IDLE_RETRY_MS)
                return
            }
            shellIdleRetries++
            mLibSuManage?.stop()
            mainHandler.postDelayed(shellRetryRunnable, SHELL_IDLE_RETRY_MS)
            return
        }
        shellIdleRetries = 0
        runScheduledCommand()
    }

    private fun runScheduledCommand() {
        if (!isActive.get()) return
        NotificationUtils.updateNotification(
            applicationContext,
            NOTIFICATION_ID,
            UUtils.getString(R.string.zt_timer_notification_timer_title),
            UUtils.getString(R.string.zt_timer_notification_timer_kill)
        )
        execCommand {
            if (isActive.get()) {
                scheduleNextExecution()
            }
        }
    }

    private fun execCommand(onComplete: Runnable?) {
        val manage = mLibSuManage ?: return
        manage.cunt = manage.cunt + 1
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        val command = if (mTimerBean!!.isZeroTermux) "shell_ZeroTermux" else "shell_Android"
        manage.shellCommandExec(command, onComplete)
    }

    private fun endTime() {
        if (!isActive.getAndSet(false)) {
            stopSelf()
            return
        }
        mainHandler.removeCallbacks(scheduleRunnable)
        mainHandler.removeCallbacks(shellRetryRunnable)
        mLibSuManage?.stop()
        mLibSuManage?.logThreadStop()
        mLibSuManage?.setTimerListener(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationUtils.cancelNotification(applicationContext, NOTIFICATION_ID)
        stopSelf()
    }

    private fun getIntervalMs(): Long {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        return if (mTimerBean!!.timerNumber == TimerBean.TIMER_OTHER) {
            mTimerBean!!.timerOtherNumber.coerceAtLeast(30_000L)
        } else {
            mTimerBean!!.timerNumber.toLong().coerceAtLeast(30_000L)
        }
    }

    private fun buildWaitingMessage(): String {
        return "${UUtils.getString(R.string.zt_timer_notification_timer_sum)} ${getTimeLabel()}\n" +
            "${UUtils.getString(R.string.zt_timer_notification_timer_cunt)} ${mLibSuManage?.cunt ?: 0}"
    }

    private fun updateWaitingNotification() {
        NotificationUtils.updateNotification(
            applicationContext,
            NOTIFICATION_ID,
            UUtils.getString(R.string.zt_timer_notification_timer_title),
            buildWaitingMessage()
        )
    }

    private fun getTimeLabel(): String {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        if (mTimerBean!!.timerNumber == TimerBean.TIMER_OTHER) {
            return if (mTimerBean!!.timerOtherNumber > 60 * 1000) {
                "${mTimerBean!!.timerOtherNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
            } else {
                "< 1 ${UUtils.getString(R.string.zt_timer_minute)}"
            }
        }
        return "${mTimerBean!!.timerNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return TimerExeLocalBinder(this)
    }

    override fun onDestroy() {
        endTime()
        super.onDestroy()
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement: $msg")
    }
}
