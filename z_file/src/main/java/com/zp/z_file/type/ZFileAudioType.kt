package com.zp.z_file.type

import android.view.View
import android.widget.ImageView
import com.zp.z_file.R
import com.zp.z_file.common.ZFileType
import com.zp.z_file.content.getZFileConfig
import com.zp.z_file.content.getZFileHelp

/**
 * 音频文件
 */
open class ZFileAudioType : ZFileType() {

    override fun openFile(filePath: String, view: View) {
        getZFileHelp().getFileOpenListener().openAudio(filePath, view)
    }

    override fun loadingFile(filePath: String, pic: ImageView) {
        val resId = getZFileConfig().resources.audioRes
        pic.setImageResource(getRes(resId, R.drawable.ic_zfile_audio))
    }

}