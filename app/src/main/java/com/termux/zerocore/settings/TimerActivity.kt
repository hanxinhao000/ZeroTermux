package com.termux.zerocore.settings

import android.app.TimePickerDialog
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
import com.termux.zerocore.settings.timer.TimerScheduleHelper
import com.termux.zerocore.settings.timer.TimerSessionPersist
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.ZtNotificationPermissionHelper
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
    private val mModeInterval: LinearLayout by lazy { findViewById(R.id.timer_mode_interval) }
    private val mModeDaily: LinearLayout by lazy { findViewById(R.id.timer_mode_daily) }
    private val mIntervalPanel: LinearLayout by lazy { findViewById(R.id.timer_interval_panel) }
    private val mDailyPanel: LinearLayout by lazy { findViewById(R.id.timer_daily_panel) }
    private val mDailyTime: LinearLayout by lazy { findViewById(R.id.daily_time) }
    private val mDailyTimeLabel: TextView by lazy { findViewById(R.id.daily_time_label) }
    private val mAlwaysAllowCard: CardView by lazy { findViewById(R.id.always_allow_timer) }
    private val mAlwaysAllowSwitch: SwitchCompat by lazy { findViewById(R.id.always_allow_timer_switch) }
    private val mStartSwitchEnvironmentSum: TextView by lazy { findViewById(R.id.start_switch_environment_sum) }
    private val mCheckTimerSum: TextView by lazy { findViewById(R.id.check_timer_sum) }
    private val mTimerCountdownText: TextView by lazy { findViewById(R.id.timer_countdown_text) }
    private val mTimerCountdownTarget: TextView by lazy { findViewById(R.id.timer_countdown_target) }
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
        if (!ZtNotificationPermissionHelper.ensurePermission(this, REQ_NOTIFICATION_PERMISSION)) {
            return
        }
        initializePage()
    }

    override fun onResume() {
        super.onResume()
        if (!pageInitialized && ZtNotificationPermissionHelper.hasPermission(this)) {
            initializePage()
        }
        if (pageInitialized) {
            reconcileStaleExecutionState()
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
            initializePage()
            return
        }
        ZtNotificationPermissionHelper.onPermissionDenied(this)
    }

    private fun ensureNotificationPermissionForAction(): Boolean {
        if (ZtNotificationPermissionHelper.hasPermission(this)) {
            return true
        }
        return ZtNotificationPermissionHelper.ensurePermission(this, REQ_NOTIFICATION_PERMISSION)
    }

    private fun initializePage() {
        if (pageInitialized) return
        setContentView(R.layout.activity_timer)
        pageInitialized = true
        initViewFun()
    }

    /** 打开页面时校正 UI：LibSu 已结束但 Service 仍标记「执行中」时清除，避免从通知进入后假卡住。 */
    private fun reconcileStaleExecutionState() {
        if (!isTimerRunning()) return
        val shellRunning = mLibSuManage?.isShellCommandRunning == true
        if (!shellRunning &&
            TimerRuntimeState.isExecutingScript() &&
            !TimerRuntimeState.isWaitingForScript()
        ) {
            TimerRuntimeState.setExecutingScript(false)
            TimerRuntimeState.statusMessage = ""
            refreshTimerServiceNotification()
        }
    }

    /** 定时已在运行时仅刷新前台通知，不重启调度。 */
    private fun refreshTimerServiceNotification() {
        if (!TimerRuntimeState.isRunning()) return
        val intent = Intent(this, TimerExeService::class.java).apply {
            action = TimerExeService.TIMER_EXE_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
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
        mModeInterval.setOnClickListener(this)
        mModeDaily.setOnClickListener(this)
        mDailyTime.setOnClickListener(this)
        mEditCodeCard.setOnClickListener { openTimerScriptEditor() }
        mViewLogButton.setOnClickListener { openTimerExecutionLog() }

        val ztTimerBean = TimerSetManage.get().getZTTimerBean()
        mStartSwitchEnvironmentSwitch.isChecked = ztTimerBean.isZeroTermux
        mAlwaysAllowSwitch.isChecked = ztTimerBean.isAlwaysAllowTimer
        setSwitchStatus(mAlwaysAllowSwitch, mAlwaysAllowCard)
        syncSwitchWithServiceState()
        applyTimerModeUi(ztTimerBean.timerMode, persist = false)
        if (ztTimerBean.timerMode == TimerBean.MODE_INTERVAL) {
            mCheckTimerSum.text = TimerScheduleHelper.formatScheduleLabel(ztTimerBean)
            if (ztTimerBean.timerNumber == TimerBean.TIMER_OTHER) {
                applyIntervalSelectionUi(TimerBean.TIMER_OTHER)
            } else {
                applyIntervalSelectionUi(ztTimerBean.timerNumber)
            }
        } else {
            updateDailyTimeLabel(ztTimerBean.scheduledHour, ztTimerBean.scheduledMinute)
            mCheckTimerSum.text = TimerScheduleHelper.formatScheduleLabel(ztTimerBean)
        }
        environmentString()
        reconcileStaleExecutionState()
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
            if (switchCompat !== mAlwaysAllowSwitch && isTimerRunning()) {
                UUtils.showMsg(getString(R.string.zt_timer_cannot_change_while_running))
                return@setOnClickListener
            }
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
                mAlwaysAllowSwitch -> {
                    val bean = TimerSetManage.get().getZTTimerBean()
                    bean.isAlwaysAllowTimer = mAlwaysAllowSwitch.isChecked
                    TimerSetManage.get().setZTTimerBean(bean)
                    if (!bean.isAlwaysAllowTimer) {
                        TimerSessionPersist.clear()
                    } else if (TimerRuntimeState.isRunning()) {
                        TimerSessionPersist.saveIfAllowed()
                    }
                }
            }
        }
    }

    private fun isTimerRunning(): Boolean {
        return TimerRuntimeState.isRunning() || mLibSuManage?.isRun == true
    }

    private fun updateStatusCard() {
        val running = isTimerRunning()
        val bean = TimerSetManage.get().getZTTimerBean()
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
        if (running && bean.timerMode == TimerBean.MODE_DAILY_TIME &&
            !scriptRunning && !TimerRuntimeState.isExecutingScript() && !TimerRuntimeState.isWaitingForScript()
        ) {
            mTimerCountdownTarget.visibility = View.VISIBLE
            mTimerCountdownTarget.text = getString(
                R.string.zt_timer_countdown_target,
                TimerScheduleHelper.formatClock(bean.scheduledHour, bean.scheduledMinute)
            )
        } else {
            mTimerCountdownTarget.visibility = View.GONE
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

    /** 仅恢复间隔选项的高亮，运行中也可调用（与用户点击 switchIndex 区分）。 */
    private fun applyIntervalSelectionUi(timer: Int) {
        resetIntervalSelectionBackground()
        when (timer) {
            TimerBean.TIMER_30_SECOND -> mS30.setBackgroundResource(R.drawable.shape_line_8cff5a)
            TimerBean.TIMER_1_MINUTE -> mM1.setBackgroundResource(R.drawable.shape_line_8cff5a)
            TimerBean.TIMER_10_MINUTE -> mM10.setBackgroundResource(R.drawable.shape_line_8cff5a)
            TimerBean.TIMER_30_MINUTE -> mM30.setBackgroundResource(R.drawable.shape_line_8cff5a)
            TimerBean.TIMER_OTHER -> mOther.setBackgroundResource(R.drawable.shape_line_8cff5a)
        }
    }

    private fun applyTimerModeUi(mode: Int, persist: Boolean) {
        val intervalSelected = mode == TimerBean.MODE_INTERVAL
        mModeInterval.setBackgroundResource(
            if (intervalSelected) R.drawable.shape_line_8cff5a else R.drawable.shape_line_2e84e6
        )
        mModeDaily.setBackgroundResource(
            if (intervalSelected) R.drawable.shape_line_2e84e6 else R.drawable.shape_line_8cff5a
        )
        mIntervalPanel.visibility = if (intervalSelected) View.VISIBLE else View.GONE
        mDailyPanel.visibility = if (intervalSelected) View.GONE else View.VISIBLE
        if (persist) {
            val bean = TimerSetManage.get().getZTTimerBean()
            bean.timerMode = mode
            TimerSetManage.get().setZTTimerBean(bean)
            mCheckTimerSum.text = TimerScheduleHelper.formatScheduleLabel(bean)
        }
    }

    private fun updateDailyTimeLabel(hour: Int, minute: Int) {
        mDailyTimeLabel.text = getString(
            R.string.zt_timer_daily_pick_with_time,
            TimerScheduleHelper.formatClock(hour, minute)
        )
    }

    private fun showDailyTimePicker() {
        if (isTimerRunning()) {
            UUtils.showMsg(getString(R.string.zt_timer_cannot_change_while_running))
            return
        }
        val bean = TimerSetManage.get().getZTTimerBean()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                bean.timerMode = TimerBean.MODE_DAILY_TIME
                bean.scheduledHour = hour
                bean.scheduledMinute = minute
                TimerSetManage.get().setZTTimerBean(bean)
                applyTimerModeUi(TimerBean.MODE_DAILY_TIME, persist = false)
                updateDailyTimeLabel(hour, minute)
                mCheckTimerSum.text = TimerScheduleHelper.formatScheduleLabel(bean)
            },
            bean.scheduledHour,
            bean.scheduledMinute,
            true
        ).show()
    }

    private fun switchTimerMode(mode: Int) {
        if (isTimerRunning()) {
            UUtils.showMsg(getString(R.string.zt_timer_cannot_change_while_running))
            return
        }
        val bean = TimerSetManage.get().getZTTimerBean()
        if (mode == TimerBean.MODE_DAILY_TIME) {
            showDailyTimePicker()
            return
        }
        bean.timerMode = TimerBean.MODE_INTERVAL
        TimerSetManage.get().setZTTimerBean(bean)
        applyTimerModeUi(TimerBean.MODE_INTERVAL, persist = false)
        if (bean.timerNumber == TimerBean.TIMER_OTHER) {
            mCheckTimerSum.text = TimerScheduleHelper.formatScheduleLabel(bean)
        } else {
            switchIndex(bean.timerNumber, persist = false)
        }
    }

    private fun formatCustomIntervalLabel(millis: Long): String {
        val bean = TimerBean()
        bean.timerMode = TimerBean.MODE_INTERVAL
        bean.timerNumber = TimerBean.TIMER_OTHER
        bean.timerOtherNumber = millis
        return TimerScheduleHelper.formatScheduleLabel(bean)
    }

    private fun switchIndex(timer: Int, persist: Boolean = true) {
        if (isTimerRunning()) {
            UUtils.showMsg(getString(R.string.zt_timer_cannot_change_while_running))
            return
        }
        LogUtils.e(TAG, "switchIndex timer: $timer")
        applyIntervalSelectionUi(timer)
        val ztUserBean = TimerSetManage.get().getZTTimerBean()
        ztUserBean.timerMode = TimerBean.MODE_INTERVAL
        applyTimerModeUi(TimerBean.MODE_INTERVAL, persist = false)
        when (timer) {
            TimerBean.TIMER_30_SECOND -> {
                ztUserBean.timerNumber = TimerBean.TIMER_30_SECOND
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_30_second)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_1_MINUTE -> {
                ztUserBean.timerNumber = TimerBean.TIMER_1_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_1_minute)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_10_MINUTE -> {
                ztUserBean.timerNumber = TimerBean.TIMER_10_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_10_minute)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_30_MINUTE -> {
                ztUserBean.timerNumber = TimerBean.TIMER_30_MINUTE
                mCheckTimerSum.text = UUtils.getString(R.string.zt_timer_30_minute)
                if (persist) TimerSetManage.get().setZTTimerBean(ztUserBean)
            }
            TimerBean.TIMER_OTHER -> {
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
            R.id.timer_mode_interval -> switchTimerMode(TimerBean.MODE_INTERVAL)
            R.id.timer_mode_daily -> switchTimerMode(TimerBean.MODE_DAILY_TIME)
            R.id.daily_time -> showDailyTimePicker()
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
