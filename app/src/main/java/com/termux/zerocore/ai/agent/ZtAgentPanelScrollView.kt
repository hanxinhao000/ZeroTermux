package com.termux.zerocore.ai.agent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

/**
 * Message list scroll: vertical drags scroll even over selectable Markdown TextViews.
 * Clears text selection when the user scrolls vertically.
 */
class ZtAgentPanelScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - downX)
                val dy = abs(ev.y - downY)
                if (dy > touchSlop && dy > dx) {
                    clearChildTextSelections(this)
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun clearChildTextSelections(root: View) {
        if (root is TextView && root.hasSelection()) {
            ZtAgentTextSelectionHelper.clearSelection(root)
        }
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                clearChildTextSelections(root.getChildAt(i))
            }
        }
    }
}
