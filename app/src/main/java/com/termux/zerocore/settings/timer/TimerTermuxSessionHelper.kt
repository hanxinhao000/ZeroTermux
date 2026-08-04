package com.termux.zerocore.settings.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.xh_lib.utils.UUtils
import com.termux.app.TermuxActivity
import com.termux.app.TermuxService
import com.termux.shared.shell.ShellUtils
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties
import com.termux.terminal.TerminalSession
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.SingletonCommunicationUtils
import com.zp.z_file.util.LogUtils
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 定时任务在 ZeroTermux 主界面未打开时，后台启动 TermuxService 并创建终端会话，
 * 同时注册无界面通信桥，避免「主程序终端不存在」。
 */
object TimerTermuxSessionHelper : ServiceConnection {

    private const val TAG = "TimerTermuxSessionHelper"
    private const val BIND_TIMEOUT_MS = 15_000L
    private const val SCRIPT_POLL_MS = 1_000L
    private const val SCRIPT_START_TIMEOUT_MS = 20_000L
    private const val SCRIPT_MAX_WAIT_MS = 30 * 60_000L
    private const val SHELL_READY_POLL_MS = 500L
    private const val SHELL_READY_MAX_MS = 12_000L
    private const val START_MARKER = "[ZT Timer] start"
    private const val DONE_MARKER = "===ZT_TIMER_SCRIPT_DONE==="

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bootstrapping = AtomicBoolean(false)
    private val pendingCallbacks = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private val scriptRunning = AtomicBoolean(false)
    private val bindAttempts = AtomicInteger(0)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var termuxService: TermuxService? = null

    @Volatile
    private var serviceBound = false

    @Volatile
    private var registeredHeadlessListener = false

    private val headlessListener = object : SingletonCommunicationUtils.SingletonCommunicationListener {
        override fun sendTextToTerminal(command: String) {
            writeToTerminal(command)
        }

        override fun sendTextToTerminalAlt(command: String, isAlt: Boolean) {
            writeToTerminal(command)
        }

        override fun sendTextToTerminalCtrl(command: String, isCtrl: Boolean) {
            if (!isCtrl) {
                writeToTerminal(command)
                return
            }
            val text = command.trim()
            if (text.length == 1) {
                val ch = text[0].lowercaseChar()
                val code = when (ch) {
                    in 'a'..'z' -> (ch.code - 'a'.code + 1)
                    else -> ch.code
                }
                writeToTerminal(code.toChar().toString())
            } else {
                writeToTerminal(command)
            }
        }

        override fun onTerminalExtraKeyButtonClick(key: String?) {
            key ?: return
            writeToTerminal(key)
        }

        override fun getTextToTerminal(): String {
            return transcript(false)
        }

        override fun getVisibleTerminalText(): String {
            return transcript(true)
        }
    }

    fun ensureSession(context: Context, callback: (Boolean) -> Unit) {
        mainHandler.post { ensureSessionOnMain(context.applicationContext, callback) }
    }

    fun isScriptRunningInSession(): Boolean = scriptRunning.get()

    /** 进程恢复或脚本卡死时强制清理会话内执行状态。 */
    fun forceResetScriptState() {
        scriptRunning.set(false)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mainHandler.removeCallbacks(scriptPollRunnable)
        } else {
            mainHandler.post { mainHandler.removeCallbacks(scriptPollRunnable) }
        }
    }

    /**
     * 在已创建的 Termux 会话中执行定时脚本，结束后回调。
     * @param onComplete true=脚本正常结束；false=未启动或超时，调用方应回退 LibSu。
     * @return false 表示会话不可用，调用方应直接回退到 LibSu。
     */
    fun runTimerScriptInSession(context: Context, onComplete: (Boolean) -> Unit): Boolean {
        if (!canDeliverToTerminal()) {
            return false
        }
        if (!scriptRunning.compareAndSet(false, true)) {
            return false
        }
        appContext = context.applicationContext
        val scriptPath = File(FileUrl.timerTermuxFile).absolutePath
        val logPath = TimerExecutionLog.logFilePath(true)
        val marker = "$DONE_MARKER${System.currentTimeMillis()}"
        waitForShellReady { ready ->
            if (!ready) {
                LogUtils.e(TAG, "shell not ready, abort session run")
                TimerDiagLog.e(TAG, "runTimerScript abort: shell not ready")
                finishScriptRun(false, onComplete)
                return@waitForShellReady
            }
            val baseline = transcript(false).length
            val command =
                "echo '$START_MARKER'; bash \"$scriptPath\" 2>&1 | tee -a \"$logPath\"; echo $marker\n"
            if (!writeToTerminal(command)) {
                LogUtils.e(TAG, "writeToTerminal failed, abort session run")
                TimerDiagLog.e(TAG, "runTimerScript abort: writeToTerminal failed")
                finishScriptRun(false, onComplete)
                return@waitForShellReady
            }
            LogUtils.e(TAG, "timer script dispatched to terminal")
            TimerDiagLog.i(TAG, "runTimerScript dispatched path=$scriptPath")
            pollScriptDone(marker, baseline, 0L, onComplete)
        }
        return true
    }

    fun releaseIfHeadless(context: Context) {
        mainHandler.post { releaseOnMain(context.applicationContext) }
    }

    fun isSessionReady(): Boolean {
        return currentSession() != null ||
            SingletonCommunicationUtils.getInstance().hasTerminalListener()
    }

    /** 仅后台定时会话可写；不占用用户当前主终端。 */
    private fun canDeliverToTerminal(): Boolean = currentSession() != null

    private fun ensureSessionOnMain(context: Context, callback: (Boolean) -> Unit) {
        if (currentSession() != null) {
            registerHeadlessListenerIfNeeded()
            acquireWakeLock(context)
            callback(true)
            return
        }
        if (SingletonCommunicationUtils.getInstance().hasTerminalListener() &&
            serviceBound &&
            termuxService != null
        ) {
            createSessionIfNeeded(termuxService!!)
            if (currentSession() != null) {
                callback(true)
                return
            }
        }
        if (!isBootstrapReady()) {
            LogUtils.e(TAG, "bootstrap not ready, launching main activity")
            launchMainActivity(context)
            callback(false)
            return
        }
        pendingCallbacks.add(callback)
        if (bootstrapping.get()) {
            scheduleBindTimeout()
            return
        }
        bootstrapping.set(true)
        bindAttempts.set(0)
        appContext = context
        startAndBindTermuxService(context)
        scheduleBindTimeout()
    }

    private fun scheduleBindTimeout() {
        mainHandler.removeCallbacks(bindTimeoutRunnable)
        mainHandler.postDelayed(bindTimeoutRunnable, BIND_TIMEOUT_MS)
    }

    private val bindTimeoutRunnable = Runnable {
        if (!bootstrapping.get()) return@Runnable
        LogUtils.e(TAG, "bind/create session timeout")
        if (bindAttempts.get() < 2) {
            val ctx = appContext
            if (ctx != null) {
                bindAttempts.incrementAndGet()
                startAndBindTermuxService(ctx)
                scheduleBindTimeout()
                return@Runnable
            }
        }
        completePending(currentSession() != null)
        if (currentSession() == null) {
            releaseOnMain(appContext)
        }
    }

    private fun startAndBindTermuxService(context: Context) {
        try {
            acquireWakeLock(context)
            val intent = Intent(context, TermuxService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            if (!serviceBound) {
                serviceBound = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
            }
            if (!serviceBound) {
                LogUtils.e(TAG, "bindService failed")
                completePending(false)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "start/bind TermuxService failed: $e")
            completePending(false)
        }
    }

    private fun acquireWakeLock(context: Context) {
        try {
            val wake = Intent(context, TermuxService::class.java).apply {
                action = TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_WAKE_LOCK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(wake)
            } else {
                context.startService(wake)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "acquire wake lock failed: $e")
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mainHandler.post {
            val boundService = TermuxService.fromBinder(service ?: return@post)
            termuxService = boundService
            serviceBound = true
            createSessionIfNeeded(boundService)
            registerHeadlessListenerIfNeeded()
            val ok = currentSession() != null
            if (!ok) {
                LogUtils.e(TAG, "session still null after create")
            }
            completePending(ok)
        }
    }

    private fun createSessionIfNeeded(boundService: TermuxService) {
        if (!boundService.isTermuxSessionsEmpty) {
            return
        }
        val workingDirectory = TermuxAppSharedProperties.getProperties()
            ?.defaultWorkingDirectory
            ?: TermuxConstants.TERMUX_HOME_DIR_PATH
        val created = boundService.createTermuxSession(
            null,
            null,
            null,
            workingDirectory,
            false,
            null
        )
        if (created == null) {
            LogUtils.e(TAG, "createTermuxSession returned null")
        } else {
            LogUtils.e(TAG, "created background terminal session")
            // 轻微提示，方便用户在会话里看到定时任务已接管
            mainHandler.postDelayed({
                created.terminalSession?.write(
                    "echo '[ZT Timer] background session ready'\n"
                )
            }, 400)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mainHandler.post {
            termuxService = null
            serviceBound = false
            unregisterHeadlessListenerIfNeeded()
        }
    }

    private fun registerHeadlessListenerIfNeeded() {
        if (registeredHeadlessListener) return
        if (SingletonCommunicationUtils.getInstance().hasTerminalListener()) return
        SingletonCommunicationUtils.getInstance()
            .setSingletonCommunicationListener(headlessListener)
        registeredHeadlessListener = true
        LogUtils.e(TAG, "registered headless terminal listener")
    }

    private fun unregisterHeadlessListenerIfNeeded() {
        if (!registeredHeadlessListener) return
        if (SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            SingletonCommunicationUtils.getInstance().setSingletonCommunicationListener(null)
        }
        registeredHeadlessListener = false
    }

    private fun releaseOnMain(context: Context?) {
        mainHandler.removeCallbacks(bindTimeoutRunnable)
        mainHandler.removeCallbacks(scriptPollRunnable)
        bootstrapping.set(false)
        scriptRunning.set(false)
        pendingCallbacks.clear()
        unregisterHeadlessListenerIfNeeded()
        if (serviceBound && context != null) {
            try {
                context.unbindService(this)
            } catch (_: Exception) {
            }
        }
        serviceBound = false
        termuxService = null
        appContext = null
    }

    private fun completePending(success: Boolean) {
        mainHandler.removeCallbacks(bindTimeoutRunnable)
        bootstrapping.set(false)
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it(success) }
    }

    private fun currentSession(): TerminalSession? {
        val service = termuxService ?: return null
        val context = appContext ?: UUtils.getContext()
        val handle = TermuxAppSharedPreferences.build(context, false)?.currentSession
        if (!handle.isNullOrEmpty()) {
            service.getTerminalSessionForHandle(handle)?.let { return it }
        }
        return service.lastTermuxSession?.terminalSession
    }

    private fun writeToTerminal(text: String): Boolean {
        val session = currentSession() ?: return false
        val deliver = {
            try {
                session.write(text)
            } catch (e: Exception) {
                LogUtils.e(TAG, "session.write failed: $e")
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            deliver()
        } else {
            mainHandler.post(deliver)
        }
        return true
    }

    private fun transcript(visibleOnly: Boolean): String {
        val session = currentSession() ?: return ""
        return ShellUtils.getTerminalSessionTranscriptText(session, visibleOnly, true).orEmpty()
    }

    private var scriptPollRunnable: Runnable = Runnable { }

    private fun waitForShellReady(onReady: (Boolean) -> Unit) {
        waitForShellReadyInner(0L, onReady)
    }

    private fun waitForShellReadyInner(elapsedMs: Long, onReady: (Boolean) -> Unit) {
        val tail = transcript(true).trimEnd()
        val ready = tail.endsWith("$") || tail.endsWith("%") ||
            tail.contains("~ $") || tail.contains(":$ ") || tail.contains("# ")
        if (ready) {
            onReady(true)
            return
        }
        if (elapsedMs >= SHELL_READY_MAX_MS) {
            onReady(canDeliverToTerminal())
            return
        }
        mainHandler.postDelayed({
            waitForShellReadyInner(elapsedMs + SHELL_READY_POLL_MS, onReady)
        }, SHELL_READY_POLL_MS)
    }

    private fun finishScriptRun(success: Boolean, onComplete: (Boolean) -> Unit) {
        scriptRunning.set(false)
        onComplete(success)
    }

    private fun pollScriptDone(
        marker: String,
        baselineLen: Int,
        elapsedMs: Long,
        onComplete: (Boolean) -> Unit
    ) {
        scriptPollRunnable = Runnable {
            val text = transcript(false)
            val newPart = if (text.length > baselineLen) text.substring(baselineLen) else text
            val started = newPart.contains(START_MARKER)
            val done = newPart.contains(marker)
            if (done) {
                finishScriptRun(true, onComplete)
                return@Runnable
            }
            if (!started && elapsedMs >= SCRIPT_START_TIMEOUT_MS) {
                LogUtils.e(TAG, "script start timeout (${SCRIPT_START_TIMEOUT_MS}ms)")
                finishScriptRun(false, onComplete)
                return@Runnable
            }
            if (elapsedMs >= SCRIPT_MAX_WAIT_MS) {
                LogUtils.e(TAG, "script max wait timeout")
                finishScriptRun(false, onComplete)
                return@Runnable
            }
            if (!canDeliverToTerminal()) {
                LogUtils.e(TAG, "terminal delivery lost while script running")
                finishScriptRun(false, onComplete)
                return@Runnable
            }
            pollScriptDone(marker, baselineLen, elapsedMs + SCRIPT_POLL_MS, onComplete)
        }
        mainHandler.postDelayed(scriptPollRunnable, SCRIPT_POLL_MS)
    }

    private fun isBootstrapReady(): Boolean {
        val prefix = File(TermuxConstants.TERMUX_PREFIX_DIR_PATH)
        return prefix.isDirectory && !prefix.list().isNullOrEmpty()
    }

    private fun launchMainActivity(context: Context) {
        val intent = Intent(context, TermuxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtils.e(TAG, "launch TermuxActivity failed: $e")
        }
    }
}
