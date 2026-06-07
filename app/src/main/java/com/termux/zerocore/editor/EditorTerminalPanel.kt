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
import android.widget.RelativeLayout
import com.termux.app.TermuxService
import com.termux.shared.logger.Logger
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import java.io.FileInputStream
import java.util.Properties

class EditorTerminalPanel(
    private val activity: Activity,
    private val panelView: View,
    private val terminalView: TerminalView,
    private val inputView: EditorTerminalInputView,
    private val contentLayout: RelativeLayout,
    private val symbolBar: View,
    private val onBlurEditor: () -> Unit,
    private val onRestoreEditorFocus: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) : ServiceConnection {

    private val context: Context = activity
    private var termuxService: TermuxService? = null
    private var serviceBound = false
    private var visible = false

    private val viewClient = EditorTerminalViewClient(
        activity,
        terminalView,
        inputView,
        { visible },
        onBlurEditor
    )

    fun init(restoredVisible: Boolean, resizeHandle: View) {
        inputView.bindTerminalView(terminalView)
        setPanelHeight(defaultPanelHeightPx())
        setupResizeHandle(resizeHandle)
        terminalView.setTerminalViewClient(viewClient)
        viewClient.onCreate()
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

    private fun ensureHidden() {
        visible = false
        panelView.visibility = View.GONE
        updateContentAnchor()
        onVisibilityChanged(false)
    }

    fun isVisible(): Boolean = visible

    fun toggle() {
        setVisible(!visible)
    }

    fun setVisible(show: Boolean) {
        if (visible == show) return
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
        if (visible) {
            attachCurrentSession()
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
        val params = contentLayout.layoutParams as? RelativeLayout.LayoutParams ?: return
        params.removeRule(RelativeLayout.ABOVE)
        params.addRule(
            RelativeLayout.ABOVE,
            if (visible) panelView.id else symbolBar.id
        )
        contentLayout.layoutParams = params
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
        private const val DEFAULT_PANEL_HEIGHT_DP = 200
        private const val MIN_PANEL_HEIGHT_DP = 120
        private const val MAX_PANEL_HEIGHT_RATIO = 0.7f
    }
}
