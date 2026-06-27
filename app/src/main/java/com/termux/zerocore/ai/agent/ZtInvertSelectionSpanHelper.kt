package com.termux.zerocore.ai.agent

import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.termux.R

internal object ZtInvertSelectionSpanHelper {

    fun applyHighlightStyle(textView: TextView) {
        textView.highlightColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.setTextSelectHandleLeft(R.drawable.ai_agent_text_select_handle_left)
            textView.setTextSelectHandleRight(R.drawable.ai_agent_text_select_handle_right)
            textView.setTextSelectHandle(R.drawable.ai_agent_text_select_handle_middle)
        }
    }

    fun onSelectionChanged(textView: TextView, selStart: Int, selEnd: Int) {
        val text = textView.text
        if (text !is Spannable) return
        clearSelectionSpans(textView)
        if (selStart < 0 || selEnd <= selStart) return
        val fgColor = ContextCompat.getColor(textView.context, R.color.color_ai_agent_selection_fg)
        val bgColor = ContextCompat.getColor(textView.context, R.color.color_ai_agent_selection_bg)
        val fgSpan = ForegroundColorSpan(fgColor)
        val bgSpan = BackgroundColorSpan(bgColor)
        text.setSpan(fgSpan, selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(bgSpan, selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setTag(R.id.zt_agent_selection_spans, Pair(fgSpan, bgSpan))
    }

    fun clearSelectionSpans(textView: TextView) {
        val text = textView.text
        if (text !is Spannable) return
        @Suppress("UNCHECKED_CAST")
        val spans = textView.getTag(R.id.zt_agent_selection_spans)
            as? Pair<ForegroundColorSpan, BackgroundColorSpan> ?: return
        text.removeSpan(spans.first)
        text.removeSpan(spans.second)
        textView.setTag(R.id.zt_agent_selection_spans, null)
    }
}
