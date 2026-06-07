package com.termux.zerocore.editor

import com.termux.terminal.TerminalSession

/**
 * Forwards terminal screen updates from [com.termux.app.terminal.TermuxTerminalSessionActivityClient]
 * to the editor's embedded terminal while it is visible.
 */
object EditorTerminalSessionRelay {

    private var listener: ((TerminalSession) -> Unit)? = null

    @JvmStatic
    fun onTextChanged(changedSession: TerminalSession) {
        listener?.invoke(changedSession)
    }

    fun setListener(listener: ((TerminalSession) -> Unit)?) {
        this.listener = listener
    }
}
