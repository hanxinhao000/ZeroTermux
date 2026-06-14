package com.termux.zerocore.workstation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.termux.app.TermuxService
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtWorkstationManager {

    const val PORT = 19999

    @Volatile
    var termuxService: TermuxService? = null
        private set

    @Volatile
    var cameraHelper: ZtWorkstationCameraHelper? = null
        private set

    fun setTermuxService(service: TermuxService?) {
        termuxService = service
    }

    fun setCameraHelper(helper: ZtWorkstationCameraHelper?) {
        cameraHelper = helper
    }

    fun start(context: Context) {
        val intent = Intent(context, ZtWorkstationService::class.java)
        ContextCompat.startForegroundService(context.applicationContext, intent)
    }

    fun stop(context: Context) {
        context.applicationContext.stopService(Intent(context, ZtWorkstationService::class.java))
    }

    @JvmStatic
    fun ensureRunningIfEnabled(context: Context) {
        val bean = UserSetManage.get().getZTUserBean()
        if (bean.isZtWorkstationEnabled && bean.isZtWorkstationAutoStart) {
            start(context)
        }
    }

    /** Start workstation while the app session is active (master on, auto-start off). */
    @JvmStatic
    fun ensureRunningForActiveSession(context: Context) {
        val bean = UserSetManage.get().getZTUserBean()
        if (bean.isZtWorkstationEnabled && !isRunning()) {
            start(context)
        }
    }

    /**
     * When auto-start is off, closing the app clears master + auto-start and stops the service.
     */
    @JvmStatic
    fun clearSessionOnAppExitIfNeeded(context: Context) {
        val bean = UserSetManage.get().getZTUserBean()
        if (bean.isZtWorkstationAutoStart) return
        var changed = false
        if (bean.isZtWorkstationEnabled) {
            bean.isZtWorkstationEnabled = false
            changed = true
        }
        if (bean.isZtWorkstationAutoStart) {
            bean.isZtWorkstationAutoStart = false
            changed = true
        }
        if (changed) {
            UserSetManage.get().setZTUserBean(bean)
        }
        if (isRunning()) {
            stop(context.applicationContext)
        }
    }

    fun isRunning(): Boolean = ZtWorkstationService.isRunning
}
