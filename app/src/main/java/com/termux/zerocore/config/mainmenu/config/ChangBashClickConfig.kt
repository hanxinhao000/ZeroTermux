package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.activity.EditTextActivity
import com.termux.zerocore.url.FileUrl

class ChangBashClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.bash_change)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.changed_bash)
    }

    override fun onClick(view: View?, context: Context?) {
        val intent = Intent(mContext, EditTextActivity::class.java)
        intent.putExtra("edit_path", FileUrl.smsBashrcFile)
        mContext?.startActivity(intent)
    }
}
