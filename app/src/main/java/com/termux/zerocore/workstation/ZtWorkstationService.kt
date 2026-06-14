package com.termux.zerocore.workstation

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
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class ZtWorkstationService : Service(), ServiceConnection {

    private var httpServer: ZtWorkstationHttpServer? = null
    private var cameraHelper: ZtWorkstationCameraHelper? = null
    private var termuxBound = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        cameraHelper = ZtWorkstationCameraHelper(this).also {
            ZtWorkstationManager.setCameraHelper(it)
        }
        bindTermuxService()
        startHttpServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!UserSetManage.get().getZTUserBean().isZtWorkstationEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        ZtWorkstationManager.clearSessionOnAppExitIfNeeded(this)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        if (termuxBound) {
            unbindService(this)
            termuxBound = false
        }
        ZtWorkstationManager.setTermuxService(null)
        cameraHelper?.stop()
        cameraHelper = null
        ZtWorkstationManager.setCameraHelper(null)
        ZtWorkstationTerminalRelay.shutdown()
        try {
            httpServer?.stop()
        } catch (_: IOException) {
        }
        httpServer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val termuxService = TermuxService.fromBinder(service) ?: return
        ZtWorkstationManager.setTermuxService(termuxService)
        termuxBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        ZtWorkstationManager.setTermuxService(null)
        termuxBound = false
    }

    private fun bindTermuxService() {
        val intent = Intent(this, TermuxService::class.java)
        try {
            termuxBound = bindService(intent, this, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {
            termuxBound = false
        }
    }

    private fun startHttpServer() {
        Thread {
            try {
                if (httpServer?.isAlive == true) return@Thread
                httpServer = ZtWorkstationHttpServer(applicationContext, ZtWorkstationManager.PORT)
                httpServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        val channelId = "zt_workstation"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                UUtils.getString(R.string.zt_workstation_notification_title),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val launchIntent = Intent(this, TermuxActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(UUtils.getString(R.string.zt_workstation_notification_title))
            .setContentText(UUtils.getString(R.string.zt_workstation_notification_text))
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 19999
        @Volatile
        var isRunning = false
    }
}
