package com.termux.zerocore.editor

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class EditorTerminalInputConnection(
    hostView: View,
    private val session: TerminalSession,
    private val terminalView: TerminalView?,
    private val viewClient: EditorTerminalViewClient?
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
        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (handleKeyCode(event.keyCode)) return true
        val unicode = event.getUnicodeChar(0)
        if (unicode != 0) {
            sendCodePoint(unicode, event.isCtrlPressed(), event.isAltPressed())
            return true
        }
        return terminalView?.dispatchKeyEvent(event) == true
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        session.write("\r")
        invalidateTerminal()
        return true
    }

    private fun sendText(text: CharSequence) {
        if (text.isEmpty()) return
        text.codePoints().forEach { codePoint ->
            sendCodePoint(if (codePoint == '\n'.code) '\r'.code else codePoint, false, false)
        }
    }

    private fun sendCodePoint(codePoint: Int, ctrlFromEvent: Boolean, altFromEvent: Boolean) {
        val terminal = terminalView
        val client = viewClient
        if (terminal != null && client != null) {
            terminal.inputCodePoint(
                TerminalView.KEY_EVENT_SOURCE_SOFT_KEYBOARD,
                codePoint,
                ctrlFromEvent || client.readControlKey(),
                altFromEvent || client.readAltKey()
            )
        } else {
            session.write(String(Character.toChars(codePoint)))
        }
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
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                sendCodePoint(keyCode - KeyEvent.KEYCODE_0 + '0'.code, false, false)
                true
            }
            in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                sendCodePoint(keyCode - KeyEvent.KEYCODE_NUMPAD_0 + '0'.code, false, false)
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
