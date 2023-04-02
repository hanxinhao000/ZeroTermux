package com.zp.z_file.util

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import com.zp.z_file.content.ZFileInfoBean
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

internal object ZFileOtherUtil {

    /**
     * 获取文件大小
     */
    fun getFileSize(fileS: Long): String {
        val df = DecimalFormat("#.00")
        val byte_size = java.lang.Double.valueOf(df.format(fileS.toDouble()))
        if (byte_size < 1024) {
            return "$byte_size B"
        }
        val kb_size = java.lang.Double.valueOf(df.format(fileS.toDouble() / 1024))
        if (kb_size < 1024) {
            return "$kb_size KB"
        }
        val mb_size = java.lang.Double.valueOf(df.format(fileS.toDouble() / 1048576))
        if (mb_size < 1024) {
            return "$mb_size MB"
        }
        val gb_size = java.lang.Double.valueOf(df.format(fileS.toDouble() / 1073741824))
        if (gb_size < 1024) {
            return "$gb_size GB"
        }
        return ">1TB"
    }

    /**
     * 时间戳格式化
     */
    fun getFormatFileDate(seconds: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).run { format(Date(seconds)) }

    /** int类型转时分秒格式 */
    fun secToTime(time: Int): String {
        val timeStr: String?
        val hour: Int
        var minute: Int
        val second: Int
        if (time <= 0) return "00:00"
        else {
            minute = time / 60
            if (minute < 60) {
                second = time % 60
                timeStr = "${unitFormat(minute)}:${unitFormat(second)}"
            } else {
                hour = minute / 60
                if (hour > 99) return "99:59:59"
                minute %= 60
                second = time - hour * 3600 - minute * 60
                timeStr = "${unitFormat(hour)}:${unitFormat(minute)}:${unitFormat(second)}"
            }
        }
        return timeStr
    }

    private fun unitFormat(i: Int) = if (i in 0..9) "0$i" else "$i"

    /**
     * 获取媒体信息
     */
    fun getMultimediaInfo(path: String, isVideo: Boolean = false): ZFileInfoBean {
        var duration = -1
        var width = "0"
        var height = "0"
        var mediaPlayer: MediaPlayer? = null
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            if (isVideo) {
                width = mediaPlayer.videoWidth.toString()
                height = mediaPlayer.videoHeight.toString()
            }
            duration = mediaPlayer.duration
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer?.release()
            return ZFileInfoBean(secToTime(duration / 1000), width, height)
        }
    }

    /**
     * 根据路径获取图片的宽、高信息
     */
    fun getImageWH(imagePath: String) : Array<Int> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        ZFileLog.i("width---${options.outWidth} height---${options.outHeight}")
        return arrayOf(options.outWidth, options.outHeight)
    }
}