package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson

object ZtAiDebugRootGate {

    private val gson = Gson()

    fun isAllowed(context: Context): Boolean {
        return ZtAiDebugRootHelper.isRootModeEnabled() && ZtAiDebugRootHelper.isRootAvailable(context)
    }

    fun denyJson(context: Context, feature: String = "root"): String {
        val rootMode = ZtAiDebugRootHelper.isRootModeEnabled()
        val rooted = ZtAiDebugRootHelper.isRootAvailable(context)
        return gson.toJson(
            mapOf(
                "ok" to false,
                "error" to when {
                    !rootMode -> "root_mode_disabled"
                    !rooted -> "root_unavailable"
                    else -> "root_access_denied"
                },
                "feature" to feature,
                "root_mode_enabled" to rootMode,
                "root_available" to rooted,
                "hint_for_ai" to when {
                    !rootMode ->
                        "Ask user to enable「Root 完全调试」in ZeroTermux Settings → External AI debug. Warn about brick/data loss risks."
                    !rooted ->
                        "Root mode is on but su failed. Ask user to grant ZeroTermux root in Magisk/SU and retry GET /api/root/status."
                    else -> "Root gate blocked unexpectedly."
                },
                "hint_for_user_zh" to when {
                    !rootMode ->
                        "请在 ZeroTermux 设置 → 外部 AI 调试 → 开启「Root 完全调试」。此功能极高危，AI 可完全控制手机（含变砖风险）。"
                    !rooted ->
                        "已开启 Root 调试，但未获得 su 权限。请在 Magisk/SU 管理器中授予 ZeroTermux Root 权限。"
                    else -> "Root 接口不可用，请检查 su 权限。"
                },
                "hint_for_user_en" to when {
                    !rootMode ->
                        "Enable「Root full debug」under External AI debug in ZeroTermux Settings. CRITICAL: AI may fully control the device including brick risk."
                    !rooted ->
                        "Root debug is enabled but su is unavailable. Grant ZeroTermux root in Magisk/SU manager."
                    else -> "Root API unavailable; check su access."
                }
            )
        )
    }
}
