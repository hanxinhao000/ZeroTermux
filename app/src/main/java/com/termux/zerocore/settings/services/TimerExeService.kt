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
import com.termux.zerocore.settings.timer.TimerScheduleHelper
import com.termux.zerocore.settings.timer.TimerSessionPersist
import com.termux.zerocore.settings.timer.TimerTermuxSessionHelper
import com.termux.zerocore.utils.SingletonCommunicationUtils
import com.termux.zerocore.utils.NotificationUtils
import com.zp.z_file.util.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

class TimerExeService : Service(), LibSuManage.TimerListener {

    companion object {
        const val TIMER_EXE_START = "timer_exe_start"
        const val TIMER_EXE_END = "timer_exe_end"
        const val EXTRA_RESUME = "timer_extra_resume"
        private const val TAG = "TimerExeService"
        private const val NOTIFICATION_ID = 1556
        private const val SCRIPT_WAIT_POLL_MS = 2_000L
        private const val LAUNCH_RETRY_MS = 1_000L
        private const val STUCK_WATCHDOG_MS = 5_000L
        /** 每日定时：过期超过该阈值则视为错过窗口，直接排到下一天，避免重复触发。 */
        private const val DAILY_OVERDUE_SKIP_MS = 120_000L
    }

    private var mTimerBean: TimerBean? = null
    private var mLibSuManage: LibSuManage? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isActive = AtomicBoolean(false)
    private val pendingRunAfterScript = AtomicBoolean(false)
    private val isLaunchingCommand = AtomicBoolean(false)

    private val scheduleRunnable = Runnable { onIntervalElapsed() }
    private val waitForScriptRunnable = Runnable { waitForScriptToFinish() }
    private val stuckWatchdogRunnable = Runnable { checkStuckCountdown() }
    private val launchRetryRunnable = Runnable {
        if (isActive.get() && TimerRuntimeState.remainingMillis() <= 0L) {
            onIntervalElapsed()
        }
    }

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
                val resume = intent.getBooleanExtra(EXTRA_RESUME, false)
                startTimer(resume)
                return START_STICKY
            }
            TIMER_EXE_END -> {
                LogUtils.e(TAG, "onStartCommand TIMER_EXE_END")
                endTime(userInitiated = true)
                return START_NOT_STICKY
            }
        }
        return if (isActive.get()) START_STICKY else START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isActive.get()) {
            TimerSessionPersist.saveForBackgroundResume()
            refreshForegroundNotification()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startTimer(resume: Boolean = false) {
        if (!isActive.compareAndSet(false, true)) {
            refreshForegroundNotification()
            return
        }
        isLaunchingCommand.set(false)
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        mLibSuManage?.setTimerListener(this)
        ensureLogWriter()
        TimerRuntimeState.setRunning(true)
        if (!resume) {
            TimerRuntimeState.setExecutionCount(mLibSuManage?.cunt ?: 0)
        } else {
            mLibSuManage?.cunt = TimerRuntimeState.getExecutionCount()
        }
        if (isScriptRunning()) {
            val continueResume = {
                resumeActiveScriptState()
                startStuckWatchdog()
            }
            if (needsZeroTermuxSession()) {
                ensureZeroTermuxSession(continueResume)
            } else {
                continueResume()
            }
            return
        }
        pendingRunAfterScript.set(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        refreshForegroundNotification()
        val startScheduling = {
            if (resume) {
                resumeFromPersistedSchedule()
            } else {
                scheduleNextExecution()
            }
            startStuckWatchdog()
        }
        startScheduling()
    }

    private fun needsZeroTermuxSession(): Boolean {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        return mTimerBean?.isZeroTermux == true
    }

    private fun ensureZeroTermuxSession(onReady: () -> Unit) {
        TimerTermuxSessionHelper.ensureSession(applicationContext) { ok ->
            mainHandler.post {
                if (!ok) {
                    LogUtils.e(TAG, "ZeroTermux background session bootstrap failed")
                }
                if (isActive.get()) {
                    onReady()
                }
            }
        }
    }

    private fun resumeFromPersistedSchedule() {
        val remaining = TimerRuntimeState.remainingMillis()
        if (remaining > 0L) {
            TimerRuntimeState.statusMessage = buildWaitingMessage()
            mainHandler.removeCallbacks(scheduleRunnable)
            mainHandler.postDelayed(scheduleRunnable, remaining)
            refreshForegroundNotification()
            return
        }
        val nextFire = TimerRuntimeState.getNextFireAtMillis()
        if (nextFire > 0L) {
            mTimerBean = TimerSetManage.get().getZTTimerBean()
            val bean = mTimerBean
            // 每日定时若已过期较久，说明错过窗口；直接排到下一天，避免重复执行后卡在 00:00
            if (bean != null &&
                bean.timerMode == TimerBean.MODE_DAILY_TIME &&
                System.currentTimeMillis() - nextFire > DAILY_OVERDUE_SKIP_MS
            ) {
                LogUtils.e(TAG, "resume: daily fire overdue, reschedule to next day")
                scheduleNextExecution()
                return
            }
            onIntervalElapsed()
            return
        }
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
        mainHandler.removeCallbacks(launchRetryRunnable)
        pendingRunAfterScript.set(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        val bean = mTimerBean ?: return
        val nextAt = TimerScheduleHelper.computeNextFireAtMillis(bean)
        val delay = (nextAt - System.currentTimeMillis()).coerceAtLeast(1_000L)
        TimerRuntimeState.setNextFireAtMillis(nextAt)
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
            return
        }
        TimerRuntimeState.setWaitingForScript(false)
        // 等待结束但无待执行任务：若倒计时已归零，推进到下一次，避免卡在 00:00
        if (TimerRuntimeState.getNextFireAtMillis() > 0L &&
            TimerRuntimeState.remainingMillis() <= 0L
        ) {
            LogUtils.e(TAG, "waitForScript: recovering stuck zero countdown")
            scheduleNextExecution()
        }
    }

    private fun isScriptRunning(): Boolean {
        return mLibSuManage?.isShellCommandRunning == true
    }

    private fun runScheduledCommand() {
        if (!isActive.get()) return
        if (!isLaunchingCommand.compareAndSet(false, true)) {
            // 正在启动中：稍后重试，避免静默丢弃导致永不 scheduleNext
            mainHandler.removeCallbacks(launchRetryRunnable)
            mainHandler.postDelayed(launchRetryRunnable, LAUNCH_RETRY_MS)
            return
        }
        mainHandler.removeCallbacks(launchRetryRunnable)
        val launch = { execCommandInternal() }
        if (needsZeroTermuxSession()) {
            ensureZeroTermuxSession(launch)
        } else {
            launch()
        }
    }

    private fun execCommandInternal() {
        execCommand {
            isLaunchingCommand.set(false)
            TimerRuntimeState.setExecutingScript(false)
            if (!isActive.get()) {
                // Service 正在停止时仍尽量把下一次绝对时间写入持久化，避免恢复后卡在过期的 00:00
                persistNextFireIfAlwaysAllow()
                return@execCommand
            }
            if (pendingRunAfterScript.get()) {
                mainHandler.post(waitForScriptRunnable)
                return@execCommand
            }
            scheduleNextExecution()
        }
    }

    private fun persistNextFireIfAlwaysAllow() {
        val bean = TimerSetManage.get().getZTTimerBean()
        if (!bean.isAlwaysAllowTimer) return
        val nextAt = TimerScheduleHelper.computeNextFireAtMillis(bean)
        TimerRuntimeState.setNextFireAtMillis(nextAt)
        TimerSessionPersist.saveForBackgroundResume()
    }

    private fun ensureLogWriter() {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        if (mLibSuManage?.isRun != true) {
            mLibSuManage?.initRunnable(mTimerBean!!.isZeroTermux)
        }
    }

    private fun execCommand(onComplete: Runnable?) {
        val manage = mLibSuManage
        if (manage == null) {
            LogUtils.e(TAG, "execCommand: LibSuManage is null")
            onComplete?.run()
            return
        }
        val count = manage.cunt + 1
        manage.cunt = count
        TimerRuntimeState.setExecutionCount(count)
        TimerSessionPersist.saveIfAllowed()
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

    private fun startStuckWatchdog() {
        mainHandler.removeCallbacks(stuckWatchdogRunnable)
        mainHandler.postDelayed(stuckWatchdogRunnable, STUCK_WATCHDOG_MS)
    }

    private fun checkStuckCountdown() {
        if (!isActive.get()) return
        try {
            if (needsZeroTermuxSession() &&
                !SingletonCommunicationUtils.getInstance().hasTerminalListener() &&
                (TimerRuntimeState.isExecutingScript() ||
                    TimerRuntimeState.isWaitingForScript() ||
                    isLaunchingCommand.get() ||
                    pendingRunAfterScript.get())
            ) {
                ensureZeroTermuxSession { }
            }
            if (!TimerRuntimeState.isExecutingScript() &&
                !TimerRuntimeState.isWaitingForScript() &&
                !isLaunchingCommand.get() &&
                TimerRuntimeState.getNextFireAtMillis() > 0L &&
                TimerRuntimeState.remainingMillis() <= 0L
            ) {
                LogUtils.e(TAG, "watchdog: stuck at 00:00, rescheduling next run")
                scheduleNextExecution()
            }
        } finally {
            if (isActive.get()) {
                mainHandler.postDelayed(stuckWatchdogRunnable, STUCK_WATCHDOG_MS)
            }
        }
    }

    private fun endTime(userInitiated: Boolean = false) {
        if (!isActive.getAndSet(false)) {
            stopSelf()
            return
        }
        mainHandler.removeCallbacks(scheduleRunnable)
        mainHandler.removeCallbacks(waitForScriptRunnable)
        mainHandler.removeCallbacks(stuckWatchdogRunnable)
        mainHandler.removeCallbacks(launchRetryRunnable)
        pendingRunAfterScript.set(false)
        isLaunchingCommand.set(false)
        mLibSuManage?.stop()
        mLibSuManage?.logThreadStop()
        mLibSuManage?.setTimerListener(null)
        TimerRuntimeState.setRunning(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        TimerRuntimeState.statusMessage = ""
        if (userInitiated) {
            TimerRuntimeState.resetForUserStop()
        } else if (TimerSetManage.get().getZTTimerBean().isAlwaysAllowTimer) {
            TimerSessionPersist.saveForBackgroundResume()
            TimerRuntimeState.clearSchedule()
        } else {
            TimerRuntimeState.resetForUserStop()
        }
        if (needsZeroTermuxSession()) {
            TimerTermuxSessionHelper.releaseIfHeadless(applicationContext)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationUtils.cancelNotification(applicationContext, NOTIFICATION_ID)
        stopSelf()
    }

    private fun getIntervalMs(): Long {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        return TimerScheduleHelper.computeNextDelayMs(mTimerBean!!)
    }

    private fun getTimeLabel(): String {
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        return TimerScheduleHelper.formatScheduleLabel(mTimerBean!!)
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

    override fun onBind(intent: Intent?): IBinder? {
        return TimerExeLocalBinder(this)
    }

    override fun onDestroy() {
        if (isActive.get()) {
            val alwaysAllow = TimerSetManage.get().getZTTimerBean().isAlwaysAllowTimer
            endTime(userInitiated = !alwaysAllow)
        }
        super.onDestroy()
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement: $msg")
    }
}
