package com.zp.z_file.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.zp.z_file.R
import com.zp.z_file.common.ZFileActivity
import com.zp.z_file.common.ZFileTypeManage
import com.zp.z_file.content.inflate
import com.zp.z_file.content.setStatusBarTransparent
import com.zp.z_file.databinding.ActivityZfileVideoPlayBinding

internal class ZFileVideoPlayActivity : ZFileActivity() {

    private val vb by inflate<ActivityZfileVideoPlayBinding>()

    override fun getContentView() = R.layout.activity_zfile_video_play

    override fun create() = Unit

    override fun init(savedInstanceState: Bundle?) {
        setStatusBarTransparent()
        val videoPath = intent.getStringExtra("videoFilePath") ?: ""
        ZFileTypeManage.getTypeManager().loadingFile(videoPath, vb.videoImg)
        vb.videoPlayerButton.setOnClickListener { v ->
            vb.videoPlayer.videoPath = videoPath
            vb.videoPlayer.play()
            v.visibility = View.GONE
            vb.videoImg.visibility = View.GONE
        }
        vb.videoPlayer.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onPause() {
        if (vb.videoPlayer.isPlaying()) {
            vb.videoPlayer.pause()
            vb.videoPlayerButton.visibility = View.VISIBLE
        }
        super.onPause()
    }

    override fun onBackPressed() {
        vb.videoImg.visibility = View.VISIBLE
        super.onBackPressed()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.zfile_out_bottom)
    }

    companion object {

        fun show(activity: Activity, videoFilePath: String) {
            activity.startActivity(Intent(activity, ZFileVideoPlayActivity::class.java).apply {
                putExtra("videoFilePath", videoFilePath)
            })
            activity.overridePendingTransition(R.anim.zfile_in_bottom, 0)
        }

    }
}
