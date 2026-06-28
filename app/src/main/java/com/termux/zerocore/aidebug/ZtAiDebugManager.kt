package com.termux.zerocore.aidebug

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.termux.app.TermuxService
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtAiDebugManager {

    const val PORT = 19998

    @Volatile
    var termuxService: TermuxService? = null
        private set

    @Volatile
    var cameraHelper: com.termux.zerocore.workstation.ZtWorkstationCameraHelper? = null
        private set

    fun setTermuxService(service: TermuxService?) {
        termuxService = service
    }

    fun setCameraHelper(helper: com.termux.zerocore.workstation.ZtWorkstationCameraHelper?) {
        cameraHelper = helper
    }

    fun start(context: Context) {
        val intent = Intent(context, ZtAiDebugService::class.java)
        ContextCompat.startForegroundService(context.applicationContext, intent)
    }

    fun stop(context: Context) {
        context.applicationContext.stopService(Intent(context, ZtAiDebugService::class.java))
    }

    @JvmStatic
    fun ensureRunningIfEnabled(context: Context) {
        if (UserSetManage.get().getZTUserBean().isZtAiDebugEnabled) {
            start(context)
        }
    }

    @JvmStatic
    fun ensureRunningForActiveSession(context: Context) {
        val bean = UserSetManage.get().getZTUserBean()
        if (bean.isZtAiDebugEnabled && !isRunning()) {
            start(context)
        }
    }

    fun isRunning(): Boolean = ZtAiDebugService.isRunning
}
