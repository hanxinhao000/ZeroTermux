package com.termux.zerocore.config.mainmenu.config

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.zero.engine.ZeroCoreManage

class X86AlpineDataClickConfig: BaseMenuClickConfig() {
    companion object{
        val TAG = X86AlpineDataClickConfig::class.simpleName
    }
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.alpine_run)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.x86_alpine_run)
    }

    override fun onClick(view: View?, context: Context?) {
        val termuxActivity: TermuxActivity = context as TermuxActivity
        if (mContext == null) {
            LogUtils.d(TAG, "runQemuOs mContext is null return")
            return
        }
        val versionName = ZeroCoreManage.getVersionName()
        if (TextUtils.isEmpty(versionName)) {
            UUtils.showMsg(UUtils.getString(R.string.zero_eg_not_install))
            val intent = Intent()
            intent.setData(Uri.parse("https://github.com/hanxinhao000/ZeroCoreManage/releases/tag/0.1.20221116")) //Url 就是你要打开的网址
            intent.setAction(Intent.ACTION_VIEW)
            context?.startActivity(intent) //启动浏览器
            return
        }
        if (FileIOUtils.isProotQemu()) {
            val switchDialog = SwitchDialog(mContext as Activity)
            switchDialog.createSwitchDialog(UUtils.getString(R.string.install_environment))
            switchDialog.ok?.text = UUtils.getString(R.string.确定)
            switchDialog.cancel?.text = UUtils.getString(R.string.取消)
            switchDialog.ok?.setOnClickListener {
                switchDialog.dismiss()
                TermuxActivity.mTerminalView.sendTextToTerminal("pkg update -y && pkg in wget proot -y && pkg install x11-repo unstable-repo -y && pkg install qemu-utils qemu-system-x86_64-headless  qemu-system-i386-headless -y &&  termux-setup-storage\n")
                Toast.makeText(
                    UUtils.getContext(),
                    UUtils.getString(R.string.请等待安装完成在进入),
                    Toast.LENGTH_SHORT
                ).show()
            }
            switchDialog.cancel?.setOnClickListener {
                switchDialog.dismiss()
            }
            switchDialog.setCancelable(false)
            switchDialog.show()
            return
        }


        val handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    ZeroCoreManage.INSTALLING -> {
                        LogUtils.d(TAG, "INSTALLING System os install....")
                        termuxActivity.showDialog(true)
                    }
                    ZeroCoreManage.INSTALL_COMPLETE -> {
                        LogUtils.d(TAG, "INSTALL_COMPLETE System os install complete.")
                        termuxActivity.showDialog(false)
                        termuxActivity.vShell(ZeroCoreManage.getEnvironment(), ZeroCoreManage.getProcessArgs())
                    }
                }
            }
        }
        ZeroCoreManage.install(handler)
    }
}
