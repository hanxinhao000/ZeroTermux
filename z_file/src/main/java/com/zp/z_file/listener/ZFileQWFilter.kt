package com.zp.z_file.listener

import com.zp.z_file.content.*
import java.io.File
import java.io.FileFilter

internal class ZFileQWFilter(
    private var filterArray: Array<String>,
    private var isOther: Boolean
) : FileFilter {

    override fun accept(file: File): Boolean {
        /*if (isOther) {
            if (acceptOther(file.name)) {
                return true
            }
        } else {
            filterArray.forEach {
                if (file.name.accept(it)) {
                    return true
                }
            }
        }*/
        if (file.isDirectory) {
            return false
        }
        filterArray.forEach {
            if (it.isNull()) {
                if (acceptOther(file.name)) {
                    return true
                }
            } else {
                if (file.name accept it) {
                    return true
                }
            }
        }
        return false
    }

    // 匹配其他文件类型
    private fun acceptOther(name: String): Boolean {
        val isImage = name accept PNG || name accept JPG || name accept JPEG || name accept GIF
        val isVideo = name accept MP4 || name accept _3GP
        val isAudio = name accept MP3 || name accept AAC || name accept WAV || name accept M4A
        val isTxt = name accept TXT || name accept XML || name accept JSON
        val isDOC = name accept DOC || name accept DOCX
        val isXLS = name accept XLS || name accept XLSX
        val isPPT = name accept PPT || name accept PPTX
        val isPDF = name accept PDF
        val isZIP = name accept ZIP
        return !isImage && !isVideo && !isAudio && !isTxt && !isDOC && !isXLS && !isPPT && !isPDF && !isZIP
    }
}