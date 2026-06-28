package com.termux.zerocore.aidebug

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.google.gson.Gson
import com.hjq.permissions.Permission

object ZtAiDebugPhoneHelper {

    private val gson = Gson()

    fun phoneInfo(context: Context): String {
        if (!ZtAiDebugPermissionHelper.isGranted(context, Permission.READ_PHONE_STATE)) {
            return ZtAiDebugPermissionHelper.denyJson(context, "phone")
        }
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val line1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                tm?.line1Number ?: ""
            } else {
                @Suppress("DEPRECATION")
                tm?.line1Number ?: ""
            }
            gson.toJson(
                mapOf(
                    "ok" to true,
                    "line1Number" to (line1 ?: ""),
                    "simOperatorName" to (tm?.simOperatorName ?: ""),
                    "networkOperatorName" to (tm?.networkOperatorName ?: ""),
                    "simCountryIso" to (tm?.simCountryIso ?: ""),
                    "networkCountryIso" to (tm?.networkCountryIso ?: ""),
                    "phoneType" to (tm?.phoneType ?: TelephonyManager.PHONE_TYPE_NONE),
                    "simState" to (tm?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN)
                )
            )
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to (e.message ?: "phone info failed")))
        }
    }
}
