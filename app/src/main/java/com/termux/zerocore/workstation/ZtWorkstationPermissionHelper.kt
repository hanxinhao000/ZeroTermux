package com.termux.zerocore.workstation

import com.google.gson.Gson
import com.termux.zerocore.bean.ZTUserBean
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtWorkstationPermissionHelper {

    private val gson = Gson()

    private fun bean(): ZTUserBean = UserSetManage.get().getZTUserBean()

    fun isMasterEnabled(): Boolean = bean().isZtWorkstationEnabled

    fun isAutoStartEnabled(): Boolean = bean().isZtWorkstationAutoStart

    fun isTerminalAllowed(): Boolean =
        isMasterEnabled() && bean().isZtWorkstationTerminalEnabled

    fun isCameraAllowed(): Boolean =
        isMasterEnabled() && bean().isZtWorkstationCameraEnabled

    fun isFilesAllowed(): Boolean =
        isMasterEnabled() && bean().isZtWorkstationFilesEnabled

    fun isPhoneSmsAllowed(): Boolean =
        isMasterEnabled() && bean().isZtWorkstationPhoneSmsEnabled

    fun toJson(): String {
        val b = bean()
        return gson.toJson(
            mapOf(
                "ok" to true,
                "master" to b.isZtWorkstationEnabled,
                "autoStart" to b.isZtWorkstationAutoStart,
                "terminal" to isTerminalAllowed(),
                "camera" to isCameraAllowed(),
                "files" to isFilesAllowed(),
                "phoneSms" to isPhoneSmsAllowed()
            )
        )
    }
}
