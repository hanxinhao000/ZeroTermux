package com.termux.zerocore.utils

import android.system.Os
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.url.FileUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File

object ModuleInstallUtils {
    private var TAG = "ModuleInstallUtils"
    private var mInstallModuleMsg: InstallModuleMsg? = null
    public fun unZipModule(mFile: File) {
        LogUtils.d(TAG, "unZipModule start...")
        val file = File(FileUrl.mainHomeUrl, "/tempmodule")
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
        val mFile = File(FileUrl.mainHomeUrl, "/tempmodule/INSTALL.txt")
        if (!mFile.exists()) {
            LogUtils.d(TAG, "installModule INSTALL is not find")
            stringBuilder.append(UUtils.getString(R.string.install_module_msg5))
            mInstallModuleMsg?.msg(stringBuilder.toString(), true, null)
            return
        }
        val lines: List<String> = mFile.readLines()
        var size = lines.size
        var index = 0
        stringBuilder.append(UUtils.getString(R.string.module_install_pro))
        mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
        lines.forEach {
            LogUtils.d(TAG, "installModule it: $it")
            if (Thread.interrupted()) {
                LogUtils.d(TAG, "installModule thread stop!")
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
                    mInstallModuleMsg?.msg(stringBuilder.toString(), true, null)
                    val split = it.split("->")
                    mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
                    LogUtils.d(TAG, "installModule split: $split")
                    if (split.size != 3) {
                        mInstallModuleMsg?.msg("${UUtils.getString(R.string.install_module_msg6)}->[$it]", true, null)
                        LogUtils.d(TAG, "installModule split.size error < 3, return")
                        return
                    }
                    val tempModuleFile = File(FileUrl.mainHomeUrl, "/tempmodule/${split[0]}")
                    val mainFile = File(FileUrl.mainFilesUrl, "/${split[1]}")
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


                        FileIOUtils.cpFile(tempModuleFile, mainFile, object : FileIOUtils.CpMsg{
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
                    stringBuilder.append("\n").append("${UUtils.getString(R.string.错误)}->[$it], $e").append("\n")
                    mInstallModuleMsg?.msg(stringBuilder.toString(), true, e)
                    return
                }

            }

        }

        try {
            FileUtils.cleanDirectory(File(FileUrl.mainHomeUrl, "/tempmodule"))
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.d(TAG, "installModule  cleanDirectory error $e")
            stringBuilder.append("\n").append(UUtils.getString(R.string.install_module_clear_tempmodule))
            mInstallModuleMsg?.msg(stringBuilder.toString(), false, null)
        }
        stringBuilder.append(UUtils.getString(R.string.install_module_msg10)).append("\n")
        mInstallModuleMsg?.msg(stringBuilder.toString(), true, null)

    }

    public fun setInstallModuleMsg(mInstallModuleMsg: InstallModuleMsg) {
        this.mInstallModuleMsg = mInstallModuleMsg
    }
    public interface InstallModuleMsg {
        fun msg(msg: String, isInstallEnd: Boolean, mThrowable: Throwable?)
    }

}
