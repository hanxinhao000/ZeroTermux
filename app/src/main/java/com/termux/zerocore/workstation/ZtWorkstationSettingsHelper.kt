package com.termux.zerocore.workstation

import com.google.gson.Gson
import com.termux.zerocore.bean.ZTUserBean
import com.termux.zerocore.ftp.utils.UserSetManage

object ZtWorkstationSettingsHelper {

    private val gson = Gson()

    data class SettingItem(
        val key: String,
        val title: String,
        val type: String,
        val value: Any?,
        val group: String
    )

    data class SettingPage(val id: String, val title: String, val description: String)

    private data class BoolSetting(
        val key: String,
        val title: String,
        val getter: (ZTUserBean) -> Boolean,
        val setter: (ZTUserBean, Boolean) -> Unit,
        val group: String
    )

    private val booleanSettings = listOf(
        BoolSetting("isOpenDownloadFileServices", "ZT下载服务器", { it.isOpenDownloadFileServices }, { b, v -> b.isOpenDownloadFileServices = v }, "ZeroTermux"),
        BoolSetting("inputMethodTriggerClose", "输入法调起侧边栏关闭", { it.isInputMethodTriggerClose }, { b, v -> b.isInputMethodTriggerClose = v }, "ZeroTermux"),
        BoolSetting("styleTriggerOff", "美化设置关闭菜单", { it.isStyleTriggerOff }, { b, v -> b.isStyleTriggerOff = v }, "ZeroTermux"),
        BoolSetting("isToolShow", "禁止工具箱显示", { it.isToolShow }, { b, v -> b.isToolShow = v }, "ZeroTermux"),
        BoolSetting("forceUseNumpad", "强制使用小键盘规则", { it.isForceUseNumpad }, { b, v -> b.isForceUseNumpad = v }, "ZeroTermux"),
        BoolSetting("isOutputLOG", "输出显示LOG", { it.isOutputLOG }, { b, v -> b.isOutputLOG = v }, "ZeroTermux"),
        BoolSetting("isResetVolume", "还原音量加减", { it.isResetVolume }, { b, v -> b.isResetVolume = v }, "ZeroTermux"),
        BoolSetting("isCloseFoldMenu", "折叠菜单", { it.isCloseFoldMenu }, { b, v -> b.isCloseFoldMenu = v }, "ZeroTermux"),
        BoolSetting("isDisableMainConfigMenu", "禁用主菜单配置文件", { it.isDisableMainConfigMenu }, { b, v -> b.isDisableMainConfigMenu = v }, "ZeroTermux"),
        BoolSetting("isEditorWordWrap", "文本编辑器自动换行", { it.isEditorWordWrap }, { b, v -> b.isEditorWordWrap = v }, "ZeroTermux"),
        BoolSetting("isSnowflakeShow", "显示雪花", { it.isSnowflakeShow }, { b, v -> b.isSnowflakeShow = v }, "ZeroTermux"),
        BoolSetting("isShowCommand", "显示/隐藏终端", { it.isShowCommand }, { b, v -> b.isShowCommand = v }, "ZeroTermux"),
        BoolSetting("isInternalPassage", "内部/外部通道", { it.isInternalPassage }, { b, v -> b.isInternalPassage = v }, "ZeroTermux"),
        BoolSetting("mIsDeepSeekVisibleTerminal", "DeepSeek可见终端", { it.isIsDeepSeekVisibleTerminal }, { b, v -> b.setIsDeepSeekVisibleTerminal(v) }, "AI"),
        BoolSetting("mIsCustomVisibleTerminal", "自定义AI可见终端", { it.isIsCustomVisibleTerminal }, { b, v -> b.setIsCustomVisibleTerminal(v) }, "AI"),
        BoolSetting("mIsCustomAi", "选择自定义AI", { it.isCustomAi }, { b, v -> b.isCustomAi = v }, "AI"),
        BoolSetting("mIsBackMenuVisible", "显示左/右侧背景图片", { it.isBackMenuVisible }, { b, v -> b.setIsBackMenuVisible(v) }, "ZeroTermux"),
        BoolSetting("isCreateFolderForSdcardAndroid", "Android/data创建文件", { it.isCreateFolderForSdcardAndroid }, { b, v -> b.isCreateFolderForSdcardAndroid = v }, "ZeroTermux"),
        BoolSetting("isJumpGuide", "跳过引导页面", { it.isJumpGuide }, { b, v -> b.isJumpGuide = v }, "ZeroTermux"),
        BoolSetting("isHideGuideLayout", "隐藏引导页面", { it.isHideGuideLayout }, { b, v -> b.isHideGuideLayout = v }, "ZeroTermux"),
        BoolSetting("isWriterMenuBack", "写入菜单背景", { it.isWriterMenuBack }, { b, v -> b.isWriterMenuBack = v }, "ZeroTermux")
    )

    private val settingPages = listOf(
        SettingPage("termux", "Termux 设置", "Termux 核心偏好设置（需在 App 内打开）"),
        SettingPage("zt", "ZeroTermux 设置", "本页可远程切换 ZeroTermux 开关"),
        SettingPage("left_menu", "左侧菜单设置", "菜单布局与样式（需在 App 内打开）"),
        SettingPage("ai", "AI 设置", "DeepSeek 与自定义 AI（需在 App 内打开）"),
        SettingPage("online_server", "在线服务器", "在线服务器配置（需在 App 内打开）"),
        SettingPage("install", "安装管理", "插件与安装项（需在 App 内打开）"),
        SettingPage("save_path", "存储路径", "xinhao 存储路径（需在 App 内打开）")
    )

    fun listPages(): String = gson.toJson(mapOf("pages" to settingPages))

    fun listSettings(): String {
        val bean = UserSetManage.get().getZTUserBean()
        val items = booleanSettings.map {
            SettingItem(it.key, it.title, "boolean", it.getter(bean), it.group)
        }
        return gson.toJson(mapOf("settings" to items))
    }

    fun updateSetting(key: String, value: String): String {
        val bean = UserSetManage.get().getZTUserBean()
        val setting = booleanSettings.find { it.key == key }
            ?: return gson.toJson(mapOf("ok" to false, "error" to "unknown key"))
        val boolValue = value.equals("true", ignoreCase = true) || value == "1"
        setting.setter(bean, boolValue)
        UserSetManage.get().setZTUserBean(bean)
        return gson.toJson(mapOf("ok" to true))
    }
}
