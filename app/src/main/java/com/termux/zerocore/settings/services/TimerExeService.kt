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
import com.termux.zerocore.settings.timer.TimerNotificationHelper
import com.termux.zerocore.settings.timer.TimerRuntimeState
import com.termux.zerocore.utils.NotificationUtils
import com.zp.z_file.util.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

class TimerExeService : Service(), LibSuManage.TimerListener {

    companion object {
        const val TIMER_EXE_START = "timer_exe_start"
        const val TIMER_EXE_END = "timer_exe_end"
        private const val TAG = "TimerExeService"
        private const val NOTIFICATION_ID = 1556
        private const val SCRIPT_WAIT_POLL_MS = 2_000L
    }

    private var mTimerBean: TimerBean? = null
    private var mLibSuManage: LibSuManage? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isActive = AtomicBoolean(false)
    private val pendingRunAfterScript = AtomicBoolean(false)
    private val isLaunchingCommand = AtomicBoolean(false)

    private val scheduleRunnable = Runnable { onIntervalElapsed() }
    private val waitForScriptRunnable = Runnable { waitForScriptToFinish() }

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
        if (!mLibSuManage!!.isFileExists) {
            mLibSuManage?.writerFile()
        }
        TimerRuntimeState.setExecutionCount(mLibSuManage?.cunt ?: 0)
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
        return if (isActive.get()) START_STICKY else START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isActive.get()) {
            refreshForegroundNotification()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startTimer() {
        if (!isActive.compareAndSet(false, true)) {
            refreshForegroundNotification()
            return
        }
        isLaunchingCommand.set(false)
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        mLibSuManage?.setTimerListener(this)
        ensureLogWriter()
        TimerRuntimeState.setRunning(true)
        TimerRuntimeState.setExecutionCount(mLibSuManage?.cunt ?: 0)
        if (isScriptRunning()) {
            resumeActiveScriptState()
            return
        }
        pendingRunAfterScript.set(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        refreshForegroundNotification()
        scheduleNextExecution()
    }

    private fun resumeActiveScriptState() {
        TimerRuntimeState.setExecutingScript(true)
        if (TimerRuntimeState.isWaitingForScript() || TimerRuntimeState.remainingMillis() <= 0L) {
            pendingRunAfterScript.set(true)
            TimerRuntimeState.setWaitingForScript(true)
            TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_waiting_script)
            mainHandler.removeCallbacks(waitForScriptRunnable)
            mainHandler.post(waitForScriptRunnable)
        } else {
            TimerRuntimeState.setWaitingForScript(false)
            TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_executing_current_script)
            val remaining = TimerRuntimeState.remainingMillis()
            mainHandler.removeCallbacks(scheduleRunnable)
            mainHandler.postDelayed(scheduleRunnable, remaining)
        }
        refreshForegroundNotification()
    }

    private fun scheduleNextExecution() {
        if (!isActive.get()) return
        mainHandler.removeCallbacks(scheduleRunnable)
        pendingRunAfterScript.set(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        val delay = getIntervalMs()
        TimerRuntimeState.scheduleNext(delay)
        TimerRuntimeState.statusMessage = buildWaitingMessage()
        refreshForegroundNotification()
        mainHandler.postDelayed(scheduleRunnable, delay)
    }

    private fun onIntervalElapsed() {
        if (!isActive.get()) return
        if (isScriptRunning()) {
            pendingRunAfterScript.set(true)
            TimerRuntimeState.setWaitingForScript(true)
            TimerRuntimeState.setExecutingScript(true)
            TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_waiting_script)
            refreshForegroundNotification()
            mainHandler.removeCallbacks(waitForScriptRunnable)
            mainHandler.post(waitForScriptRunnable)
            return
        }
        runScheduledCommand()
    }

    private fun waitForScriptToFinish() {
        if (!isActive.get()) return
        if (isScriptRunning()) {
            TimerRuntimeState.setExecutingScript(true)
            TimerRuntimeState.setWaitingForScript(pendingRunAfterScript.get())
            TimerRuntimeState.statusMessage = if (pendingRunAfterScript.get()) {
                UUtils.getString(R.string.zt_timer_waiting_script)
            } else {
                UUtils.getString(R.string.zt_timer_executing_current_script)
            }
            refreshForegroundNotification()
            mainHandler.postDelayed(waitForScriptRunnable, SCRIPT_WAIT_POLL_MS)
            return
        }
        TimerRuntimeState.setExecutingScript(false)
        if (pendingRunAfterScript.compareAndSet(true, false)) {
            TimerRuntimeState.setWaitingForScript(false)
            runScheduledCommand()
        }
    }

    private fun isScriptRunning(): Boolean {
        return mLibSuManage?.isShellCommandRunning == true
    }

    private fun runScheduledCommand() {
        if (!isActive.get()) return
        if (!isLaunchingCommand.compareAndSet(false, true)) {
            return
        }
        execCommand {
            isLaunchingCommand.set(false)
            TimerRuntimeState.setExecutingScript(false)
            if (!isActive.get()) return@execCommand
            if (pendingRunAfterScript.get()) {
                mainHandler.post(waitForScriptRunnable)
                return@execCommand
            }
            scheduleNextExecution()
        }
    }

    private fun ensureLogWriter() {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        if (mLibSuManage?.isRun != true) {
            mLibSuManage?.initRunnable(mTimerBean!!.isZeroTermux)
        }
    }

    private fun execCommand(onComplete: Runnable?) {
        val manage = mLibSuManage ?: return
        val count = manage.cunt + 1
        manage.cunt = count
        TimerRuntimeState.setExecutionCount(count)
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        val command = if (mTimerBean!!.isZeroTermux) "shell_ZeroTermux" else "shell_Android"
        ensureLogWriter()
        manage.writeRunHeader(count)
        TimerRuntimeState.setExecutingScript(true)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_executing_current_script)
        refreshForegroundNotification()
        manage.shellCommandExec(command, onComplete)
    }

    private fun endTime() {
        if (!isActive.getAndSet(false)) {
            stopSelf()
            return
        }
        mainHandler.removeCallbacks(scheduleRunnable)
        mainHandler.removeCallbacks(waitForScriptRunnable)
        pendingRunAfterScript.set(false)
        isLaunchingCommand.set(false)
        mLibSuManage?.stop()
        mLibSuManage?.logThreadStop()
        mLibSuManage?.setTimerListener(null)
        TimerRuntimeState.setRunning(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        TimerRuntimeState.clearSchedule()
        TimerRuntimeState.statusMessage = ""
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationUtils.cancelNotification(applicationContext, NOTIFICATION_ID)
        stopSelf()
    }

    private fun getIntervalMs(): Long {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        return if (mTimerBean!!.timerNumber == TimerBean.TIMER_OTHER) {
            mTimerBean!!.timerOtherNumber.coerceAtLeast(1_000L)
        } else {
            mTimerBean!!.timerNumber.toLong().coerceAtLeast(1_000L)
        }
    }

    private fun buildWaitingMessage(): String {
        return "${UUtils.getString(R.string.zt_timer_notification_timer_sum)} ${getTimeLabel()}\n" +
            "${UUtils.getString(R.string.zt_timer_notification_timer_cunt)} ${TimerRuntimeState.getExecutionCount()}"
    }

    private fun refreshForegroundNotification() {
        val content = when {
            TimerRuntimeState.isExecutingScript() && TimerRuntimeState.isWaitingForScript() ->
                UUtils.getString(R.string.zt_timer_waiting_script)
            TimerRuntimeState.isExecutingScript() ->
                UUtils.getString(R.string.zt_timer_executing_current_script)
            TimerRuntimeState.statusMessage.isNotBlank() -> TimerRuntimeState.statusMessage
            else -> buildWaitingMessage()
        }
        val notification = NotificationUtils.buildTimerNotification(
            applicationContext,
            UUtils.getString(R.string.zt_timer_notification_timer_title),
            content,
            TimerNotificationHelper.buildOpenTimerIntent(applicationContext)
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getTimeLabel(): String {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        if (mTimerBean!!.timerNumber == TimerBean.TIMER_OTHER) {
            return if (mTimerBean!!.timerOtherNumber >= 60 * 1000) {
                "${mTimerBean!!.timerOtherNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
            } else if (mTimerBean!!.timerOtherNumber >= 1000) {
                "${mTimerBean!!.timerOtherNumber / 1000} ${UUtils.getString(R.string.zt_timer_second_unit)}"
            } else {
                "< 1 ${UUtils.getString(R.string.zt_timer_second_unit)}"
            }
        }
        return when (mTimerBean!!.timerNumber) {
            TimerBean.TIMER_30_SECOND -> UUtils.getString(R.string.zt_timer_30_second)
            TimerBean.TIMER_1_MINUTE -> UUtils.getString(R.string.zt_timer_1_minute)
            TimerBean.TIMER_10_MINUTE -> UUtils.getString(R.string.zt_timer_10_minute)
            TimerBean.TIMER_30_MINUTE -> UUtils.getString(R.string.zt_timer_30_minute)
            else -> "${mTimerBean!!.timerNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return TimerExeLocalBinder(this)
    }

    override fun onDestroy() {
        if (isActive.get()) {
            endTime()
        }
        super.onDestroy()
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement: $msg")
    }
}
