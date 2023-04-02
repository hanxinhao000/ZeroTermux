package com.zp.z_file.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.media.MediaPlayer
import android.os.*
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.zp.z_file.R
import com.zp.z_file.common.ZFileManageDialog
import com.zp.z_file.content.setNeedWH
import com.zp.z_file.content.toast
import com.zp.z_file.databinding.DialogZfileAudioPlayBinding
import com.zp.z_file.util.ZFileOtherUtil
import java.lang.ref.WeakReference

internal class ZFileAudioPlayDialog : ZFileManageDialog(), SeekBar.OnSeekBarChangeListener, Runnable {

    private var vb: DialogZfileAudioPlayBinding? = null

    companion object {

        private const val UNIT = -1
        private const val PLAY = 0
        private const val PAUSE = 1

        fun getInstance(filePath: String) = ZFileAudioPlayDialog().apply {
            arguments = Bundle().apply { putString("filePath", filePath) }
        }
    }

    private var filePath = ""

    private var playerState = UNIT

    private var audioHandler: AudioHandler? = null
    private var mediaPlayer: MediaPlayer? = null

    private var beginTime = 0L
    private var falgTime = 0L
    private var pauseTime = 0L

    override fun getContentView() = R.layout.dialog_zfile_audio_play

    override fun create(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = DialogZfileAudioPlayBinding.inflate(inflater, container, false)
        return vb?.root
    }

    override fun createDialog(savedInstanceState: Bundle?) = Dialog(context!!, R.style.ZFile_Common_Dialog).apply {
        window?.setGravity(Gravity.CENTER)
    }

    override fun init(savedInstanceState: Bundle?) {
        audioHandler = AudioHandler(this)
        filePath = arguments?.getString("filePath") ?: ""
        initPlayer()
        vb?.dialogZfileAudioPlay?.setOnClickListener {
            when (playerState) {
                PAUSE -> {
                    startPlay()
                    falgTime = SystemClock.elapsedRealtime()
                    beginTime = falgTime - (vb?.dialogZfileAudioBar?.progress ?: 0)
                    vb?.dialogZfileAudioNowTime?.base = beginTime
                    vb?.dialogZfileAudioNowTime?.start()
                }
                PLAY -> {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        playerState = PAUSE
                        vb?.dialogZfileAudioNowTime?.stop()
                        pauseTime = SystemClock.elapsedRealtime()
                        vb?.dialogZfileAudioPlay?.setImageResource(R.drawable.zfile_play)
                    }
                }
                else -> {
                    initPlayer()
                }
            }
        }
        vb?.dialogZfileAudioBar?.setOnSeekBarChangeListener(this)
        vb?.dialogZfileAudioName?.text = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length)
    }

    override fun onStart() {
        super.onStart()
        setNeedWH()
    }

    private fun initPlayer() {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(filePath)
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnPreparedListener {
            vb?.dialogZfileAudioBar?.max = it.duration
            audioHandler?.post(this)
            vb?.dialogZfileAudioCountTime?.text = ZFileOtherUtil.secToTime(it.duration / 1000)

            // 设置运动时间
            falgTime = SystemClock.elapsedRealtime()
            pauseTime = 0
            vb?.dialogZfileAudioNowTime?.base = falgTime
            vb?.dialogZfileAudioNowTime?.start()

            startPlay()
        }
        mediaPlayer?.setOnCompletionListener {
            stopPlay()
            vb?.dialogZfileAudioBar?.isEnabled = false
            vb?.dialogZfileAudioBar?.progress = 0
            vb?.dialogZfileAudioNowTime?.base = SystemClock.elapsedRealtime()
            vb?.dialogZfileAudioNowTime?.start()
            vb?.dialogZfileAudioNowTime?.stop()
        }
    }

    // 开始播放
    private fun startPlay() {
        mediaPlayer?.start()
        playerState = PLAY
        vb?.dialogZfileAudioPlay?.setImageResource(R.drawable.zfile_pause)
        vb?.dialogZfileAudioBar?.isEnabled = true
    }

    // 停止播放
    private fun stopPlay() {
        vb?.dialogZfileAudioPlay?.setImageResource(R.drawable.zfile_play)
        mediaPlayer?.release()
        mediaPlayer = null
        playerState = UNIT
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && mediaPlayer != null) {
            mediaPlayer?.seekTo(progress)
            falgTime = SystemClock.elapsedRealtime()
            beginTime = falgTime - seekBar.progress
            vb?.dialogZfileAudioNowTime?.base = beginTime
            vb?.dialogZfileAudioNowTime?.start()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        playerState = UNIT
        audioHandler?.removeMessages(0)
        audioHandler?.removeCallbacks(this)
        audioHandler?.removeCallbacksAndMessages(null)
        audioHandler?.clear()
        audioHandler = null
    }

    override fun onPause() {
        if (playerState == PLAY) {
            vb?.dialogZfileAudioPlay?.performClick()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        vb = null
        super.onDestroyView()
    }

    override fun run() {
        // 获得歌曲现在播放位置并设置成播放进度条的值
        if (mediaPlayer != null) {
            audioHandler?.sendEmptyMessage(0)
            audioHandler?.postDelayed(this, 100)
        }
    }

    class AudioHandler(dialog: ZFileAudioPlayDialog) : Handler(Looper.myLooper()!!) {
        private val week: WeakReference<ZFileAudioPlayDialog> by lazy {
            WeakReference<ZFileAudioPlayDialog>(dialog)
        }

        override fun handleMessage(msg: Message) {
            week.get()?.vb?.dialogZfileAudioBar?.progress = week.get()?.mediaPlayer?.currentPosition ?: 0
        }

        fun clear() {
            week.clear()
        }
    }

}