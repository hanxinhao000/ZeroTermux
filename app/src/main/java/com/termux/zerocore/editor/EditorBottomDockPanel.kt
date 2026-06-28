package com.termux.zerocore.editor

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.TextView
import com.termux.R
import java.io.File
import kotlin.math.abs

class EditorBottomDockPanel(
    private val activity: Activity,
    private val dockView: View,
    private val terminalTabChip: View,
    private val tabTerminal: TextView,
    private val closeTerminal: ImageView,
    private val x11TabChip: View,
    private val tabX11: TextView,
    private val closeX11: ImageView,
    private val tabSpacer: View,
    private val ctrlCButton: TextView,
    private val x11StatusView: TextView,
    private val x11MaximizeButton: ImageView,
    private val hideDockButton: ImageView,
    private val terminalSection: View,
    private val x11Section: View,
    private val terminalPanel: EditorTerminalPanel,
    private val x11Panel: EditorVncPanel,
    private val onLayoutChanged: () -> Unit,
    private val onDockVisibilityChanged: () -> Unit,
    private val onOpenTerminalAtDirectory: () -> Unit
) {

    enum class Tab {
        TERMINAL,
        X11
    }

    private var dockVisible = false
    private var terminalOpen = false
    private var x11Open = false
    private var activeTab = Tab.TERMINAL
    private var dockedHeightPx = 0
    private var pendingTerminalDirectory: File? = null
    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop

    fun init(
        restoredTerminalOpen: Boolean,
        restoredX11Open: Boolean,
        restoredDockPanelVisible: Boolean,
        restoredTab: Tab?
    ) {
        dockedHeightPx = defaultPanelHeightPx()
        setDockHeight(dockedHeightPx)
        setupTabBarDrag(tabSpacer)
        setupTabChipDrag(terminalTabChip, closeTerminal, tabTerminal)
        setupTabChipDrag(x11TabChip, closeX11, tabX11)
        tabTerminal.setOnClickListener { selectTab(Tab.TERMINAL, ensureSession = true) }
        tabX11.setOnClickListener { selectTab(Tab.X11, ensureSession = true) }
        closeTerminal.setOnClickListener { closeTerminalTab() }
        closeX11.setOnClickListener { closeX11Tab() }
        ctrlCButton.setOnClickListener { terminalPanel.sendCtrlC() }
        hideDockButton.setOnClickListener { hideDockPanel() }
        if (restoredTerminalOpen || restoredX11Open) {
            terminalOpen = restoredTerminalOpen
            x11Open = restoredX11Open
            dockVisible = restoredDockPanelVisible
            dockView.visibility = if (restoredDockPanelVisible) View.VISIBLE else View.GONE
            val tab = restoredTab ?: when {
                restoredTerminalOpen -> Tab.TERMINAL
                restoredX11Open -> Tab.X11
                else -> Tab.TERMINAL
            }
            refreshTabVisibility()
            if (restoredDockPanelVisible) {
                selectTab(tab, ensureSession = false)
            } else {
                activeTab = tab
                updateTabStyles()
                updateBarActions()
            }
            onDockVisibilityChanged()
            onLayoutChanged()
        } else {
            ensureHidden()
        }
    }

    fun hasOpenTabs(): Boolean = terminalOpen || x11Open

    fun isVisible(): Boolean = dockVisible

    fun isTerminalOpen(): Boolean = terminalOpen

    fun isX11Open(): Boolean = x11Open

    fun getActiveTab(): Tab = activeTab

    fun isTerminalTabActive(): Boolean = dockVisible && terminalOpen && activeTab == Tab.TERMINAL

    fun isX11TabActive(): Boolean = dockVisible && x11Open && activeTab == Tab.X11

    fun isX11Maximized(): Boolean = x11Panel.isMaximized()

    fun openTerminalTab() {
        if (terminalOpen && !dockVisible) {
            restoreDockPanel(Tab.TERMINAL)
            return
        }
        terminalOpen = true
        showDockIfNeeded()
        refreshTabVisibility()
        selectTab(Tab.TERMINAL, ensureSession = true)
    }

    fun openX11Tab() {
        if (x11Open && !dockVisible) {
            restoreDockPanel(Tab.X11)
            return
        }
        x11Open = true
        showDockIfNeeded()
        refreshTabVisibility()
        selectTab(Tab.X11, ensureSession = true)
    }

    fun showTerminalAtDirectory(directory: File?) {
        pendingTerminalDirectory = directory
        openTerminalTab()
    }

    /** Run flow: show GUI tab, boot VNC, then invoke [onGuiReady] before switching to terminal. */
    fun openGuiThenRun(directory: File?, onGuiReady: () -> Unit) {
        terminalOpen = true
        x11Open = true
        pendingTerminalDirectory = directory
        showDockIfNeeded()
        refreshTabVisibility()
        terminalPanel.prepareBackgroundSession(directory)
        x11Panel.prepareForProgramRun(onGuiReady)
        selectTab(Tab.X11, ensureSession = false)
    }

    fun prepareTerminalBackground(directory: File?) {
        terminalPanel.prepareBackgroundSession(directory)
    }

    fun handleBackPressed(): Boolean {
        if (x11Panel.restoreFromMaximized()) {
            onLayoutChanged()
            return true
        }
        if (!dockVisible) return false
        when (activeTab) {
            Tab.TERMINAL -> closeTerminalTab()
            Tab.X11 -> closeX11Tab()
        }
        return true
    }

    fun onHostLayoutChanged() {
        terminalPanel.onHostLayoutChanged()
        x11Panel.onHostLayoutChanged()
    }

    fun setDockHeight(heightPx: Int) {
        val clamped = heightPx.coerceIn(minPanelHeightPx(), maxPanelHeightPx())
        dockedHeightPx = clamped
        val params = dockView.layoutParams ?: return
        if (params.height == clamped) return
        params.height = clamped
        dockView.layoutParams = params
        onHostLayoutChanged()
    }

    fun syncDockedHeightFromView() {
        val height = dockView.layoutParams?.height ?: return
        if (height > 0) {
            dockedHeightPx = height.coerceIn(minPanelHeightPx(), maxPanelHeightPx())
        }
    }

    fun saveDockedHeightBeforeMaximize() {
        dockedHeightPx = dockView.layoutParams.height.coerceAtLeast(minPanelHeightPx())
    }

    fun restoreDockedHeight() {
        setDockHeight(dockedHeightPx.coerceAtLeast(minPanelHeightPx()))
    }

    fun onTerminalSessionFailed() {
        closeTerminalTab()
    }

    private fun closeTerminalTab() {
        if (!terminalOpen) return
        terminalOpen = false
        terminalPanel.setVisible(false)
        if (x11Open) {
            refreshTabVisibility()
            selectTab(Tab.X11, ensureSession = true)
        } else {
            hideDockCompletely()
        }
    }

    private fun closeX11Tab() {
        if (!x11Open) return
        x11Open = false
        x11Panel.onDockHidden()
        if (terminalOpen) {
            refreshTabVisibility()
            selectTab(Tab.TERMINAL, ensureSession = true)
        } else {
            hideDockCompletely()
        }
    }

    private fun showDockIfNeeded() {
        if (dockVisible) return
        dockVisible = true
        dockView.visibility = View.VISIBLE
        onDockVisibilityChanged()
        onLayoutChanged()
    }

    /** Collapse dock UI without closing tabs or tearing down sessions. */
    fun hideDockPanel() {
        if (!dockVisible) return
        if (x11Panel.isMaximized()) {
            x11Panel.restoreFromMaximized()
        }
        dockVisible = false
        dockView.visibility = View.GONE
        if (activeTab == Tab.X11 && x11Open) {
            x11Panel.onTabHidden()
        }
        terminalPanel.setContentVisible(false)
        onDockVisibilityChanged()
        onLayoutChanged()
    }

    private fun restoreDockPanel(tab: Tab) {
        dockVisible = true
        dockView.visibility = View.VISIBLE
        refreshTabVisibility()
        selectTab(tab, ensureSession = true)
    }

    private fun hideDockCompletely() {
        if (!dockVisible) return
        dockVisible = false
        terminalOpen = false
        x11Open = false
        dockView.visibility = View.GONE
        x11Panel.onDockHidden()
        terminalPanel.setVisible(false)
        terminalSection.visibility = View.GONE
        x11Section.visibility = View.GONE
        onDockVisibilityChanged()
        onLayoutChanged()
    }

    private fun selectTab(tab: Tab, ensureSession: Boolean) {
        if (tab == Tab.TERMINAL && !terminalOpen) {
            if (x11Open) {
                activeTab = Tab.X11
            }
            return
        }
        if (tab == Tab.X11 && !x11Open) {
            if (terminalOpen) {
                activeTab = Tab.TERMINAL
            }
            return
        }
        syncDockedHeightFromView()
        activeTab = tab
        updateTabStyles()
        updateBarActions()
        terminalSection.visibility = if (tab == Tab.TERMINAL && terminalOpen) View.VISIBLE else View.GONE
        x11Section.visibility = if (tab == Tab.X11 && x11Open) View.VISIBLE else View.GONE
        if (tab == Tab.TERMINAL) {
            x11Panel.onTabHidden()
            if (ensureSession) {
                if (!terminalPanel.isSessionActive()) {
                    val directory = pendingTerminalDirectory
                    pendingTerminalDirectory = null
                    if (directory != null) {
                        terminalPanel.setVisible(true, directory)
                    } else {
                        onOpenTerminalAtDirectory()
                    }
                } else {
                    terminalPanel.setContentVisible(true)
                    terminalPanel.onResume()
                }
            } else if (terminalPanel.isSessionActive()) {
                terminalPanel.setContentVisible(true)
            }
        } else {
            terminalPanel.setContentVisible(false)
            x11Panel.onTabShown()
        }
        onLayoutChanged()
        onDockVisibilityChanged()
    }

    private fun refreshTabVisibility() {
        terminalTabChip.visibility = if (terminalOpen) View.VISIBLE else View.GONE
        x11TabChip.visibility = if (x11Open) View.VISIBLE else View.GONE
    }

    private fun updateTabStyles() {
        styleTabChip(terminalTabChip, tabTerminal, terminalOpen && activeTab == Tab.TERMINAL)
        styleTabChip(x11TabChip, tabX11, x11Open && activeTab == Tab.X11)
    }

    private fun styleTabChip(chip: View, label: TextView, selected: Boolean) {
        chip.setBackgroundResource(
            if (selected) R.drawable.shape_editor_bottom_tab_active
            else R.drawable.shape_editor_bottom_tab_inactive
        )
        label.setTextColor(if (selected) 0xFFCCCCCC.toInt() else 0xFF888888.toInt())
    }

    private fun updateBarActions() {
        val terminalActive = terminalOpen && activeTab == Tab.TERMINAL
        val x11Active = x11Open && activeTab == Tab.X11
        ctrlCButton.visibility = if (terminalActive) View.VISIBLE else View.GONE
        val showGuiOps = x11Active && (!x11Panel.isSetupMode() || x11Panel.isBootstrapInProgress())
        x11StatusView.visibility = if (showGuiOps) View.VISIBLE else View.GONE
        x11MaximizeButton.visibility = if (showGuiOps) View.VISIBLE else View.GONE
    }

    fun refreshBarActions() {
        updateBarActions()
    }

    private fun ensureHidden() {
        dockVisible = false
        terminalOpen = false
        x11Open = false
        dockView.visibility = View.GONE
        terminalTabChip.visibility = View.GONE
        x11TabChip.visibility = View.GONE
        terminalSection.visibility = View.GONE
        x11Section.visibility = View.GONE
        updateBarActions()
    }

    private fun setupTabBarDrag(handle: View) {
        var startY = 0f
        var startHeight = 0
        var dragging = false
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startHeight = dockView.layoutParams.height
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.rawY
                    if (!dragging && abs(deltaY) > touchSlop) {
                        dragging = true
                    }
                    if (dragging && !x11Panel.isMaximized()) {
                        val height = (startHeight + deltaY).toInt()
                            .coerceIn(minPanelHeightPx(), maxPanelHeightPx())
                        setDockHeight(height)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    true
                }
                else -> true
            }
        }
    }

    private fun setupTabChipDrag(chip: View, closeView: View, tabView: View) {
        var startY = 0f
        var startHeight = 0
        var dragging = false
        chip.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN &&
                isTouchOnChild(chip, closeView, event)
            ) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startHeight = dockView.layoutParams.height
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.rawY
                    if (!dragging && abs(deltaY) > touchSlop) {
                        dragging = true
                    }
                    if (dragging && !x11Panel.isMaximized()) {
                        val height = (startHeight + deltaY).toInt()
                            .coerceIn(minPanelHeightPx(), maxPanelHeightPx())
                        setDockHeight(height)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        tabView.performClick()
                    }
                    dragging = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    true
                }
                else -> true
            }
        }
    }

    private fun isTouchOnChild(parent: View, child: View, event: MotionEvent): Boolean {
        val location = IntArray(2)
        child.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        return x >= location[0] && x <= location[0] + child.width &&
            y >= location[1] && y <= location[1] + child.height
    }

    private fun defaultPanelHeightPx(): Int = dp(DEFAULT_PANEL_HEIGHT_DP)

    private fun minPanelHeightPx(): Int = dp(MIN_PANEL_HEIGHT_DP)

    private fun maxPanelHeightPx(): Int {
        val screen = activity.resources.displayMetrics.heightPixels
        return (screen - dp(MIN_EDITOR_STRIP_DP)).coerceAtLeast(minPanelHeightPx())
    }

    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    companion object {
        const val STATE_VISIBLE = "editor_dock_visible"
        const val STATE_DOCK_PANEL_VISIBLE = "editor_dock_panel_visible"
        const val STATE_TAB = "editor_dock_tab"
        const val STATE_TERMINAL_OPEN = "editor_dock_terminal_open"
        const val STATE_X11_OPEN = "editor_dock_x11_open"
        private const val DEFAULT_PANEL_HEIGHT_DP = 260
        private const val MIN_PANEL_HEIGHT_DP = 120
        /** Leave a thin editor strip visible when dock is dragged to the top. */
        private const val MIN_EDITOR_STRIP_DP = 48
    }
}
