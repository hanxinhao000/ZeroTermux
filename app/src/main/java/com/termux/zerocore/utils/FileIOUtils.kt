package com.termux.zerocore.utils

import android.content.Context
import android.os.Environment
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.bean.ClipboardBean
import com.termux.zerocore.bean.MinLBean
import com.termux.zerocore.bean.MinLBean.DataNum
import com.termux.zerocore.url.FileUrl
import java.io.File
import java.text.DecimalFormat

object FileIOUtils {

    public const val COMMEND_KEY = "commi22"
    public const val COMMEND_DEF = "def"

    public const val CLIP_BOARD_KEY = "ClipBoardKey"
    public const val TAG = "FileIOUtils"

    //环境
    public const val TERMUX_CHROOT = "/data/data/com.termux/files/usr/bin/termux-chroot"
    public const val TERMUX_WGET = "/data/data/com.termux/files/usr/bin/wget"
    public const val TERMUX_QEMU = "/data/data/com.termux/files/usr/bin/qemu-system-x86_64"

    public  val TERMUX_XINHAO_CONFIG = Environment.getExternalStorageDirectory().absolutePath + "/xinhao/config/"

    //VIDEO
    public const val VIDEO_KEY = "videoPath"
    public const val IMAGE_KEY = "videoPath"

    public const val MB5 = 1024 * 1024 * 5
    public const val MB100 = 1024 * 1024 * 100

    public fun commendSave(nameString:String, commitString:String, isChecked:Boolean) {
        val commi22 = SaveData.getData(COMMEND_KEY)
        if (commi22 == null || commi22.isEmpty() || commi22 == COMMEND_DEF) {
            val minLBean = MinLBean()
            val data = MinLBean.Data()
            minLBean.data = data
            val arrayList = ArrayList<DataNum>()
            val dataNum = DataNum()
            dataNum.id = System.currentTimeMillis()
            dataNum.name = nameString
            dataNum.value = commitString
            dataNum.isChecked = isChecked
            arrayList.add(0, dataNum)
            data.list = arrayList
            val s = Gson().toJson(minLBean)
            SaveData.saveData(COMMEND_KEY, s)
        } else {
            val minLBean = Gson().fromJson(commi22, MinLBean::class.java)
            val list = minLBean.data.list
            val dataNum = DataNum()
            dataNum.id = System.currentTimeMillis()
            dataNum.name = nameString
            dataNum.value = commitString
            dataNum.isChecked = isChecked
            list.add(0, dataNum)
            val s = Gson().toJson(minLBean)
            SaveData.saveData(COMMEND_KEY, s)
            UUtils.showMsg(UUtils.getString(R.string.添加成功))
        }

    }

    public fun getClipBoardData() : List<ClipboardBean.Clipboard>? {
        val data = SaveData.getData(CLIP_BOARD_KEY)
        if (!isEmpty(data)) {
            val mClipboardBean = Gson().fromJson(data, ClipboardBean::class.java)
            return mClipboardBean.data.list
        }
        return null
    }

    public fun clearClipBoardString(){
        SaveData.saveData(CLIP_BOARD_KEY, "def")
    }
    public fun deleteClipBoardString(mClipboard: ClipboardBean.Clipboard) :Boolean {
        val data = SaveData.getData(CLIP_BOARD_KEY)
        try {
            if (!isEmpty(data)) {
                val mClipboardBeanTemp = Gson().fromJson(data, ClipboardBean::class.java)
                val list = mClipboardBeanTemp.data.list
                for (i in 0 until list.size) {
                    if (list[i].index == mClipboard.index) {
                        LogUtils.d(TAG, "deleteClipBoardString remove data :${list[i]}")
                        list.removeAt(i)
                        break
                    }
                }
                val s = Gson().toJson(mClipboardBeanTemp)
                SaveData.saveData(CLIP_BOARD_KEY, s)
                return true
            }
        }catch (e:Exception) {
            LogUtils.d(TAG, "deleteClipBoardString remove error :$e")
        }
        return false
    }

    public fun setClipBoardString(value:String) {
        val data = SaveData.getData(CLIP_BOARD_KEY)
        if (isEmpty(data)) {
            val clipboardBean = ClipboardBean()
            val data = ClipboardBean.Data()
            var list:ArrayList<ClipboardBean.Clipboard> = ArrayList()
            val dataNum = ClipboardBean.Clipboard()
            dataNum.name = value
            dataNum.index = 0
            list.add(dataNum)
            data.list = list
            clipboardBean.data = data
            val s = Gson().toJson(clipboardBean)
            SaveData.saveData(CLIP_BOARD_KEY, s)
            LogUtils.d(TAG, "setClipBoardString ClipBoard is Empty,save data :$s")
        } else {
            try {
                val mClipboardBean = Gson().fromJson(data, ClipboardBean::class.java)
                val list = mClipboardBean.data.list
                val dataNum = ClipboardBean.Clipboard()
                dataNum.name = value
                dataNum.index = list.size
                list.add(0, dataNum)
                val s = Gson().toJson(mClipboardBean)
                SaveData.saveData(CLIP_BOARD_KEY, s)
                LogUtils.d(TAG, "setClipBoardString ClipBoard save data :$s")
            } catch (e:Exception) {
                LogUtils.d(TAG, "data error:$e")
            }
        }
    }

    private fun isEmpty(text:String?) :Boolean{
        return text == null || text.isEmpty() || text == COMMEND_DEF
    }

    public fun isFileSize5Mb(file: File) :Boolean{
        return file.length() > MB5
    }

    public fun isFileSize100Mb(file: File) :Boolean{
        return file.length() > MB100
    }

    public fun setPathVideo(file: File) {
        SaveData.saveData(VIDEO_KEY, file.absolutePath)
    }

    public fun clearPathVideo() {
        SaveData.saveData(VIDEO_KEY, COMMEND_DEF)
    }

    public fun getPathVideo() :String {
        return SaveData.getData(VIDEO_KEY)
    }

    public fun isPathVideo() :Boolean {
        val data = SaveData.getData(VIDEO_KEY)
        return !isEmpty(data)
    }


    public fun isProotQemu() :Boolean {
        val fileProot = File(TERMUX_CHROOT)
        val fileWget = File(TERMUX_WGET)
        val fileQemu = File(TERMUX_QEMU)
        return !(fileProot.exists()) || !(fileWget.exists()) || !(fileQemu.exists())
    }

    public fun getLengthToMb(mFile: File): String? {
        return formatFileSize(mFile.length())
    }

    public fun getExtension(mFile: File): String {
        val fileName = mFile.name
        val contains = mFile.name.contains(".")
        var extension = ""
        if (contains) {
            val i: Int = fileName.lastIndexOf('.')
            if (i > 0) {
                return fileName.substring(i + 1)
            } else {
                return "N/A"
            }
        } else {
            return "N/A"
        }
    }

    fun formatFileSize(fileS: Long): String? {
        val df = DecimalFormat("#.00")
        var fileSizeString = ""
        val wrongSize = "0B"
        if (fileS == 0L) {
            return wrongSize
        }
        fileSizeString = if (fileS < 1024) {
            df.format(fileS.toDouble()) + "B"
        } else if (fileS < 1048576) {
            df.format(fileS.toDouble() / 1024) + "KB"
        } else if (fileS < 1073741824) {
            df.format(fileS.toDouble() / 1048576) + "MB"
        } else {
            df.format(fileS.toDouble() / 1073741824) + "GB"
        }
        LogUtils.d(TAG, "formatFileSize fileS:$fileS")
        return fileSizeString
    }

    public fun clearStyle() {
        com.example.xh_lib.utils.SaveData.saveStringOther("font_color_progress", "def")
        com.example.xh_lib.utils.SaveData.saveStringOther("font_color", "def")

        com.example.xh_lib.utils.SaveData.saveStringOther("back_color", "def")
        com.example.xh_lib.utils.SaveData.saveStringOther("back_color_progress", "def")

        com.example.xh_lib.utils.SaveData.saveStringOther("change_text_show", "def")
        com.example.xh_lib.utils.SaveData.saveStringOther("change_text", "def")


        val fileImg = File("${FileUrl.mainConfigImg}/back.jpg")
        if(fileImg.exists()){
            fileImg.delete()
        }
        clearPathVideo()
    }

    public fun getHomePath(mContext: Context): String {
        return mContext.filesDir.absolutePath + "/home/"
    }

    public fun getFilePath(): String {
        return UUtils.getContext().filesDir.absolutePath
    }

    public fun getSdcardPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    public fun getBinPath(mContext: Context): String {
        return mContext.filesDir.absolutePath + "/usr/bin/"
    }

    public fun isBinFileExists(fileName: String): Boolean {
        val binPath = getBinPath(UUtils.getContext())
        val file = File(binPath, fileName)
        LogUtils.d(TAG, "isBinFileExists file path: ${file.absolutePath}")
        if (file.exists()) {
            return true
        }
        return false
    }

    public fun getConfigFilePath() :String {
        return TERMUX_XINHAO_CONFIG
    }



}
