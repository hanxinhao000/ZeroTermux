package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

object ZtAiDebugPermissionHelper {

    private val gson = Gson()

    data class FeaturePermission(
        val feature: String,
        val permissions: List<String>,
        val labelZh: String,
        val labelEn: String,
        val settingsPathZh: String
    )

    val FEATURES: List<FeaturePermission> = listOf(
        FeaturePermission(
            "camera",
            listOf(Permission.CAMERA),
            "摄像头拍照",
            "Camera",
            "系统设置 → 应用 → ZeroTermux → 权限 → 相机"
        ),
        FeaturePermission(
            "sms",
            listOf(Permission.READ_SMS),
            "读取短信",
            "SMS read",
            "系统设置 → 应用 → ZeroTermux → 权限 → 短信"
        ),
        FeaturePermission(
            "contacts",
            listOf(Permission.READ_CONTACTS),
            "读取联系人",
            "Contacts",
            "系统设置 → 应用 → ZeroTermux → 权限 → 联系人"
        ),
        FeaturePermission(
            "phone",
            listOf(Permission.READ_PHONE_STATE),
            "电话/设备信息",
            "Phone state",
            "系统设置 → 应用 → ZeroTermux → 权限 → 电话"
        ),
        FeaturePermission(
            "storage",
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE),
            "读取文件",
            "Storage",
            "系统设置 → 应用 → ZeroTermux → 权限 → 存储"
        )
    )

    fun allPermissions(): Array<String> {
        val list = linkedSetOf<String>()
        FEATURES.forEach { list.addAll(it.permissions) }
        return list.toTypedArray()
    }

    fun isGranted(context: Context, permission: String): Boolean {
        return XXPermissions.isGranted(context, permission)
    }

    fun isFeatureGranted(context: Context, feature: String): Boolean {
        val spec = FEATURES.find { it.feature == feature } ?: return true
        return spec.permissions.all { isGranted(context, it) }
    }

    fun statusJson(context: Context): String {
        val items = FEATURES.map { spec ->
            val granted = spec.permissions.all { isGranted(context, it) }
            mapOf(
                "feature" to spec.feature,
                "granted" to granted,
                "permissions" to spec.permissions.map { perm ->
                    mapOf(
                        "name" to perm,
                        "granted" to isGranted(context, perm)
                    )
                },
                "label_zh" to spec.labelZh,
                "label_en" to spec.labelEn,
                "settings_hint_zh" to spec.settingsPathZh
            )
        }
        val missing = items.filter { !(it["granted"] as Boolean) }
        val aiReminder = if (missing.isEmpty()) {
            "All optional permissions granted."
        } else {
            "Some permissions denied. Tell the user (in their language) to grant: " +
                missing.joinToString("; ") {
                    "${it["label_zh"]} — ${it["settings_hint_zh"]}"
                }
        }
        return gson.toJson(
            mapOf(
                "ok" to true,
                "all_granted" to missing.isEmpty(),
                "features" to items,
                "missing_features" to missing.map { it["feature"] },
                "hint_for_ai" to aiReminder
            )
        )
    }

    fun denyJson(context: Context, feature: String): String {
        val spec = FEATURES.find { it.feature == feature }
            ?: return gson.toJson(mapOf("ok" to false, "error" to "unknown feature"))
        val missingPerms = spec.permissions.filterNot { isGranted(context, it) }
        return gson.toJson(
            mapOf(
                "ok" to false,
                "feature" to feature,
                "permission_required" to missingPerms,
                "label_zh" to spec.labelZh,
                "label_en" to spec.labelEn,
                "hint_for_user_zh" to "请在系统设置中为 ZeroTermux 开启「${spec.labelZh}」权限：${spec.settingsPathZh}",
                "hint_for_user_en" to "Grant ZeroTermux permission: ${spec.labelEn} in system app settings.",
                "hint_for_ai" to "Permission denied for $feature. Ask the user to open Android Settings → Apps → ZeroTermux → Permissions and enable ${spec.labelEn}. Then retry GET /api/permissions."
            )
        )
    }
}
