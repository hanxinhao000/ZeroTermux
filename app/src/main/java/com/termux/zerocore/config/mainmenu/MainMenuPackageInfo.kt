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

        const val ID_NETWORK = "__network_update__"
        const val ID_INSTALL = "__install__"
    }
}
