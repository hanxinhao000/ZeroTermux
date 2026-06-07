package com.termux.zerocore.editor

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class EditorTerminalInputConnection(
    hostView: View,
    private val session: TerminalSession,
    private val terminalView: TerminalView?
) : BaseInputConnection(hostView, true) {

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        super.commitText(text, newCursorPosition)
        if (text != null) {
            sendText(text)
        }
        clearEditable()
        return true
    }

    override fun finishComposingText(): Boolean {
        super.finishComposingText()
        getEditable()?.let { buffer ->
            sendText(buffer)
            buffer.clear()
        }
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        sendBackspaces(beforeLength)
        return super.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        sendBackspaces(beforeLength)
        return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && handleKeyCode(event.keyCode)) {
            return true
        }
        return super.sendKeyEvent(event)
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        session.write("\r")
        invalidateTerminal()
        return true
    }

    private fun sendText(text: CharSequence) {
        if (text.isEmpty()) return
        val builder = StringBuilder(text.length)
        for (i in text.indices) {
            val ch = text[i]
            builder.append(if (ch == '\n') '\r' else ch)
        }
        session.write(builder.toString())
        invalidateTerminal()
    }

    private fun sendBackspaces(count: Int) {
        if (count <= 0) return
        repeat(count) {
            session.write("\u007f")
        }
        invalidateTerminal()
    }

    private fun handleKeyCode(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                session.write("\r")
                invalidateTerminal()
                true
            }
            KeyEvent.KEYCODE_DEL -> {
                session.write("\u007f")
                invalidateTerminal()
                true
            }
            else -> false
        }
    }

    private fun invalidateTerminal() {
        terminalView?.post { terminalView?.onScreenUpdated() }
    }

    private fun clearEditable() {
        getEditable()?.clear()
    }
}
