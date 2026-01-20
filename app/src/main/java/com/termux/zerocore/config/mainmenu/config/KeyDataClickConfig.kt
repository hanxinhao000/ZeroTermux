package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.keybord.KeyBordManage
import com.termux.zerocore.zero.engine.ZeroCoreManage

class KeyDataClickConfig: BaseMenuClickConfig() {
    companion object {
        val TAG = KeyDataClickConfig::class.simpleName
    }
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.keyboard_img_menu)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.keyboard_select_name)
    }

    override fun onClick(view: View?, context: Context?) {
        val termuxActivity: TermuxActivity = context as TermuxActivity
        val versionName = ZeroCoreManage.getVersionName()
        if (TextUtils.isEmpty(versionName)) {
            UUtils.showMsg(UUtils.getString(R.string.zero_eg_not_install))
            val intent = Intent()
            intent.setData(Uri.parse("https://github.com/hanxinhao000/ZeroCoreManage/releases/tag/0.1.20221116")) //Url 就是你要打开的网址
            intent.setAction(Intent.ACTION_VIEW)
            context.startActivity(intent) //启动浏览器
            return
        }
        val handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    KeyBordManage.KEY_DEF -> {
                        LogUtils.d(TAG, "handleMessage DEF:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminal(msg.obj as String?)
                        }
                    }
                    KeyBordManage.KEY_ALT -> {
                        LogUtils.d(TAG, "handleMessage ALT:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminalAlt(msg.obj as String?, true)
                        }
                    }
                    KeyBordManage.KEY_CTRL -> {
                        LogUtils.d(TAG, "handleMessage CTRL:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminalCtrl(msg.obj as String?, true)
                        }
                    }
                    KeyBordManage.KEY_OTHER -> {
                        LogUtils.d(TAG, "handleMessage OTHER:${msg.obj}")
                        if (msg.obj != null) {
                            TermuxActivity.mTermuxTerminalExtraKeys.onTerminalExtraKeyButtonClick(null, msg.obj as String?, false ,false ,false , false)
                        }
                    }
                }


            }
        }
        KeyBordManage.getInstance().initKeyBord(handler)
        termuxActivity.setKeyBordView(KeyBordManage.getInstance().keyBordView)
    }
}
