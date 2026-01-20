package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.scrcpy.MainActivity

class RemoteConnectionClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.yc_connect)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.remote_connection)
    }

    override fun onClick(view: View?, context: Context?) {
        val intent = Intent(mContext, MainActivity::class.java)
        mContext?.startActivity(intent)
    }
}
