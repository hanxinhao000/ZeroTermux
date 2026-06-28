package com.termux.zerocore.editor

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.shared.view.KeyboardUtils
import com.termux.x11.MainActivity
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.settings.ZeroTermuxX11Settings
import com.termux.zerocore.url.FileUrl
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorX11Panel(
    private val activity: Activity,
    private val surfaceContainer: FrameLayout,
    private val setupPanel: View,
    private val setupMessageView: TextView,
    private val setupActionView: TextView,
    private val statusView: TextView,
    private val displayButton: TextView,
    private val connectButton: TextView,
    private val maximizeButton: ImageView,
    private val codeEditor: CodeEditor?,
    private val dockHeightController: DockHeightController,
    private val onWriteTerminal: (String) -> Unit,
    private val onEnsureTerminalVisible: () -> Unit,
    private val onLayoutChanged: () -> Unit,
    private val onTabActiveChanged: (Boolean) -> Unit,
    private val onBarActionsChanged: () -> Unit
) {

    interface DockHeightController {
        fun saveDockedHeight()
        fun restoreDockedHeight()
    }

    private enum class SetupReason {
        INTERNAL_REQUIRED,
        ENV_MISSING
    }

    private var mainActivity: MainActivity? = null
    private var tabActive = false
    private var maximized = false
    private var setupMode = false

    fun init() {
        connectButton.setOnClickListener { connectX11() }
        displayButton.setOnClickListener { exportDisplay() }
        maximizeButton.setOnClickListener { toggleMaximized() }
        setupActionView.setOnClickListener {
            activity.startActivity(Intent(activity, ZeroTermuxX11Settings::class.java))
        }
        updateStatus()
        updateMaximizeButton()
    }

    fun isSetupMode(): Boolean = setupMode

    fun isAvailable(): Boolean = resolveSetupReason() == null

    fun onTabShown() {
        if (tabActive) return
        tabActive = true
        blurEditor()
        onTabActiveChanged(true)
        refreshPanelMode()
        if (setupMode) {
            onLayoutChanged()
            return
        }
        if (!ensureEmbedded()) {
            tabActive = false
            onTabActiveChanged(false)
            restoreEditor()
            UUtils.showMsg(activity.getString(R.string.editor_x11_init_failed))
            return
        }
        onLayoutChanged()
        mainActivity?.init()
        mainActivity?.onResume()
        updateStatus()
        if (!MainActivity.isConnected()) {
            connectX11()
        } else {
            exportDisplay()
        }
    }

    fun onTabHidden() {
        if (!tabActive) return
        tabActive = false
        if (maximized) {
            maximized = false
            updateMaximizeButton()
        }
        onTabActiveChanged(false)
        onLayoutChanged()
        mainActivity?.onPause()
        restoreEditor()
    }

    fun onDockHidden() {
        onTabHidden()
        setupMode = false
    }

    fun isTabActive(): Boolean = tabActive

    fun isMaximized(): Boolean = maximized && !setupMode

    fun restoreFromMaximized(): Boolean {
        if (!maximized || setupMode) return false
        setMaximized(false)
        return true
    }

    fun onResume() {
        if (!tabActive) return
        if (setupMode && resolveSetupReason() == null) {
            refreshPanelMode()
            if (!setupMode) {
                if (!ensureEmbedded()) {
                    showSetupMode(SetupReason.ENV_MISSING)
                    UUtils.showMsg(activity.getString(R.string.editor_x11_init_failed))
                    return
                }
                onLayoutChanged()
                mainActivity?.init()
                mainActivity?.onResume()
                updateStatus()
                if (!MainActivity.isConnected()) {
                    connectX11()
                } else {
                    exportDisplay()
                }
            }
            return
        }
        if (!setupMode) {
            mainActivity?.onResume()
            updateStatus()
        }
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
        if (!tabActive || setupMode) return
        mainActivity?.onWindowFocusChanged(hasFocus)
    }

    fun onHostLayoutChanged() {
        if (!tabActive || maximized || setupMode) return
        surfaceContainer.post {
            mainActivity?.requestLayout()
        }
    }

    private fun refreshPanelMode() {
        val reason = resolveSetupReason()
        if (reason == null) {
            showOperationalMode()
            return
        }
        showSetupMode(reason)
    }

    private fun showSetupMode(reason: SetupReason) {
        setupMode = true
        maximized = false
        setupPanel.visibility = View.VISIBLE
        surfaceContainer.visibility = View.GONE
        setupMessageView.text = activity.getString(
            when (reason) {
                SetupReason.INTERNAL_REQUIRED -> R.string.editor_x11_internal_required
                SetupReason.ENV_MISSING -> R.string.editor_x11_env_missing
            }
        )
        setupActionView.text = activity.getString(R.string.editor_x11_setup_open_settings)
        updateMaximizeButton()
        onBarActionsChanged()
    }

    private fun showOperationalMode() {
        setupMode = false
        setupPanel.visibility = View.GONE
        surfaceContainer.visibility = View.VISIBLE
        updateMaximizeButton()
        onBarActionsChanged()
    }

    private fun resolveSetupReason(): SetupReason? {
        if (!UserSetManage.get().getZTUserBean().isInternalPassage) {
            return SetupReason.INTERNAL_REQUIRED
        }
        if (!File(FileUrl.aislePathAPK).exists()) {
            return SetupReason.ENV_MISSING
        }
        return null
    }

    private fun toggleMaximized() {
        if (setupMode) return
        setMaximized(!maximized)
    }

    private fun setMaximized(maximize: Boolean) {
        if (setupMode || maximized == maximize) return
        maximized = maximize
        if (maximize) {
            dockHeightController.saveDockedHeight()
        } else {
            dockHeightController.restoreDockedHeight()
        }
        updateMaximizeButton()
        onLayoutChanged()
        surfaceContainer.post {
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
        surfaceContainer.postDelayed({
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
        if (setupMode) return
        val connected = MainActivity.isConnected()
        statusView.text = activity.getString(
            if (connected) R.string.editor_x11_status_connected else R.string.editor_x11_status_disconnected
        )
        statusView.setTextColor(
            if (connected) 0xFF4CAF50.toInt() else 0xFFFF8A80.toInt()
        )
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
    }
}
