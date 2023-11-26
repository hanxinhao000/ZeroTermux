package com.termux.zerocore.settings

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.dialog.YesNoDialog
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.services.TimerExeService
import com.termux.zerocore.settings.timer.TimerBean
import com.termux.zerocore.utils.FileHttpUtils
import com.zp.z_file.util.LogUtils

class TimerActivity : AppCompatActivity(), LibSuManage.TimerListener, View.OnClickListener {
    private val TAG = "TimerActivity"
    private val mStartTimer: CardView by lazy { findViewById(R.id.start_timer)}
    private val mStartTimerSwitch: SwitchCompat by lazy { findViewById(R.id.start_timer_switch)}
    private val mStartSwitchEnvironment: CardView by lazy { findViewById(R.id.start_switch_environment)}
    private val mStartSwitchEnvironmentSwitch: SwitchCompat by lazy { findViewById(R.id.start_switch_environment_switch)}
    private val mS30: LinearLayout by lazy { findViewById(R.id.s_30)}
    private val mM1: LinearLayout by lazy { findViewById(R.id.m_1)}
    private val mM10: LinearLayout by lazy { findViewById(R.id.m_10)}
    private val mM30: LinearLayout by lazy { findViewById(R.id.m_30)}
    private val mOther: LinearLayout by lazy { findViewById(R.id.other)}
    private val mStartSwitchEnvironmentSum: TextView by lazy { findViewById(R.id.start_switch_environment_sum)}
    private val mCheckTimerSum: TextView by lazy { findViewById(R.id.check_timer_sum)}
    private var mLibSuManage: LibSuManage? = null
    companion object {
        fun newInstance(context: Context): Intent {
            val intent = Intent(context, TimerExeService::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)
        initViewFun()
    }
    private fun initViewFun() {
        setSwitchStatus(mStartTimerSwitch, mStartTimer)
        setSwitchStatus(mStartSwitchEnvironmentSwitch, mStartSwitchEnvironment)
        mLibSuManage = LibSuManage.getInstall()

        mS30.setOnClickListener(this)
        mM1.setOnClickListener(this)
        mM10.setOnClickListener(this)
        mM30.setOnClickListener(this)
        mOther.setOnClickListener(this)
        val ztTimerBean = TimerSetManage.get().getZTTimerBean()
        mStartSwitchEnvironmentSwitch.isChecked = TimerSetManage.get().getZTTimerBean().isZeroTermux
        mStartTimerSwitch.isChecked = mLibSuManage!!.isRun
        if (TimerSetManage.get().getZTTimerBean().timerNumber != TimerBean.TIMER_OTHER) {
            switchIndex(ztTimerBean.timerNumber)
        } else {

            mCheckTimerSum.text = "-"
            if (ztTimerBean.timerOtherNumber > (60 * 1000)) {
                mCheckTimerSum.text = "${ztTimerBean.timerOtherNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
            } else {
                mCheckTimerSum.text = "< 1 ${UUtils.getString(R.string.zt_timer_minute)}"
            }
            mS30.setBackgroundResource(R.drawable.shape_line_2e84e6)
            mM1.setBackgroundResource(R.drawable.shape_line_2e84e6)
            mM10.setBackgroundResource(R.drawable.shape_line_2e84e6)
            mM30.setBackgroundResource(R.drawable.shape_line_2e84e6)
            mOther.setBackgroundResource(R.drawable.shape_line_2e84e6)
            mOther.setBackgroundResource(R.drawable.shape_line_8cff5a)
        }

        environmentString()
    }

    private fun setSwitchStatus(switchCompat: SwitchCompat, linearLayout: CardView) {
        linearLayout.setOnClickListener {
            switchCompat.isChecked = !(switchCompat.isChecked)
        }
        switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            when (switchCompat) {
                mStartTimerSwitch -> {
                    if (mStartTimerSwitch.isChecked) {
                        if (mLibSuManage!!.isRun) {
                            return@setOnCheckedChangeListener
                        }
                        LogUtils.e(TAG, "onAddElement: start")
                       // mLibSuManage?.writerDebugFile()
                       // mLibSuManage?.shellCommandExec("shell_main")
                        mLibSuManage?.initRunnable()
                        val intent = Intent(this, TimerExeService::class.java)
                        intent.action = TimerExeService.TIMER_EXE_START
                        startService(intent)
                    } else {
                        mLibSuManage?.logThreadStop()
                        LogUtils.e(TAG, "onAddElement: stop")
                       // mLibSuManage?.stop()
                        val intent = Intent(this, TimerExeService::class.java)
                        intent.action = TimerExeService.TIMER_EXE_END
                        stopService(intent)
                    }
                }
                mStartSwitchEnvironmentSwitch -> {
                    val ztTimerBean = TimerSetManage.get().getZTTimerBean()
                    ztTimerBean.setIsZeroTermux(mStartSwitchEnvironmentSwitch.isChecked)
                    TimerSetManage.get().setZTTimerBean(ztTimerBean)
                    environmentString()
                }
            }
        }
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement: $msg")
    }

    private fun environmentString() {
        val ztTimerBean = TimerSetManage.get().getZTTimerBean()
        mStartSwitchEnvironmentSum.text = if (ztTimerBean.isZeroTermux) {
            "${UUtils.getString(R.string.zt_timer_environment_sum)} ZeroTermux"
        } else {
            "${UUtils.getString(R.string.zt_timer_environment_sum)} Shell"
        }
    }

    private fun switchIndex(timer: Int) {
        LogUtils.e(TAG, "switchIndex timer: $timer")
        mS30.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mM1.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mM10.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mM30.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mOther.setBackgroundResource(R.drawable.shape_line_2e84e6)
        val ztUserBean = TimerSetManage.get().getZTTimerBean()
        when (timer) {
            TimerBean.TIMER_30_SECOND -> {
                mS30.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_30_SECOND
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_30_second)
                TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_1_MINUTE -> {
                mM1.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_1_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_1_minute)
                TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_10_MINUTE -> {
                mM10.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_10_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_10_minute)
                TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_30_MINUTE -> {
                mM30.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_30_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_30_minute)
                TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_OTHER -> {
                mOther.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_OTHER
                val yesNoDialog = YesNoDialog(this)

                yesNoDialog.titleTv.text = UUtils.getString(R.string.zt_timer_other_title_dialog)
                yesNoDialog.show()
                yesNoDialog.noTv.setOnClickListener {
                    yesNoDialog.dismiss()
                }
                yesNoDialog.createEditDialog(UUtils.getString(R.string.zt_timer_other_title_dialog))
                yesNoDialog.yesTv.setOnClickListener {
                    val text = yesNoDialog.inputSystemName.text.toString()
                    try {
                        val toInt = text.toLong()
                        ztUserBean.timerOtherNumber = toInt * 1000L
                        TimerSetManage.get().setZTTimerBean(ztUserBean)
                        mCheckTimerSum.text = "-"
                        if (ztUserBean.timerOtherNumber > (60 * 1000)) {
                            mCheckTimerSum.text = "${ztUserBean.timerOtherNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
                        } else {
                            mCheckTimerSum.text = "< 1 ${UUtils.getString(R.string.zt_timer_minute)}"
                        }
                        yesNoDialog.dismiss()
                    }catch (e: Exception) {
                        UUtils.showMsg(UUtils.getString(R.string.zt_timer_other_input_numer_dialog))
                        return@setOnClickListener
                    }
                }
            }
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.s_30 -> {switchIndex(TimerBean.TIMER_30_SECOND)}
            R.id.m_1 -> {switchIndex(TimerBean.TIMER_1_MINUTE)}
            R.id.m_10 -> {switchIndex(TimerBean.TIMER_10_MINUTE)}
            R.id.m_30 -> {switchIndex(TimerBean.TIMER_30_MINUTE)}
            R.id.other -> {switchIndex(TimerBean.TIMER_OTHER)}
        }
    }
}
