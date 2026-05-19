package com.termux.zerocore.filetype

import android.content.Context
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zp.z_file.listener.ZFileOperateListener
import java.io.File

/**
 * 自定义 ZFile 文件操作监听器
 * 修改重命名对话框：旧文件名显示为可编辑文本，光标置于末尾，方便用户直接修改
 * 重命名成功后自动刷新当前文件夹内容
 */
class MyZFileOperateListener : ZFileOperateListener() {

    override fun renameFile(
        filePath: String,
        context: Context,
        block: (Boolean, String) -> Unit
    ) {
        val oldName = File(filePath).name

        val editText = EditText(context).apply {
            setText(oldName)
            setSingleLine(true)
            maxLines = 1
            // 光标放在末尾，不清除旧名称
            post {
                val textLength = text?.length ?: 0
                if (textLength > 0) {
                    setSelection(textLength)
                }
            }
        }

        val container = FrameLayout(context).apply {
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding / 2)
            addView(editText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        AlertDialog.Builder(context)
            .setTitle("文件重命名")
            .setView(container)
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("确定") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    block(false, "名称不能为空")
                    return@setPositiveButton
                }
                if (newName == oldName) {
                    block(false, "名称未改变")
                    return@setPositiveButton
                }
                val parentDir = File(filePath).parentFile
                val newFile = File(parentDir, newName)
                if (newFile.exists()) {
                    block(false, "该名称已存在")
                    return@setPositiveButton
                }
                val success = File(filePath).renameTo(newFile)
                block(success, if (success) newFile.absolutePath else filePath)
                
                // 重命名成功后刷新文件列表
                if (success) {
                    refreshFileList(context)
                }
                
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 通过反射调用 ZFileListFragment.observer(true) 刷新当前文件夹
     */
    private fun refreshFileList(context: Context) {
        try {
            val activity = context as? AppCompatActivity ?: return
            val fragment = activity.supportFragmentManager.findFragmentByTag("ZFileListFragment")
            if (fragment != null) {
                fragment.javaClass
                    .getMethod("observer", Boolean::class.java)
                    .invoke(fragment, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
