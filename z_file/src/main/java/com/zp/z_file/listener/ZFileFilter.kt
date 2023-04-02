package com.zp.z_file.listener

import com.zp.z_file.content.accept
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * 过滤规则
 * @param fileArray     规则
 * @param isOnlyFolder  是否只需要显示文件夹
 * @param isOnlyFile    是否只需要显示文件
 */
internal class ZFileFilter(
    private var fileArray: Array<String>?,
    private var isOnlyFolder: Boolean,
    private var isOnlyFile: Boolean
) : FileFilter {

    override fun accept(file: File): Boolean {
        if (isOnlyFolder) { // 只显示文件夹
            return file.isDirectory
        }
        if (isOnlyFile) { // 只显示文件
            return file.isFile
        }
        if (file.isDirectory) { // 文件夹直接返回
            return true
        }
        if (fileArray != null && fileArray!!.isNotEmpty()) {
            fileArray?.forEach {
                if (file.name accept it) {
                    return true
                }
            }
        } else {
            return true
        }
        return false
    }
}