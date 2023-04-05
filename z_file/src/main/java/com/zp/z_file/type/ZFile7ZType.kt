package com.zp.z_file.type

import android.view.View
import android.widget.ImageView
import com.zp.z_file.R
import com.zp.z_file.common.ZFileType
import com.zp.z_file.content.getZFileConfig
import com.zp.z_file.content.getZFileHelp

/**
 * TAR压缩包文件
 */
open class ZFile7ZType : ZFileType() {

    override fun openFile(filePath: String, view: View) {
        getZFileHelp().getFileOpenListener().open7Z(filePath, view)
    }

    override fun loadingFile(filePath: String, pic: ImageView) {
        pic.setImageResource(getRes(getZFileConfig().resources.zipRes, R.drawable.ic_zfile_moudel))
    }
}
