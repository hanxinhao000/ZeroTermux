package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.system.Os
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.BuildConfig
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.dialog.MingLShowDialog
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.utils.FileIOUtils
import java.io.File

class AdbShellRunClickConfig: BaseMenuClickConfig() {
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.adb_shell)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(R.string.zt_adb_shell_run)
    }

    override fun onClick(view: View?, context: Context?) {
        val termuxActivity: TermuxActivity = context as TermuxActivity
        if (isShowDisableIco) {
            val switchDialog = SwitchDialog(termuxActivity)
            switchDialog.createSwitchDialog(UUtils.getString(R.string.zt_adb_shell_run_debug))
            switchDialog.show()
            switchDialog.ok?.setOnClickListener {
                switchDialog.dismiss()
                val intent = Intent()
                intent.setData(Uri.parse("https://github.com/hanxinhao000/ZeroTermux/releases"))
                intent.setAction(Intent.ACTION_VIEW)
                context.startActivity(intent) //启动浏览器
            }
            return
        }
        UUtils.writerFile("runcommand/termux-adb-shell.sh", File(FileIOUtils.getAdbShellFilePath(context)))
        Os.chmod(FileIOUtils.getAdbShellFilePath(context), "755".toInt(8))
        val mingLShowDialog = MingLShowDialog(context!!)
        mingLShowDialog.mTitleCard.visibility = View.GONE
        mingLShowDialog.mSwitchCard.visibility = View.GONE
        mingLShowDialog.edit_text.setText(UUtils.getString(R.string.zt_adb_shell_run_summary))
        mingLShowDialog.start.setOnClickListener {
            mingLShowDialog.dismiss()
        }
        mingLShowDialog.show()
    }

    override fun isShowDisableIco(): Boolean {
        return !BuildConfig.DEBUG
    }
}
