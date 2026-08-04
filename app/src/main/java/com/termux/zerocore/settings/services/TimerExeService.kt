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
import com.termux.zerocore.settings.timer.TimerDiagLog
import com.termux.zerocore.settings.timer.TimerNotificationHelper
import com.termux.zerocore.settings.timer.TimerRuntimeState
import com.termux.zerocore.settings.timer.TimerScheduleHelper
import com.termux.zerocore.settings.timer.TimerSessionPersist
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
        private const val SCRIPT_STUCK_RESET_MS = 20_000L
        private const val SCRIPT_MAX_RUNTIME_MS = 600_000L
        /** 每日定时：过期超过该阈值则视为错过窗口，直接排到下一天，避免重复触发。 */
        private const val DAILY_OVERDUE_SKIP_MS = 120_000L
    }

    private var mTimerBean: TimerBean? = null
    private var mLibSuManage: LibSuManage? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isActive = AtomicBoolean(false)
    private val pendingRunAfterScript = AtomicBoolean(false)
    private val isLaunchingCommand = AtomicBoolean(false)
    private var scriptExecStartedAtMillis = 0L

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
                val resume = intent.getBooleanExtra(EXTRA_RESUME, false)
                LogUtils.e(TAG, "onStartCommand TIMER_EXE_START resume=$resume")
                TimerDiagLog.i(TAG, "onStartCommand START resume=$resume flags=$flags startId=$startId")
                TimerDiagLog.logSnapshot(TAG, "before_startTimer")
                startTimer(resume)
                return START_STICKY
            }
            TIMER_EXE_END -> {
                LogUtils.e(TAG, "onStartCommand TIMER_EXE_END")
                TimerDiagLog.i(TAG, "onStartCommand END")
                endTime(userInitiated = true)
                return START_NOT_STICKY
            }
            else -> {
                TimerDiagLog.w(TAG, "onStartCommand unknown action=${intent?.action} isActive=${isActive.get()}")
            }
        }
        return if (isActive.get()) START_STICKY else START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isActive.get()) {
            TimerDiagLog.w(TAG, "onTaskRemoved -> saveForBackgroundResume")
            TimerDiagLog.logSnapshot(TAG, "task_removed")
            TimerSessionPersist.saveForBackgroundResume()
            refreshForegroundNotification()
        } else {
            TimerDiagLog.i(TAG, "onTaskRemoved ignored (not active)")
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startTimer(resume: Boolean = false) {
        if (!isActive.compareAndSet(false, true)) {
            TimerDiagLog.w(TAG, "startTimer ignored: already active resume=$resume")
            TimerDiagLog.logSnapshot(TAG, "start_already_active")
            refreshForegroundNotification()
            return
        }
        TimerDiagLog.i(TAG, "startTimer begin resume=$resume paths=${TimerDiagLog.logFilePaths()}")
        isLaunchingCommand.set(false)
        if (resume) {
            TimerRuntimeState.setExecutingScript(false)
            TimerRuntimeState.setWaitingForScript(false)
            TimerRuntimeState.statusMessage = ""
            pendingRunAfterScript.set(false)
            scriptExecStartedAtMillis = 0L
            isLaunchingCommand.set(false)
        }
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
            TimerDiagLog.w(TAG, "startTimer: script already running, resumeActiveScriptState")
            resumeActiveScriptState()
            startStuckWatchdog()
            return
        }
        pendingRunAfterScript.set(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        refreshForegroundNotification()
        if (resume) {
            resumeFromPersistedSchedule()
        } else {
            scheduleNextExecution()
        }
        startStuckWatchdog()
        TimerDiagLog.logSnapshot(TAG, "startTimer_done")
    }

    private fun resumeFromPersistedSchedule() {
        val remaining = TimerRuntimeState.remainingMillis()
        TimerDiagLog.i(TAG, "resumeFromPersistedSchedule remaining=$remaining next=${TimerRuntimeState.getNextFireAtMillis()}")
        if (remaining > 0L) {
            TimerRuntimeState.statusMessage = buildWaitingMessage()
            mainHandler.removeCallbacks(scheduleRunnable)
            mainHandler.postDelayed(scheduleRunnable, remaining)
            refreshForegroundNotification()
            TimerDiagLog.i(TAG, "resume: re-armed delay=${remaining}ms")
            return
        }
        val nextFire = TimerRuntimeState.getNextFireAtMillis()
        if (nextFire > 0L) {
            mTimerBean = TimerSetManage.get().getZTTimerBean()
            val bean = mTimerBean
            val overdue = System.currentTimeMillis() - nextFire
            if (bean != null &&
                bean.timerMode == TimerBean.MODE_DAILY_TIME &&
                overdue > DAILY_OVERDUE_SKIP_MS
            ) {
                LogUtils.e(TAG, "resume: daily fire overdue, reschedule to next day")
                TimerDiagLog.w(
                    TAG,
                    "resume SKIP overdue daily fire overdueMs=$overdue threshold=$DAILY_OVERDUE_SKIP_MS -> schedule next day"
                )
                scheduleNextExecution()
                return
            }
            TimerDiagLog.i(TAG, "resume: nextFire already due overdueMs=$overdue -> onIntervalElapsed NOW")
            onIntervalElapsed()
            return
        }
        TimerDiagLog.w(TAG, "resume: no nextFire, scheduleNextExecution")
        scheduleNextExecution()
    }

    private fun resumeActiveScriptState() {
        TimerDiagLog.logSnapshot(TAG, "resumeActiveScriptState")
        TimerRuntimeState.setExecutingScript(true)
        if (TimerRuntimeState.isWaitingForScript() || TimerRuntimeState.remainingMillis() <= 0L) {
            pendingRunAfterScript.set(true)
            TimerRuntimeState.setWaitingForScript(true)
            TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_waiting_script)
            mainHandler.removeCallbacks(waitForScriptRunnable)
            mainHandler.post(waitForScriptRunnable)
            TimerDiagLog.i(TAG, "resumeActiveScript: wait for script then run pending")
        } else {
            TimerRuntimeState.setWaitingForScript(false)
            TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_executing_current_script)
            val remaining = TimerRuntimeState.remainingMillis()
            mainHandler.removeCallbacks(scheduleRunnable)
            mainHandler.postDelayed(scheduleRunnable, remaining)
            TimerDiagLog.i(TAG, "resumeActiveScript: keep countdown remaining=$remaining")
        }
        refreshForegroundNotification()
    }

    private fun scheduleNextExecution() {
        if (!isActive.get()) {
            TimerDiagLog.w(TAG, "scheduleNextExecution aborted: not active")
            return
        }
        mainHandler.removeCallbacks(scheduleRunnable)
        mainHandler.removeCallbacks(launchRetryRunnable)
        pendingRunAfterScript.set(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.setExecutingScript(false)
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        val bean = mTimerBean
        if (bean == null) {
            TimerDiagLog.e(TAG, "scheduleNextExecution aborted: TimerBean null")
            return
        }
        val nextAt = TimerScheduleHelper.computeNextFireAtMillis(bean)
        val now = System.currentTimeMillis()
        val rawDelay = nextAt - now
        val delay = if (rawDelay <= 0L) 0L else rawDelay.coerceAtLeast(100L)
        TimerRuntimeState.setNextFireAtMillis(if (rawDelay <= 0L) now else nextAt)
        TimerRuntimeState.statusMessage = buildWaitingMessage()
        refreshForegroundNotification()
        TimerDiagLog.i(
            TAG,
            "scheduleNextExecution mode=${bean.timerMode} rawDelay=$rawDelay delay=$delay nextAt=$nextAt label=${TimerScheduleHelper.formatScheduleLabel(bean)}"
        )
        if (delay <= 0L) {
            TimerDiagLog.i(TAG, "scheduleNextExecution: fire immediately (post)")
            mainHandler.post(scheduleRunnable)
        } else {
            mainHandler.postDelayed(scheduleRunnable, delay)
        }
    }

    private fun onIntervalElapsed() {
        if (!isActive.get()) {
            TimerDiagLog.w(TAG, "onIntervalElapsed ignored: not active")
            return
        }
        TimerDiagLog.logSnapshot(TAG, "onIntervalElapsed")
        if (isScriptRunning()) {
            pendingRunAfterScript.set(true)
            TimerRuntimeState.setWaitingForScript(true)
            TimerRuntimeState.setExecutingScript(true)
            TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_waiting_script)
            refreshForegroundNotification()
            mainHandler.removeCallbacks(waitForScriptRunnable)
            mainHandler.post(waitForScriptRunnable)
            TimerDiagLog.w(TAG, "onIntervalElapsed DEFER: script still running, wait then run")
            return
        }
        TimerDiagLog.i(TAG, "onIntervalElapsed -> runScheduledCommand")
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
            TimerDiagLog.i(TAG, "waitForScript done -> run deferred scheduled command")
            runScheduledCommand()
            return
        }
        TimerRuntimeState.setWaitingForScript(false)
        if (TimerRuntimeState.getNextFireAtMillis() > 0L &&
            TimerRuntimeState.remainingMillis() <= 0L
        ) {
            LogUtils.e(TAG, "waitForScript: recovering stuck zero countdown")
            TimerDiagLog.w(TAG, "waitForScript: stuck zero countdown -> scheduleNext")
            scheduleNextExecution()
        }
    }

    private fun isScriptRunning(): Boolean {
        return mLibSuManage?.isShellCommandRunning == true
    }

    private fun runScheduledCommand() {
        if (!isActive.get()) {
            TimerDiagLog.w(TAG, "runScheduledCommand ignored: not active")
            return
        }
        if (!isLaunchingCommand.compareAndSet(false, true)) {
            TimerDiagLog.w(TAG, "runScheduledCommand busy launching, retry in ${LAUNCH_RETRY_MS}ms")
            mainHandler.removeCallbacks(launchRetryRunnable)
            mainHandler.postDelayed(launchRetryRunnable, LAUNCH_RETRY_MS)
            return
        }
        mainHandler.removeCallbacks(launchRetryRunnable)
        scriptExecStartedAtMillis = System.currentTimeMillis()
        TimerDiagLog.i(TAG, "runScheduledCommand launch")
        execCommandInternal()
    }

    private fun execCommandInternal() {
        execCommand {
            isLaunchingCommand.set(false)
            TimerRuntimeState.setExecutingScript(false)
            TimerRuntimeState.statusMessage = ""
            if (!isActive.get()) {
                TimerDiagLog.w(TAG, "execCommand complete but service stopping -> persistNextFireIfAlwaysAllow")
                persistNextFireIfAlwaysAllow()
                return@execCommand
            }
            if (pendingRunAfterScript.get()) {
                TimerDiagLog.i(TAG, "execCommand complete -> pending waitForScript")
                mainHandler.post(waitForScriptRunnable)
                return@execCommand
            }
            TimerDiagLog.i(TAG, "execCommand complete -> scheduleNextExecution")
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
            TimerDiagLog.e(TAG, "execCommand FAIL: LibSuManage null")
            onComplete?.run()
            return
        }
        val count = manage.cunt + 1
        manage.cunt = count
        TimerRuntimeState.setExecutionCount(count)
        TimerSessionPersist.saveIfAllowed()
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        val useZeroTermux = mTimerBean!!.isZeroTermux
        ensureLogWriter()
        manage.writeRunHeader(count)
        TimerRuntimeState.setExecutingScript(true)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.statusMessage = UUtils.getString(R.string.zt_timer_executing_current_script)
        refreshForegroundNotification()
        scriptExecStartedAtMillis = System.currentTimeMillis()

        val finish: () -> Unit = {
            val elapsed = if (scriptExecStartedAtMillis > 0L) {
                System.currentTimeMillis() - scriptExecStartedAtMillis
            } else {
                0L
            }
            TimerDiagLog.i(TAG, "execCommand FINISH #$count elapsedMs=$elapsed zeroTermux=$useZeroTermux")
            scriptExecStartedAtMillis = 0L
            onComplete?.run()
            Unit
        }

        val command = if (useZeroTermux) "shell_ZeroTermux" else "shell_Android"
        LogUtils.e(TAG, "execCommand: LibSu $command #$count")
        TimerDiagLog.i(TAG, "execCommand START #$count cmd=$command")
        manage.shellCommandExec(command, Runnable { finish() })
    }

    private fun startStuckWatchdog() {
        mainHandler.removeCallbacks(stuckWatchdogRunnable)
        mainHandler.postDelayed(stuckWatchdogRunnable, STUCK_WATCHDOG_MS)
    }

    private fun recoverFromStuckScript(reason: String) {
        LogUtils.e(TAG, "watchdog: recover - $reason")
        TimerDiagLog.e(TAG, "watchdog RECOVER reason=$reason")
        TimerDiagLog.logSnapshot(TAG, "watchdog_recover")
        mLibSuManage?.stop()
        TimerRuntimeState.setExecutingScript(false)
        TimerRuntimeState.setWaitingForScript(false)
        TimerRuntimeState.statusMessage = ""
        isLaunchingCommand.set(false)
        pendingRunAfterScript.set(false)
        scriptExecStartedAtMillis = 0L
        scheduleNextExecution()
    }

    private fun checkStuckCountdown() {
        if (!isActive.get()) return
        try {
            TimerDiagLog.maybeHeartbeat(TAG)
            val elapsed = if (scriptExecStartedAtMillis > 0L) {
                System.currentTimeMillis() - scriptExecStartedAtMillis
            } else {
                0L
            }
            val shellRunning = mLibSuManage?.isShellCommandRunning == true
            if (TimerRuntimeState.isExecutingScript() && scriptExecStartedAtMillis > 0L) {
                if (elapsed > SCRIPT_MAX_RUNTIME_MS) {
                    recoverFromStuckScript("script exceeded max runtime elapsed=$elapsed")
                    return
                }
                if (!shellRunning && elapsed > SCRIPT_STUCK_RESET_MS) {
                    recoverFromStuckScript("executing flag set but LibSu not running elapsed=$elapsed")
                    return
                }
            }
            if (isLaunchingCommand.get() && !shellRunning && elapsed > SCRIPT_STUCK_RESET_MS) {
                recoverFromStuckScript("launch flag stuck elapsed=$elapsed")
                return
            }
            if (!TimerRuntimeState.isExecutingScript() &&
                !TimerRuntimeState.isWaitingForScript() &&
                TimerRuntimeState.getNextFireAtMillis() > 0L &&
                TimerRuntimeState.remainingMillis() <= 0L
            ) {
                if (isLaunchingCommand.get() || shellRunning) {
                    recoverFromStuckScript("countdown zero while launch/shell active")
                    return
                }
                LogUtils.e(TAG, "watchdog: stuck at 00:00, rescheduling next run")
                TimerDiagLog.w(TAG, "watchdog: stuck at 00:00 -> scheduleNext")
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
            TimerDiagLog.w(TAG, "endTime ignored: already inactive userInitiated=$userInitiated")
            stopSelf()
            return
        }
        TimerDiagLog.i(TAG, "endTime begin userInitiated=$userInitiated")
        TimerDiagLog.logSnapshot(TAG, "endTime")
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
            TimerDiagLog.i(TAG, "endTime: user stop, cleared persist")
        } else if (TimerSetManage.get().getZTTimerBean().isAlwaysAllowTimer) {
            TimerSessionPersist.saveForBackgroundResume()
            TimerRuntimeState.clearSchedule()
            TimerDiagLog.w(TAG, "endTime: non-user stop, saved persist for resume")
        } else {
            TimerRuntimeState.resetForUserStop()
            TimerDiagLog.i(TAG, "endTime: cleared (alwaysAllow=false)")
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
            TimerDiagLog.w(TAG, "onDestroy while active alwaysAllow=$alwaysAllow")
            endTime(userInitiated = !alwaysAllow)
        } else {
            TimerDiagLog.i(TAG, "onDestroy")
        }
        super.onDestroy()
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement: $msg")
    }
}
