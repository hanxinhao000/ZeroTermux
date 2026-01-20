package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.utils.FileIOUtils

class ClearStyleClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.clear_style)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.clear_style_dialog)
    }

    override fun onClick(view: View?, context: Context?) {
        val termuxActivity: TermuxActivity = context as TermuxActivity
        FileIOUtils.clearStyle()
        termuxActivity.clear()
    }
}
