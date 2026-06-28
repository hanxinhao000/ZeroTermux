package com.termux.zerocore.ai.agent

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import kotlin.math.abs

/**
 * Short tap on links still works; long-press and vertical scroll are left to [TextView] / parent.
 */
object ZtAgentSelectionLinkMovementMethod : LinkMovementMethod() {

    private val tableMovement by lazy { TableAwareMovementMethod.create() }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (tableMovement.onTouchEvent(widget, buffer, event)) {
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            val slop = ViewConfiguration.get(widget.context).scaledTouchSlop
            if (isNearClickableSpan(widget, buffer, event, slop)) {
                return super.onTouchEvent(widget, buffer, event)
            }
        }
        return false
    }

    private fun isNearClickableSpan(
        widget: TextView,
        buffer: Spannable,
        event: MotionEvent,
        slop: Int
    ): Boolean {
        var x = event.x.toInt()
        var y = event.y.toInt()
        x -= widget.totalPaddingLeft
        y -= widget.totalPaddingTop
        x += widget.scrollX
        y += widget.scrollY
        val layout = widget.layout ?: return false
        val line = layout.getLineForVertical(y)
        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
        val links = buffer.getSpans(offset, offset, ClickableSpan::class.java)
        return links.isNotEmpty()
    }
}
