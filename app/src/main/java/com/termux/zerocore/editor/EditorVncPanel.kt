package com.termux.zerocore.editor

import android.os.Bundle
import android.app.Activity
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.example.xh_lib.utils.UUtils
import com.gaurav.avnc.model.ServerProfile
import com.gaurav.avnc.ui.vnc.EmbeddedVncFragment
import com.gaurav.avnc.ui.vnc.FrameView
import com.termux.R
import com.termux.shared.view.KeyboardUtils
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.zerocore.editor.EditorVncEnvironment.VNC_HOST
import com.termux.zerocore.editor.EditorVncEnvironment.VNC_PORT
import io.github.rosemoe.sora.widget.CodeEditor

class EditorVncPanel(
    private val activity: Activity,
    private val surfaceContainer: FrameLayout,
    private val setupPanel: View,
    private val setupMessageView: TextView,
    private val setupActionView: TextView,
    private val statusView: TextView,
    private val maximizeButton: ImageView,
    private val extraKeysView: ExtraKeysView,
    private val codeEditor: CodeEditor?,
    private val dockHeightController: DockHeightController,
    private val onWriteTerminal: (String) -> Unit,
    private val onEnsureTerminal: () -> Unit,
    private val isShellVncReady: () -> Boolean,
    private val isGuiProcessStartedInTerminal: () -> Boolean,
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
        ENV_MISSING,
        START_FAILED
    }

    private var tabActive = false
    private var maximized = false
    private var setupMode = false
    private var serverBootRequested = false
    private var bootstrapInProgress = false
    private var bootstrapPollToken = 0
    private var installingPackages = false
    private var disconnectedStreak = 0
    private var lastAutoReconnectMs = 0L
    private var runReadyCallback: (() -> Unit)? = null
    private var runReadyDelivered = false
    private var waitForConnection = false
    private var guiRefreshPollToken = 0
    private var viewerRecoveryPending = false
    private var lastViewerRecoveryMs = 0L
    private var connectedPollStreak = 0
    private var vncExtraKeys: EditorVncExtraKeys? = null

    fun init() {
        vncExtraKeys = EditorVncExtraKeys(activity, extraKeysView) { findEmbeddedFragment() }.also {
            it.bindView()
        }
        extraKeysView.visibility = View.GONE
        maximizeButton.setOnClickListener { toggleMaximized() }
        setupActionView.setOnClickListener { retryFromSetup() }
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
        Log.i(LOG_TAG, "prepareForProgramRun: waiting for VNC before run")
        if (!tabActive) return
        val fragmentActivity = activity as? FragmentActivity
        val fragment = fragmentActivity?.supportFragmentManager
            ?.findFragmentByTag(VNC_FRAGMENT_TAG) as? EmbeddedVncFragment
        if (fragment?.isConnected() == true) {
            surfaceContainer.post { deliverRunReady("already connected") }
            return
        }
        if (fragment != null) {
            waitForVncConnection()
        } else {
            runVncBootstrap()
        }
    }

    fun onGuiAppStarted() {
        guiRefreshPollToken++
        waitForGuiProcessThenRefresh(guiRefreshPollToken, attempt = 0)
    }

    private fun waitForGuiProcessThenRefresh(token: Int, attempt: Int) {
        if (token != guiRefreshPollToken) return
        if (isGuiProcessStartedInTerminalSafe() || attempt >= 60) {
            surfaceContainer.postDelayed({
                if (token != guiRefreshPollToken) return@postDelayed
                refreshViewerAfterGuiStart()
            }, 1000)
            return
        }
        surfaceContainer.postDelayed({
            waitForGuiProcessThenRefresh(token, attempt + 1)
        }, 400)
    }

    private fun refreshViewerAfterGuiStart() {
        val fragment = findEmbeddedFragment()
        when {
            fragment?.isConnected() == true -> refreshViewerFramebuffer(fragment)
            else -> recoverViewerOnce("gui started")
        }
    }

    private fun refreshViewerFramebuffer(fragment: EmbeddedVncFragment) {
        syncEmbeddedViewport(fragment)
        fragment.refreshDisplay()
        Log.d(LOG_TAG, "VNC refreshDisplay after GUI start")
    }

    /**
     * EmbeddedVncFragment lacks VncActivity's LayoutManager; syncLayoutMetrics() updates
     * FrameState viewport/window sizes so the GL renderer does not stay black.
     */
    private fun syncEmbeddedViewport(fragment: EmbeddedVncFragment) {
        val frameView = findFrameView(fragment.view ?: return) ?: return
        if (frameView.width <= 0 || frameView.height <= 0) {
            surfaceContainer.post { syncEmbeddedViewport(fragment) }
            return
        }
        fragment.syncLayoutMetrics()
        Log.d(LOG_TAG, "syncEmbeddedViewport ${frameView.width}x${frameView.height}")
    }

    private fun findFrameView(root: View): FrameView? {
        if (root is FrameView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findFrameView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun recoverViewerOnce(reason: String) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (viewerRecoveryPending || now - lastViewerRecoveryMs < 2500L) {
            Log.d(LOG_TAG, "Skip viewer recovery ($reason): debounced")
            return
        }
        viewerRecoveryPending = true
        lastViewerRecoveryMs = now
        Log.i(LOG_TAG, "Recovering VNC viewer: $reason")
        surfaceContainer.postDelayed({
            viewerRecoveryPending = false
            val fragment = findEmbeddedFragment()
            if (fragment?.isConnected() == true) {
                refreshViewerFramebuffer(fragment)
                return@postDelayed
            }
            reembedViewer()
        }, 400)
    }

    private fun reembedViewer() {
        removeEmbeddedFragment()
        surfaceContainer.postDelayed({
            if (!tabActive && runReadyCallback == null) return@postDelayed
            embedViewer()
        }, 350)
    }

    private fun isGuiProcessStartedInTerminalSafe(): Boolean {
        return try {
            isGuiProcessStartedInTerminal()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "isGuiProcessStartedInTerminal failed", e)
            false
        }
    }

    private fun scheduleViewerReconnectIfNeeded() {
        surfaceContainer.postDelayed({
            if (!tabActive) return@postDelayed
            val fragment = findEmbeddedFragment() ?: return@postDelayed
            if (!fragment.isConnected()) {
                recoverViewerOnce("tab shown disconnected")
            }
        }, 500)
    }

    private fun findEmbeddedFragment(): EmbeddedVncFragment? {
        val fragmentActivity = activity as? FragmentActivity ?: return null
        return fragmentActivity.supportFragmentManager
            .findFragmentByTag(VNC_FRAGMENT_TAG) as? EmbeddedVncFragment
    }

    fun onTabShown() {
        if (tabActive) return
        tabActive = true
        blurEditor()
        onTabActiveChanged(true)
        onLayoutChanged()
        val fragment = findEmbeddedFragment()
        val connected = fragment?.isConnected() == true
        val shellReady = isShellVncReadySafe()
        if (connected && shellReady) {
            showOperationalMode()
            scheduleStatusRefresh()
            if (waitForConnection) {
                waitForVncConnection()
            }
            return
        }
        if (serverBootRequested && shellReady) {
            showOperationalMode()
            ensureViewerPresent()
            scheduleViewerReconnectIfNeeded()
            if (waitForConnection) {
                waitForVncConnection()
            } else {
                scheduleStatusRefresh()
            }
            return
        }
        runVncBootstrap()
    }

    private fun ensureViewerPresent() {
        val fragmentActivity = activity as? FragmentActivity ?: return
        if (fragmentActivity.supportFragmentManager.findFragmentByTag(VNC_FRAGMENT_TAG) != null) {
            scheduleStatusRefresh()
            return
        }
        surfaceContainer.postDelayed({
            if (!tabActive) return@postDelayed
            embedViewer()
        }, 500)
    }

    fun onTabHidden() {
        if (!tabActive) return
        tabActive = false
        guiRefreshPollToken++
        if (!waitForConnection) {
            bootstrapPollToken++
        }
        surfaceContainer.removeCallbacks(statusRefreshRunnable)
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
        if (!tabActive || setupMode) return
        updateStatus()
    }

    fun onPause() {
    }

    fun onDestroy() {
        val fragmentActivity = activity as? FragmentActivity ?: return
        fragmentActivity.supportFragmentManager.findFragmentByTag(VNC_FRAGMENT_TAG)?.let { fragment ->
            fragmentActivity.supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
    }

    fun onHostLayoutChanged() {
        if (!tabActive || maximized || setupMode) return
        surfaceContainer.post {
            surfaceContainer.requestLayout()
        }
    }

    private fun runVncBootstrap(forceInstall: Boolean = false) {
        val packagesInstalled = EditorVncEnvironment.isPackagesInstalled()
        val needInstall = forceInstall || !packagesInstalled
        val shellReady = isShellVncReadySafe()
        if (!needInstall && shellReady) {
            serverBootRequested = true
            bootstrapInProgress = false
            showOperationalMode()
            embedViewerIfNeeded()
            scheduleStatusRefresh()
            if (waitForConnection) {
                waitForVncConnection()
            }
            return
        }
        Log.i(
            LOG_TAG,
            "runVncBootstrap needInstall=$needInstall shellReady=$shellReady forceInstall=$forceInstall"
        )
        bootstrapInProgress = true
        installingPackages = needInstall
        bootstrapPollToken++
        val token = bootstrapPollToken
        showSetupMode(SetupReason.INSTALLING)
        onEnsureTerminal()
        val script = if (needInstall) {
            EditorVncEnvironment.ensureAndStartScript(
                installRepoEcho = activity.getString(R.string.editor_vnc_install_repo),
                installPackagesEcho = activity.getString(R.string.editor_vnc_install_packages)
            )
        } else {
            EditorVncEnvironment.startServerOnlyScript()
        }
        onWriteTerminal(script)
        serverBootRequested = true
        scheduleStatusRefresh()
        surfaceContainer.postDelayed({ pollBootstrapReady(token, attempt = 0) }, BOOTSTRAP_INITIAL_DELAY_MS)
    }

    private fun pollBootstrapReady(token: Int, attempt: Int) {
        if (token != bootstrapPollToken) return
        if (!tabActive && runReadyCallback == null) return
        val shellReady = isShellVncReadySafe()
        if (shellReady) {
            finishBootstrapSuccess(token)
            return
        }
        if (installingPackages && EditorVncEnvironment.isPackagesInstalled()) {
            installingPackages = false
            updateInstallingSetupMessage(starting = true)
        }
        if (attempt >= BOOTSTRAP_MAX_ATTEMPTS) {
            bootstrapInProgress = false
            showSetupMode(SetupReason.START_FAILED)
            Log.w(LOG_TAG, "VNC bootstrap timed out after $attempt polls")
            return
        }
        surfaceContainer.postDelayed({ pollBootstrapReady(token, attempt + 1) }, BOOTSTRAP_POLL_MS)
    }

    private fun finishBootstrapSuccess(token: Int) {
        if (token != bootstrapPollToken) return
        bootstrapInProgress = false
        installingPackages = false
        showOperationalMode()
        embedViewerIfNeeded()
        if (waitForConnection) {
            waitForVncConnection()
        } else {
            scheduleStatusRefresh()
        }
    }

    private fun embedViewerIfNeeded() {
        if (findEmbeddedFragment() == null) {
            embedViewer()
        } else {
            scheduleStatusRefresh()
        }
    }

    private fun updateInstallingSetupMessage(starting: Boolean) {
        if (!setupMode) return
        setupMessageView.text = activity.getString(
            if (starting) R.string.editor_vnc_starting_env
            else R.string.editor_vnc_installing_env
        )
        statusView.text = activity.getString(
            if (starting) R.string.editor_vnc_status_starting
            else R.string.editor_vnc_status_installing
        )
    }

    private fun bootServerAndConnect() {
        runVncBootstrap()
    }

    private fun waitForVncConnection(attempt: Int = 0) {
        if (runReadyDelivered) return
        if (!tabActive && runReadyCallback == null) return
        refreshStatusOnce()
        val fragmentActivity = activity as? FragmentActivity
        val fragment = fragmentActivity?.supportFragmentManager
            ?.findFragmentByTag(VNC_FRAGMENT_TAG) as? EmbeddedVncFragment
        val connected = fragment?.isConnected() == true
        if (connected) {
            connectedPollStreak++
        } else {
            connectedPollStreak = 0
        }
        val shellReady = !waitForConnection || isShellVncReadySafe()
        val readyToRun = connected && (shellReady || connectedPollStreak >= 3)
        Log.d(
            LOG_TAG,
            "waitForVncConnection attempt=$attempt connected=$connected shellReady=$shellReady streak=$connectedPollStreak"
        )
        if (readyToRun) {
            deliverRunReady(if (shellReady) "vnc connected" else "vnc connected (no shell marker)")
            return
        }
        if (attempt >= RUN_READY_MAX_ATTEMPTS) {
            Log.w(LOG_TAG, "VNC connection timeout after $attempt attempts, continuing run")
            deliverRunReady("timeout")
            return
        }
        surfaceContainer.postDelayed({ waitForVncConnection(attempt + 1) }, RUN_READY_POLL_MS)
    }

    private fun isShellVncReadySafe(): Boolean {
        return try {
            isShellVncReady()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "isShellVncReady failed", e)
            false
        }
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

    private fun embedViewer() {
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity == null) {
            logEmbedFailure("activity is not FragmentActivity: ${activity.javaClass.name}")
            showSetupMode(SetupReason.START_FAILED)
            UUtils.showMsg(activity.getString(R.string.editor_vnc_init_failed))
            return
        }
        val containerId = surfaceContainer.id
        if (containerId == View.NO_ID) {
            logEmbedFailure("surfaceContainer has no android:id")
            showSetupMode(SetupReason.START_FAILED)
            UUtils.showMsg(activity.getString(R.string.editor_vnc_init_failed))
            return
        }
        if (fragmentActivity.supportFragmentManager.findFragmentByTag(VNC_FRAGMENT_TAG) != null) {
            scheduleStatusRefresh()
            return
        }
        try {
            Log.i(LOG_TAG, "Embedding VNC fragment into containerId=$containerId")
            fragmentActivity.supportFragmentManager.beginTransaction()
                .replace(
                    containerId,
                    createEmbeddedVncFragment(),
                    VNC_FRAGMENT_TAG
                )
                .commitNowAllowingStateLoss()
            showOperationalMode()
            scheduleStatusRefresh()
            findEmbeddedFragment()?.let { fragment ->
                surfaceContainer.post { syncEmbeddedViewport(fragment) }
                scheduleViewportSyncBurst()
            }
        } catch (e: Throwable) {
            logEmbedFailure("commit fragment failed", e)
            showSetupMode(SetupReason.START_FAILED)
            UUtils.showMsg(activity.getString(R.string.editor_vnc_init_failed))
        }
    }

    private fun scheduleViewportSyncBurst() {
        val delays = longArrayOf(200, 500, 1000, 2000, 4000)
        delays.forEach { delay ->
            surfaceContainer.postDelayed({
                if (!tabActive && runReadyCallback == null) return@postDelayed
                findEmbeddedFragment()?.let { syncEmbeddedViewport(it) }
            }, delay)
        }
    }

    private fun logEmbedFailure(message: String, error: Throwable? = null) {
        if (error != null) {
            Log.e(LOG_TAG, message, error)
        } else {
            Log.e(LOG_TAG, message)
        }
    }

    private fun scheduleStatusRefresh() {
        surfaceContainer.removeCallbacks(statusRefreshRunnable)
        surfaceContainer.post(statusRefreshRunnable)
    }

    private val statusRefreshRunnable = object : Runnable {
        override fun run() {
            if (!tabActive && runReadyCallback == null) return
            refreshStatusOnce()
            if ((tabActive && !setupMode) || bootstrapInProgress) {
                surfaceContainer.postDelayed(this, STATUS_REFRESH_MS)
            }
        }
    }

    private fun refreshStatusOnce() {
        val fragmentActivity = activity as? FragmentActivity ?: return
        val fragment = fragmentActivity.supportFragmentManager
            .findFragmentByTag(VNC_FRAGMENT_TAG) as? EmbeddedVncFragment
        if (fragment == null) {
            if (bootstrapInProgress) {
                statusView.text = activity.getString(
                    if (installingPackages) R.string.editor_vnc_status_installing
                    else R.string.editor_vnc_status_starting
                )
                statusView.setTextColor(0xFFFF8A80.toInt())
            }
            return
        }
        val connected = fragment.isConnected()
        if (connected) {
            syncEmbeddedViewport(fragment)
            disconnectedStreak = 0
            if (bootstrapInProgress && isShellVncReadySafe()) {
                finishBootstrapSuccess(bootstrapPollToken)
            }
        } else {
            Log.w(LOG_TAG, "VNC viewer disconnected: ${readDisconnectReason(fragment)}")
            maybeAutoReconnect(connected)
        }
        statusView.text = activity.getString(
            when {
                connected -> R.string.editor_vnc_status_connected
                bootstrapInProgress -> {
                    if (installingPackages) R.string.editor_vnc_status_installing
                    else R.string.editor_vnc_status_starting
                }
                serverBootRequested -> R.string.editor_vnc_status_connecting
                else -> R.string.editor_vnc_status_disconnected
            }
        )
        statusView.setTextColor(
            when {
                connected -> 0xFF4CAF50.toInt()
                bootstrapInProgress || serverBootRequested -> 0xFFFF8A80.toInt()
                else -> 0xFFFF5252.toInt()
            }
        )
    }

    private fun maybeAutoReconnect(connected: Boolean) {
        if (connected || !tabActive || setupMode || bootstrapInProgress) return
        disconnectedStreak++
        if (disconnectedStreak < DISCONNECT_RECONNECT_THRESHOLD) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastAutoReconnectMs < AUTO_RECONNECT_DEBOUNCE_MS) return
        lastAutoReconnectMs = now
        disconnectedStreak = 0
        Log.i(LOG_TAG, "Auto reconnect VNC (viewer disconnected)")
        if (!isShellVncReadySafe()) {
            runVncBootstrap(forceInstall = !EditorVncEnvironment.isPackagesInstalled())
        } else {
            recoverViewerOnce("auto reconnect")
        }
    }

    private fun createEmbeddedVncFragment(): EmbeddedVncFragment {
        return EmbeddedVncFragment().apply {
            arguments = Bundle().apply {
                putParcelable(VNC_PROFILE_ARG, createServerProfile())
            }
        }
    }

    private fun createServerProfile(): ServerProfile {
        return ServerProfile().apply {
            name = "ZeroTermux Editor"
            host = VNC_HOST
            port = VNC_PORT
            useRawEncoding = false
            gestureStyle = "touchscreen"
            zoom1 = 1f
            zoom2 = 1f
        }
    }

    private fun retryFromSetup() {
        setupMode = false
        serverBootRequested = false
        bootstrapInProgress = false
        bootstrapPollToken++
        disconnectedStreak = 0
        surfaceContainer.removeCallbacks(statusRefreshRunnable)
        removeEmbeddedFragment()
        onBarActionsChanged()
        runVncBootstrap(forceInstall = true)
    }

    private fun removeEmbeddedFragment() {
        val fragmentActivity = activity as? FragmentActivity ?: return
        fragmentActivity.supportFragmentManager.findFragmentByTag(VNC_FRAGMENT_TAG)?.let { fragment ->
            fragmentActivity.supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss()
        }
    }

    private fun showSetupMode(reason: SetupReason) {
        setupMode = true
        maximized = false
        surfaceContainer.removeCallbacks(statusRefreshRunnable)
        setupPanel.visibility = View.VISIBLE
        surfaceContainer.visibility = View.GONE
        extraKeysView.visibility = View.GONE
        setupMessageView.text = activity.getString(
            when (reason) {
                SetupReason.INSTALLING -> {
                    if (installingPackages) R.string.editor_vnc_installing_env
                    else R.string.editor_vnc_starting_env
                }
                SetupReason.ENV_MISSING -> R.string.editor_vnc_env_missing
                SetupReason.START_FAILED -> R.string.editor_vnc_start_failed
            }
        )
        setupActionView.visibility = if (reason == SetupReason.INSTALLING) View.GONE else View.VISIBLE
        setupActionView.text = activity.getString(R.string.editor_vnc_retry)
        updateMaximizeButton()
        onBarActionsChanged()
        updateStatus()
    }

    private fun showOperationalMode() {
        setupMode = false
        setupPanel.visibility = View.GONE
        surfaceContainer.visibility = View.VISIBLE
        extraKeysView.visibility = View.VISIBLE
        updateMaximizeButton()
        onBarActionsChanged()
        scheduleStatusRefresh()
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
        surfaceContainer.post { surfaceContainer.requestLayout() }
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

    private fun updateConnectionStatus() {
        if (setupMode || !tabActive) return
        val fragmentActivity = activity as? FragmentActivity ?: return
        val fragment = fragmentActivity.supportFragmentManager
            .findFragmentByTag(VNC_FRAGMENT_TAG) as? EmbeddedVncFragment
        if (fragment == null) {
            if (serverBootRequested) {
                logEmbedFailure("EmbeddedVncFragment missing after boot (check logcat for fragment crash)")
                showSetupMode(SetupReason.START_FAILED)
            }
            return
        }
        updateStatus()
    }

    private fun updateStatus() {
        refreshStatusOnce()
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

    private fun readDisconnectReason(fragment: EmbeddedVncFragment): String {
        return try {
            val vmField = EmbeddedVncFragment::class.java.getDeclaredField("viewModel")
            vmField.isAccessible = true
            val viewModel = vmField.get(fragment) ?: return "no viewModel"
            val reasonField = viewModel.javaClass.getDeclaredField("disconnectReason")
            reasonField.isAccessible = true
            val liveData = reasonField.get(viewModel)
            val getValue = liveData.javaClass.getMethod("getValue")
            getValue.invoke(liveData)?.toString()?.takeIf { it.isNotBlank() } ?: "empty"
        } catch (e: Exception) {
            "unavailable (${e.javaClass.simpleName})"
        }
    }

    companion object {
        /** Logcat filter: adb logcat -s EditorVncPanel EmbeddedVncFragment VncViewModel EditorTerminalPanel */
        const val LOG_TAG = "EditorVncPanel"
        private const val VNC_FRAGMENT_TAG = "editor_embedded_vnc"
        /** Must match [EmbeddedVncFragment] argument key. */
        private const val VNC_PROFILE_ARG = "com.gaurav.avnc.embedded_profile"
        private const val RUN_READY_POLL_MS = 500L
        private const val RUN_READY_MAX_ATTEMPTS = 40
        private const val BOOTSTRAP_POLL_MS = 500L
        private const val BOOTSTRAP_INITIAL_DELAY_MS = 800L
        private const val BOOTSTRAP_MAX_ATTEMPTS = 180
        private const val STATUS_REFRESH_MS = 1000L
        private const val DISCONNECT_RECONNECT_THRESHOLD = 3
        private const val AUTO_RECONNECT_DEBOUNCE_MS = 6000L
    }
}
