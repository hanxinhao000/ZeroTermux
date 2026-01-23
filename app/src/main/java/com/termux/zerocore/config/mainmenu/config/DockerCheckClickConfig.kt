package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.code.CodeString
import com.termux.zerocore.url.FileUrl
import java.io.File

class DockerCheckClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.docker)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(R.string.docker_check)
    }

    override fun onClick(view: View?, context: Context?) {
        UUtils.writerFile("runcommand/check-config.sh", File(FileUrl.mainHomeUrl, "/check-config.sh"))
        com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener().sendTextToTerminal(CodeString.runDocker)
    }
}
