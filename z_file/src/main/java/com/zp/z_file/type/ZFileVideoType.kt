package com.zp.z_file.type

import android.view.View
import android.widget.ImageView
import com.zp.z_file.common.ZFileType
import com.zp.z_file.content.getZFileHelp
import com.zp.z_file.content.toFile

/**
 * 视频文件
 */
open class ZFileVideoType : ZFileType() {

    override fun openFile(filePath: String, view: View) {
        getZFileHelp().getFileOpenListener().openVideo(filePath, view)
    }

    override fun loadingFile(filePath: String, pic: ImageView) {
        getZFileHelp().getImageLoadListener().loadVideo(pic, filePath.toFile())
    }
}