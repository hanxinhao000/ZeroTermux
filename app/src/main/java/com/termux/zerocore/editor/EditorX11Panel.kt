package com.termux.zerocore.editor

import android.app.Activity
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.shared.view.KeyboardUtils
import com.termux.x11.MainActivity
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorX11Panel(
    private val activity: Activity,
    private val panelView: View,
    private val surfaceContainer: FrameLayout,
    private val statusView: TextView,
    private val maximizeButton: ImageView,
    private val codeEditor: CodeEditor?,
    private val onWriteTerminal: (String) -> Unit,
    private val onEnsureTerminalVisible: () -> Unit,
    private val onLayoutChanged: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {

    private var mainActivity: MainActivity? = null
    private var visible = false
    private var maximized = false
    private var dockedHeightPx = 0

    fun init(resizeHandle: View) {
        panelView.findViewById<View>(R.id.editor_x11_hide)?.setOnClickListener {
            setVisible(false)
        }
        panelView.findViewById<View>(R.id.editor_x11_connect)?.setOnClickListener {
            connectX11()
        }
        panelView.findViewById<View>(R.id.editor_x11_display)?.setOnClickListener {
            exportDisplay()
        }
        maximizeButton.setOnClickListener {
            toggleMaximized()
        }
        dockedHeightPx = defaultPanelHeightPx()
        setPanelHeight(dockedHeightPx)
        setupResizeHandle(resizeHandle)
        updateStatus()
        updateMaximizeButton()
    }

    fun isAvailable(): Boolean {
        return UserSetManage.get().getZTUserBean().isInternalPassage &&
            File(FileUrl.aislePathAPK).exists()
    }

    fun toggle() {
        if (visible) {
            setVisible(false)
        } else {
            setVisible(true)
        }
    }

    fun setVisible(show: Boolean) {
        if (visible == show) return
        if (show) {
            if (!UserSetManage.get().getZTUserBean().isInternalPassage) {
                UUtils.showMsg(activity.getString(R.string.editor_x11_internal_required))
                return
            }
            if (!File(FileUrl.aislePathAPK).exists()) {
                UUtils.showMsg(activity.getString(R.string.editor_x11_env_missing))
                return
            }
        }
        visible = show
        if (show) {
            blurEditor()
            panelView.visibility = View.VISIBLE
            if (!ensureEmbedded()) {
                panelView.visibility = View.GONE
                visible = false
                restoreEditor()
                UUtils.showMsg(activity.getString(R.string.editor_x11_init_failed))
                return
            }
            onVisibilityChanged(true)
            onLayoutChanged()
            mainActivity?.init()
            mainActivity?.onResume()
            updateStatus()
            if (!MainActivity.isConnected()) {
                connectX11()
            } else {
                exportDisplay()
            }
        } else {
            if (maximized) {
                maximized = false
                updateMaximizeButton()
            }
            panelView.visibility = View.GONE
            onVisibilityChanged(false)
            onLayoutChanged()
            mainActivity?.onPause()
            restoreEditor()
        }
    }

    fun isVisible(): Boolean = visible

    fun isMaximized(): Boolean = maximized

    fun restoreFromMaximized(): Boolean {
        if (!maximized) return false
        setMaximized(false)
        return true
    }

    fun onResume() {
        if (!visible) return
        mainActivity?.onResume()
        updateStatus()
    }

    fun onPause() {
        mainActivity?.onPause()
    }

    fun onDestroy() {
        mainActivity?.onDestroy(activity)
        mainActivity = null
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        mainActivity?.onConfigurationChanged(newConfig)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!visible) return
        mainActivity?.onWindowFocusChanged(hasFocus)
    }

    fun onHostLayoutChanged() {
        if (!visible || maximized) return
        panelView.post {
            mainActivity?.requestLayout()
        }
    }

    private fun toggleMaximized() {
        setMaximized(!maximized)
    }

    private fun setMaximized(maximize: Boolean) {
        if (maximized == maximize) return
        maximized = maximize
        panelView.findViewById<View>(R.id.editor_x11_resize_handle)?.visibility =
            if (maximize) View.GONE else View.VISIBLE
        if (maximize) {
            dockedHeightPx = panelView.layoutParams.height.coerceAtLeast(minPanelHeightPx())
        } else {
            setPanelHeight(dockedHeightPx.coerceAtLeast(minPanelHeightPx()))
        }
        updateMaximizeButton()
        onLayoutChanged()
        panelView.post {
            mainActivity?.requestLayout()
            mainActivity?.onResume()
        }
    }

    private fun updateMaximizeButton() {
        if (maximized) {
            maximizeButton.setImageResource(R.drawable.ic_editor_restore)
            maximizeButton.contentDescription = activity.getString(R.string.editor_x11_restore)
        } else {
            maximizeButton.setImageResource(R.drawable.ic_editor_maximize)
            maximizeButton.contentDescription = activity.getString(R.string.editor_x11_maximize)
        }
    }

    private fun connectX11() {
        onEnsureTerminalVisible()
        onWriteTerminal(
            buildString {
                append("if [ -x ${shellQuote(FileUrl.aislePathSh)} ]; then\n")
                append("  ${shellQuote(FileUrl.aislePathSh)} >/dev/null 2>&1 &\n")
                append("else\n")
                append("  echo \"termux-x11 未安装，请先在侧栏安装 X11 环境\"\n")
                append("fi\n")
            }
        )
        panelView.postDelayed({
            exportDisplay()
            updateStatus()
            mainActivity?.onResume()
        }, 800)
    }

    private fun exportDisplay() {
        onEnsureTerminalVisible()
        onWriteTerminal("export DISPLAY=:0\n")
        UUtils.showMsg(activity.getString(R.string.editor_x11_display_set))
    }

    private fun ensureEmbedded(): Boolean {
        if (mainActivity != null) return true
        return try {
            val view = MainActivity(activity)
            surfaceContainer.removeAllViews()
            surfaceContainer.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            mainActivity = view
            true
        } catch (e: Throwable) {
            android.util.Log.e(LOG_TAG, "Failed to embed X11 MainActivity", e)
            false
        }
    }

    private fun updateStatus() {
        val connected = MainActivity.isConnected()
        statusView.text = activity.getString(
            if (connected) R.string.editor_x11_status_connected else R.string.editor_x11_status_disconnected
        )
        statusView.setTextColor(
            if (connected) 0xFF4CAF50.toInt() else 0xFFFF8A80.toInt()
        )
    }

    private fun setupResizeHandle(handle: View) {
        var startY = 0f
        var startHeight = 0
        handle.setOnTouchListener { _, event ->
            if (!visible || maximized) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startHeight = panelView.layoutParams.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.rawY
                    val height = (startHeight + deltaY).toInt()
                        .coerceIn(minPanelHeightPx(), maxPanelHeightPx())
                    setPanelHeight(height)
                    dockedHeightPx = height
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
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    private fun blurEditor() {
        codeEditor?.let { editor ->
            editor.setSoftKeyboardEnabled(false)
            KeyboardUtils.hideSoftKeyboard(activity, editor)
            editor.clearFocus()
        }
        activity.currentFocus?.clearFocus()
    }

    private fun restoreEditor() {
        codeEditor?.setSoftKeyboardEnabled(true)
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    companion object {
        private const val LOG_TAG = "EditorX11Panel"
        private const val DEFAULT_PANEL_HEIGHT_DP = 280
        private const val MIN_PANEL_HEIGHT_DP = 120
        private const val MAX_PANEL_HEIGHT_RATIO = 0.7f
    }
}
