package com.termux.zerocore.editor

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.termux.view.TerminalView

class EditorTerminalInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var terminalView: TerminalView? = null
    private var viewClient: EditorTerminalViewClient? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
    }

    fun bindTerminalView(view: TerminalView) {
        terminalView = view
    }

    fun bindViewClient(client: EditorTerminalViewClient) {
        viewClient = client
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val session = terminalView?.currentSession ?: return null
        // 与主界面 TerminalView 一致使用 TYPE_NULL，避免输入法对数字键走 sendKeyEvent 却无法送达终端
        outAttrs.inputType = InputType.TYPE_NULL
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        return EditorTerminalInputConnection(this, session, terminalView, viewClient)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val terminal = terminalView ?: return super.onTouchEvent(event)
        return terminal.onTouchEvent(event)
    }
}
