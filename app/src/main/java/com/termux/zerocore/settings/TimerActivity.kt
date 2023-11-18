package com.termux.zerocore.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.utils.FileHttpUtils
import com.zp.z_file.util.LogUtils

class TimerActivity : AppCompatActivity(), LibSuManage.TimerListener {
    private val TAG = "TimerActivity"
    private val mStartTimer: CardView by lazy { findViewById(R.id.start_timer)}
    private val mStartTimerSwitch: SwitchCompat by lazy { findViewById(R.id.start_timer_switch)}
    private val mZtTimerLog: CardView by lazy { findViewById(R.id.zt_timer_log)}
    private var mLibSuManage: LibSuManage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)
        initViewFun()
    }
    private fun initViewFun() {
        setSwitchStatus(mStartTimerSwitch, mStartTimer)
        mLibSuManage = LibSuManage.getInstall()
        mLibSuManage?.setTimerListener(this)
    }

    private fun setSwitchStatus(switchCompat: SwitchCompat, linearLayout: CardView) {
        linearLayout.setOnClickListener {
            switchCompat.isChecked = !(switchCompat.isChecked)
        }
        switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            when (switchCompat) {
                mStartTimerSwitch -> {
                    LogUtils.e(TAG, "onAddElement: start")
                    mLibSuManage?.writerDebugFile()
                    mLibSuManage?.shellCommandExec("shell_main")
                }
            }
        }
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement: $msg")
    }
}
