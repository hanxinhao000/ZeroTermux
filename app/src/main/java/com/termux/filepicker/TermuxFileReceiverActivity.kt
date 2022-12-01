package com.termux.filepicker

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.utils.FileIOUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

class TermuxFileReceiverActivity : ComponentActivity() {
    private val TAG = "TermuxFileReceiverActivity"
    private val start_fz: LinearLayout by lazy { findViewById(R.id.start_fz) }
    private val msg_file: TextView by lazy { findViewById(R.id.msg_file) }
    private val msg_pro: TextView by lazy { findViewById(R.id.msg_pro) }
    private val image_view: ImageView by lazy { findViewById(R.id.image_view) }
    private val pro: ProgressBar by lazy { findViewById(R.id.pro) }
    private var mFile: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux_file_receiver)
        intent?.data?.encodedPath?.let {
            mFile = File(it)
            LogUtils.d(TAG, "onCreate file Size: ${mFile?.length()}")
            LogUtils.d(TAG, "onCreate file Path: ${mFile?.absolutePath}")
            val lengthToMb = FileIOUtils.getLengthToMb(mFile!!)
            if (lengthToMb != null) {
                msg_file.text = UUtils.getString(R.string.file_name_copy)
                    .replace("{file}", mFile!!.name)
                    .replace("{size}", lengthToMb)
                    .replace("{path}", mFile!!.absolutePath)
                    .replace("{suffix}", FileIOUtils.getExtension(mFile!!))
            } else {
                msg_file.text = UUtils.getString(R.string.file_name_copy)
                    .replace("{file}", "N/A")
                    .replace("{size}", "N/A")
                    .replace("{path}", "N/A")
                    .replace("{suffix}", "N/A")
            }
        }

        start_fz.setOnClickListener {
            if (mFile == null) {
                UUtils.showMsg(UUtils.getString(R.string.not_file_msg))
                finish()
                return@setOnClickListener
            }
            val file = File(FileIOUtils.getHomePath(UUtils.getContext()), mFile!!.name)
            LogUtils.d(TAG, "onCreate file: ${file.absolutePath}")
            if (!file.exists()) {
                file.createNewFile()
            }
            try {
                val fileInputStream = FileInputStream(mFile!!)
                image_view.visibility = View.GONE
                pro.visibility = View.VISIBLE
                GlobalScope.launch(Dispatchers.IO) {
                    showText(file, fileInputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UUtils.showMsg(UUtils.getString(R.string.not_file_msg))
                finish()
            }
        }
    }

    private suspend fun showText(file: File, fileInputStream: FileInputStream) {
        UUtils.writerFileInput(file, fileInputStream) { l, isEnd ->
           UUtils.getHandler().post {
               if (!isEnd) {
                   LogUtils.d(TAG, "showText copy File: ${l}")
                   LogUtils.d(TAG, "showText File Size: ${mFile!!.length()}")
                   LogUtils.d(TAG, "showText %: ${l.toFloat() / mFile!!.length()}")
                   pro.progress = (l.toFloat() / mFile!!.length() * 100).toInt()
                   msg_pro.text = FileIOUtils.formatFileSize(l)
               } else {
                   UUtils.showMsg(UUtils.getString(R.string.copy_file_to_zt))
                   finish()
               }
           }
        }

    }
}
