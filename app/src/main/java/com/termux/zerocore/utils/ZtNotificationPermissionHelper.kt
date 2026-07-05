package com.termux.zerocore.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.xh_lib.utils.UUtils
import com.termux.R

object ZtNotificationPermissionHelper {

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 打开本 APP 的系统通知设置页。 */
    fun openNotificationSettings(context: Context) {
        val packageName = context.packageName
        val intents = listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                putExtra("app_package", packageName)
                putExtra("app_uid", context.applicationInfo.uid)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        for (intent in intents) {
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    // try next fallback
                }
            }
        }
    }

    /** 提示用户去系统设置开启通知，并跳转通知设置页。 */
    fun promptOpenSettings(context: Context) {
        UUtils.showMsg(UUtils.getString(R.string.zt_timer_notification_open_settings))
        openNotificationSettings(context)
    }

    /**
     * 确保有通知权限：无权限时先尝试系统授权弹窗；仍拒绝则跳转设置。
     * @return true 表示已拥有权限
     */
    fun ensurePermission(activity: Activity, requestCode: Int): Boolean {
        if (hasPermission(activity)) {
            return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
            return false
        }
        val requestedKey = "zt_notification_permission_requested"
        val prefs = activity.getSharedPreferences("zt_permission_flags", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(requestedKey, false)) {
            prefs.edit().putBoolean(requestedKey, true).apply()
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
            return false
        }
        promptOpenSettings(activity)
        return false
    }

    fun onPermissionDenied(activity: Activity) {
        promptOpenSettings(activity)
    }
}
