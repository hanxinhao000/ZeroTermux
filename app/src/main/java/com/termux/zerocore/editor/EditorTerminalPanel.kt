package com.termux.zerocore.editor

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import com.termux.app.TermuxService
import com.termux.R
import com.termux.shared.logger.Logger
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.termux.zerocore.ai.agent.ZtTerminalAiSnapshot
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class EditorTerminalPanel(
    private val activity: Activity,
    private val panelView: View,
    private val terminalView: TerminalView,
    private val inputView: EditorTerminalInputView,
    private val extraKeysView: ExtraKeysView,
    private val onBlurEditor: () -> Unit,
    private val onRestoreEditorFocus: () -> Unit,
    private val onLayoutChanged: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) : ServiceConnection {

    private val context: Context = activity
    private var termuxService: TermuxService? = null
    private var serviceBound = false
    private var visible = false
    private var pendingCdDirectory: File? = null
    private var terminalExtraKeys: EditorTerminalExtraKeys? = null
    private val pendingCommands = ArrayDeque<String>()
    private var drainingCommands = false
    /** 已通过命令切换到的目录，避免 AI 每轮快照重复 cd。 */
    private var lastEnsuredCdPath: String? = null

    private val viewClient = EditorTerminalViewClient(
        activity,
        terminalView,
        inputView,
        extraKeysView,
        { visible },
        onBlurEditor
    )

    fun init(restoredVisible: Boolean, resizeHandle: View?) {
        inputView.bindTerminalView(terminalView)
        inputView.bindViewClient(viewClient)
        if (resizeHandle != null) {
            setPanelHeight(defaultPanelHeightPx())
            setupResizeHandle(resizeHandle)
        }
        terminalView.setTerminalViewClient(viewClient)
        viewClient.onCreate()
        setupExtraKeys()
        val prefs = getTermuxPrefs()
        terminalView.setTextSize(prefs?.fontSize ?: DEFAULT_TERMINAL_FONT_SIZE)
        terminalView.setKeepScreenOn(prefs?.shouldKeepScreenOn() == true)
        applyFontAndColors()
        if (restoredVisible) {
            setVisible(true)
        } else {
            ensureHidden()
        }
    }

    fun sendCtrlC() {
        android.util.Log.d(LOG_TAG, "sendCtrlC")
        ensureServiceBound()
        terminalView.post {
            attachCurrentSession()
            val session = terminalView.currentSession
            if (session != null) {
                session.write("\u0003")
                terminalView.onScreenUpdated()
            } else {
                terminalView.sendTextToTerminalCtrl("c", true)
                terminalView.onScreenUpdated()
            }
        }
    }

    fun prepareBackgroundSession(workingDirectory: File? = null) {
        ensureServiceBound()
        terminalView.post {
            attachCurrentSession()
            workingDirectory?.let { enqueueCd(it) }
        }
    }

    /** Serializes shell input so concurrent editor commands do not interleave. */
    fun enqueueTerminalCommand(command: String) {
        synchronized(pendingCommands) {
            pendingCommands.addLast(command)
        }
        ensureServiceBound()
        terminalView.post { drainCommandQueue() }
    }

    fun writeCommandHidden(command: String) {
        if (!visible) {
            ensureServiceBound()
        }
        enqueueTerminalCommand(command)
    }

    private fun drainCommandQueue() {
        if (drainingCommands) return
        val command = synchronized(pendingCommands) {
            pendingCommands.removeFirstOrNull()
        } ?: return
        drainingCommands = true
        attachCurrentSession()
        val session = terminalView.currentSession
        if (session == null) {
            synchronized(pendingCommands) {
                pendingCommands.addFirst(command)
            }
            drainingCommands = false
            terminalView.postDelayed({ drainCommandQueue() }, COMMAND_RETRY_MS)
            return
        }
        session.write(command)
        terminalView.onScreenUpdated()
        drainingCommands = false
        val hasMore = synchronized(pendingCommands) { pendingCommands.isNotEmpty() }
        if (hasMore) {
            terminalView.postDelayed({ drainCommandQueue() }, COMMAND_GAP_MS)
        }
    }

    private fun enqueueCd(directory: File) {
        ensureCd(directory)
    }

    /**
     * 绑定终端会话供 AI 使用：不强制展开终端面板，且同一目录只 cd 一次。
     */
    fun ensureSessionForAi(workingDirectory: File? = null) {
        ensureServiceBound()
        attachCurrentSession()
        workingDirectory?.let { ensureCd(it) }
    }

    fun ensureForAi(workingDirectory: File? = null) {
        if (!visible) {
            setVisible(true, workingDirectory)
            return
        }
        ensureSessionForAi(workingDirectory)
    }

    fun getRecentTerminalText(): String {
        return try {
            if (terminalView.currentSession == null) {
                return ""
            }
            terminalView.getVisibleTerminalText()
        } catch (e: Exception) {
            Logger.logStackTraceWithMessage(LOG_TAG, "getRecentTerminalText failed", e)
            ""
        }
    }

    fun captureAiSnapshot(maxChars: Int): String {
        val session = terminalView.currentSession
        if (session == null) {
            return "(terminal session not attached)"
        }
        val visible = terminalView.getVisibleTerminalText().trim()
        val full = try {
            terminalView.getText555()?.trim().orEmpty()
        } catch (e: Exception) {
            visible
        }
        return ZtTerminalAiSnapshot.format(visible, full, maxChars)
    }

    fun sendTextToTerminal(text: String) {
        attachCurrentSession()
        val session = terminalView.currentSession
        if (session != null) {
            session.write(text)
            terminalView.onScreenUpdated()
            return
        }
        terminalView.sendTextToTerminal(text)
        terminalView.onScreenUpdated()
    }

    fun sendTerminalKey(key: String) {
        if (terminalView.currentSession == null) {
            attachCurrentSession()
        }
        val keys = terminalExtraKeys ?: return
        when (key.lowercase()) {
            "enter" -> keys.dispatchKey("ENTER")
            "tab" -> keys.dispatchKey("TAB")
            "escape" -> keys.dispatchKey("ESC")
            "backspace" -> keys.dispatchKey("BKSP")
            "up" -> keys.dispatchKey("UP")
            "down" -> keys.dispatchKey("DOWN")
            "left" -> keys.dispatchKey("LEFT")
            "right" -> keys.dispatchKey("RIGHT")
            "ctrl_c" -> terminalView.sendTextToTerminalCtrl("c", true)
            "ctrl_d" -> terminalView.sendTextToTerminalCtrl("d", true)
            "ctrl_l" -> terminalView.sendTextToTerminalCtrl("l", true)
            "ctrl_z" -> terminalView.sendTextToTerminalCtrl("z", true)
            else -> throw IllegalArgumentException("unsupported key: $key")
        }
        terminalView.onScreenUpdated()
    }

    fun writeToSession(command: String) {
        if (!visible) {
            setVisible(true, null)
        }
        terminalView.post {
            if (!visible) return@post
            attachCurrentSession()
            terminalView.postDelayed({
                if (!visible) return@postDelayed
                val session = terminalView.currentSession ?: return@postDelayed
                session.write(command)
                terminalView.onScreenUpdated()
            }, 120)
        }
    }

    private fun ensureHidden() {
        visible = false
        panelView.visibility = View.GONE
        updateContentAnchor()
        onVisibilityChanged(false)
    }

    fun isVisible(): Boolean = visible

    fun isSessionActive(): Boolean = visible

    fun setContentVisible(show: Boolean) {
        panelView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun toggle(workingDirectory: File? = null) {
        if (visible) {
            setVisible(false)
        } else {
            setVisible(true, workingDirectory)
        }
    }

    fun showAtDirectory(directory: File) {
        if (!visible) {
            setVisible(true, directory)
        } else {
            sendCdCommand(directory)
        }
    }

    fun setVisible(show: Boolean) {
        setVisible(show, null)
    }

    fun setVisible(show: Boolean, workingDirectory: File?) {
        if (visible == show && !(show && workingDirectory != null)) {
            if (show && workingDirectory != null) {
                sendCdCommand(workingDirectory)
            }
            return
        }
        visible = show
        if (show) {
            onVisibilityChanged(true)
        }
        panelView.visibility = if (show) View.VISIBLE else View.GONE
        updateContentAnchor()
        if (show) {
            ensureServiceBound()
            registerRelay()
            attachCurrentSession()
            workingDirectory?.let { scheduleCdCommand(it) }
        } else {
            unregisterRelay()
            onRestoreEditorFocus()
            onVisibilityChanged(false)
        }
    }

    fun onResume() {
        if (visible) {
            attachCurrentSession()
        }
    }

    /** 侧栏展开/收起导致可用宽度变化时，重新计算终端行列并刷新画面。 */
    fun onHostLayoutChanged() {
        if (!visible) return
        terminalView.post {
            terminalView.updateSize()
            terminalView.onScreenUpdated()
        }
    }

    fun destroy() {
        unregisterRelay()
        if (serviceBound) {
            try {
                context.unbindService(this)
            } catch (e: Exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to unbind TermuxService", e)
            }
            serviceBound = false
        }
        termuxService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        termuxService = TermuxService.fromBinder(service ?: return)
        attachCurrentSession()
        if (visible) {
            pendingCdDirectory?.let { scheduleCdCommand(it) }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        termuxService = null
    }

    private fun ensureServiceBound() {
        if (serviceBound) return
        val intent = Intent(context, TermuxService::class.java)
        context.startService(intent)
        serviceBound = context.bindService(intent, this, 0)
    }

    private fun registerRelay() {
        EditorTerminalSessionRelay.setListener { changedSession ->
            if (!visible) return@setListener
            if (terminalView.currentSession != changedSession) return@setListener
            terminalView.post { terminalView.onScreenUpdated() }
        }
    }

    private fun unregisterRelay() {
        EditorTerminalSessionRelay.setListener(null)
    }

    private fun attachCurrentSession() {
        val service = termuxService ?: return
        val session = resolveCurrentSession(service) ?: return
        if (terminalView.attachSession(session)) {
            terminalView.onScreenUpdated()
        }
        applyFontAndColors()
    }

    private fun scheduleCdCommand(directory: File) {
        pendingCdDirectory = directory
        terminalView.post {
            if (!visible) return@post
            attachCurrentSession()
            terminalView.postDelayed({
                if (!visible) return@postDelayed
                sendCdCommand(directory)
                pendingCdDirectory = null
            }, 120)
        }
    }

    private fun sendCdCommand(directory: File) {
        ensureCd(directory)
    }

    private fun ensureCd(directory: File) {
        if (!directory.isDirectory) return
        val path = try {
            directory.canonicalPath
        } catch (_: Exception) {
            directory.absolutePath
        }
        if (path == lastEnsuredCdPath) return
        lastEnsuredCdPath = path
        enqueueTerminalCommand("cd ${shellQuote(path)}\n")
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun resolveCurrentSession(service: TermuxService): TerminalSession? {
        val handle = getTermuxPrefs()?.currentSession
        if (!handle.isNullOrEmpty()) {
            service.getTerminalSessionForHandle(handle)?.let { return it }
        }
        return service.lastTermuxSession?.terminalSession
    }

    private fun getTermuxPrefs(): TermuxAppSharedPreferences? {
        return TermuxAppSharedPreferences.build(context, false)
    }

    private fun setupResizeHandle(handle: View) {
        var startY = 0f
        var startHeight = 0
        handle.setOnTouchListener { _, event ->
            if (!visible) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startHeight = panelView.layoutParams.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.rawY
                    setPanelHeight(
                        (startHeight + deltaY).toInt().coerceIn(minPanelHeightPx(), maxPanelHeightPx())
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setPanelHeight(heightPx: Int) {
        val params = panelView.layoutParams ?: return
        if (params.height == heightPx) return
        params.height = heightPx
        panelView.layoutParams = params
        onHostLayoutChanged()
    }

    private fun defaultPanelHeightPx(): Int = dp(DEFAULT_PANEL_HEIGHT_DP)

    private fun minPanelHeightPx(): Int = dp(MIN_PANEL_HEIGHT_DP)

    private fun maxPanelHeightPx(): Int {
        return (activity.resources.displayMetrics.heightPixels * MAX_PANEL_HEIGHT_RATIO).toInt()
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun updateContentAnchor() {
        onLayoutChanged()
    }

    private fun setupExtraKeys() {
        val keys = EditorTerminalExtraKeys(activity, terminalView, extraKeysView, viewClient)
        terminalExtraKeys = keys
        extraKeysView.setExtraKeysViewClient(keys)
        val properties = TermuxAppSharedProperties.getProperties()
        extraKeysView.setButtonTextAllCaps(properties?.shouldExtraKeysTextBeAllCaps() == true)
        reloadExtraKeys()
    }

    private fun reloadExtraKeys() {
        val info = terminalExtraKeys?.getExtraKeysInfo() ?: run {
            extraKeysView.visibility = View.GONE
            return
        }
        val rowCount = info.matrix.size.coerceAtLeast(1)
        val scale = TermuxAppSharedProperties.getProperties()?.terminalToolbarHeightScaleFactor ?: 1f
        val rowHeightPx = dp(DEFAULT_EXTRA_KEYS_ROW_HEIGHT_DP)
        val totalHeight = (rowHeightPx * rowCount * scale).toInt().coerceAtLeast(rowHeightPx)
        val params = extraKeysView.layoutParams
        if (params != null && params.height != totalHeight) {
            params.height = totalHeight
            extraKeysView.layoutParams = params
        }
        extraKeysView.visibility = View.VISIBLE
        extraKeysView.reload(info, rowHeightPx.toFloat())
    }

    private fun applyFontAndColors() {
        try {
            val colorsFile = TermuxConstants.TERMUX_COLOR_PROPERTIES_FILE
            val fontFile = TermuxConstants.TERMUX_FONT_FILE
            val props = Properties()
            if (colorsFile.isFile) {
                FileInputStream(colorsFile).use { props.load(it) }
            }
            TerminalColors.COLOR_SCHEME.updateWith(props)
            terminalView.currentSession?.emulator?.mColors?.reset()
            val typeface = if (fontFile.exists() && fontFile.length() > 0) {
                Typeface.createFromFile(fontFile)
            } else {
                Typeface.MONOSPACE
            }
            terminalView.setTypeface(typeface)
            terminalView.currentSession?.emulator?.mColors?.mCurrentColors
                ?.get(TextStyle.COLOR_INDEX_BACKGROUND)
                ?.let { panelView.setBackgroundColor(it) }
        } catch (e: Exception) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Error applying terminal font/colors", e)
        }
    }

    companion object {
        private const val LOG_TAG = "EditorTerminalPanel"
        private const val DEFAULT_TERMINAL_FONT_SIZE = 14
        private const val DEFAULT_PANEL_HEIGHT_DP = 240
        private const val MIN_PANEL_HEIGHT_DP = 120
        private const val MAX_PANEL_HEIGHT_RATIO = 0.7f
        private const val DEFAULT_EXTRA_KEYS_ROW_HEIGHT_DP = 38
        private const val COMMAND_GAP_MS = 180L
        private const val COMMAND_RETRY_MS = 300L
    }
}
