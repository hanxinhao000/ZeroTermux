package com.termux.zerocore.gui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.hypot

/**
 * GPU（OpenGL ES）显示 Xvfb 镜像，支持拖动、双指缩放、点击。
 */
class ZtGuiGlSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = ZtGuiGlRenderer()
    private var bitmapWidth = 1
    private var bitmapHeight = 1
    private var desktopWidth = 800
    private var desktopHeight = 600
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var panning = false
    private var downX = 0f
    private var downY = 0f
    private var lastPanX = 0f
    private var lastPanY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            panning = false
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            syncRendererTransform()
            requestRender()
            return true
        }
    })

    init {
        isClickable = true
        isFocusable = true
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        preserveEGLContextOnPause = true
    }

    fun setDesktopSize(width: Int, height: Int) {
        desktopWidth = width.coerceAtLeast(1)
        desktopHeight = height.coerceAtLeast(1)
    }

    fun setFrame(width: Int, height: Int, rgb: ByteArray) {
        bitmapWidth = width.coerceAtLeast(1)
        bitmapHeight = height.coerceAtLeast(1)
        queueEvent {
            renderer.setFrame(width, height, rgb)
        }
        requestRender()
    }

    fun resetTransform() {
        scaleFactor = 1f
        translateX = 0f
        translateY = 0f
        renderer.resetTransform()
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) {
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastPanX = event.x
                lastPanY = event.y
                panning = scaleFactor > 1.01f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (panning && event.pointerCount == 1) {
                    translateX += event.x - lastPanX
                    translateY += event.y - lastPanY
                    lastPanX = event.x
                    lastPanY = event.y
                    syncRendererTransform()
                    requestRender()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!scaleDetector.isInProgress && hypot(dx, dy) < TAP_SLOP_PX) {
                    mapToDesktop(event.x, event.y)?.let { (x, y) ->
                        ZtGuiInputWriter.sendClick(x, y)
                    }
                }
                panning = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                panning = false
                return true
            }
        }
        return true
    }

    private fun syncRendererTransform() {
        renderer.userScale = scaleFactor
        renderer.userTranslateX = translateX
        renderer.userTranslateY = translateY
    }

    private fun mapToDesktop(viewX: Float, viewY: Float): Pair<Int, Int>? {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || width <= 0 || height <= 0) return null
        val baseScale = minOf(width.toFloat() / bitmapWidth, height.toFloat() / bitmapHeight)
        val fit = baseScale * scaleFactor
        val quadW = bitmapWidth * fit
        val quadH = bitmapHeight * fit
        val left = (width - quadW) / 2f + translateX
        val top = (height - quadH) / 2f + translateY
        val localX = (viewX - left) / fit
        val localY = (viewY - top) / fit
        if (localX < 0 || localY < 0 || localX >= bitmapWidth || localY >= bitmapHeight) return null
        val scaleX = desktopWidth.toFloat() / bitmapWidth
        val scaleY = desktopHeight.toFloat() / bitmapHeight
        val x = (localX * scaleX).toInt().coerceIn(0, desktopWidth - 1)
        val y = (localY * scaleY).toInt().coerceIn(0, desktopHeight - 1)
        return x to y
    }

    companion object {
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 4f
        private const val TAP_SLOP_PX = 24f
    }
}
