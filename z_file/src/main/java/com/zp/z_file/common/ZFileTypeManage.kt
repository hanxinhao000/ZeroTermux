package com.zp.z_file.common

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.zp.z_file.content.ZFileBean
import com.zp.z_file.content.getZFileHelp

/**
 * 文件类型 管理
 */
internal class ZFileTypeManage {

    private object Builder {
        val MANAGER = ZFileTypeManage()
    }

    companion object {
        fun getTypeManager() = Builder.MANAGER
    }

    fun openFile(filePath: String, view: View) {
        getFileType(filePath).openFile(filePath, view)
    }

    fun loadingFile(filePath: String, pic: ImageView) {
        getFileType(filePath).loadingFile(filePath, pic)
    }

    fun infoFile(bean: ZFileBean, context: Context) {
        getFileType(bean.filePath).infoFile(bean, context)
    }

    fun getFileType(filePath: String) =
            getZFileHelp().getFileTypeListener().getFileType(filePath)

}