package com.termux.zerocore.workstation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.google.gson.Gson
import com.termux.BuildConfig

object ZtWorkstationDeviceHelper {

    private val gson = Gson()

    fun getDeviceInfo(context: Context): String {
        val battery = getBatteryPercent(context)
        val androidVersion = Build.VERSION.RELEASE ?: "unknown"
        val ztVersion = BuildConfig.VERSION_NAME ?: "unknown"
        return gson.toJson(
            mapOf(
                "ok" to true,
                "battery" to battery,
                "androidVersion" to androidVersion,
                "ztVersion" to ztVersion
            )
        )
    }

    private fun getBatteryPercent(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (capacity in 0..100) return capacity
        }
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return level * 100 / scale
    }
}
