package com.termux.zerocore.otg

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.view.View
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.termux.R
import com.termux.zerocore.data.UsbFileData.Companion.get
import com.termux.zerocore.dialog.LoadingDialog
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.utils.FileIOUtils
import java.io.File
import java.util.Arrays

class OTGManager {
   object OTGManagerConstant {
       public val ACTION_USB_PERMISSION = "com.androidinspain.otgviewer.USB_PERMISSION"
   }
    public val TAG = "OTGManager"
    private var mContext: Context? = null
    private var otgSwitchDialog: SwitchDialog? = null
    private var isTS = false
    public fun initOtg(mContext: Context, mIntent: Intent) {
        this.mContext = mContext
        val action: String? = mIntent.getAction()

        if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
            LogUtils.d(TAG, "initOtg OTG is unplugged")
            return
        }
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
            LogUtils.d(TAG, "initOtg OTG plugged in")
        }
        if (otgSwitchDialog == null) {
            otgSwitchDialog = switchDialogShow(
                UUtils.getString(R.string.提示),
                UUtils.getString(R.string.检测到OTG设备)
            )
        }
        val devices = UsbMassStorageDevice.getMassStorageDevices(mContext!!)
        LogUtils.d(TAG, "initOtg OTG size(" + devices.size + ")")
        if (get().mRefFileList != null) {
            try {
                get().mRefFileList!!.ref()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        LogUtils.d(TAG, "initOtg devices isNotEmpty:${devices.isNotEmpty()}")
        if (devices.isNotEmpty()) {
            //获取管理者
            val usbManager = UUtils.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
            //枚举设备
            val storageDevices =
                UsbMassStorageDevice.getMassStorageDevices(UUtils.getContext()) //获取存储设备
            val pendingIntent = PendingIntent.getBroadcast(
                mContext,
                0,
                Intent(OTGManagerConstant.ACTION_USB_PERMISSION),
                0
            )
            for (device in storageDevices) { //可能有几个 一般只有一个 因为大部分手机只有1个otg插口
                LogUtils.d(TAG, "initOtg devices hasPermission:${usbManager.hasPermission(device.usbDevice)}")
                if (usbManager.hasPermission(device.usbDevice)) { //有就直接读取设备是否有权限
                    LogUtils.d(TAG, "initOtg devices has Permission")
                    if (!(otgSwitchDialog!!.isShowing && otgSwitchDialog != null)) {
                        otgSwitchDialog?.show()
                        otgSwitchDialog?.cancel?.setOnClickListener {
                            LogUtils.d(TAG, "initOtg cancel click")
                            otgSwitchDialog?.dismiss()
                        }
                        otgSwitchDialog?.ok?.setOnClickListener {
                            otgSwitchDialog?.dismiss()
                            LogUtils.d(TAG, "initOtg ok click")
                            createLinkPath(device)
                        }
                        otgSwitchDialog?.setCancelable(true)
                    }
                } else { //没有就去发起意图申请
                    LogUtils.d(TAG, "initOtg devices requestPermission")
                    usbManager.requestPermission(
                        device.usbDevice,
                        pendingIntent
                    ) //该代码执行后，系统弹出一个对话框，
                }
            }
        }
    }

    private fun createLinkPath(device: UsbMassStorageDevice) {
        LogUtils.d(TAG, "createLinkPath ")
        if (!FileIOUtils.isXinhaoLinkPath(mContext!!)) {
            val createXinhaoPath = FileIOUtils.createXinhaoPath(mContext!!)
            if (!createXinhaoPath) {
                UUtils.showMsg(UUtils.getString(R.string.create_xinhao_path_fail))
                LogUtils.d(TAG, "createLinkPath create link xinhaoPath is Fail return.")
                return
            }
        }
        device.init()
        device.partitions.forEach {
            val file =
                File(FileIOUtils.getXinhaoLinkPath(mContext!!), "/${it.volumeLabel}")
            if (!file.exists()) {
                val mkdirs = file.mkdirs()
                if (!mkdirs) {
                    LogUtils.d(TAG, "createLinkPath create link deviceName Path is Fail return.")
                    return
                }
            }
            LogUtils.d(TAG, "createLinkPath create link OTG path:${device.usbDevice.deviceName}. link path: ${file.absolutePath}")
           // FileIOUtils.setupFileSymlinks("/dev/bus/usb/", file.absolutePath)

        }


    }
    //显示Dialog
    private fun showExceptionDialog(mLoadingDialog: LoadingDialog?) {
        if (mLoadingDialog != null && mLoadingDialog.isShowing) {
            try {
                mLoadingDialog.dismiss()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        UUtils.getHandler().post(Runnable {
            val switchDialog =
                switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.OTG设备异常))
            switchDialog.cancel!!.setOnClickListener { switchDialog.dismiss() }
            switchDialog.ok!!.setOnClickListener { switchDialog.dismiss() }
        })
    }

    private fun switchDialogShow(title: String, msg: String): SwitchDialog {
        val switchDialog = SwitchDialog(mContext!!)
        switchDialog.title!!.text = title
        switchDialog.msg!!.text = msg
        switchDialog.other!!.visibility = View.GONE
        switchDialog.ok!!.text = UUtils.getString(R.string.确定)
        switchDialog.cancel!!.text = UUtils.getString(R.string.取消)
        return switchDialog
    }
}
