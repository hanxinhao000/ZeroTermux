package com.termux.zerocore.editor

import android.app.Activity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.shared.termux.extrakeys.SpecialButton
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties
import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase
import com.termux.shared.view.KeyboardUtils
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class EditorTerminalViewClient(
    private val activity: Activity,
    private val terminalView: TerminalView,
    private val inputView: EditorTerminalInputView,
    private val extraKeysView: ExtraKeysView,
    private val isPanelVisible: () -> Boolean,
    private val onRequestEditorBlur: () -> Unit
) : TermuxTerminalViewClientBase() {

    private val showSoftKeyboardRunnable = Runnable {
        KeyboardUtils.showSoftKeyboard(activity, inputView)
        restartTerminalInput()
    }

    fun onCreate() {
        KeyboardUtils.setSoftInputModeAdjustResize(activity)
        inputView.setOnFocusChangeListener { _, hasFocus ->
            if (!isPanelVisible()) return@setOnFocusChangeListener
            if (hasFocus) {
                onRequestEditorBlur()
                restartTerminalInput()
            } else {
                inputView.removeCallbacks(showSoftKeyboardRunnable)
                KeyboardUtils.hideSoftKeyboard(activity, inputView)
            }
        }
    }

    fun showKeyboardForInput() {
        if (!isPanelVisible()) return
        onRequestEditorBlur()
        inputView.requestFocus()
        if (!KeyboardUtils.areDisableSoftKeyboardFlagsSet(activity)) {
            inputView.removeCallbacks(showSoftKeyboardRunnable)
            inputView.postDelayed(showSoftKeyboardRunnable, 100)
        } else {
            restartTerminalInput()
        }
    }

    override fun onSingleTapUp(e: MotionEvent) {
        if (!isPanelVisible()) return
        if (terminalView.isSelectingText) return
        val session = terminalView.currentSession ?: return
        val term = session.emulator ?: return
        if (term.isMouseTrackingActive || e.isFromSource(InputDevice.SOURCE_MOUSE)) return
        showKeyboardForInput()
    }

    override fun isTerminalViewSelected(): Boolean {
        if (!isPanelVisible()) return false
        if (terminalView.isSelectingText) return true
        return inputView.hasFocus()
    }

    override fun copyModeChanged(copyMode: Boolean) {
        if (!isPanelVisible()) return
        if (copyMode) {
            inputView.removeCallbacks(showSoftKeyboardRunnable)
            KeyboardUtils.hideSoftKeyboard(activity, inputView)
            inputView.clearFocus()
        }
    }

    override fun onLongPress(event: MotionEvent): Boolean {
        return false
    }

    override fun shouldEnforceCharBasedInput(): Boolean {
        return false
    }

    override fun shouldUseCtrlSpaceWorkaround(): Boolean {
        return TermuxAppSharedProperties.getProperties()?.isUsingCtrlSpaceWorkaround == true
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean {
        return TermuxAppSharedProperties.getProperties()?.isBackKeyTheEscapeKey == true
    }

    override fun readControlKey(): Boolean {
        return readExtraKeysSpecialButton(SpecialButton.CTRL)
    }

    override fun readAltKey(): Boolean {
        return readExtraKeysSpecialButton(SpecialButton.ALT)
    }

    override fun readShiftKey(): Boolean {
        return readExtraKeysSpecialButton(SpecialButton.SHIFT)
    }

    override fun readFnKey(): Boolean {
        return readExtraKeysSpecialButton(SpecialButton.FN)
    }

    private fun readExtraKeysSpecialButton(specialButton: SpecialButton): Boolean {
        if (!isPanelVisible()) return false
        return extraKeysView.readSpecialButton(specialButton, true) == true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent, session: TerminalSession): Boolean {
        // 与主 Termux 一致：交给 TerminalView 处理，以便 CTRL/ALT 修饰键与方向键等生效
        return false
    }

    private fun restartTerminalInput() {
        val imm = activity.getSystemService(InputMethodManager::class.java) ?: return
        imm.restartInput(inputView)
    }
}
