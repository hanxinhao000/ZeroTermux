package com.termux.zerocore.config.mainmenu

data class MenuUpdateSource(
    val id: String,
    val url: String,
    val isDefault: Boolean = false,
    val isAddAction: Boolean = false
) {
    companion object {
        const val DEFAULT_ID = "default"
        const val ADD_ID = "__add_source__"
    }
}
