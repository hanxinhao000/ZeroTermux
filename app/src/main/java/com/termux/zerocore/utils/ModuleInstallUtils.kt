package com.termux.zerocore.utils

import android.system.Os
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.url.FileUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

object ModuleInstallUtils {
    private var TAG = "ModuleInstallUtils"
    private var mInstallModuleMsg: InstallModuleMsg? = null
    public fun unZipModule(mFile: File) {
        val file = File(FileUrl.mainHomeUrl, "/tempmodule")
        if (!file.exists()) {
            LogUtils.d(TAG, "installModule file path not exists is create")
            if (!file.mkdirs()) {
                LogUtils.d(TAG, "installModule file path create error! return!")
                return
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            LogUtils.d(TAG, "installModule inputFilePath:" + mFile.absolutePath + ",outputPath:" + file.absolutePath)
            Z7ExtracatUtils.unZipFile(mFile, file)

        }

    }

    public fun installModule(mInstallModuleMsg: InstallModuleMsg?) {
        LogUtils.d(TAG, "installModule start Install")
        val mFile = File(FileUrl.mainHomeUrl, "/tempmodule/INSTALL.txt")
        if (!mFile.exists()) {
            LogUtils.d(TAG, "installModule INSTALL is not find")
            mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg5), true, null)
            return
        }
        val lines: List<String> = mFile.readLines()
        lines.forEach {
            LogUtils.d(TAG, "installModule it: $it")
            if (Thread.interrupted()) {
                LogUtils.d(TAG, "installModule thread stop!")
                return@forEach
            }
            if (it.startsWith("#") || it.trim().isEmpty()) {
                mInstallModuleMsg?.msg(it, false, null)
            } else {
                try {
                    val split = it.split("->")
                    mInstallModuleMsg?.msg(it.replace("# ", ""), false, null)
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
                        FileIOUtils.cpFile(tempModuleFile, mainFile, object : FileIOUtils.CpMsg{
                            override fun msg(msg: String, isEndInstall: Boolean) {
                                mInstallModuleMsg?.msg(msg, false, null)
                            }
                        })
                        when (split[2].length) {
                            0 -> {
                                mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg11), false, null)
                            }
                            1,2 -> {
                                Os.chmod(mainFile.absolutePath, 700)
                                mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg9), false, null)
                            }
                            3 -> {
                                val toInt = split[2].toInt()
                                Os.chmod(mainFile.absolutePath, toInt)
                            }
                            else ->{
                                Os.chmod(mainFile.absolutePath, 700)
                                mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg9), false, null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    LogUtils.d(TAG, "installModule  error $e")
                    mInstallModuleMsg?.msg("${UUtils.getString(R.string.错误)}->[$it], $e", true, e)
                    return
                }

            }

        }

        mInstallModuleMsg?.msg(UUtils.getString(R.string.install_module_msg10), true, null)

    }

    public fun setInstallModuleMsg(mInstallModuleMsg: InstallModuleMsg) {
        this.mInstallModuleMsg = mInstallModuleMsg
    }
    public interface InstallModuleMsg {
        fun msg(msg: String, isInstallEnd: Boolean, mThrowable: Throwable?)
    }

}
