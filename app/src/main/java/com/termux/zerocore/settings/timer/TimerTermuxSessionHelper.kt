package com.termux.zerocore.settings.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import com.termux.zerocore.utils.SingletonCommunicationUtils
import com.zp.z_file.util.LogUtils
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 定时任务在 ZeroTermux 主界面未打开时，后台启动 TermuxService 并创建终端会话，
 * 同时注册无界面通信桥，避免「主程序终端不存在」。
 */
object TimerTermuxSessionHelper : ServiceConnection {

    private const val TAG = "TimerTermuxSessionHelper"
    private const val BIND_TIMEOUT_MS = 12_000L
    private const val SESSION_NAME = "timer-bg"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bootstrapping = AtomicBoolean(false)
    private val pendingCallbacks = CopyOnWriteArrayList<(Boolean) -> Unit>()

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
            writeToSession(command)
        }

        override fun sendTextToTerminalAlt(command: String, isAlt: Boolean) {
            writeToSession(command)
        }

        override fun sendTextToTerminalCtrl(command: String, isCtrl: Boolean) {
            if (!isCtrl) {
                writeToSession(command)
                return
            }
            val text = command.trim()
            if (text.length == 1) {
                val ch = text[0].lowercaseChar()
                val code = when (ch) {
                    in 'a'..'z' -> (ch.code - 'a'.code + 1)
                    else -> ch.code
                }
                writeToSession(code.toChar().toString())
            } else {
                writeToSession(command)
            }
        }

        override fun onTerminalExtraKeyButtonClick(key: String?) {
            key ?: return
            writeToSession(key)
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

    fun releaseIfHeadless(context: Context) {
        mainHandler.post { releaseOnMain(context.applicationContext) }
    }

    fun isSessionReady(): Boolean {
        return SingletonCommunicationUtils.getInstance().hasTerminalListener() ||
            currentSession() != null
    }

    private fun ensureSessionOnMain(context: Context, callback: (Boolean) -> Unit) {
        if (SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            callback(true)
            return
        }
        if (serviceBound && termuxService != null && currentSession() != null) {
            registerHeadlessListenerIfNeeded()
            callback(true)
            return
        }
        if (!isBootstrapReady()) {
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
        appContext = context
        bindTermuxService(context)
        scheduleBindTimeout()
    }

    private fun scheduleBindTimeout() {
        mainHandler.removeCallbacks(bindTimeoutRunnable)
        mainHandler.postDelayed(bindTimeoutRunnable, BIND_TIMEOUT_MS)
    }

    private val bindTimeoutRunnable = Runnable {
        if (!bootstrapping.get()) return@Runnable
        LogUtils.e(TAG, "bind/create session timeout")
        completePending(false)
        releaseOnMain(appContext)
    }

    private fun bindTermuxService(context: Context) {
        val intent = Intent(context, TermuxService::class.java)
        try {
            context.startService(intent)
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

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mainHandler.post {
            val ctx = appContext ?: return@post
            val boundService = TermuxService.fromBinder(service ?: return@post)
            termuxService = boundService
            serviceBound = true
            if (boundService.isTermuxSessionsEmpty) {
                val workingDirectory = TermuxAppSharedProperties.getProperties()
                    ?.defaultWorkingDirectory
                    ?: TermuxConstants.TERMUX_HOME_DIR_PATH
                val created = boundService.createTermuxSession(
                    null,
                    null,
                    null,
                    workingDirectory,
                    false,
                    SESSION_NAME
                )
                if (created == null) {
                    LogUtils.e(TAG, "createTermuxSession returned null")
                    completePending(false)
                    return@post
                }
                val handle = created.terminalSession?.mHandle
                if (!handle.isNullOrEmpty()) {
                    TermuxAppSharedPreferences.build(ctx, false)?.setCurrentSession(handle)
                }
            }
            registerHeadlessListenerIfNeeded()
            completePending(true)
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
        bootstrapping.set(false)
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

    private fun writeToSession(text: String) {
        val session = currentSession() ?: return
        mainHandler.post { session.write(text) }
    }

    private fun transcript(visibleOnly: Boolean): String {
        val session = currentSession() ?: return ""
        return ShellUtils.getTerminalSessionTranscriptText(session, visibleOnly, true).orEmpty()
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
