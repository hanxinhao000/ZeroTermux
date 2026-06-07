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
        const val TYPE_NETWORK = 0
        const val TYPE_INSTALLED = 1
        const val TYPE_INSTALL = 2
        const val TYPE_PROGRAM = 3

        const val ID_NETWORK = "__network_update__"
        const val ID_INSTALL = "__install__"
        const val ID_PROGRAM = "program_config"
        /** 使用现有 XML 菜单，不覆盖文件。 */
        const val ID_DEFAULT_XML = "default_xml"
    }
}
