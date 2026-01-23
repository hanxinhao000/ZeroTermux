package com.termux.zerocore.config.mainmenu.config

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.data.CommendShellData
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.utils.FileIOUtils

class WebDataClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.web_linux)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.web_linux)
    }

    override fun onClick(view: View?, context: Context?) {
        var replace = ""
        if (FileIOUtils.isBinFileExists("ttyd")) {
            replace = UUtils.getString(R.string.ttyd_install_complete)
                .replace("0.0.0.0", UUtils.getHostIP())
            com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener().sendTextToTerminal(CommendShellData.SHELL_DATA_RUN_WEB)
        } else {
            replace = UUtils.getString(R.string.ttyd_install_msg)
            com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener().sendTextToTerminal(CommendShellData.SHELL_DATA_WEB_LINUX)
        }
        val switchDialog = SwitchDialog(mContext as Activity)
        switchDialog.createSwitchDialog(replace)
        switchDialog.ok?.setOnClickListener {
            switchDialog.dismiss()
        }
        switchDialog.cancel?.setOnClickListener {
            switchDialog.dismiss()
        }
        switchDialog.show()
    }
}
