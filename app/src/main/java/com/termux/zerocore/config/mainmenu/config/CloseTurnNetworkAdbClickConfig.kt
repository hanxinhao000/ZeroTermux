package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.scottyab.rootbeer.RootBeer
import com.termux.R
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.shell.ExeCommand

class CloseTurnNetworkAdbClickConfig : BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.adb_root)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(R.string.网络ADBClose)
    }

    override fun onClick(view: View?, context: Context?) {
        val rootBeer = RootBeer(context)
        if (rootBeer.isRooted) {
            //有ROOT
            runRootAdbHttpClose()
        } else {

            val switchDialog = switchDialogShow(UUtils.getString(R.string.警告),UUtils.getString(R.string.没有找到本机的ROOT权限))
            switchDialog.show()
            switchDialog.setCancelable(false)
            switchDialog.cancel!!.setOnClickListener { switchDialog.dismiss() }
            switchDialog.ok!!.setOnClickListener {
                switchDialog.dismiss()
                runRootAdbHttpClose()
            }
            //没有ROOT
        }

    }

    //执行关闭ROOT ADB
    private fun runRootAdbHttpClose(){
        val run = ExeCommand().run("setprop service.adb.tcp.port -1", 6000,true)
        var timeString = ""
        UUtils.runOnThread {
            while (run.isRunning){
                timeString+="\n${run.result}"
            }
        }
        val run1 = ExeCommand().run("stop adbd", 6000,true)
        UUtils.runOnThread {
            while (run1.isRunning){
                timeString+="\n${run.result}"
            }
        }
        val run2 = ExeCommand().run("start adbd", 6000,true)
        UUtils.runOnThread {
            while (run2.isRunning){
                timeString+="\n${run.result}"
            }
        }

        val switchDialog = switchDialogShow(UUtils.getString(R.string.警告),"${UUtils.getString(R.string.关闭成功)}\n")
        switchDialog.show()
        switchDialog.setCancelable(false)
        switchDialog.ok!!.setOnClickListener {
            switchDialog.dismiss()
        }
        switchDialog.cancel!!.setOnClickListener {
            switchDialog.dismiss()
        }
    }

    //执行ROOT ADB
    private fun runRootAdbHttp(){
        val run = ExeCommand().run("setprop service.adb.tcp.port 5555", 6000,true)
        var timeString = ""
        UUtils.runOnThread {
            while (run.isRunning){
                timeString+="\n${run.result}"
            }
        }
        val run1 = ExeCommand().run("stop adbd", 6000,true)
        UUtils.runOnThread {
            while (run1.isRunning){
                timeString+="\n${run.result}"
            }
        }
        val run2 = ExeCommand().run("start adbd", 6000,true)
        UUtils.runOnThread {
            while (run2.isRunning){
                timeString+="\n${run.result}"
            }
        }
        val switchDialog = switchDialogShow(UUtils.getString(R.string.警告),"${UUtils.getString(R.string.运行完成)}\n${UUtils.getHostIP()}:5555")
        switchDialog.show()
        switchDialog.setCancelable(false)
        switchDialog.ok!!.setOnClickListener {
            switchDialog.dismiss()
        }
        switchDialog.cancel!!.setOnClickListener {
            switchDialog.dismiss()
        }
    }

    private fun switchDialogShow(title: String, msg: String): SwitchDialog {
        val switchDialog = SwitchDialog(mContext)
        switchDialog.title!!.text = title
        switchDialog.msg!!.text = msg
        switchDialog.other!!.visibility = View.GONE
        switchDialog.ok!!.text = UUtils.getString(R.string.确定)
        switchDialog.cancel!!.text = UUtils.getString(R.string.取消)
        switchDialog.show()
        return switchDialog
    }
}
