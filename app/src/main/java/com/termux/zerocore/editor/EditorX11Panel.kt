package com.termux.zerocore.editor

import android.app.Activity
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.shared.view.KeyboardUtils
import com.termux.zerocore.gui.ZtGuiFramePoller
import com.termux.zerocore.gui.ZtGuiGlSurfaceView
import io.github.rosemoe.sora.widget.CodeEditor

class EditorX11Panel(
    private val activity: Activity,
    private val surfaceContainer: FrameLayout,
    private val setupPanel: View,
    private val setupMessageView: TextView,
    private val setupActionView: TextView,
    private val statusView: TextView,
    private val displayButton: TextView?,
    private val connectButton: TextView?,
    private val maximizeButton: ImageView,
    private val codeEditor: CodeEditor?,
    private val dockHeightController: DockHeightController,
    private val onWriteTerminal: (String) -> Unit,
    private val onEnsureTerminal: () -> Unit,
    private val isShellX11Ready: () -> Boolean,
    private val onLayoutChanged: () -> Unit,
    private val onTabActiveChanged: (Boolean) -> Unit,
    private val onBarActionsChanged: () -> Unit
) {

    interface DockHeightController {
        fun saveDockedHeight()
        fun restoreDockedHeight()
    }

    private enum class SetupReason {
        INSTALLING,
        START_FAILED
    }

    private var guiSurface: ZtGuiGlSurfaceView? = null
    private var framePoller: ZtGuiFramePoller? = null
    private var tabActive = false
    private var maximized = false
    private var setupMode = false
    private var serverBootRequested = false
    private var bootstrapInProgress = false
    private var bootstrapPollToken = 0
    private var installingPackages = false
    private var runReadyCallback: (() -> Unit)? = null
    private var runReadyDelivered = false
    private var waitForConnection = false

    fun init() {
        displayButton?.setOnClickListener { exportDisplay() }
        connectButton?.setOnClickListener { startGuiServerInShell() }
        maximizeButton.setOnClickListener { toggleMaximized() }
        setupActionView.setOnClickListener { retryFromSetup() }
        displayButton?.visibility = View.VISIBLE
        connectButton?.visibility = View.VISIBLE
        updateStatus()
        updateMaximizeButton()
    }

    fun isSetupMode(): Boolean = setupMode

    fun isBootstrapInProgress(): Boolean = bootstrapInProgress

    fun isAvailable(): Boolean = true

    fun prepareForProgramRun(onReady: () -> Unit) {
        runReadyCallback = onReady
        runReadyDelivered = false
        waitForConnection = true
        Log.i(LOG_TAG, "prepareForProgramRun: waiting for GUI before run")
        if (!tabActive) return
        if (isGuiConnected()) {
            surfaceContainer.post { deliverRunReady("already connected") }
            return
        }
        runGuiBootstrap()
    }

    fun onGuiAppStarted() {
        guiSurface?.resetTransform()
        updateStatus()
    }

    fun onTabShown() {
        if (tabActive) return
        tabActive = true
        blurEditor()
        onTabActiveChanged(true)
        ensureGuiSurface()
        startFramePolling()
        refreshPanelMode()
        if (setupMode) {
            onLayoutChanged()
            return
        }
        if (isGuiConnected() && isShellX11ReadySafe()) {
            showOperationalMode()
            if (waitForConnection) {
                waitForGuiConnection()
            }
            return
        }
        if (serverBootRequested) {
            showOperationalMode()
            scheduleStatusRefresh()
            if (waitForConnection) {
                waitForGuiConnection()
            }
            return
        }
        runGuiBootstrap()
        onLayoutChanged()
    }

    fun onTabHidden() {
        if (!tabActive) return
        tabActive = false
        stopFramePolling()
        if (!waitForConnection) {
            bootstrapPollToken++
        }
        if (maximized) {
            maximized = false
            updateMaximizeButton()
        }
        onTabActiveChanged(false)
        onLayoutChanged()
        restoreEditor()
    }

    fun onDockHidden() {
        onTabHidden()
        setupMode = false
        serverBootRequested = false
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
                startFramePolling()
                updateStatus()
                if (!isGuiConnected()) {
                    runGuiBootstrap()
                }
            }
            return
        }
        if (!setupMode) {
            startFramePolling()
            updateStatus()
        }
    }

    fun onPause() {
        if (!tabActive) return
        stopFramePolling()
    }

    fun onDestroy() {
        stopFramePolling()
        guiSurface = null
        surfaceContainer.removeAllViews()
    }

    fun onConfigurationChanged(@Suppress("UNUSED_PARAMETER") newConfig: Configuration) {
        guiSurface?.resetTransform()
    }

    fun onWindowFocusChanged(@Suppress("UNUSED_PARAMETER") hasFocus: Boolean) {
        if (!tabActive || setupMode) return
        updateStatus()
    }

    fun onHostLayoutChanged() {
        if (!tabActive || maximized || setupMode) return
        surfaceContainer.post {
            guiSurface?.resetTransform()
        }
    }

    private fun runGuiBootstrap(forceInstall: Boolean = false) {
        val packagesInstalled = EditorX11Environment.isPackagesInstalled()
        val needInstall = forceInstall || !packagesInstalled
        val shellReady = isShellX11ReadySafe()
        if (!needInstall && (shellReady || isGuiConnected())) {
            serverBootRequested = true
            bootstrapInProgress = false
            showOperationalMode()
            startFramePolling()
            scheduleStatusRefresh()
            if (waitForConnection) {
                waitForGuiConnection()
            }
            return
        }
        Log.i(
            LOG_TAG,
            "runGuiBootstrap needInstall=$needInstall shellReady=$shellReady forceInstall=$forceInstall"
        )
        bootstrapInProgress = true
        installingPackages = needInstall
        bootstrapPollToken++
        val token = bootstrapPollToken
        showSetupMode(if (needInstall) SetupReason.INSTALLING else SetupReason.START_FAILED)
        onEnsureTerminal()
        val script = if (needInstall) {
            EditorX11Environment.ensureAndStartScript(
                installRepoEcho = activity.getString(R.string.editor_x11_install_repo),
                installPackagesEcho = activity.getString(R.string.editor_x11_install_packages)
            )
        } else {
            EditorX11Environment.startServerOnlyScript()
        }
        onWriteTerminal(script)
        pollBootstrap(token, attempt = 0)
    }

    private fun pollBootstrap(token: Int, attempt: Int) {
        if (token != bootstrapPollToken) return
        if (installingPackages && EditorX11Environment.isPackagesInstalled()) {
            installingPackages = false
            updateInstallingSetupMessage(starting = true)
        }
        val shellReady = isShellX11ReadySafe()
        val connected = isGuiConnected()
        if (connected || shellReady || attempt >= BOOTSTRAP_MAX_ATTEMPTS) {
            bootstrapInProgress = false
            serverBootRequested = true
            if (connected || shellReady) {
                showOperationalMode()
                startFramePolling()
                scheduleStatusRefresh()
                if (waitForConnection) {
                    waitForGuiConnection()
                }
            } else {
                showSetupMode(SetupReason.START_FAILED)
            }
            return
        }
        updateInstallingSetupMessage(starting = !installingPackages)
        surfaceContainer.postDelayed({
            pollBootstrap(token, attempt + 1)
        }, BOOTSTRAP_POLL_MS)
    }

    private fun retryFromSetup() {
        runGuiBootstrap(forceInstall = !EditorX11Environment.isPackagesInstalled())
    }

    private fun waitForGuiConnection(attempt: Int = 0) {
        if (runReadyDelivered) return
        if (!tabActive && runReadyCallback == null) return
        updateStatus()
        val connected = isGuiConnected()
        val shellReady = !waitForConnection || isShellX11ReadySafe()
        if (connected && shellReady) {
            deliverRunReady("gui connected")
            return
        }
        if (attempt >= RUN_READY_MAX_ATTEMPTS) {
            Log.w(LOG_TAG, "GUI connection timeout after $attempt attempts, continuing run")
            deliverRunReady("timeout")
            return
        }
        surfaceContainer.postDelayed({ waitForGuiConnection(attempt + 1) }, RUN_READY_POLL_MS)
    }

    private fun deliverRunReady(reason: String) {
        if (runReadyDelivered) return
        runReadyDelivered = true
        waitForConnection = false
        val callback = runReadyCallback
        runReadyCallback = null
        Log.i(LOG_TAG, "GUI ready ($reason), invoking run callback")
        callback?.invoke()
    }

    private fun refreshPanelMode() {
        if (resolveSetupReason() == null) {
            showOperationalMode()
            return
        }
        showSetupMode(SetupReason.START_FAILED)
    }

    private fun showSetupMode(reason: SetupReason) {
        setupMode = true
        maximized = false
        setupPanel.visibility = View.VISIBLE
        surfaceContainer.visibility = View.GONE
        setupMessageView.text = activity.getString(
            when (reason) {
                SetupReason.INSTALLING -> R.string.editor_x11_installing_env
                SetupReason.START_FAILED -> R.string.editor_x11_start_failed
            }
        )
        setupActionView.text = activity.getString(R.string.editor_x11_retry)
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

    private fun resolveSetupReason(): SetupReason? = null

    private fun updateInstallingSetupMessage(starting: Boolean) {
        if (!setupMode) return
        setupMessageView.text = activity.getString(
            if (starting) R.string.editor_x11_starting_env
            else R.string.editor_x11_installing_env
        )
        statusView.text = activity.getString(
            if (starting) R.string.editor_x11_status_starting
            else R.string.editor_x11_status_installing
        )
    }

    private val statusRefreshRunnable = Runnable {
        if (!tabActive || setupMode) return@Runnable
        updateStatus()
        scheduleStatusRefresh()
    }

    private fun scheduleStatusRefresh() {
        surfaceContainer.removeCallbacks(statusRefreshRunnable)
        surfaceContainer.postDelayed(statusRefreshRunnable, STATUS_REFRESH_MS)
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
            guiSurface?.resetTransform()
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

    private fun exportDisplay() {
        onEnsureTerminal()
        onWriteTerminal(EditorX11Environment.exportDisplayScript())
        UUtils.showMsg(activity.getString(R.string.editor_x11_display_set))
    }

    private fun startGuiServerInShell() {
        onEnsureTerminal()
        onWriteTerminal(
            buildString {
                append(EditorX11Environment.startServerOnlyScript())
                append("\n")
                append(EditorX11Environment.exportDisplayScript())
            }
        )
        surfaceContainer.postDelayed({ updateStatus() }, 800)
    }

    private fun ensureGuiSurface() {
        if (guiSurface != null) return
        val view = ZtGuiGlSurfaceView(activity).apply {
            setDesktopSize(800, 600)
        }
        surfaceContainer.removeAllViews()
        surfaceContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        guiSurface = view
    }

    private fun startFramePolling() {
        if (framePoller != null) return
        framePoller = ZtGuiFramePoller(
            onFrame = { w, h, rgb -> guiSurface?.setFrame(w, h, rgb) },
            onActivity = null
        ).also { it.start() }
    }

    private fun stopFramePolling() {
        framePoller?.stop()
        framePoller = null
    }

    private fun isGuiConnected(): Boolean = framePoller?.hasRecentFrame() == true

    private fun updateStatus() {
        if (setupMode) return
        val connected = isGuiConnected()
        statusView.text = activity.getString(
            when {
                connected -> R.string.editor_x11_status_connected
                bootstrapInProgress && installingPackages -> R.string.editor_x11_status_installing
                bootstrapInProgress -> R.string.editor_x11_status_starting
                serverBootRequested -> R.string.editor_x11_status_connecting
                else -> R.string.editor_x11_status_disconnected
            }
        )
        statusView.setTextColor(
            if (connected) 0xFF4CAF50.toInt() else 0xFFFF8A80.toInt()
        )
    }

    private fun isShellX11ReadySafe(): Boolean {
        return try {
            isShellX11Ready()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "isShellX11Ready failed", e)
            false
        }
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

    companion object {
        const val LOG_TAG = "EditorX11Panel"
        private const val BOOTSTRAP_POLL_MS = 500L
        private const val BOOTSTRAP_MAX_ATTEMPTS = 120
        private const val RUN_READY_POLL_MS = 400L
        private const val RUN_READY_MAX_ATTEMPTS = 45
        private const val STATUS_REFRESH_MS = 2000L
    }
}
