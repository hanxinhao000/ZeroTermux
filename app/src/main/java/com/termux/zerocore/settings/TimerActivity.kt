package com.termux.zerocore.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.activity.EditTextActivity
import com.termux.zerocore.dialog.YesNoDialog
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.services.TimerExeService
import com.termux.zerocore.settings.timer.TimerBean
import com.termux.zerocore.settings.timer.TimerExecutionLog
import com.termux.zerocore.settings.timer.TimerNotificationHelper
import com.termux.zerocore.settings.timer.TimerRuntimeState
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.SingletonCommunicationUtils
import com.zp.z_file.util.LogUtils
import java.io.File

class TimerActivity : AppCompatActivity(), LibSuManage.TimerListener, View.OnClickListener {
    private val TAG = "TimerActivity"
    private val mStartTimer: CardView by lazy { findViewById(R.id.start_timer) }
    private val mStartTimerSwitch: SwitchCompat by lazy { findViewById(R.id.start_timer_switch) }
    private val mStartSwitchEnvironment: CardView by lazy { findViewById(R.id.start_switch_environment) }
    private val mStartSwitchEnvironmentSwitch: SwitchCompat by lazy { findViewById(R.id.start_switch_environment_switch) }
    private val mS30: LinearLayout by lazy { findViewById(R.id.s_30) }
    private val mM1: LinearLayout by lazy { findViewById(R.id.m_1) }
    private val mM10: LinearLayout by lazy { findViewById(R.id.m_10) }
    private val mM30: LinearLayout by lazy { findViewById(R.id.m_30) }
    private val mOther: LinearLayout by lazy { findViewById(R.id.other) }
    private val mStartSwitchEnvironmentSum: TextView by lazy { findViewById(R.id.start_switch_environment_sum) }
    private val mCheckTimerSum: TextView by lazy { findViewById(R.id.check_timer_sum) }
    private val mTimerCountdownText: TextView by lazy { findViewById(R.id.timer_countdown_text) }
    private val mTimerExecuteCountText: TextView by lazy { findViewById(R.id.timer_execute_count_text) }
    private val mEditCodeCard: CardView by lazy { findViewById(R.id.edit_code) }
    private val mViewLogButton: TextView by lazy { findViewById(R.id.view_log) }
    private val mExecutionLogText: TextView by lazy { findViewById(R.id.timer_execution_log_text) }
    private var mLibSuManage: LibSuManage? = null
    private var pageInitialized = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val uiTickRunnable = object : Runnable {
        override fun run() {
            updateStatusCard()
            refreshExecutionLogPreview()
            uiHandler.postDelayed(this, 1000L)
        }
    }

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "timer_from_notification"
        private const val REQ_NOTIFICATION_PERMISSION = 3001

        fun notificationIntent(context: Context): Intent {
            return TimerNotificationHelper.buildOpenTimerIntent(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureNotificationPermission()) {
            return
        }
        if (!handleNotificationEntry()) {
            return
        }
        initializePage()
    }

    override fun onResume() {
        super.onResume()
        if (pageInitialized) {
            syncSwitchWithServiceState()
            updateStatusCard()
            uiHandler.post(uiTickRunnable)
        }
    }

    override fun onPause() {
        uiHandler.removeCallbacks(uiTickRunnable)
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_NOTIFICATION_PERMISSION) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!handleNotificationEntry()) return
            initializePage()
            return
        }
        UUtils.showMsg(getString(R.string.zt_timer_notification_permission_required))
        finish()
    }

    private fun ensureNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_NOTIFICATION_PERMISSION
        )
        return false
    }

    private fun handleNotificationEntry(): Boolean {
        if (!intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            return true
        }
        val isZeroTermux = TimerSetManage.get().getZTTimerBean().isZeroTermux
        if (!isZeroTermux) {
            return true
        }
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            UUtils.showMsg(getString(R.string.zt_timer_main_program_missing))
            finish()
            return false
        }
        return true
    }

    private fun initializePage() {
        if (pageInitialized) return
        setContentView(R.layout.activity_timer)
        pageInitialized = true
        initViewFun()
    }

    private fun initViewFun() {
        setSwitchStatus(mStartTimerSwitch, mStartTimer)
        setSwitchStatus(mStartSwitchEnvironmentSwitch, mStartSwitchEnvironment)
        mLibSuManage = LibSuManage.getInstall()
        if (!mLibSuManage!!.isFileExists) {
            mLibSuManage?.writerFile()
        }

        mS30.setOnClickListener(this)
        mM1.setOnClickListener(this)
        mM10.setOnClickListener(this)
        mM30.setOnClickListener(this)
        mOther.setOnClickListener(this)
        mEditCodeCard.setOnClickListener { openTimerScriptEditor() }
        mViewLogButton.setOnClickListener { openTimerExecutionLog() }

        val ztTimerBean = TimerSetManage.get().getZTTimerBean()
        mStartSwitchEnvironmentSwitch.isChecked = ztTimerBean.isZeroTermux
        syncSwitchWithServiceState()
        if (ztTimerBean.timerNumber != TimerBean.TIMER_OTHER) {
            switchIndex(ztTimerBean.timerNumber, persist = false)
        } else {
            mCheckTimerSum.text = formatCustomIntervalLabel(ztTimerBean.timerOtherNumber)
            resetIntervalSelectionBackground()
            mOther.setBackgroundResource(R.drawable.shape_line_8cff5a)
        }
        environmentString()
        updateStatusCard()
        refreshExecutionLogPreview()
    }

    private fun syncSwitchWithServiceState() {
        val running = TimerRuntimeState.isRunning() || mLibSuManage?.isRun == true
        if (mStartTimerSwitch.isChecked != running) {
            mStartTimerSwitch.setOnCheckedChangeListener(null)
            mStartTimerSwitch.isChecked = running
            setSwitchStatus(mStartTimerSwitch, mStartTimer)
        }
    }

    private fun setSwitchStatus(switchCompat: SwitchCompat, linearLayout: CardView) {
        linearLayout.setOnClickListener {
            switchCompat.isChecked = !switchCompat.isChecked
        }
        switchCompat.setOnCheckedChangeListener { _, _ ->
            when (switchCompat) {
                mStartTimerSwitch -> {
                    if (mStartTimerSwitch.isChecked) {
                        if (!ensureNotificationPermissionForAction()) {
                            mStartTimerSwitch.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                        if (TimerRuntimeState.isRunning() || mLibSuManage?.isRun == true) {
                            return@setOnCheckedChangeListener
                        }
                        LogUtils.e(TAG, "start timer")
                        mLibSuManage?.cunt = 0
                        TimerRuntimeState.setExecutionCount(0)
                        mLibSuManage?.initRunnable(TimerSetManage.get().getZTTimerBean().isZeroTermux)
                        startTimerService()
                    } else {
                        mLibSuManage?.logThreadStop()
                        LogUtils.e(TAG, "stop timer")
                        stopTimerService()
                    }
                    updateStatusCard()
                }
                mStartSwitchEnvironmentSwitch -> {
                    val ztTimerBean = TimerSetManage.get().getZTTimerBean()
                    ztTimerBean.setIsZeroTermux(mStartSwitchEnvironmentSwitch.isChecked)
                    TimerSetManage.get().setZTTimerBean(ztTimerBean)
                    environmentString()
                    refreshExecutionLogPreview()
                }
            }
        }
    }

    private fun ensureNotificationPermissionForAction(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        UUtils.showMsg(getString(R.string.zt_timer_notification_permission_required))
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_NOTIFICATION_PERMISSION
        )
        return false
    }

    private fun updateStatusCard() {
        val running = TimerRuntimeState.isRunning() || mLibSuManage?.isRun == true
        val scriptRunning = mLibSuManage?.isShellCommandRunning == true
        mTimerCountdownText.text = when {
            !running -> "--:--"
            scriptRunning && TimerRuntimeState.isWaitingForScript() ->
                getString(R.string.zt_timer_waiting_script)
            scriptRunning || TimerRuntimeState.isExecutingScript() ->
                getString(R.string.zt_timer_executing_current_script)
            TimerRuntimeState.isWaitingForScript() ->
                getString(R.string.zt_timer_waiting_script)
            else -> TimerRuntimeState.formatCountdown()
        }
        val count = if (running) {
            TimerRuntimeState.getExecutionCount()
        } else {
            mLibSuManage?.cunt ?: 0
        }
        mTimerExecuteCountText.text = count.toString()
    }

    private fun openTimerScriptEditor() {
        mLibSuManage?.writerFile()
        val scriptPath = if (TimerSetManage.get().getZTTimerBean().isZeroTermux) {
            FileUrl.timerTermuxFile
        } else {
            FileUrl.timerShellFile
        }
        val scriptFile = File(scriptPath)
        if (!scriptFile.exists()) {
            UUtils.showMsg(getString(R.string.zt_timer_script_missing))
            return
        }
        val intent = Intent(this, EditTextActivity::class.java)
        intent.putExtra("edit_path", scriptFile.absolutePath)
        startActivity(intent)
    }

    private fun openTimerExecutionLog() {
        TimerExecutionLog.ensureLogDir()
        val isZeroTermux = TimerSetManage.get().getZTTimerBean().isZeroTermux
        val logFile = TimerExecutionLog.logFile(isZeroTermux)
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (_: Exception) {
                UUtils.showMsg(getString(R.string.zt_timer_script_missing))
                return
            }
        }
        val intent = Intent(this, EditTextActivity::class.java)
        intent.putExtra("edit_path", logFile.absolutePath)
        startActivity(intent)
    }

    private fun refreshExecutionLogPreview() {
        val isZeroTermux = TimerSetManage.get().getZTTimerBean().isZeroTermux
        val tail = TimerExecutionLog.readLastLines(isZeroTermux, maxLines = 100)
        mExecutionLogText.text = if (tail.isBlank()) {
            getString(R.string.zt_timer_log_empty)
        } else {
            tail
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

    private fun resetIntervalSelectionBackground() {
        mS30.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mM1.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mM10.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mM30.setBackgroundResource(R.drawable.shape_line_2e84e6)
        mOther.setBackgroundResource(R.drawable.shape_line_2e84e6)
    }

    private fun formatCustomIntervalLabel(millis: Long): String {
        return if (millis >= 60 * 1000) {
            "${millis / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
        } else if (millis >= 1000) {
            "${millis / 1000} ${UUtils.getString(R.string.zt_timer_second_unit)}"
        } else {
            "< 1 ${UUtils.getString(R.string.zt_timer_second_unit)}"
        }
    }

    private fun switchIndex(timer: Int, persist: Boolean = true) {
        LogUtils.e(TAG, "switchIndex timer: $timer")
        resetIntervalSelectionBackground()
        val ztUserBean = TimerSetManage.get().getZTTimerBean()
        when (timer) {
            TimerBean.TIMER_30_SECOND -> {
                mS30.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_30_SECOND
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_30_second)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_1_MINUTE -> {
                mM1.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_1_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_1_minute)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_10_MINUTE -> {
                mM10.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_10_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_10_minute)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_30_MINUTE -> {
                mM30.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_30_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_30_minute)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_OTHER -> {
                mOther.setBackgroundResource(R.drawable.shape_line_8cff5a)
                ztUserBean.timerNumber = TimerBean.TIMER_OTHER
                val yesNoDialog = YesNoDialog(this)
                yesNoDialog.titleTv.text = UUtils.getString(R.string.zt_timer_other_title_dialog)
                yesNoDialog.show()
                yesNoDialog.noTv.setOnClickListener { yesNoDialog.dismiss() }
                yesNoDialog.createEditDialog(UUtils.getString(R.string.zt_timer_other_title_dialog))
                yesNoDialog.yesTv.setOnClickListener {
                    val text = yesNoDialog.inputSystemName.text.toString()
                    try {
                        val seconds = text.toLong()
                        ztUserBean.timerOtherNumber = seconds * 1000L
                        TimerSetManage.get().setZTTimerBean(ztUserBean)
                        mCheckTimerSum.text = formatCustomIntervalLabel(ztUserBean.timerOtherNumber)
                        yesNoDialog.dismiss()
                    } catch (e: Exception) {
                        UUtils.showMsg(UUtils.getString(R.string.zt_timer_other_input_numer_dialog))
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.s_30 -> switchIndex(TimerBean.TIMER_30_SECOND)
            R.id.m_1 -> switchIndex(TimerBean.TIMER_1_MINUTE)
            R.id.m_10 -> switchIndex(TimerBean.TIMER_10_MINUTE)
            R.id.m_30 -> switchIndex(TimerBean.TIMER_30_MINUTE)
            R.id.other -> switchIndex(TimerBean.TIMER_OTHER)
        }
    }

    private fun startTimerService() {
        val intent = Intent(this, TimerExeService::class.java)
        intent.action = TimerExeService.TIMER_EXE_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopTimerService() {
        val intent = Intent(this, TimerExeService::class.java)
        intent.action = TimerExeService.TIMER_EXE_END
        startService(intent)
    }
}
