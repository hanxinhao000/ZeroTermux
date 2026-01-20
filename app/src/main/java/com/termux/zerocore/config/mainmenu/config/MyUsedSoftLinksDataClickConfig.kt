package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.YesNoDialog
import com.termux.zerocore.utils.FileIOUtils
import java.io.File

class MyUsedSoftLinksDataClickConfig: BaseMenuClickConfig() {
    companion object {
        val TAG = MyUsedSoftLinksDataClickConfig::class.simpleName
    }
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.link_ico)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.my_commonly_used_soft_links)
    }

    override fun onClick(view: View?, context: Context?) {
        val yesNoDialog = YesNoDialog(mContext!!)
        yesNoDialog.createEditDialog(UUtils.getString(R.string.my_commonly_used_soft_links_path))
        yesNoDialog.inputSystemName.setHint("/xinhao/data/")
        yesNoDialog.yesTv.setOnClickListener {
            val toString = yesNoDialog.inputSystemName.text.toString()
            val file1 = File(FileIOUtils.getXinhaoLinkPath(UUtils.getContext()))
            if (!file1.exists()) {
                file1.mkdirs()
            }
            yesNoDialog.dismiss()
            if (TextUtils.isEmpty(toString)) {
                FileIOUtils.setupFileSymlinks(
                    File(FileIOUtils.getSdcardPath(), "/xinhao/data").absolutePath,
                    "${FileIOUtils.getXinhaoLinkPath(UUtils.getContext())}/xinhao_data")
                UUtils.showMsg(UUtils.getString(R.string.成功))
            } else {
                val file =
                    File(FileIOUtils.getSdcardPath(), toString)
                if (!file.exists()) {
                    UUtils.showMsg(UUtils.getString(R.string.my_commonly_used_soft_links_repeat))
                    LogUtils.d(TAG, "clickItem path is not exists")
                    return@setOnClickListener
                }
                FileIOUtils.setupFileSymlinks(
                    File(FileIOUtils.getSdcardPath(), toString).absolutePath,
                    "${FileIOUtils.getXinhaoLinkPath(UUtils.getContext())}/${toString.replace("/", "_").replace("\\", "_")}")
                UUtils.showMsg(UUtils.getString(R.string.成功))
            }
        }
        yesNoDialog.noTv.setOnClickListener {
            yesNoDialog.dismiss()
        }
        yesNoDialog.show()
    }
}
