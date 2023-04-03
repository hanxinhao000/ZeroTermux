package com.termux.zerocore.bosybox

import android.content.Context
import android.system.Os
import android.util.Log
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.app.TermuxInstaller
import com.termux.zerocore.shell.ExeCommand
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.Z7ExtracatUtils
import com.zp.z_file.zerotermux.StartTarGzListener
import com.zp.z_file.zerotermux.ZTConfig
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object BusyBoxManager : StartTarGzListener{
    private const val TAG = "BusyBoxManager"
    private const val AARCH64 = 0
    private const val ARM = 1
    private var ARCHITECTURE = 0
    private var busyboxRunPath = ""
    fun init() {
        installBusyBox()
    }

    private fun installBusyBox() {
        val pathFile = File(getBusyBoxRootPath())
        if (!pathFile.exists()) {
            pathFile.mkdirs()
        }
        val assetsBusyBoxPath = getAssetsBusyBoxPath()
        if (assetsBusyBoxPath.isEmpty()) {
            LogUtils.d(TAG, "installBusyBox architecture not support!")
            return
        }
        when (ARCHITECTURE) {
            AARCH64 -> {
                val file = File(pathFile, "/ztbusybox_arm64.7z")
                val fileBusybox = File(pathFile, "/busybox_arm64")
                busyboxRunPath = "busybox_arm64"
                if (!file.exists()) {
                    UUtils.writerFileRawInput(
                        file,
                        UUtils.getContext().assets.open(assetsBusyBoxPath)
                    )
                    Z7ExtracatUtils.unZipFile(file, pathFile)
                }
                Os.chmod(fileBusybox.absolutePath, 511)
                file.delete()
            }
            ARM -> {
                val file = File(pathFile, "/ztbusybox_arm.7z")
                val fileBusybox = File(pathFile, "/busybox_arm")
                busyboxRunPath = "busybox_arm"
                if (!file.exists()) {
                    UUtils.writerFileRawInput(file, UUtils.getContext().assets.open(assetsBusyBoxPath))
                    Z7ExtracatUtils.unZipFile(file, pathFile)
                }
                Os.chmod(fileBusybox.absolutePath, 511)
                file.delete()
            }
            else -> {
                LogUtils.d(TAG, "installBusyBox architecture not support!")
            }
        }


    }

    private fun getBusyBoxRootPath(): String {
        return FileUrl.mainFilesUrl + "/ztbusybox"
    }

    private fun getAssetsBusyBoxPath(): String {
        return when (TermuxInstaller.determineTermuxArchName()) {
            "aarch64" -> {
                 ARCHITECTURE = AARCH64
                "zipcommand/busybox_arm64.7z"
            }
            "arm" -> {
                 ARCHITECTURE = ARM
                "zipcommand/busybox_arm.7z"
            }
            else -> {
                ARCHITECTURE = -1
                ""
            }
        }

    }
    public fun un7Z(mInputFile: String, mOutputPath: String) {

       RunShell.shell(false ,arrayOf("/bin/sh", "-c", "cd ztbusybox", "./${busyboxRunPath}  tar -xvf $mInputFile $mOutputPath").toList())
      // RunShell.shell(false ,arrayOf( "pwd").toList())
       // ExeCommand().run(".${busyboxPath}  tar -xvf $mInputFile $mOutputPath", 2000, false)
        LogUtils.d(TAG, "installBusyBox tar exe ok!")
    }

    private fun shellExec(command: String) {
        val mRuntime = Runtime.getRuntime()
        try {
            //Process中封装了返回的结果和执行错误的结果
            val mProcess = mRuntime.exec(command)
            LogUtils.d(TAG, "shellExec in: $command")
            val mReader = BufferedReader(InputStreamReader(mProcess.inputStream))
            val mRespBuff = StringBuffer()
            val buff = CharArray(1024)
            var ch = 0
            while (mReader.read(buff).also { ch = it } != -1) {
                mRespBuff.append(buff, 0, ch)
            }
            mReader.close()
            Log.e(TAG, "shellExec:${mRespBuff.toString()} " )
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    override fun start(inputString: String, outputString: String) {
        un7Z(inputString, outputString)
        ZTConfig.getCallBackListener()?.call()
    }
}
