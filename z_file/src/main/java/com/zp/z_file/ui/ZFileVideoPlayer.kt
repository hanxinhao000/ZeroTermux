package com.zp.z_file.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import com.zp.z_file.content.ZFileException
import com.zp.z_file.content.isNull
import com.zp.z_file.util.ZFileLog

/**
 * 该处的实现请查看 JZVideoPlayer ，VideoView
 * 只是单纯的实现一个视频播放功能，如需手势功能，进度条拖拽可直接使用 JZVideoPlayer
 */
open class ZFileVideoPlayer : TextureView, TextureView.SurfaceTextureListener {

    private var player: MediaPlayer? = null

    /**
     * 视频地址
     */
    var videoPath = ""

    /**
     * assets 目录下的视频名称
     */
    var assetsVideoName = ""

    /**
     * 视频播放完成 是否继续 重复播放
     */
    var completionAutoPlayer = true

    /**
     * 视频裁切类型
     */
    var sizeType = CENTER_MODE

    /** 视频准备完毕 */
    var videoPrepared: (MediaPlayer?.() -> Unit)? = null
    /** 视频播放完成 */
    var videoCompletion: (MediaPlayer?.() -> Unit)? = null
    /** 视频播放错误 */
    var videoPlayError: (MediaPlayer?.() -> Unit)? = null

    protected var videoWidth = 0
    protected var videoHeight = 0

    protected var leftVolume = 1f
    protected var rightVolume = 1f

    protected var playState = IS_INIT

    companion object {
        /** 中心裁剪模式 */
        const val CENTER_CROP_MODE = 0x10001
        /** 中心填充模式 */
        const val CENTER_MODE = 0x10002

        const val IS_INIT = 0
        const val IS_PLAYING = 1
        const val IS_PAUSE = 2
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, 0)
    constructor(context: Context, attributes: AttributeSet?, defStyleAttr: Int) : super(context, attributes, defStyleAttr) {
        player = MediaPlayer()
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        player?.setOnPreparedListener {
            ZFileLog.i("媒体装载完成")
            setVolume()
            videoPrepared?.invoke(it)
        }
        player?.setOnBufferingUpdateListener { _, percent ->
            ZFileLog.i("缓存中：$percent")
        }
        player?.setOnCompletionListener {
            ZFileLog.i("播放完成")
            if (completionAutoPlayer) play()
            videoCompletion?.invoke(it)
        }
        player?.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
            this.videoWidth = videoWidth
            this.videoHeight = videoHeight
            checkViewSizeByMode()
        }
        player?.setOnErrorListener { _, _, _ ->
            ZFileLog.e("播放失败")
            false
        }
        val s = Surface(surface)
        player?.setSurface(s)
        ZFileLog.i("播放器初始化....")
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        player?.pause()
        player?.stop()
        player?.release()
        player = null
        videoPrepared = null
        videoCompletion = null
        videoPlayError = null
        ZFileLog.i("播放器被销毁...")
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun getPlayer() = player

    open fun setVolume(leftVolume: Float = 1f, rightVolume: Float = 1f) {
        this.leftVolume = leftVolume
        this.rightVolume = rightVolume
        player?.setVolume(leftVolume, rightVolume)
    }

    /**
     * 播放或暂停后播放
     */
    open fun play() {
        if (player == null) {
            ZFileLog.e("player is null")
            return
        }
        if (videoPath.isNull() && assetsVideoName.isNull()) {
            throw ZFileException("videoPath is Null or assetsVideoName is Null")
        }
        if (!videoPath.isNull() && !assetsVideoName.isNull()) {
            throw ZFileException("videoPath or assetsVideoName, You can only choose one")
        }
        if (isPlaying()) {
            ZFileLog.i("视频正在播放...")
            return
        }
        try {
            if (isPause()) {
                player?.start()
            } else {
                player?.reset()
                setVideoSource()
                player?.prepare()
                player?.start()
            }
            playState = IS_PLAYING
        } catch (e: Exception) {
            e.printStackTrace()
            ZFileLog.e("播放失败！ videoPath --->>> $videoPath <<<===>>> videoName --->>> $assetsVideoName")
            videoPlayError?.invoke(player)
        }
    }

    private fun setVideoSource() {
        if (!videoPath.isNull()) player?.setDataSource(videoPath)
        if (!assetsVideoName.isNull()) {
            val fd = context.assets.openFd(assetsVideoName)
            player?.setDataSource(fd.fileDescriptor, fd.startOffset, fd.declaredLength)
        }
    }

    /**
     * 暂停
     */
    open fun pause() {
        if (player == null) return
        if (isPlaying()) {
            player?.pause()
            playState = IS_PAUSE
        }
    }

    open fun isPlaying() = player?.isPlaying ?: false
    open fun isPause() = playState == IS_PAUSE

    open fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
        requestLayout()
    }

    open fun checkViewSizeByMode() {
        when (sizeType) {
            CENTER_MODE -> sizeCenter()
            CENTER_CROP_MODE -> sizeCenterCrop()
            else -> throw ZFileException("sizeType error")
        }
    }

    open fun sizeCenterCrop() {
        val sx = width.toFloat() / videoWidth.toFloat()
        val sy = height.toFloat() / videoHeight.toFloat()
        val matrix = Matrix()
        val maxScale = sx.coerceAtLeast(sy)
        matrix.preTranslate((width - videoWidth) / 2f, (height - videoHeight) / 2f)
        matrix.preScale(videoWidth / width.toFloat(), videoHeight / height.toFloat())
        matrix.postScale(maxScale, maxScale, width / 2f, height / 2f)
        setTransform(matrix)
        postInvalidate()
    }

    open fun sizeCenter(rotation: Float = 0f) {
        val sx = width.toFloat() / videoWidth.toFloat()
        val sy = height.toFloat() / videoHeight.toFloat()
        val matrix = Matrix()
        matrix.preTranslate((width - videoWidth) / 2f, (height - videoHeight) / 2f)
        matrix.preScale(videoWidth / width.toFloat(), videoHeight / height.toFloat())
        if (sx >= sy) {
            matrix.postScale(sy, sy, width / 2f, height / 2f)
        } else {
            matrix.postScale(sx, sx, width / 2f, height / 2f)
        }
        setTransform(matrix)
        postInvalidate()
    }
}