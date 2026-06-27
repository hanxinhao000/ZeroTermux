package com.termux.zerocore.config.mainmenu

import java.io.File

data class MainMenuPackageInfo(
    val id: String,
    val label: String,
    val installTime: Long = 0L,
    val packageDir: File? = null,
    val type: Int = TYPE_INSTALLED,
    val isActive: Boolean = false
) {
    companion object {
        /** 默认菜单（升级兼容 / main_menu_path.xml），不可删除。 */
        const val TYPE_DEFAULT = 0
        /** @deprecated 使用 [TYPE_DEFAULT] */
        const val TYPE_NETWORK = TYPE_DEFAULT
        /** 用户可配置菜单包（zip / AI 等），可删除、可备份。 */
        const val TYPE_INSTALLED = 1
        const val TYPE_INSTALL = 2
        /** 程序内置菜单，不可删除、不读配置文件。 */
        const val TYPE_PROGRAM = 3

        const val ID_NETWORK = "__network_update__"
        const val ID_INSTALL = "__install__"
        const val ID_PROGRAM = "program_config"
        /** AI 菜单包目录前缀（ai_created、ai_created_1 …）。 */
        const val ID_AI_CREATED = "ai_created"
        const val ID_AI_CREATED_PREFIX = "ai_created"
        /** 使用现有 XML 菜单，不覆盖文件。 */
        const val ID_DEFAULT_XML = "default_xml"
    }

    /** 用户可删除/备份/由 AI 写入的安装包。 */
    fun isConfigurable(): Boolean = type == TYPE_INSTALLED
}
