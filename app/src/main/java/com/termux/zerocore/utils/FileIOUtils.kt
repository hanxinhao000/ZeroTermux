package com.termux.zerocore.utils

import android.content.Context
import android.os.Environment
import android.system.Os
import android.widget.Toast
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.bean.ClipboardBean
import com.termux.zerocore.bean.CreateSystemBean
import com.termux.zerocore.bean.MinLBean
import com.termux.zerocore.bean.MinLBean.DataNum
import com.termux.zerocore.url.FileUrl
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
    //ROM信息文件地址
    public const val DATA_MESSAGE_PATH = "/ZtInfo/data.config"
    public const val DATA_MESSAGE_PATH_FOLDER = "/ZtInfo"
    //网站默认地址
    public const val HTML_PATH = "/ztlink/html"
    public const val XINHAO_PATH = "/ztlink/xinhao"
    public const val HTML_ZT_LINK_PATH = "/ztlink"


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

    public fun getStoragePath(mContext: Context): String {
        return mContext.filesDir.absolutePath + "/home/storage"
    }
    public fun getTermuxPathFile(mContext: Context): File {
        return File("/data/data/com.termux/")
    }

    public fun isStoragePath(mContext: Context): Boolean {
        return File(getStoragePath(mContext)).exists()
    }

    public fun getFilePath(): String {
        return UUtils.getContext().filesDir.absolutePath
    }

    public fun getXinHaoDataPathFile(): File {
        return File(getSdcardPath(), "/xinhao/data/")
    }

    public fun getSdcardPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    public fun getBinPath(mContext: Context): String {
        return mContext.filesDir.absolutePath + "/usr/bin/"
    }

    public fun getTimeFileName(name: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        return "${simpleDateFormat.format(Date().time)}$name"
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

    public fun createSystem(mContext: Context, name: String): File? {
        val mFile = getTermuxPathFile(mContext)
        var createFile: File? = null
        val files: Array<File> = mFile.listFiles()
        if (files.size == 1) {
            createFile = File(mFile, "files1")
            createFile.mkdirs()
            val createSystemBean = CreateSystemBean()
            createSystemBean.dir = createFile.getAbsolutePath()
            createSystemBean.systemName = name
            val s = Gson().toJson(createSystemBean)
            val fileInfo: File = File(createFile, "/xinhao_system.infoJson")
            var printWriter: PrintWriter? = null
            try {
                fileInfo.createNewFile()
                printWriter = PrintWriter(OutputStreamWriter(FileOutputStream(fileInfo)))
                printWriter.print(s)
                printWriter.flush()
                printWriter.close()
            } catch (e: IOException) {
                Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.system_create_container_fail), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                return null
            } finally {
                printWriter?.close()
            }
        } else {
            val arrayList = ArrayList<Int>()
            for (i in files.indices) {
                if (files[i].name.startsWith("files")) {
                    val name1 = files[i].name
                    val substring = name1.substring(5, name1.length)
                    if (substring.isEmpty()) {
                        arrayList.add(0)
                    } else {
                        arrayList.add(substring.toInt())
                    }
                }
            }
            val max: Int = getMax(arrayList)
            LogUtils.d(TAG, "createSystem max:$max")
            createFile = File(mFile, "files" + (max + 1))
            createFile.mkdirs()
            val createSystemBean = CreateSystemBean()
            createSystemBean.dir = createFile.getAbsolutePath()
            createSystemBean.systemName = name
            val s = Gson().toJson(createSystemBean)
            val fileInfo: File = File(createFile, "/xinhao_system.infoJson")
            var printWriter: PrintWriter? = null
            try {
                fileInfo.createNewFile()
                printWriter = PrintWriter(OutputStreamWriter(FileOutputStream(fileInfo)))
                printWriter.print(s)
                printWriter.flush()
                printWriter.close()
            } catch (e: IOException) {
                Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.system_create_container_fail), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                return null
            } finally {
                printWriter?.close()
            }
        }
        return createFile
    }

    public fun getMax(number: ArrayList<Int>): Int {
        var temp = number[0]
        for (i in number.indices) {
            if (number[i] > temp) {
                temp = number[i]
            }
        }
        return temp
    }

    public fun isPacketFormat(name: String): Boolean {
        return name.endsWith("tar.gz") || name.endsWith("tar.bz2") || name.endsWith("tar.xz")
    }

    public fun isModuleFormat(name: String): Boolean {
        return name.endsWith("7Z") || name.endsWith("7z")
    }

    public fun isXinhaoLinkPath(mContext: Context): Boolean {
        return File(getXinhaoLinkPath(mContext)).exists()
    }

    public fun getXinhaoLinkPath(mContext: Context): String {
        return mContext.filesDir.absolutePath + "/home/xinhaoLink"
    }

    public fun createXinhaoPath(mContext: Context): Boolean {
        return File(getXinhaoLinkPath(mContext)).mkdirs()
    }

    public fun setupFileSymlinks(originalPath: String, linkPath: String) {
        LogUtils.d(TAG, "setupFileSymlinks originalPath:${originalPath} linkPath:$linkPath")
        try {
            Os.symlink(originalPath, linkPath)
        } catch (e: Exception) {
            e.printStackTrace()
            if (!e.toString().contains("File exists")) {
                UUtils.showMsg(e.toString())
            }
            LogUtils.d(TAG, "setupFileSymlinks error${e.toString()}")
        }
    }

    public fun getLogPath(): String {
        return getSdcardPath() + "/xinhao/ZeroTermuxLog"
    }

    public fun isLogPath(): Boolean {
        return File(getLogPath()).exists()
    }

    public fun createLogPath(): Boolean {
        return File(getLogPath()).mkdirs()
    }

    public fun getQQAndroidDownloadPath(): String {
        return getSdcardPath() + "/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv"
    }

    public fun getDownLoadPath(): String {
        return getSdcardPath() + "/Download"
    }

    public fun getWeiXinPath(): String {
        return getSdcardPath() + "/tencent/MicroMsg/WeiXin"
    }

    public fun getWeiXinAndroidPath(): String {
        return getSdcardPath() + "/Android/data/com.tencent.mm/MicroMsg/Download"
    }

    public fun getHtmlPath(): String {
        val file = File(getHomePath(UUtils.getContext()), "/.html/index")
        LogUtils.d(TAG, "getHtmlPath path is:" + file.absolutePath)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }

    public fun getDataMessagePathFile(): File {
        val homePath = getHomePath(UUtils.getContext())
        val file = File(homePath, DATA_MESSAGE_PATH_FOLDER)
        if (!file.exists()) {
            LogUtils.d(TAG, "folder is create")
            file.mkdirs()
        }
        createWebConfig()
        return File(homePath, DATA_MESSAGE_PATH)
    }

    public fun createWebConfig() {
        val fileZtLinkPATH = File(getHomePath(UUtils.getContext()), HTML_ZT_LINK_PATH)
        val fileHtmlPATH = File(getHomePath(UUtils.getContext()), HTML_PATH)
        val fileXinHaoPATH = File(getHomePath(UUtils.getContext()), XINHAO_PATH)

        val file1 = File(FileUrl.zeroTermuxHome, "web_config")

        if (!fileZtLinkPATH.exists()) {
            fileZtLinkPATH.mkdirs()
        }
        if (!file1.exists()) {
            file1.mkdirs()
        }
        try {
            Os.symlink(file1.absolutePath, fileHtmlPATH.absolutePath)
            Os.symlink(FileUrl.zeroTermuxHome.absolutePath, fileXinHaoPATH.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public fun getDataMessageFileString(): String {
        val dataMessagePathFile = getDataMessagePathFile()
        if (!dataMessagePathFile.exists()) {
            return ""
        }
        val dataMessage = UUtils.getFileString(dataMessagePathFile)
        return if (dataMessage.isNullOrEmpty()) {
            ""
        } else {
            dataMessage
        }
    }

    public fun saveDataMessageFileString(msg: String): Boolean {
        val dataMessagePathFile = getDataMessagePathFile()
        return UUtils.setFileString(dataMessagePathFile, msg)
    }
    //获取模块包路径
    public fun getModuleFiles(): ArrayList<File>? {
        if (!FileUrl.zeroTermuxModule.exists()) {
            if (!FileUrl.zeroTermuxModule.mkdirs()) {
                LogUtils.d(TAG, "getModuleFiles create folder is fail, path: " + FileUrl.zeroTermuxModule.absolutePath)
                return null
            }
        }
        val listFiles = FileUrl.zeroTermuxModule.listFiles()
        if (listFiles == null || listFiles.isEmpty()) {
            LogUtils.d(TAG, "getModuleFiles module listFiles is empty ")
            return null
        }
        val arrayList = ArrayList<File>()
        arrayList.addAll(listFiles)
        return arrayList
    }

    public fun cpFile(inputFile: File, outputFile: File, mCpMsg: CpMsg) {
        if (outputFile.exists()) {
           // mCpMsg.msg(UUtils.getString(R.string.install_module_msg7), false)
            if (!outputFile.delete()) {
                mCpMsg.msg(UUtils.getString(R.string.install_module_msg8) + ":${outputFile.absolutePath}", false)
            }
        }
        try {
            FileInputStream(inputFile).use { fis ->
                FileOutputStream(outputFile).use { os ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (fis.read(buffer).also { len = it } != -1) {
                        os.write(buffer, 0, len)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mCpMsg.msg(e.toString(), true)
        }

    }

    public interface CpMsg {
        fun msg(msg: String, isEndInstall: Boolean)
    }

}
