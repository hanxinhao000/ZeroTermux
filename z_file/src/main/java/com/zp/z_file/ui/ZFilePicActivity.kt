package com.zp.z_file.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.zp.z_file.R
import com.zp.z_file.common.ZFileActivity
import com.zp.z_file.common.ZFileTypeManage
import com.zp.z_file.content.getZFileHelp
import com.zp.z_file.content.inflate
import com.zp.z_file.content.setStatusBarTransparent
import com.zp.z_file.databinding.ActivityZfilePicBinding

internal class ZFilePicActivity : ZFileActivity() {

    private val vb by inflate<ActivityZfilePicBinding>()

    override fun getContentView() = R.layout.activity_zfile_pic

    override fun create() = Unit

    override fun init(savedInstanceState: Bundle?) {
        setStatusBarTransparent()
        val filePath = intent.getStringExtra("picFilePath") ?: ""

        val imgView = getZFileHelp().getOtherListener()?.getImgInfoView(this, filePath)
        if (imgView == null) {
            vb.zfilePicShow.visibility = View.VISIBLE
            ZFileTypeManage.getTypeManager().loadingFile(filePath, vb.zfilePicShow)
            vb.zfilePicShow.setOnClickListener { onBackPressed() }
        } else {
            vb.zfilePicShow.visibility = View.GONE
            vb.zfilePicRootLayout.addView(imgView)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.zfile_out_bottom)
    }

    companion object {

        fun show(activity: Activity, picFilePath: String) {
            activity.startActivity(Intent(activity, ZFilePicActivity::class.java).apply {
                putExtra("picFilePath", picFilePath)
            })
            activity.overridePendingTransition(R.anim.zfile_in_bottom, 0)
        }

    }
}
