package com.termux.zerocore.config.mainmenu.config

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.termux.R

class InstallModuleClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.install_module)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(R.string.install_module)
    }

    override fun onClick(view: View?, context: Context?) {
        val installModuleDialog = com.zp.z_file.ui.dialog.InstallModuleDialog(mContext as Activity)
        installModuleDialog.show()
        installModuleDialog.setCancelable(false)
    }
}
