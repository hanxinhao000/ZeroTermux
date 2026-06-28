package com.termux.zerocore.ai.agent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.EditText
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.termux.R
import kotlin.math.abs

/**
 * AI agent panel root: swipe right to dismiss; vertical scroll handled by [ZtAgentPanelScrollView].
 */
class ZtAiAgentPanelCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    interface SwipeDismissCallback {
        fun onSwipeDrag(translationX: Float, panelWidth: Float)
        fun onSwipeRelease(translationX: Float, panelWidth: Float)
    }

    var swipeDismissCallback: SwipeDismissCallback? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downRawX = 0f
    private var downRawY = 0f
    private var startTranslationX = 0f
    private var swipeDragging = false
    private var downOnInput = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (swipeDismissCallback == null) return super.onInterceptTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swipeDragging = false
                downRawX = ev.rawX
                downRawY = ev.rawY
                startTranslationX = translationX
                downOnInput = isTouchOnInput(ev)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeDragging) return true
                if (downOnInput) return false
                val dx = ev.rawX - downRawX
                val dy = ev.rawY - downRawY
                if (abs(dx) < touchSlop && abs(dy) < touchSlop) return false
                if (abs(dy) >= abs(dx)) return false
                if (dx <= touchSlop) return false
                swipeDragging = true
                clearSelectionUnder(ev)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (swipeDragging) return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!swipeDragging) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val next = (startTranslationX + dx).coerceAtLeast(0f)
                translationX = next
                swipeDismissCallback?.onSwipeDrag(next, swipePanelWidth())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                swipeDismissCallback?.onSwipeRelease(translationX, swipePanelWidth())
                swipeDragging = false
                return true
            }
        }
        return true
    }

    private fun swipePanelWidth(): Float {
        return if (width > 0) width.toFloat() else resources.displayMetrics.widthPixels * 0.45f
    }

    private fun isTouchOnInput(ev: MotionEvent): Boolean {
        val target = findViewAt(ev.x, ev.y) ?: return false
        return target is EditText || target.id == R.id.ai_agent_panel_input
    }

    private fun clearSelectionUnder(ev: MotionEvent) {
        val target = findViewAt(ev.x, ev.y)
        if (target is TextView && target.hasSelection()) {
            ZtAgentTextSelectionHelper.clearSelection(target)
        }
    }

    private fun findViewAt(x: Float, y: Float): View? {
        return findViewAtRecursive(this, x, y)
    }

    private fun findViewAtRecursive(group: View, x: Float, y: Float): View? {
        if (x < 0 || y < 0 || x >= group.width || y >= group.height) return null
        if (group !== this && group.isClickable && group.isEnabled) {
            return group
        }
        if (group is android.view.ViewGroup) {
            for (i in group.childCount - 1 downTo 0) {
                val child = group.getChildAt(i)
                if (child.visibility != View.VISIBLE) continue
                val found = findViewAtRecursive(child, x - child.left, y - child.top)
                if (found != null) return found
            }
        }
        return if (group === this) null else group
    }
}
