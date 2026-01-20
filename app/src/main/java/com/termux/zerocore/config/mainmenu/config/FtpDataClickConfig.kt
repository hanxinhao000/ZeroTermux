package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.FtpWindowsDialog

class FtpDataClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.ftp_web)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.ftp)
    }

    override fun onClick(view: View?, context: Context?) {
        val popupFtpWindows = FtpWindowsDialog(mContext!!)
        popupFtpWindows.show()
    }
}
