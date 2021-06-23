package com.termux.zerocore.utils

import android.app.Activity
import android.view.Gravity
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.url.FileUrl.smsUrl
import java.io.File

object IsInstallCommand {


    public fun isInstall(activity: Activity, command:String, commandInstall:String):Boolean{

        val file = File(FileUrl.mainBinUrl, "/${command}")

        if(!file.exists()){

            val temp = "${UUtils.getString(R.string.您当前未安装)} $command ${UUtils.getString(R.string.是否安装)}"

            val msg: SwitchDialog = switchDialogShow(activity,UUtils.getString(R.string.警告), temp)

            ///data/data/com.termux/files/usr/bin/vim
            msg.cancel!!.setOnClickListener { msg.dismiss() }
            UUtils.showLog("vim的安装目录:${file.absoluteFile}")
            msg.ok!!.setOnClickListener {
                msg.dismiss()
                if (file.exists()) {
                    UUtils.showMsg(UUtils.getString(R.string.您已安装工具1) + "(${command})")
                } else {
                    TermuxActivity.mTerminalView.sendTextToTerminal(commandInstall)
                }
            }

            return false

        }else{

            return true
        }


    }


    public fun switchDialogShow(activity: Activity,title: String, msg: String): SwitchDialog {

        val switchDialog = SwitchDialog(activity)
        switchDialog.title!!.text = title
        switchDialog.msg!!.text = msg
        switchDialog.other!!.visibility = View.GONE
        switchDialog.ok!!.text = UUtils.getString(R.string.确定)
        switchDialog.cancel!!.text = UUtils.getString(R.string.取消)
        switchDialog.show()
        return switchDialog
    }

}
