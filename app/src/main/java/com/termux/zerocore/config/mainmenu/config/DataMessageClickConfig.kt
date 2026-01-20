package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.view.View
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.dialog.MingLShowDialog
import com.termux.zerocore.utils.FileIOUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataMessageClickConfig: BaseMenuClickConfig() {
    companion object {
        val TAG = DataMessageClickConfig::class.simpleName
    }
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.data_msg)
    }

    override fun getString(context: Context?): String? {
       return  UUtils.getString(R.string.create_data_message)
    }

    override fun onClick(view: View?, context: Context?) {
        val termuxActivity: TermuxActivity = context as TermuxActivity
        val mingLShowDialog = MingLShowDialog(context!!)
        mingLShowDialog.mTitleCard.visibility = View.GONE
        mingLShowDialog.mSwitchCard.visibility = View.GONE
        mingLShowDialog.edit_text.hint = UUtils.getString(R.string.data_message_hint)
        val dataMessageFileString = FileIOUtils.getDataMessageFileString()
        if (!(dataMessageFileString.isNullOrEmpty())) {
            mingLShowDialog.edit_text.setText(dataMessageFileString)
        } else {
            mingLShowDialog.edit_text.setText(UUtils.getString(R.string.zt_data_info_content))
        }
        mingLShowDialog.start.setOnClickListener {
            mingLShowDialog.dismiss()
            //在手动关闭和意外关闭都会自动保存，所以此显示信息只是增加保存按钮的一个反馈信息
            UUtils.showMsg(UUtils.getString(R.string.保存成功))
        }
        mingLShowDialog.setOnDismissListener {
            val text = mingLShowDialog.edit_text.text
            //设置自动保存，关闭Dialog之后也会自动保存
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    FileIOUtils.saveDataMessageFileString(text.toString())
                }
                withContext(Dispatchers.Main) {
                    termuxActivity.initDataMsgInfo()
                }
            }
        }
        mingLShowDialog.show()
    }
}
