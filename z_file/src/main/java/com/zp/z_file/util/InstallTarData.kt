package com.zp.z_file.util

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.zp.z_file.R
import com.zp.z_file.bean.CreateSystemBean
import com.zp.z_file.ui.dialog.LoadingDialog
import com.zp.z_file.ui.dialog.YesNoDialog
import java.io.*

/**
 *
 * 此类是重新集成了一个核心恢复包类，临时编写，代码很乱，请谅解
 *
 */

object InstallTarData {
    private const val TAG = "InstallTarData"
    private const val SHELL_WELCOME_MESSAGE =  "echo \"+++++++++++++START+++++++++++++\" \n"

    //恢复
    private const val SHELL_TAR_RESTORE_GZ = "xzvf"
    private const val SHELL_TAR_RESTORE_BZ2 = "xjf"
    private const val SHELL_TAR_RESTORE_XZ = "xvJf"
    private const val SHELL_TAR_RESTORE_Z = "xZf"
    public fun installTar(mContext: Context, filePath: String) {
        val mFile = File(filePath)
        val storagePath = isStoragePath(mContext)
        if (!storagePath) {
            ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.storage_error))
            return
        }
        LogUtils.d(TAG, "installTar file name is:${mFile.name}")
/*        if (isPacketFormat(mFile.name)) {
            ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.unrecognized_compression_format))
            return
        }*/
        val mSwitchDialog = YesNoDialog(mContext)
        mSwitchDialog.createEditDialog(ZFileUUtils.getString(R.string.system_create_container))
        mSwitchDialog.show()
        mSwitchDialog.inputSystemName.hint = ZFileUUtils.getString(R.string.input_system_name)
        mSwitchDialog.yesTv.setOnClickListener {
            val text = mSwitchDialog.inputSystemName.text
            if (TextUtils.isEmpty(text)) {
                ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.system_create_container_empty))
                return@setOnClickListener
            }
            mSwitchDialog.dismiss()
            val createSystem =
                createSystem(ZFileUUtils.getContext(), text.toString())
            if (createSystem == null || !(createSystem.exists())) {
                LogUtils.d(TAG, "setRestoreFileDataListener -> file createSystem is fail return")
                return@setOnClickListener
            }
            var mLoadingDialog: LoadingDialog? = null
            ZFileUUtils.runOnUIThread {
                mLoadingDialog = LoadingDialog(mContext!!)
                mLoadingDialog?.msg?.text = ZFileUUtils.getString(R.string.正在载入中)
                mLoadingDialog?.show()
                mLoadingDialog?.setCancelable(false)
                // mCreateConversationListener?.create()
                ZFileUUtils.runOnThread {
                    Thread.sleep(1000)
                    ZFileUUtils.runOnUIThread {
                        sendTextToTerminal(SHELL_WELCOME_MESSAGE)
                        ZFileUUtils.runOnThread {
                            Thread.sleep(1000)
                            ZFileUUtils.runOnUIThread {
                                mLoadingDialog?.dismiss()
                                mFile?.let {
                                    LogUtils.d(TAG, "setRestoreFileDataListener -> file file name is: ${it.name}")
                                    if (it.name.endsWith("tar.gz")) {
                                        sendTextToTerminal(getShellRestore(SHELL_TAR_RESTORE_GZ, it, createSystem))
                                    } else if (it.name.endsWith("tar.bz2")) {
                                        sendTextToTerminal(getShellRestore(SHELL_TAR_RESTORE_BZ2, it, createSystem))
                                    } else if (it.name.endsWith("tar.xz")) {
                                        sendTextToTerminal(getShellRestore(SHELL_TAR_RESTORE_XZ, it, createSystem))
                                    } else {
                                        ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.unrecognized_compression_format))
                                        LogUtils.d(TAG, "setRestoreFileDataListener -> file unrecognized compression format: ${it.name}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun isStoragePath(mContext: Context): Boolean {
        return File(getStoragePath(mContext)).exists()
    }

    private fun getStoragePath(mContext: Context): String {
        return mContext.filesDir.absolutePath + "/home/storage"
    }

    private fun isPacketFormat(name: String): Boolean {
        return name.endsWith("tar.gz") || name.endsWith("tar.bz2") || name.endsWith("tar.xz")
    }


    private fun createSystem(mContext: Context, name: String): File? {
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
                Toast.makeText(ZFileUUtils.getContext(), ZFileUUtils.getString(R.string.system_create_container_fail), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(ZFileUUtils.getContext(), ZFileUUtils.getString(R.string.system_create_container_fail), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                return null
            } finally {
                printWriter?.close()
            }
        }
        return createFile
    }

    private fun getTermuxPathFile(mContext: Context): File {
        return File("/data/data/com.termux/")
    }

    private fun getMax(number: ArrayList<Int>): Int {
        var temp = number[0]
        for (i in number.indices) {
            if (number[i] > temp) {
                temp = number[i]
            }
        }
        return temp
    }

    private fun sendTextToTerminal(msg: String){
        LocalBroadcastManager.getInstance(ZFileUUtils.getContext()).apply {
            val intent = Intent()
            intent.action = "localbroadcast"
            intent.putExtra("broadcastString", msg)
            sendBroadcast(intent)
        }
    }
    private fun getShellRestore(command: String, tarFle: File, createFile: File): String {
        return  "cd ~ && cd ~ && tar -v -${command} ./storage/shared/xinhao/data/" + tarFle.getName().replace(" ","") + "  -C ../../" + createFile.getName() + " && mv ../../" + createFile.getName() + "/data/data/com.termux/files/home ../../" + createFile.getName() +" && "+ "mv ../../" + createFile.getName() + "/data/data/com.termux/files/usr ../../" + createFile.getName()+" && rm -rf ../../"+createFile.getName()+"/data && echo \"${ZFileUUtils.getString(R.string.system_restore_success)}\" \n"
    }

}
