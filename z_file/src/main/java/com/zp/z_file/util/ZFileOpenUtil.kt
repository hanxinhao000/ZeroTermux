package com.zp.z_file.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.FileProvider
import com.zp.z_file.R
import com.zp.z_file.content.ZFileConfiguration
import com.zp.z_file.content.getStringById
import com.zp.z_file.content.getZFileConfig
import com.zp.z_file.content.toast
import java.io.File

/**
 * 文件打开帮助类
 */
internal object ZFileOpenUtil {

    private const val TXT = "text/plain"
    private const val ZIP = "application/x-zip-compressed"
    // Word
    private const val DOC = "application/msword"
    // excel
    private const val XLS = "application/vnd.ms-excel"
    // ppt
    private const val PPT = "application/vnd.ms-powerpoint"
    // pdf
    private const val PDF = "application/pdf"

    fun openTXT(filePath: String, view: View) {
        open(filePath, TXT, view.context)
    }

    fun openZIP(filePath: String, view: View) {
        open(filePath, ZIP, view.context)
    }

    fun openDOC(filePath: String, view: View) {
        open(filePath, DOC, view.context)
    }

    fun openXLS(filePath: String, view: View) {
        open(filePath, XLS, view.context)
    }

    fun openPPT(filePath: String, view: View) {
        open(filePath, PPT, view.context)
    }

    fun openPDF(filePath: String, view: View) {
        open(filePath, PDF, view.context)
    }
    fun openOtherFile(filePath: String, type: String, view: View) {
        open(filePath, type, view.context)
    }

    private fun open(filePath: String, type: String, context: Context?) {
        getZFileConfig().authority = ZFileConfiguration.mApplicationContext?.packageName + ".fileProvider"
        Log.e("open", "System.err: ${getZFileConfig().authority}" )
        context?.let {
            try {
                it.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    addCategory("android.intent.category.DEFAULT")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val contentUri = FileProvider.getUriForFile(
                            it,
                            getZFileConfig().authority, File(filePath)
                        )
                        setDataAndType(contentUri, type)
                    } else {
                        val uri = Uri.fromFile(File(filePath))
                        setDataAndType(uri, type)
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                ZFileLog.e("ZFileConfiguration.authority 未设置？？？")
                it.toast(it getStringById R.string.zfile_no_app_open_file)
            }
        }
    }

}
