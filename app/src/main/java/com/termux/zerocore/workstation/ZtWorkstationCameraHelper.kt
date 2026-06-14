package com.termux.zerocore.workstation

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class ZtWorkstationCameraHelper(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var handlerThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private val sessions = HashMap<String, CameraSession>()
    private val frameCache = HashMap<String, ByteArray>()
    private val cameraLock = Any()

    @Volatile
    private var enabled = false

    data class CameraSession(
        val cameraId: String,
        val facing: String,
        var device: CameraDevice? = null,
        var imageReader: ImageReader? = null,
        var captureSession: CameraCaptureSession? = null,
        @Volatile var latestJpeg: ByteArray? = null,
        @Volatile var lastEncodeTimeMs: Long = 0,
        @Volatile var closing: Boolean = false,
        val openLock: Semaphore = Semaphore(1)
    )

    fun isEnabled(): Boolean = enabled

    fun startCameras() {
        enabled = true
        ensureHandler()
    }

    fun ensureHandler() {
        if (handlerThread != null) return
        synchronized(cameraLock) {
            if (handlerThread != null) return
            handlerThread = HandlerThread("ZtWorkstationCamera").also { it.start() }
            cameraHandler = Handler(handlerThread!!.looper)
        }
    }

    fun releaseCameras() {
        enabled = false
        synchronized(cameraLock) {
            frameCache.clear()
        }
        val snapshot = synchronized(cameraLock) {
            sessions.values.toList()
        }
        snapshot.forEach { session -> closeSession(session) }
    }

    fun stop() {
        enabled = false
        val snapshot = synchronized(cameraLock) {
            sessions.values.toList()
        }
        snapshot.forEach { session -> closeSession(session) }
        synchronized(cameraLock) {
            sessions.clear()
            handlerThread?.quitSafely()
            handlerThread = null
            cameraHandler = null
        }
    }

    fun getFrame(facing: String): ByteArray? {
        if (!enabled) return null
        ensureHandler()
        val normalized = normalizeFacing(facing)
        synchronized(cameraLock) {
            findCameraId(normalized)?.let { id -> sessions[id] }?.let { session ->
                if (!session.closing && session.captureSession != null) {
                    session.latestJpeg?.takeIf { it.isNotEmpty() }?.let { return it }
                    frameCache[normalized]?.let { return it }
                }
            }
        }
        val cached = synchronized(cameraLock) { frameCache[normalized] }
        activateFacing(normalized)
        var waitMs = 0
        while (waitMs < 800) {
            if (!enabled) return cached
            val jpeg = synchronized(cameraLock) {
                findCameraId(normalized)?.let { id -> sessions[id]?.latestJpeg }
            }
            if (jpeg != null && jpeg.isNotEmpty()) {
                return jpeg
            }
            try {
                Thread.sleep(40)
            } catch (_: InterruptedException) {
                break
            }
            waitMs += 40
        }
        return synchronized(cameraLock) {
            findCameraId(normalized)?.let { id -> sessions[id]?.latestJpeg }
                ?: frameCache[normalized]
        } ?: cached
    }

    private fun activateFacing(facing: String) {
        val handler = cameraHandler ?: return
        if (Looper.myLooper() == handler.looper) {
            activateFacingInternal(facing)
            return
        }
        val latch = CountDownLatch(1)
        handler.post {
            try {
                activateFacingInternal(facing)
            } finally {
                latch.countDown()
            }
        }
        try {
            latch.await(5, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
        }
    }

    private fun activateFacingInternal(facing: String) {
        synchronized(cameraLock) {
            if (!enabled) return
            closeOtherSessionsInternal(facing)
            ensureSessionOpenInternal(facing)
        }
    }

    private fun normalizeFacing(facing: String): String {
        return if (facing.equals("front", ignoreCase = true)) "front" else "back"
    }

    private fun closeOtherSessionsInternal(activeFacing: String) {
        sessions.values
            .filter { session -> session.facing != activeFacing }
            .toList()
            .forEach { session -> closeSessionInternal(session) }
    }

    private fun ensureSessionOpenInternal(facing: String): CameraSession? {
        if (!enabled) return null
        val cameraId = findCameraId(facing) ?: return null
        sessions[cameraId]?.let { existing ->
            if (existing.closing) return null
            if (existing.device != null && existing.captureSession != null) return existing
            if (existing.device == null) return existing
        }
        openCameraInternal(cameraId, facing)
        return sessions[cameraId]
    }

    private fun findCameraId(facing: String): String? {
        val lensFacing = if (facing == "front") {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == lensFacing
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun choosePreviewSize(characteristics: CameraCharacteristics): Size {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
        if (sizes.isNullOrEmpty()) return Size(480, 270)
        val targetPixels = 480L * 270L
        return sizes
            .filter { it.width in 240..640 && it.height in 180..480 }
            .minByOrNull { size ->
                val pixels = size.width.toLong() * size.height
                kotlin.math.abs(pixels - targetPixels)
            }
            ?: sizes.minByOrNull { it.width.toLong() * it.height }
            ?: Size(480, 270)
    }

    private fun openCameraInternal(cameraId: String, facing: String) {
        val handler = cameraHandler ?: return
        val existing = sessions[cameraId]
        if (existing != null && !existing.closing && existing.device != null) return
        if (existing != null && !existing.closing && existing.device == null) return
        val session = CameraSession(cameraId, facing)
        sessions[cameraId] = session
        try {
            if (!session.openLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) return
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val previewSize = choosePreviewSize(characteristics)
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (!isSessionActive(session)) {
                        session.openLock.release()
                        safeCloseDevice(camera)
                        return
                    }
                    session.device = camera
                    session.openLock.release()
                    try {
                        startPreview(session, previewSize)
                    } catch (_: CameraAccessException) {
                        cleanupSessionAfterDisconnect(session)
                    } catch (_: Exception) {
                        cleanupSessionAfterDisconnect(session)
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    session.openLock.release()
                    cleanupSessionAfterDisconnect(session)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    session.openLock.release()
                    cleanupSessionAfterDisconnect(session)
                }
            }, handler)
        } catch (_: SecurityException) {
            session.openLock.release()
            removeSession(session)
        } catch (_: CameraAccessException) {
            session.openLock.release()
            removeSession(session)
        } catch (_: Exception) {
            session.openLock.release()
            removeSession(session)
        }
    }

    private fun isSessionActive(session: CameraSession): Boolean {
        synchronized(cameraLock) {
            return enabled &&
                !session.closing &&
                sessions[session.cameraId] === session
        }
    }

    private fun removeSession(session: CameraSession) {
        synchronized(cameraLock) {
            if (sessions[session.cameraId] === session) {
                sessions.remove(session.cameraId)
            }
        }
    }

    private fun startPreview(session: CameraSession, size: Size) {
        val device = session.device ?: return
        val handler = cameraHandler ?: return
        if (!isSessionActive(session)) return
        val reader = ImageReader.newInstance(
            size.width,
            size.height,
            ImageFormat.YUV_420_888,
            2
        )
        if (!isSessionActive(session)) {
            reader.close()
            return
        }
        session.imageReader = reader
        reader.setOnImageAvailableListener({ imgReader ->
            if (!enabled || session.closing) return@setOnImageAvailableListener
            val image = imgReader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val now = System.currentTimeMillis()
                if (now - session.lastEncodeTimeMs < 45) return@setOnImageAvailableListener
                val jpeg = yuvImageToJpeg(image) ?: return@setOnImageAvailableListener
                if (jpeg.isEmpty()) return@setOnImageAvailableListener
                session.lastEncodeTimeMs = now
                session.latestJpeg = jpeg
                synchronized(cameraLock) {
                    frameCache[session.facing] = jpeg
                }
            } finally {
                image.close()
            }
        }, handler)

        val surfaces = listOf<Surface>(reader.surface)
        try {
            device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) {
                    if (!isSessionActive(session)) {
                        safeCloseCaptureSession(captureSession)
                        return
                    }
                    session.captureSession = captureSession
                    try {
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(reader.surface)
                            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST)
                        }.build()
                        captureSession.setRepeatingRequest(request, null, handler)
                    } catch (_: Exception) {
                        closeSession(session)
                    }
                }

                override fun onConfigureFailed(captureSession: CameraCaptureSession) {
                    safeCloseCaptureSession(captureSession)
                    closeSession(session)
                }
            }, handler)
        } catch (_: CameraAccessException) {
            reader.close()
            session.imageReader = null
            cleanupSessionAfterDisconnect(session)
        } catch (_: Exception) {
            reader.close()
            session.imageReader = null
            cleanupSessionAfterDisconnect(session)
        }
    }

    private fun yuvImageToJpeg(image: Image): ByteArray? {
        if (image.format != ImageFormat.YUV_420_888) return null
        val width = image.width
        val height = image.height
        val nv21 = yuv420888ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val stream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 50, stream)
        return stream.toByteArray()
    }

    private fun yuv420888ToNv21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val nv21 = ByteArray(ySize + ySize / 2)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        copyYPlaneToBuffer(
            yPlane.buffer,
            yPlane.rowStride,
            yPlane.pixelStride,
            width,
            height,
            nv21,
            0
        )

        val chromaWidth = width / 2
        val chromaHeight = height / 2
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        var offset = ySize
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                nv21[offset++] = vBuffer.get(vIndex)
                nv21[offset++] = uBuffer.get(uIndex)
            }
        }
        return nv21
    }

    private fun copyYPlaneToBuffer(
        buffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int
    ) {
        var output = outOffset
        if (pixelStride == 1 && rowStride == width) {
            buffer.position(0)
            buffer.get(out, output, width * height)
            return
        }
        for (row in 0 until height) {
            var input = row * rowStride
            for (col in 0 until width) {
                out[output++] = buffer.get(input)
                input += pixelStride
            }
        }
    }

    private fun closeSession(session: CameraSession) {
        val handler = cameraHandler
        if (handler != null && Looper.myLooper() != handler.looper) {
            val latch = CountDownLatch(1)
            handler.post {
                try {
                    closeSessionInternal(session)
                } finally {
                    latch.countDown()
                }
            }
            try {
                latch.await(3, TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
            }
        } else {
            closeSessionInternal(session)
        }
    }

    private fun closeSessionInternal(session: CameraSession) {
        if (session.closing) return
        session.closing = true
        session.latestJpeg = null

        val captureSession = session.captureSession
        val imageReader = session.imageReader
        val device = session.device
        session.captureSession = null
        session.imageReader = null
        session.device = null
        removeSession(session)

        safeStopCaptureSession(captureSession)
        safeCloseCaptureSession(captureSession)
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        safeCloseDevice(device)
    }

    private fun cleanupSessionAfterDisconnect(session: CameraSession) {
        session.closing = true
        session.latestJpeg = null
        val captureSession = session.captureSession
        val imageReader = session.imageReader
        val device = session.device
        session.captureSession = null
        session.imageReader = null
        session.device = null
        removeSession(session)
        safeStopCaptureSession(captureSession)
        safeCloseCaptureSession(captureSession)
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        safeCloseDevice(device)
    }

    private fun safeStopCaptureSession(captureSession: CameraCaptureSession?) {
        try {
            captureSession?.stopRepeating()
        } catch (_: Exception) {
        }
        try {
            captureSession?.abortCaptures()
        } catch (_: Exception) {
        }
    }

    private fun safeCloseCaptureSession(captureSession: CameraCaptureSession?) {
        try {
            captureSession?.close()
        } catch (_: Exception) {
        }
    }

    private fun safeCloseDevice(camera: CameraDevice?) {
        if (camera == null) return
        try {
            camera.close()
        } catch (_: Exception) {
        }
    }
}
