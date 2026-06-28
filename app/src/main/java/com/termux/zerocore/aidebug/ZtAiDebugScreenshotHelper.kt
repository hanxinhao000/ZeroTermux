package com.termux.zerocore.aidebug

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ZtAiDebugScreenshotHelper {

    fun capturePng(context: Context? = null, source: String = "auto"): ByteArray? {
        val preferRoot = source == "root" || (source == "auto" && context != null &&
            ZtAiDebugRootHelper.isRootModeEnabled() && ZtAiDebugRootHelper.isRootAvailable(context))
        if (preferRoot && context != null) {
            ZtAiDebugRootHelper.screencapPng(context)?.let { return it }
            if (source == "root") return null
        }
        return captureAppPng()
    }

    fun captureAppPng(): ByteArray? {
        val activity = ZtAiDebugForegroundActivity.current() ?: return null
        val latch = CountDownLatch(1)
        var bytes: ByteArray? = null
        activity.runOnUiThread {
            bytes = captureOnUiThread(activity)
            latch.countDown()
        }
        latch.await(8, TimeUnit.SECONDS)
        return bytes
    }

    private fun captureOnUiThread(activity: Activity): ByteArray? {
        return try {
            val root = activity.window?.decorView?.rootView ?: return null
            val w = root.width
            val h = root.height
            if (w <= 0 || h <= 0) return null
            val bitmap = captureComposited(activity, root, w, h) ?: return null
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, stream)
                stream.toByteArray()
            }.also { bitmap.recycle() }
        } catch (_: Exception) {
            null
        }
    }

    private fun captureComposited(activity: Activity, root: View, w: Int, h: Int): Bitmap? {
        val base = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(base))
        val gl = findGlSurfaceView(root)
        if (gl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            gl.width > 0 && gl.height > 0
        ) {
            val glBitmap = Bitmap.createBitmap(gl.width, gl.height, Bitmap.Config.ARGB_8888)
            if (pixelCopySurface(gl, glBitmap)) {
                val canvas = Canvas(base)
                val rootLoc = IntArray(2)
                val glLoc = IntArray(2)
                root.getLocationOnScreen(rootLoc)
                gl.getLocationOnScreen(glLoc)
                canvas.drawBitmap(
                    glBitmap,
                    (glLoc[0] - rootLoc[0]).toFloat(),
                    (glLoc[1] - rootLoc[1]).toFloat(),
                    null
                )
            }
            glBitmap.recycle()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pixelCopyWindow(activity, w, h)?.let { windowBmp ->
                base.recycle()
                return windowBmp
            }
        }
        return base
    }

    private fun pixelCopyWindow(activity: Activity, w: Int, h: Int): Bitmap? {
        val window = activity.window ?: return null
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var ok = false
        val callbackThread = HandlerThread("zt-pixelcopy").apply { start() }
        try {
            PixelCopy.request(
                window,
                bitmap,
                { result ->
                    ok = result == PixelCopy.SUCCESS
                    latch.countDown()
                },
                Handler(callbackThread.looper)
            )
            latch.await(5, TimeUnit.SECONDS)
        } finally {
            callbackThread.quitSafely()
        }
        return if (ok) bitmap else {
            bitmap.recycle()
            null
        }
    }

    private fun pixelCopySurface(surfaceView: GLSurfaceView, bitmap: Bitmap): Boolean {
        val latch = CountDownLatch(1)
        var ok = false
        val callbackThread = HandlerThread("zt-pixelcopy-gl").apply { start() }
        try {
            PixelCopy.request(
                surfaceView,
                bitmap,
                { result ->
                    ok = result == PixelCopy.SUCCESS
                    latch.countDown()
                },
                Handler(callbackThread.looper)
            )
            latch.await(5, TimeUnit.SECONDS)
        } finally {
            callbackThread.quitSafely()
        }
        return ok
    }

    private fun findGlSurfaceView(root: View): GLSurfaceView? {
        if (root is GLSurfaceView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findGlSurfaceView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
