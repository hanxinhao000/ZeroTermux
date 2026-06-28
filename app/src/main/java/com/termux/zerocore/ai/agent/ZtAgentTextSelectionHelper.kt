package com.termux.zerocore.ai.agent

import android.text.Selection
import android.text.Spannable
import android.widget.TextView

internal object ZtAgentTextSelectionHelper {

    fun clearSelection(textView: TextView) {
        val text = textView.text
        if (text is Spannable) {
            Selection.removeSelection(text)
        }
        textView.clearFocus()
    }
}
