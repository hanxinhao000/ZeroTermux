package com.termux.zerocore.utils

import android.content.Context

/**
 * 左侧菜单 assets 子目录：cn / en / in。
 * Java Locale 对印尼语可能是 "in" 或 "id"，资源目录统一用 values-in 与 mainmenu/in。
 */
object ZtMenuLocale {
    const val CN = "cn"
    const val EN = "en"
    const val IN = "in"

    @JvmStatic
    fun assetSubdir(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        return assetSubdir(locale.language)
    }

    @JvmStatic
    fun assetSubdir(language: String?): String {
        return when (language.orEmpty().lowercase()) {
            "en" -> EN
            "in", "id" -> IN
            else -> CN
        }
    }

    @JvmStatic
    fun menuAssetPath(context: Context): String =
        "mainmenu/${assetSubdir(context)}/zt_menu_config.xml"

    @JvmStatic
    fun menuAssetPath(language: String?): String =
        "mainmenu/${assetSubdir(language)}/zt_menu_config.xml"
}
