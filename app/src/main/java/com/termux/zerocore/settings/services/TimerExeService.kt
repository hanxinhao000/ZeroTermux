package com.termux.zerocore.settings.services


import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Message
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.TimerSetManage
import com.termux.zerocore.libsu.LibSuManage
import com.termux.zerocore.settings.timer.TimerBean
import com.termux.zerocore.utils.NotificationUtils
import com.topjohnwu.superuser.Shell
import com.zp.z_file.util.LogUtils
import java.util.Locale


class TimerExeService : Service(), LibSuManage.TimerListener {

    companion object {
        public const val TIMER_EXE_START = "timer_exe_start"
        public const val TIMER_EXE_END = "timer_exe_end"
        private const val TAG = "TimerExeService"
        private const val NOTIFICATION_ID = 1556
        private const val IS_DEBUG = false
    }

    private var mTimerBean: TimerBean? = null
    private var mLibSuManage: LibSuManage? = null
    private var mCountDownTimer: CountDownTimer? = null
     class TimerExeLocalBinder : Binder {
        val service: TimerExeService
        constructor(timerExeService: TimerExeService) {
            service = timerExeService
        }
    }

    private val mHandler1: Handler =
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            LogUtils.e(TAG, "startTimerNum  sendHandler.....RUN")
            val cachedShell = Shell.getCachedShell()
            if (cachedShell == null || !(cachedShell.isAlive)) {
                execCommand()
                startTimerNum()
            } else {
                LibSuManage.getInstall().stop()
                mHandler2.sendMessageDelayed(Message(), 5000)
            }

        }
    }

    private val mHandler2: Handler =
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val cachedShell = Shell.getCachedShell()
            if (cachedShell == null || !(cachedShell.isAlive)) {
                execCommand()
                startTimerNum()
            } else {
                LibSuManage.getInstall().stop()
                mHandler1.sendMessageDelayed(Message(), 5000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mTimerBean = TimerSetManage.get().getZTTimerBean()
        mLibSuManage = LibSuManage.getInstall()
        mLibSuManage?.setTimerListener(this)
        mLibSuManage?.cunt = 0
        if (IS_DEBUG) {
            mLibSuManage?.deleteAllFile()
        }
        if (!(mLibSuManage!!.isFileExists)) {
            mLibSuManage?.writerFile()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null) {
                when (action) {
                    // 启动定时任务
                    TIMER_EXE_START -> {
                        LogUtils.e(TAG, "onStartCommand....TIMER_EXE_START")
                        startTimer()
                    }
                    //关闭定时任务
                    TIMER_EXE_END -> {
                        LogUtils.e(TAG, "onStartCommand....TIMER_EXE_END")
                        //endTime()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun endTime() {
        LibSuManage.getInstall().stop()
        NotificationUtils.cancelNotification(applicationContext, NOTIFICATION_ID)
        mHandler1.removeCallbacksAndMessages(null)
        mHandler2.removeCallbacksAndMessages(null)
        mCountDownTimer?.cancel()
    }
    private fun startTimer() {
       // mLibSuManage?.writerDebugFile()
       // mLibSuManage?.shellCommandExec("shell_main")
        NotificationUtils.showNotification(applicationContext,
            NOTIFICATION_ID,
            UUtils.getString(R.string.zt_timer_notification_timer_title),
            "${UUtils.getString(R.string.zt_timer_notification_timer_sum)} ${getTime()}\n${UUtils.getString(R.string.zt_timer_notification_timer_cunt)} ${mLibSuManage!!.cunt}"
        )
        startTimerNum()
    }

    private fun execCommand() {
        if (mLibSuManage == null) {
            return
        }
        var cunt = mLibSuManage!!.cunt
        mLibSuManage!!.cunt = cunt + 1
        mTimerBean =  TimerSetManage.get().getZTTimerBean()
        if (mTimerBean!!.isZeroTermux) {
            mLibSuManage?.shellCommandExec("shell_ZeroTermux")
        } else {
            mLibSuManage?.shellCommandExec("shell_Android")
        }
    }
    //开始计时
    private fun startTimerNum() {
        mLibSuManage?.stop()
        var time = 0L
        if (TimerSetManage.get().getZTTimerBean().timerNumber == TimerBean.TIMER_OTHER) {
            time = mTimerBean!!.timerOtherNumber
        } else {
            time = mTimerBean!!.timerNumber.toLong()
        }

        mCountDownTimer = object : CountDownTimer(time, 1000) {
            // 参数分别是总时长和间隔时长（这里是1秒）
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val timeLeftFormatted = String.format(
                    Locale.getDefault(), "%02d:%02d:%02d",
                    hours % 24, minutes % 60, seconds % 60
                )
                NotificationUtils.updateNotification(
                    applicationContext,
                    NOTIFICATION_ID,
                    UUtils.getString(R.string.zt_timer_notification_timer_title),
                    "${UUtils.getString(R.string.zt_timer_notification_timer_sum)} $timeLeftFormatted \n" +
                        "${UUtils.getString(R.string.zt_timer_notification_timer_cunt)} ${mLibSuManage!!.cunt}"
                )
            }

            override fun onFinish() {
                NotificationUtils.updateNotification(
                    applicationContext,
                    NOTIFICATION_ID,
                    UUtils.getString(R.string.zt_timer_notification_timer_title),
                    "${UUtils.getString(R.string.zt_timer_notification_timer_kill)}"
                )
                LogUtils.e(TAG, "startTimerNum  sendHandler.....")
                mHandler1.sendMessageDelayed(Message(), 5000)
            }
        }
        mCountDownTimer?.start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return TimerExeLocalBinder(this)
    }

    private fun getTime(): String {
        if (TimerSetManage.get().getZTTimerBean().timerNumber == TimerBean.TIMER_OTHER) {
            if (mTimerBean!!.timerOtherNumber > (60 * 1000)) {
                return "${mTimerBean!!.timerOtherNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
            } else {
                return "< 1 ${UUtils.getString(R.string.zt_timer_minute)}"
            }
        } else {
            return "${mTimerBean!!.timerNumber / 60 / 1000} ${UUtils.getString(R.string.zt_timer_minute)}"
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.e(TAG, "onDestroy....")
        endTime()
    }

    override fun onAddElement(msg: String?) {
        LogUtils.e(TAG, "onAddElement....: $msg")

    }
}
