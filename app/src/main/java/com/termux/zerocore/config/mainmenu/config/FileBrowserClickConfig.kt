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
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.zero.engine.ZeroCoreManage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileBrowserClickConfig : BaseMenuClickConfig(){
    companion object {
        val TAG = FileBrowserClickConfig::class.simpleName
    }
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.filebrowser_ico)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.网络访问linux目录)
    }

    override fun onClick(view: View?, context: Context?) {
        val versionName = ZeroCoreManage.getVersionName()
        if (TextUtils.isEmpty(versionName)) {
            UUtils.showMsg(UUtils.getString(R.string.zero_eg_not_install))
            val intent = Intent()
            intent.setData(Uri.parse("https://github.com/hanxinhao000/ZeroCoreManage/releases/tag/0.1.20221116")) //Url 就是你要打开的网址
            intent.setAction(Intent.ACTION_VIEW)
            context?.startActivity(intent) //启动浏览器
            return
        }

        val mHandler = object :Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                LogUtils.d(TAG, "handleMessage handler what: ${msg.what}")
                if (msg.what == ZeroCoreManage.INSTALL_COMPLETE) {
                    val smsBashrcFile = File(FileUrl.smsBashrcFile)
                    var fileString = UUtils.getFileString(smsBashrcFile)
                    if(!fileString.contains("filebrowser")){
                        fileString += "\n cd ~ > /dev/null && ./.filebrowser/filebrowser -a 0.0.0.0 -p 19951 -r "+FileUrl.mainFilesUrl+" & > /dev/null"
                        fileString += "\n echo '" + UUtils.getString(R.string.filebrowser已运行) + "'"
                        UUtils.setFileString(smsBashrcFile,fileString)
                    }
                    if (msg?.obj != null) {
                        TermuxActivity.mTerminalView.sendTextToTerminal(msg!!.obj as String?)
                    }
                }
            }
        }
        GlobalScope.launch {
            installFileBrowserIo(mHandler)
        }
    }


    private suspend fun installFileBrowserIo(mHandler: Handler) {
        withContext(Dispatchers.IO) {
            ZeroCoreManage.installFileBrowser(mHandler)
        }
    }
}
