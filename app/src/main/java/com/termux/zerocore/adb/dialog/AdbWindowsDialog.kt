package com.termux.zerocore.adb.dialog

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.blockchain.ub.util.custom.dialog.BaseDialogCentre
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.github.iielse.switchbutton.SwitchView
import com.github.iielse.switchbutton.SwitchView.OnStateChangedListener
import com.termux.R
import com.termux.zerocore.bean.SaveDataZeroEngine
import com.termux.zerocore.dialog.FtpWindowsDialog
import com.termux.zerocore.ftp.FsService
import com.termux.zerocore.utils.FileIOUtils
import kotlinx.coroutines.*

class AdbWindowsDialog : BaseDialogCentre {
    private var mAdbCommand: TextView? = null
    private var mAdbTool: TextView? = null
    private val ADB_COMMAND = 0
    private val ADB_TOOL = 1
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    override fun initViewDialog(mView: View) {
        mAdbCommand = mView.findViewById(R.id.adb_command)
        mAdbTool = mView.findViewById(R.id.adb_tool)
        mAdbCommand?.setOnClickListener {
            switchIndex(ADB_COMMAND)
        }
        mAdbCommand?.setOnClickListener {
            switchIndex(ADB_TOOL)
        }
    }

    override fun getContentView(): Int {
      return R.layout.pupu_window_adb_start
    }

    private fun switchIndex(index: Int) {
        mAdbCommand?.setBackgroundResource(R.drawable.shape_r_3dp_161823_232635)
        mAdbTool?.setBackgroundResource(R.drawable.shape_r_3dp_161823_232635)
        when (index) {
            ADB_COMMAND -> {
                mAdbCommand?.setBackgroundResource(R.drawable.shape_line_8cff5a)
            }
            ADB_TOOL -> {
                mAdbTool?.setBackgroundResource(R.drawable.shape_line_8cff5a)
            }
        }
    }

}


