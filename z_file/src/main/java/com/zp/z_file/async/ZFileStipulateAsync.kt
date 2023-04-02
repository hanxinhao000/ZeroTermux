package com.zp.z_file.async

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.zp.z_file.content.ZFileBean
import com.zp.z_file.util.ZFileLog
import com.zp.z_file.util.ZFileOtherUtil

/**
 * 根据特定条件 获取文件
 * ZFileAsyncImpl新的实现方式
 */
open class ZFileStipulateAsync(
    context: Context,
    block: MutableList<ZFileBean>?.() -> Unit
) : ZFileAsync(context, block) {

    override fun onPreExecute() {
        ZFileLog.i("根据特定条件 获取文件...")
    }

    override fun onPostExecute() {
        ZFileLog.i("根据特定条件 获取文件 完成")
    }

    /**
     * 获取数据
     */
    override fun doingWork(filterArray: Array<String>) = getLocalData(filterArray)

    private fun getLocalData(filterArray: Array<String>): MutableList<ZFileBean> {
        val list = arrayListOf<ZFileBean>()
        var cursor: Cursor? = null
        try {
            val fileUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val sb = StringBuilder()
            filterArray.forEach {
                if (it == filterArray.last()) {
                    sb.append(MediaStore.Files.FileColumns.DATA).append(" LIKE '%.$it'")
                } else {
                    sb.append(MediaStore.Files.FileColumns.DATA).append(" LIKE '%.$it' OR ")
                }
            }
            val selection = sb.toString()
            val sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED
            val resolver = getContext()?.contentResolver
            cursor = resolver?.query(fileUri, projection, selection, null, sortOrder)
            if (cursor?.moveToLast() == true) {
                do {
                    val path =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                    val size =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE))
                    val date =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED))
                    val fileSize = ZFileOtherUtil.getFileSize(size)
                    val lastModified = ZFileOtherUtil.getFormatFileDate(date * 1000)
                    if (size > 0.0) {
                        val name = path.substring(path.lastIndexOf("/") + 1, path.length)
                        list.add(
                            ZFileBean(
                                name,
                                true,
                                path,
                                lastModified,
                                date.toString(),
                                fileSize,
                                size
                            )
                        )
                    }
                } while (cursor.moveToPrevious())
            }
        } catch (e: Exception) {
            ZFileLog.e("根据特定条件 获取文件 ERROR ...")
            e.printStackTrace()
        } finally {
            cursor?.close()
            return list
        }
    }
}