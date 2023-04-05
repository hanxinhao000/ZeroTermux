package com.zp.z_file.util

import android.annotation.SuppressLint
import android.system.Os
import com.zp.z_file.R

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ModuleInstallUtils {
    private var TAG = "ModuleInstallUtils"
    /*
     * Termux app core directory paths.
     */
    /** Termux app internal private app data directory path  */
    const val TERMUX_PACKAGE_NAME = "com.termux"
    @SuppressLint("SdCardPath")
    val TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH =
        "/data/data/$TERMUX_PACKAGE_NAME" // Default: "/data/data/com.termux"

    /** Termux app internal private app data directory  */
    val TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR = File(TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH)


    /** Termux app Files directory path  */
    private val TERMUX_FILES_DIR_PATH =
        "$TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH/files" // Default: "/data/data/com.termux/files"

    private var mInstallModuleMsg: InstallModuleMsg? = null
    private val mainHomeUrl = "$TERMUX_FILES_DIR_PATH/home"
    private val mainFilesUrl = TERMUX_FILES_DIR_PATH
    public fun unZipModule(mFile: File) {
        LogUtils.d(TAG, "unZipModule start...")
        val file = File(mainHomeUrl, "/tempmodule")
        if (!file.exists()) {
            LogUtils.d(TAG, "installModule file path not exists is create")
            if (!file.mkdirs()) {
                LogUtils.d(TAG, "installModule file path create error! return!")
                return
            }
        }
       Thread {
           LogUtils.d(TAG, "installModule inputFilePath:" + mFile.absolutePath + ",outputPath:" + file.absolutePath)
           Z7ExtracatUtils.unZipFile(mFile, file)
       }.start()
    }

    public fun installModule(mInstallModuleMsg: InstallModuleMsg?) {
        LogUtils.d(TAG, "installModule start Install")
        val stringBuilder = StringBuilder()
        val mFile = File(mainHomeUrl, "/tempmodule/INSTALL.txt")
        if (!mFile.exists()) {
            LogUtils.d(TAG, "installModule INSTALL is not find")
            stringBuilder.append(ZFileUUtils.getString(R.string.install_module_msg5))
            mInstallModuleMsg?.msg(stringBuilder.toString(), true, null)
            clearData(stringBuilder)
            return
        }
        val lines: List<String> = mFile.readLines()
        var size = lines.size
        var index = 0
        stringBuilder.append(ZFileUUtils.getString(R.string.module_install_pro))
        mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
        lines.forEach {
            LogUtils.d(TAG, "installModule it: $it")
            if (Thread.interrupted()) {
                LogUtils.d(TAG, "installModule thread stop!")
                clearData(stringBuilder)
                return@forEach
            }
            if (it.startsWith("#") || it.trim().isEmpty()) {
                stringBuilder.append("\n").append(it.replace("# ", ""))
               // stringBuilder.append(it).append("\n")
                mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
            } else {
                try {
                    index ++
                    stringBuilder.append(".")
                    mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
                    val split = it.split("->")
                    LogUtils.d(TAG, "installModule split: $split")
                    if (split.size != 3) {
                        mInstallModuleMsg?.msg("${ZFileUUtils.getString(R.string.install_module_msg6)}->[$it]", true, null)
                        LogUtils.d(TAG, "installModule split.size error < 3, return")
                        clearData(stringBuilder)
                        return
                    }
                    val tempModuleFile = File(mainHomeUrl, "/tempmodule/${split[0]}")
                    val mainFile = File(mainFilesUrl, "/${split[1]}")
                    LogUtils.d(TAG, "installModule mainFile path:" + mainFile.absolutePath)
                    if (!(it.startsWith("#")) && it.contains("bash.bashrc")) {
                        LogUtils.d(TAG, "installModule open bash.bashrc")
                        val readLines = tempModuleFile.readLines()
                        val arrayList = ArrayList<String>()
                        arrayList.addAll(readLines)
                        BashFileUtils.setStartCommand(arrayList)
                    } else {
                        //创建文件夹
                            if (tempModuleFile.isDirectory) {
                               // mInstallModuleMsg?.msg(UUtils.getString(R.string.create_folder) + ":${mainFile.absolutePath}", false, null)
                                mainFile.mkdirs()
                            } else {
                                if (mainFile.parentFile != null && !(mainFile.parentFile.exists())) {
                                    mainFile.parentFile.mkdirs()
                                  //  mInstallModuleMsg?.msg(UUtils.getString(R.string.create_folder) + ":${mainFile.absolutePath}", false, null)
                                }
                            }


                        cpFile(tempModuleFile, mainFile, object : CpMsg{
                            override fun msg(msg: String, isEndInstall: Boolean) {
                                stringBuilder.append("\n").append(msg).append("\n")
                                mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
                            }
                        })

                        when (split[2].length) {
                            0 -> {
                               // mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg11), false, null)
                            }
                            1,2 -> {
                                Os.chmod(mainFile.absolutePath, 700)
                               // mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg9), false, null)
                            }
                            3 -> {
                                val toInt = split[2].toInt()
                                Os.chmod(mainFile.absolutePath, toInt)
                            }
                            else ->{
                                Os.chmod(mainFile.absolutePath, 700)
                              //  mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg9), false, null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    LogUtils.d(TAG, "installModule  error:$e")
                    stringBuilder.append("\n").append("${ZFileUUtils.getString(R.string.错误)}->[$it], $e").append("\n")
                    clearData(stringBuilder)
                    mInstallModuleMsg?.msg(stringBuilder.toString(), true, e)
                    return
                }

            }

        }
        clearData(stringBuilder)
        stringBuilder.append(ZFileUUtils.getString(R.string.install_module_msg10)).append("\n")
        mInstallModuleMsg?.msg(stringBuilder.toString(), true, null)

    }

    public fun clearData(stringBuilder: StringBuilder): StringBuilder {
        try {
            FileUtils.cleanDirectory(File(mainHomeUrl, "/tempmodule"))
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.d(TAG, "installModule  cleanDirectory error $e")
            stringBuilder.append("\n").append(ZFileUUtils.getString(R.string.install_module_clear_tempmodule))
            mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
        }
        return stringBuilder
    }

    public fun setInstallModuleMsg(mInstallModuleMsg: InstallModuleMsg) {
        this.mInstallModuleMsg = mInstallModuleMsg
    }
    public interface InstallModuleMsg {
        fun msg(msg: String, isInstallEnd: Boolean, mThrowable: Throwable?)
    }

    public fun cpFile(inputFile: File, outputFile: File, mCpMsg: CpMsg) {
        if (outputFile.exists()) {
            // mCpMsg.msg(UUtils.getString(R.string.install_module_msg7), false)
            if (!outputFile.delete()) {
                mCpMsg.msg(ZFileUUtils.getString(R.string.install_module_msg8) + ":${outputFile.absolutePath}", false)
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
