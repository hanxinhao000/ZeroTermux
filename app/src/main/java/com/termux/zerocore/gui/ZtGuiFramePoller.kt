package com.termux.zerocore.gui

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ZtGuiFramePoller(
    private val onFrame: (width: Int, height: Int, rgb: ByteArray) -> Unit,
    private val onActivity: (() -> Unit)? = null
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val decodeExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ZtGuiFrameDecode").apply { isDaemon = true }
    }
    private val decodeBusy = AtomicBoolean(false)
    private var polling = false
    private var lastModified = 0L
    private var pollToken = 0

    fun start(intervalMs: Long = POLL_MS) {
        stop()
        polling = true
        pollToken++
        schedule(pollToken, intervalMs)
    }

    fun stop() {
        polling = false
        pollToken++
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun hasRecentFrame(maxAgeMs: Long = 2500L): Boolean {
        val file = ZtGuiPaths.frameFile()
        if (!file.isFile || file.length() <= 0) return false
        return System.currentTimeMillis() - file.lastModified() <= maxAgeMs
    }

    private fun schedule(token: Int, intervalMs: Long) {
        mainHandler.postDelayed({
            if (!polling || token != pollToken) return@postDelayed
            pollOnce()
            schedule(token, intervalMs)
        }, intervalMs)
    }

    private fun pollOnce() {
        val file = ZtGuiPaths.frameFile()
        if (!file.isFile || file.length() <= 0) return
        val modified = file.lastModified()
        if (modified == lastModified || !decodeBusy.compareAndSet(false, true)) return
        lastModified = modified
        val path = file.absolutePath
        decodeExecutor.execute {
            try {
                val bytes = File(path).readBytes()
                if (bytes.isEmpty()) return@execute
                val frame = ZtGuiFrameDecoder.decode(bytes) ?: return@execute
                frame.bitmap?.recycle()
                mainHandler.post {
                    decodeBusy.set(false)
                    if (!polling) return@post
                    onActivity?.invoke()
                    onFrame(frame.width, frame.height, frame.rgb)
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "decode frame failed", e)
                mainHandler.post { decodeBusy.set(false) }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "ZtGuiFramePoller"
        private const val POLL_MS = 50L
    }
}
