package com.termux.zerocore.ai.agent

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.termux.R

class ZtAgentSelectableEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        ZtInvertSelectionSpanHelper.applyHighlightStyle(this)
        applyWhiteCursor()
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false
            override fun onDestroyActionMode(mode: ActionMode) {
                ZtInvertSelectionSpanHelper.clearSelectionSpans(this@ZtAgentSelectableEditText)
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        ZtInvertSelectionSpanHelper.onSelectionChanged(this, selStart, selEnd)
    }

    private fun applyWhiteCursor() {
        val cursor = ContextCompat.getDrawable(context, R.drawable.ai_agent_text_cursor) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable = cursor
        }
    }
}
