package com.termux.zerocore.editor

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.HorizontalScrollView
import kotlin.math.abs

class EditorFileTreeScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : HorizontalScrollView(context, attrs) {

    interface SidebarGestureDelegate {
        fun onSidebarGestureDragStart(startRawX: Float, startRawY: Float, ev: MotionEvent)
        fun onSidebarGestureMove(ev: MotionEvent): Boolean
        fun onSidebarGestureUp(ev: MotionEvent): Boolean
    }

    var sidebarGestureDelegate: SidebarGestureDelegate? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downRawX = 0f
    private var downRawY = 0f
    private var draggingSidebar = false
    private var handlingHorizontalScroll = false

    fun canScrollContentHorizontally(): Boolean {
        val child = getChildAt(0) ?: return false
        return child.width > width + 1
    }

    fun maxScrollX(): Int {
        val child = getChildAt(0) ?: return 0
        return (child.width - width).coerceAtLeast(0)
    }

    fun isScrolledToRightEdge(): Boolean {
        if (!canScrollContentHorizontally()) return true
        return scrollX >= maxScrollX() - touchSlop
    }

    fun canStartSidebarCloseGesture(): Boolean {
        return !canScrollContentHorizontally() || isScrolledToRightEdge()
    }

    fun resetSidebarDragState() {
        draggingSidebar = false
        handlingHorizontalScroll = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = ev.rawX
                downRawY = ev.rawY
                draggingSidebar = false
                handlingHorizontalScroll = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!draggingSidebar) {
                    val dx = ev.rawX - downRawX
                    val dy = ev.rawY - downRawY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        val horizontal = abs(dx) > abs(dy)
                        if (horizontal) {
                            val swipeLeft = dx < 0f
                            val swipeRight = dx > 0f
                            if (swipeLeft && !canStartSidebarCloseGesture()) {
                                handlingHorizontalScroll = true
                                parent?.requestDisallowInterceptTouchEvent(true)
                            } else if (swipeRight && scrollX > 0) {
                                handlingHorizontalScroll = true
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handlingHorizontalScroll = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (draggingSidebar) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = ev.rawX
                downRawY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downRawX
                val dy = ev.rawY - downRawY
                if (abs(dx) <= touchSlop && abs(dy) <= touchSlop) {
                    return false
                }
                if (abs(dy) > abs(dx)) {
                    return false
                }
                if (dx < 0f) {
                    if (!canStartSidebarCloseGesture()) {
                        handlingHorizontalScroll = true
                        return true
                    }
                    startSidebarDrag(ev)
                    return true
                }
                if (dx > 0f && scrollX > 0) {
                    handlingHorizontalScroll = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (draggingSidebar) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    return sidebarGestureDelegate?.onSidebarGestureMove(ev) ?: true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    draggingSidebar = false
                    handlingHorizontalScroll = false
                    return sidebarGestureDelegate?.onSidebarGestureUp(ev) ?: true
                }
            }
        }
        if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
            val dx = ev.rawX - downRawX
            val dy = ev.rawY - downRawY
            if (dx < -touchSlop && abs(dx) > abs(dy) && canStartSidebarCloseGesture()) {
                startSidebarDrag(ev)
                return sidebarGestureDelegate?.onSidebarGestureMove(ev) ?: true
            }
        }
        val handled = super.onTouchEvent(ev)
        if (handlingHorizontalScroll || (ev.actionMasked == MotionEvent.ACTION_MOVE && scrollX in 1 until maxScrollX())) {
            return true
        }
        return handled
    }

    private fun startSidebarDrag(ev: MotionEvent) {
        draggingSidebar = true
        handlingHorizontalScroll = false
        sidebarGestureDelegate?.onSidebarGestureDragStart(downRawX, downRawY, ev)
    }
}
