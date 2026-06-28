package com.termux.zerocore.ai.agent

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class ZtAgentSelectableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        ZtInvertSelectionSpanHelper.applyHighlightStyle(this)
        linksClickable = true
        setLinkTextColor(0xFF4EA1F3.toInt())
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selStart < 0 || selEnd <= selStart) {
            ZtInvertSelectionSpanHelper.clearSelectionSpans(this)
        } else {
            ZtInvertSelectionSpanHelper.onSelectionChanged(this, selStart, selEnd)
        }
    }
}
