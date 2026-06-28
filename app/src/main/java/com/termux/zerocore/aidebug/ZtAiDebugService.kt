package com.termux.zerocore.aidebug

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.app.TermuxService
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.workstation.ZtWorkstationCameraHelper
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class ZtAiDebugService : Service(), ServiceConnection {

    private var httpServer: ZtAiDebugHttpServer? = null
    private var cameraHelper: ZtWorkstationCameraHelper? = null
    private var termuxBound = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ZtAiDebugMatchCodeHelper.ensureCode()
        ZtAiDebugForegroundActivity.ensureRegistered(application)
        startForeground(NOTIFICATION_ID, buildNotification())
        cameraHelper = ZtWorkstationCameraHelper(this).also {
            ZtAiDebugManager.setCameraHelper(it)
            it.startCameras()
        }
        bindTermuxService()
        startHttpServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!UserSetManage.get().getZTUserBean().isZtAiDebugEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        if (termuxBound) {
            unbindService(this)
            termuxBound = false
        }
        ZtAiDebugManager.setTermuxService(null)
        cameraHelper?.stop()
        cameraHelper = null
        ZtAiDebugManager.setCameraHelper(null)
        try {
            httpServer?.stop()
        } catch (_: IOException) {
        }
        httpServer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        ZtAiDebugManager.setTermuxService(TermuxService.fromBinder(service))
        termuxBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        ZtAiDebugManager.setTermuxService(null)
        termuxBound = false
    }

    private fun bindTermuxService() {
        try {
            termuxBound = bindService(
                Intent(this, TermuxService::class.java),
                this,
                Context.BIND_AUTO_CREATE
            )
        } catch (_: Exception) {
            termuxBound = false
        }
    }

    private fun startHttpServer() {
        Thread {
            try {
                if (httpServer?.isAlive == true) return@Thread
                httpServer = ZtAiDebugHttpServer(applicationContext, ZtAiDebugManager.PORT)
                httpServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        val channelId = "zt_ai_debug"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                UUtils.getString(R.string.zt_ai_debug_notification_title),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TermuxActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(UUtils.getString(R.string.zt_ai_debug_notification_title))
            .setContentText(UUtils.getString(R.string.zt_ai_debug_notification_text))
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 19998
        @Volatile
        var isRunning = false
    }
}
