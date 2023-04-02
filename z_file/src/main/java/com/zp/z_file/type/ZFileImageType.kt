package com.zp.z_file.type

import android.view.View
import android.widget.ImageView
import com.zp.z_file.common.ZFileType
import com.zp.z_file.content.getZFileHelp
import com.zp.z_file.content.toFile

/**
 * 图片文件
 */
open class ZFileImageType : ZFileType() {

    override fun openFile(filePath: String, view: View) {
        getZFileHelp().getFileOpenListener().openImage(filePath, view)
    }

    override fun loadingFile(filePath: String, pic: ImageView) {
        getZFileHelp().getImageLoadListener().loadImage(pic, filePath.toFile())
    }

}