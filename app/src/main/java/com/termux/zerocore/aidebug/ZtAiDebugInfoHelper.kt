package com.termux.zerocore.aidebug

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.termux.BuildConfig
import com.termux.zerocore.ftp.new_ftp.utils.NetworkEnvironmentUtil
import com.termux.zerocore.workstation.ZtWorkstationDeviceHelper

object ZtAiDebugInfoHelper {

    private val gson = Gson()
    private const val API_VERSION = 5

    fun publicLockedDiscovery(context: Context): String {
        val ips = NetworkEnvironmentUtil.getLocalIpv4Addresses()
        val port = ZtAiDebugManager.PORT
        val sampleIp = ips.firstOrNull() ?: "<phone-ip>"
        val docs = ZtAiDebugApiDocs.buildI18nDocs(port, sampleIp, "XXXXXXX")
        return gson.toJson(
            mapOf(
                "ok" to false,
                "locked" to true,
                "service" to "ZeroTermux External AI Debug API",
                "port" to port,
                "version" to API_VERSION,
                "auth" to mapOf(
                    "required" to true,
                    "match_code" to mapOf(
                        "format" to "7-digit number",
                        "query_param" to ZtAiDebugMatchCodeHelper.QUERY_PARAM,
                        "header" to ZtAiDebugMatchCodeHelper.HEADER_NAME,
                        "example" to "http://$sampleIp:$port/?code=1234567"
                    )
                ),
                "risk_warning" to mapOf(
                    "level" to "CRITICAL",
                    "summary_zh" to "此接口需匹配码才能访问。【无论是否 Root】匹配码可让 AI 完全控制手机全部可访问数据。开启 Root 完全调试后 AI 还可变砖、黑屏、永久丢失数据。切勿将匹配码提供给不信任的人。",
                    "summary_en" to "Match code required. 【With or without Root】Code grants AI full control. Root mode adds brick/black-screen risk. Never share with untrusted parties."
                ),
                "hint_for_ai" to "Read the docs and for_ai fields in this JSON (multilingual). Ask user for 7-digit match code from ZeroTermux Settings → External AI debug → eye icon. Then GET /?code=XXXXXXX",
                "hint_for_user_zh" to "请在 ZeroTermux 设置中开启「启用外部AI调用ZeroTermux」，点击匹配码旁的眼睛图标查看匹配码后提供给 AI。",
                "hint_for_user_en" to "Enable External AI debug in ZeroTermux Settings, tap the eye icon to reveal the 7-digit match code, and give it to your AI.",
                "lan_ips" to ips,
                "html_docs" to "GET /?format=html or /?format=html&lang=zh|en for human-readable documentation",
                "docs" to docs,
                "for_ai" to ZtAiDebugApiDocs.buildForAiBlock(
                    authorized = false,
                    port = port,
                    sampleIp = sampleIp,
                    code = null
                )
            )
        )
    }

    fun discovery(context: Context): String {
        val ips = NetworkEnvironmentUtil.getLocalIpv4Addresses()
        val port = ZtAiDebugManager.PORT
        val code = ZtAiDebugMatchCodeHelper.getStoredCode()
        val sampleIp = ips.firstOrNull() ?: "<phone-ip>"
        val codePh = code ?: "XXXXXXX"
        val lanUrls = ips.map { "http://$it:$port/?code=$codePh" }
        val permStatus = gson.fromJson(
            ZtAiDebugPermissionHelper.statusJson(context),
            Map::class.java
        )
        val docs = ZtAiDebugApiDocs.buildI18nDocs(port, sampleIp, codePh)
        return gson.toJson(
            mapOf(
                "ok" to true,
                "locked" to false,
                "service" to "ZeroTermux External AI Debug API",
                "port" to port,
                "version" to API_VERSION,
                "auth" to mapOf(
                    "match_code_required" to true,
                    "query_param" to ZtAiDebugMatchCodeHelper.QUERY_PARAM,
                    "header" to ZtAiDebugMatchCodeHelper.HEADER_NAME,
                    "usage" to "Append ?code=7-digit-code to ALL requests including this one",
                    "current_code_hint" to "Match code is configured; use the code the user provided — do not log it"
                ),
                "risk_warning" to mapOf(
                    "level" to "CRITICAL",
                    "summary_zh" to "同一局域网内的 AI 可通过本接口完全控制手机：终端/exec、截屏、UI 自动化、logcat、短信/联系人/文件等。【Root 模式】还可 su 变砖、黑屏。用完必须关闭。",
                    "summary_en" to "LAN AI can fully control device: terminal, screenshot, UI automation, logcat, SMS/contacts/files. Root mode adds su/brick risk. Disable when done.",
                    "capabilities" to listOf(
                        "terminal_exec",
                        "logcat",
                        "screenshot",
                        "ui_automation",
                        "system_status",
                        "root_exec",
                        "network_adb",
                        "camera",
                        "sms",
                        "contacts",
                        "phone_info",
                        "files",
                        "android_version"
                    ),
                    "root_mode_enabled" to ZtAiDebugRootHelper.isRootModeEnabled(),
                    "root_available" to ZtAiDebugRootHelper.isRootAvailable(context)
                ),
                "permissions" to permStatus,
                "docs" to docs,
                "for_ai" to ZtAiDebugApiDocs.buildForAiBlock(
                    authorized = true,
                    port = port,
                    sampleIp = sampleIp,
                    code = code
                ),
                "lan_urls" to lanUrls,
                "lan_ips" to ips,
                "html_docs" to "GET /?code=***&format=html&lang=zh|en",
                "enabled" to true
            )
        )
    }

    fun discoveryHtml(context: Context, authorized: Boolean, preferredLang: String?): String {
        val ips = NetworkEnvironmentUtil.getLocalIpv4Addresses()
        val port = ZtAiDebugManager.PORT
        val code = if (authorized) ZtAiDebugMatchCodeHelper.getStoredCode() else null
        return ZtAiDebugApiDocs.buildHtml(port, ips, authorized, code, preferredLang)
    }

    fun deviceInfo(context: Context): String {
        val ips = NetworkEnvironmentUtil.getLocalIpv4Addresses()
        val port = ZtAiDebugManager.PORT
        return gson.toJson(
            mapOf(
                "ok" to true,
                "androidVersion" to (Build.VERSION.RELEASE ?: "unknown"),
                "androidSdk" to Build.VERSION.SDK_INT,
                "deviceModel" to (Build.MODEL ?: "unknown"),
                "deviceManufacturer" to (Build.MANUFACTURER ?: "unknown"),
                "ztVersion" to (BuildConfig.VERSION_NAME ?: "unknown"),
                "apiPort" to port,
                "workstationPort" to 19999,
                "lanIps" to ips,
                "baseUrls" to ips.map { "http://$it:$port" },
                "battery" to parseBattery(context),
                "serviceRunning" to ZtAiDebugManager.isRunning(),
                "rootModeEnabled" to ZtAiDebugRootHelper.isRootModeEnabled(),
                "rootAvailable" to ZtAiDebugRootHelper.isRootAvailable(context),
                "adbTcpPort" to ZtAiDebugRootHelper.readAdbTcpPort(),
                "permissionsUrl" to "/api/permissions",
                "discoveryUrl" to "/"
            )
        )
    }

    private fun parseBattery(context: Context): Int {
        return try {
            val json = ZtWorkstationDeviceHelper.getDeviceInfo(context)
            gson.fromJson(json, Map::class.java)["battery"]?.toString()?.toIntOrNull() ?: -1
        } catch (_: Exception) {
            -1
        }
    }
}
