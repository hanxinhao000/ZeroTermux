package com.termux.zerocore.gui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * 独立 GUI 视图：显示 Xvfb 截图，支持拖动、双指缩放、点击。
 */
class ZtGuiSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.MATRIX
    }
    private val drawMatrix = Matrix()
    private var bitmapWidth = 1
    private var bitmapHeight = 1
    private var desktopWidth = 800
    private var desktopHeight = 600
    private var scaleFactor = 1f
    private var layoutApplied = false
    private var dragging = false
    private var lastMoveMs = 0L
    private var lastMoveX = -1
    private var lastMoveY = -1

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(MIN_SCALE, MAX_SCALE)
            drawMatrix.postScale(detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY)
            imageView.imageMatrix = drawMatrix
            layoutApplied = true
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (scaleFactor <= 1.01f) return false
            drawMatrix.postTranslate(-distanceX, -distanceY)
            imageView.imageMatrix = drawMatrix
            layoutApplied = true
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            mapToDesktop(e.x, e.y)?.let { (x, y) ->
                ZtGuiInputWriter.sendClick(x, y)
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            dragging = true
            return true
        }
    })

    init {
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setDesktopSize(width: Int, height: Int) {
        desktopWidth = width.coerceAtLeast(1)
        desktopHeight = height.coerceAtLeast(1)
    }

    fun setFrame(bitmap: Bitmap) {
        val sizeChanged = bitmap.width != bitmapWidth || bitmap.height != bitmapHeight
        bitmapWidth = bitmap.width.coerceAtLeast(1)
        bitmapHeight = bitmap.height.coerceAtLeast(1)

        val previous = imageView.drawable
        imageView.setImageBitmap(bitmap)
        if (previous is android.graphics.drawable.BitmapDrawable) {
            val oldBitmap = previous.bitmap
            if (oldBitmap != null && oldBitmap !== bitmap && !oldBitmap.isRecycled) {
                oldBitmap.recycle()
            }
        }

        if (sizeChanged && scaleFactor <= 1.01f) {
            fitCenter()
            layoutApplied = true
        } else if (!layoutApplied && scaleFactor <= 1.01f) {
            fitCenter()
            layoutApplied = true
        }
    }

    fun resetTransform() {
        scaleFactor = 1f
        layoutApplied = false
        fitCenter()
    }

    private fun fitCenter() {
        drawMatrix.reset()
        val viewW = width.toFloat().coerceAtLeast(1f)
        val viewH = height.toFloat().coerceAtLeast(1f)
        val scale = minOf(viewW / bitmapWidth, viewH / bitmapHeight)
        val dx = (viewW - bitmapWidth * scale) / 2f
        val dy = (viewH - bitmapHeight * scale) / 2f
        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)
        scaleFactor = scale
        imageView.imageMatrix = drawMatrix
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (imageView.drawable != null && scaleFactor <= 1.01f) {
            layoutApplied = false
            fitCenter()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (dragging && event.pointerCount == 1 && scaleFactor <= 1.01f) {
                    mapToDesktop(event.x, event.y)?.let { (x, y) ->
                        val now = SystemClock.uptimeMillis()
                        if (now - lastMoveMs >= MOVE_INTERVAL_MS || x != lastMoveX || y != lastMoveY) {
                            lastMoveMs = now
                            lastMoveX = x
                            lastMoveY = y
                            ZtGuiInputWriter.sendMove(x, y)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    private fun mapToDesktop(viewX: Float, viewY: Float): Pair<Int, Int>? {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return null
        val inverse = Matrix()
        if (!drawMatrix.invert(inverse)) return null
        val pts = floatArrayOf(viewX, viewY)
        inverse.mapPoints(pts)
        val scaleX = desktopWidth.toFloat() / bitmapWidth
        val scaleY = desktopHeight.toFloat() / bitmapHeight
        val x = (pts[0] * scaleX).toInt().coerceIn(0, desktopWidth - 1)
        val y = (pts[1] * scaleY).toInt().coerceIn(0, desktopHeight - 1)
        return x to y
    }

    companion object {
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 4f
        private const val MOVE_INTERVAL_MS = 50L
    }
}
