package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.example.xh_lib.utils.SaveData
import com.example.xh_lib.utils.UUtils
import com.scottyab.rootbeer.RootBeer
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.http.HTTPIP
import com.termux.zerocore.shell.ExeCommand


class SYFunBoomDialog : BaseDialogDown, View.OnClickListener {

    private var adb_root:LinearLayout? = null
    private var adb_root_close:LinearLayout? = null
    private var start_new_old:LinearLayout? = null
    private var text_new_old:TextView? = null
    private var alpine:LinearLayout? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {

        adb_root = mView.findViewById(R.id.adb_root)
        adb_root_close = mView.findViewById(R.id.adb_root_close)
        alpine = mView.findViewById(R.id.alpine)
        start_new_old = mView.findViewById(R.id.start_new_old)
        text_new_old = mView.findViewById(R.id.text_new_old)


        adb_root!!.setOnClickListener(this)
        adb_root_close!!.setOnClickListener(this)
        alpine!!.setOnClickListener(this)
        start_new_old!!.setOnClickListener(this)

        val stringOther = SaveData.getStringOther("new_old")
        if (stringOther == null || stringOther.isEmpty() || stringOther == "def") {
            text_new_old?.text = UUtils.getString(R.string.启动器新)
        } else {
            text_new_old?.text = UUtils.getString(R.string.启动器旧)
        }
    }

    override fun getContentView(): Int {

        return R.layout.dialog_sy_boom
    }

    override fun onClick(v: View?) {
        when(v!!.id){


            R.id.adb_root->{

                val rootBeer = RootBeer(context)
                if (rootBeer.isRooted) {
                    //有ROOT
                    runRootAdbHttp()
                } else {

                    val switchDialog = switchDialogShow(UUtils.getString(R.string.警告),UUtils.getString(R.string.没有找到本机的ROOT权限))
                    switchDialog.show()
                    switchDialog.setCancelable(false)
                    switchDialog.cancel!!.setOnClickListener { switchDialog.dismiss() }
                    switchDialog.ok!!.setOnClickListener {
                        switchDialog.dismiss()
                        runRootAdbHttp()

                    }

                    //没有ROOT
                }


            }

            R.id.adb_root_close->{


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

            R.id.alpine->{


                try {
                    val intent = Intent()
                    intent.action = "com.alpine.action.ENTER"
                    val activity = mContext as Activity
                    activity.startActivity(intent)
                }catch (e:Exception){
                    e.printStackTrace()
                    UUtils.showMsg(UUtils.getString(R.string.请在下载站下载Alpine插件))
                    dismiss()
                    val activity = mContext as TermuxActivity
                    activity.startHttp1(HTTPIP.IP)
                }



            }

            R.id.start_new_old->{

                val stringOther = SaveData.getStringOther("new_old")
                if (stringOther == null || stringOther.isEmpty() || stringOther == "def") {
                    //旧启动器
                    SaveData.saveStringOther("new_old","true")
                    text_new_old?.text = UUtils.getString(R.string.启动器旧)
                } else {
                    //新启动器
                    SaveData.saveStringOther("new_old","def")
                    text_new_old?.text = UUtils.getString(R.string.启动器新)
                }
                UUtils.showMsg(UUtils.getString(R.string.系统切换成功))


            }



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
