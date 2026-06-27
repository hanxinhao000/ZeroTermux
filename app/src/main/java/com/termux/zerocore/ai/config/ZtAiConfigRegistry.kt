package com.termux.zerocore.ai.config

import com.example.xh_lib.utils.SaveData
import com.google.gson.Gson
import com.termux.zerocore.bean.ZTUserBean
import com.termux.zerocore.config.ztcommand.navigation.ZtNavigationHelper
import com.termux.zerocore.ftp.utils.UserSetManage
import org.json.JSONArray
import org.json.JSONObject

/**
 * ZeroTermux 全量配置项注册表，供 AI 列举 / 读取 / 写入。
 * 文案随系统语言（values / values-en）。
 */
object ZtAiConfigRegistry {

    private val gson = Gson()

    data class KeyDef(
        val key: String,
        val title: String,
        val type: String,
        val group: String,
        val description: String = "",
        val risky: Boolean = false,
        val requiresRestart: Boolean = false
    )

    private data class BoolBinding(
        val def: KeyDef,
        val getter: (ZTUserBean) -> Boolean,
        val setter: (ZTUserBean, Boolean) -> Unit
    )

    private data class IntBinding(
        val def: KeyDef,
        val getter: (ZTUserBean) -> Int,
        val setter: (ZTUserBean, Int) -> Unit
    )

    private data class StringBinding(
        val def: KeyDef,
        val getter: (ZTUserBean) -> String?,
        val setter: (ZTUserBean, String) -> Unit,
        val secret: Boolean = false
    )

    private fun cfg(key: String, type: String, group: String, risky: Boolean = false, requiresRestart: Boolean = false): KeyDef {
        return KeyDef(
            key = key,
            title = ZtAiStrings.resolveCfgTitle(key),
            type = type,
            group = group,
            description = ZtAiStrings.resolveCfgDesc(key),
            risky = risky,
            requiresRestart = requiresRestart
        )
    }

    private fun beautifyKeys(): List<KeyDef> = listOf(
        cfg("font_color", "string", "beautify"),
        cfg("font_color_progress", "int", "beautify"),
        cfg("back_color", "string", "beautify"),
        cfg("back_color_progress", "int", "beautify"),
        cfg("change_text", "int", "beautify"),
        cfg("change_text_show", "boolean", "beautify"),
        cfg("blur_enabled", "boolean", "beautify"),
        cfg("blur_radius", "int", "beautify"),
        cfg("text_shadow_enabled", "boolean", "beautify"),
        cfg("text_shadow_strength", "int", "beautify")
    )

    private fun booleanBindings(): List<BoolBinding> = listOf(
        bindBool("isOpenDownloadFileServices", "ZeroTermux", ZTUserBean::isOpenDownloadFileServices, ZTUserBean::setOpenDownloadFileServices),
        bindBool("isZtWorkstationEnabled", "workstation", ZTUserBean::isZtWorkstationEnabled, ZTUserBean::setZtWorkstationEnabled),
        bindBool("isZtWorkstationAutoStart", "workstation", ZTUserBean::isZtWorkstationAutoStart, ZTUserBean::setZtWorkstationAutoStart),
        bindBool("isZtWorkstationTerminalEnabled", "workstation", ZTUserBean::isZtWorkstationTerminalEnabled, ZTUserBean::setZtWorkstationTerminalEnabled),
        bindBool("isZtWorkstationCameraEnabled", "workstation", ZTUserBean::isZtWorkstationCameraEnabled, ZTUserBean::setZtWorkstationCameraEnabled),
        bindBool("isZtWorkstationFilesEnabled", "workstation", ZTUserBean::isZtWorkstationFilesEnabled, ZTUserBean::setZtWorkstationFilesEnabled),
        bindBool("isZtWorkstationPhoneSmsEnabled", "workstation", ZTUserBean::isZtWorkstationPhoneSmsEnabled, ZTUserBean::setZtWorkstationPhoneSmsEnabled),
        bindBool("inputMethodTriggerClose", "ZeroTermux", ZTUserBean::isInputMethodTriggerClose, ZTUserBean::setInputMethodTriggerClose),
        bindBool("styleTriggerOff", "ZeroTermux", ZTUserBean::isStyleTriggerOff, ZTUserBean::setStyleTriggerOff),
        bindBool("isToolShow", "ZeroTermux", ZTUserBean::isToolShow, ZTUserBean::setToolShow),
        bindBool("forceUseNumpad", "ZeroTermux", ZTUserBean::isForceUseNumpad, ZTUserBean::setForceUseNumpad),
        bindBool("isOutputLOG", "ZeroTermux", ZTUserBean::isOutputLOG, ZTUserBean::setOutputLOG),
        bindBool("isSnowflakeShow", "beautify_ui", ZTUserBean::isSnowflakeShow, ZTUserBean::setSnowflakeShow),
        bindBool("isRainShow", "beautify_ui", ZTUserBean::isRainShow, ZTUserBean::setRainShow),
        bindBool("isResetVolume", "ZeroTermux", ZTUserBean::isResetVolume, ZTUserBean::setResetVolume),
        bindBool("isAiAgentPanelEnabled", "ZeroTermux", ZTUserBean::isAiAgentPanelEnabled, ZTUserBean::setAiAgentPanelEnabled),
        bindBool("isShowCommand", "x11", ZTUserBean::isShowCommand, ZTUserBean::setShowCommand),
        bindBool("isInternalPassage", "x11", ZTUserBean::isInternalPassage, ZTUserBean::setInternalPassage, risky = true, requiresRestart = true),
        bindBool("isCloseFoldMenu", "ZeroTermux", ZTUserBean::isCloseFoldMenu, ZTUserBean::setCloseFoldMenu),
        bindBool("isDisableMainConfigMenu", "ZeroTermux", ZTUserBean::isDisableMainConfigMenu, ZTUserBean::setDisableMainConfigMenu),
        bindBool("isHideGuideLayout", "ZeroTermux", ZTUserBean::isHideGuideLayout, ZTUserBean::setHideGuideLayout),
        bindBool("isWriterMenuBack", "ZeroTermux", ZTUserBean::isWriterMenuBack, ZTUserBean::setWriterMenuBack),
        bindBool("mIsBackMenuVisible", "beautify_ui", ZTUserBean::isBackMenuVisible, ZTUserBean::setIsBackMenuVisible),
        bindBool("isCreateFolderForSdcardAndroid", "ZeroTermux", ZTUserBean::isCreateFolderForSdcardAndroid, ZTUserBean::setCreateFolderForSdcardAndroid),
        bindBool("isJumpGuide", "ZeroTermux", ZTUserBean::isJumpGuide, ZTUserBean::setJumpGuide),
        bindBool("isEditorWordWrap", "editor", ZTUserBean::isEditorWordWrap, ZTUserBean::setEditorWordWrap),
        bindBool("mIsDeepSeekVisibleTerminal", "ai_legacy", ZTUserBean::isIsDeepSeekVisibleTerminal, ZTUserBean::setIsDeepSeekVisibleTerminal),
        bindBool("mIsCustomVisibleTerminal", "ai_legacy", ZTUserBean::isIsCustomVisibleTerminal, ZTUserBean::setIsCustomVisibleTerminal),
        bindBool("mIsCustomAi", "ai_legacy", ZTUserBean::isCustomAi, ZTUserBean::setCustomAi),
        bindBool("agentAiTerminalEnabled", "agent_ai", ZTUserBean::isAgentAiTerminalEnabled, ZTUserBean::setAgentAiTerminalEnabled),
        bindBool("agentAiZtControlEnabled", "agent_ai", ZTUserBean::isAgentAiZtControlEnabled, ZTUserBean::setAgentAiZtControlEnabled)
    )

    private fun intBindings(): List<IntBinding> = listOf(
        IntBinding(cfg("mDoubleClickFun", "int", "ZeroTermux"), { it.doubleClickFun }, { b, v -> b.doubleClickFun = v })
    )

    private fun stringBindings(): List<StringBinding> = listOf(
        StringBinding(cfg("agentAiActiveProvider", "string", "agent_ai"), { it.agentAiActiveProvider }, { b, v -> b.agentAiActiveProvider = v }),
        StringBinding(cfg("agentAiApiUrl", "string", "agent_ai"), { it.agentAiApiUrl }, { b, v -> b.agentAiApiUrl = v }),
        StringBinding(cfg("agentAiApiKey", "string", "agent_ai"), { it.agentAiApiKey }, { b, v -> b.agentAiApiKey = v }, secret = true),
        StringBinding(cfg("agentAiModel", "string", "agent_ai"), { it.agentAiModel }, { b, v -> b.agentAiModel = v }),
        StringBinding(cfg("agentAiSystemPrompt", "string", "agent_ai"), { it.agentAiSystemPrompt }, { b, v -> b.agentAiSystemPrompt = v }),
        StringBinding(cfg("mDeepSeekApiUrl", "string", "ai_legacy"), { it.deepSeekApiUrl }, { b, v -> b.deepSeekApiUrl = v }),
        StringBinding(cfg("mDeepSeekApiKey", "string", "ai_legacy"), { it.deepSeekApiKey }, { b, v -> b.deepSeekApiKey = v }, secret = true),
        StringBinding(cfg("mCustomApiUrl", "string", "ai_legacy"), { it.customApiUrl }, { b, v -> b.customApiUrl = v }),
        StringBinding(cfg("mCustomApiKey", "string", "ai_legacy"), { it.customApiKey }, { b, v -> b.customApiKey = v }, secret = true),
        StringBinding(cfg("mCustomSystemPrompt", "string", "ai_legacy"), { it.customSystemPrompt }, { b, v -> b.customSystemPrompt = v })
    )

    private fun ztCommands(): JSONArray {
        val signatures = listOf(
            "help", "version / v", "toast <msg>", "openleft / ol", "openright / or",
            "reboot / rb", "openpage <id> / op <id>", "menu update|reset", "backgroundimage <path> / bgi",
            "x11commandshow / x11xs", "x11commandhide / x11xh", "x11status",
            "x11keyboardshow / x11kbs", "x11keyboardhide / x11kbh", "dialog ...",
            "vnc <ip_port_user_pass>", "ln <src> <dst>", "qr"
        )
        val risky = setOf("reboot / rb")
        return JSONArray(signatures.map { sig ->
            JSONObject()
                .put("signature", sig)
                .put("description", ZtAiStrings.resolveZtCmdDesc(sig))
                .put("risky", sig in risky)
        })
    }

    private fun bindBool(
        key: String,
        group: String,
        getter: (ZTUserBean) -> Boolean,
        setter: (ZTUserBean, Boolean) -> Unit,
        risky: Boolean = false,
        requiresRestart: Boolean = false
    ): BoolBinding {
        return BoolBinding(cfg(key, "boolean", group, risky, requiresRestart), getter, setter)
    }

    fun listCapabilities(category: String): String {
        val cat = category.trim().lowercase().ifBlank { "all" }
        val root = JSONObject().put("ok", true)
        when (cat) {
            "settings" -> root.put("settings", listSettingDefs())
            "beautify", "beautify_ui" -> {
                if (cat == "beautify_ui") {
                    root.put("beautify_ui", listBeautifyUiDefs())
                    root.put("beautify_ui_hint", ZtAiStrings.beautifyUiHint())
                } else {
                    root.put("beautify", JSONArray(beautifyKeys().map { it.toJson() }))
                    root.put("beautify_ui", listBeautifyUiDefs())
                    root.put("beautify_hint", ZtAiStrings.beautifyHint())
                    root.put("beautify_ui_hint", ZtAiStrings.beautifyUiHint())
                }
            }
            "pages" -> root.put("pages", JSONObject(ZtNavigationHelper.listPagesJson()))
            "zt_commands" -> root.put("zt_commands", ztCommands())
            "left_menu" -> {
                root.put("left_menu_hint", ZtAiStrings.leftMenuHint())
                root.put("left_menu_xml_rules", ZtAiLeftMenuXmlGuide.buildRulesJson(
                    com.example.xh_lib.utils.UUtils.getContext()
                ))
            }
            "pkg_source" -> {
                root.put("pkg_source_hint", ZtAiStrings.pkgSourceHint())
                root.put("pkg_sources", JSONObject(ZtAiPkgSourceHelper.listSources()))
            }
            "all" -> {
                root.put("settings", listSettingDefs())
                root.put("beautify", JSONArray(beautifyKeys().map { it.toJson() }))
                root.put("beautify_ui", listBeautifyUiDefs())
                root.put("beautify_hint", ZtAiStrings.beautifyHint())
                root.put("beautify_ui_hint", ZtAiStrings.beautifyUiHint())
                root.put("pages", JSONObject(ZtNavigationHelper.listPagesJson()))
                root.put("zt_commands", ztCommands())
                root.put("left_menu_hint", ZtAiStrings.leftMenuHint())
                root.put("left_menu_xml_rules", ZtAiLeftMenuXmlGuide.buildRulesJson(
                    com.example.xh_lib.utils.UUtils.getContext()
                ))
                root.put("left_menu_tools", JSONArray(listOf(
                    "get_zerotermux_left_menu",
                    "update_zerotermux_left_menu"
                )))
                root.put("pkg_source_hint", ZtAiStrings.pkgSourceHint())
                root.put("pkg_source_tools", JSONArray(listOf(
                    "list_zerotermux_pkg_sources",
                    "switch_zerotermux_pkg_source"
                )))
                root.put("groups", JSONArray(listOf(
                    "ZeroTermux", "x11", "beautify", "beautify_ui", "workstation",
                    "agent_ai", "ai_legacy", "editor"
                )))
            }
            else -> return gson.toJson(mapOf("ok" to false, "error" to "unknown category: $category"))
        }
        return root.toString(2)
    }

    private fun listSettingDefs(): JSONArray {
        val arr = JSONArray()
        booleanBindings().forEach { arr.put(it.def.toJson()) }
        intBindings().forEach { arr.put(it.def.toJson()) }
        stringBindings().forEach { arr.put(it.def.toJson()) }
        return arr
    }

    private fun listBeautifyUiDefs(): JSONArray {
        val arr = JSONArray()
        booleanBindings().filter { it.def.group == "beautify_ui" }.forEach { arr.put(it.def.toJson()) }
        return arr
    }

    private fun KeyDef.toJson(): JSONObject {
        return JSONObject()
            .put("key", key)
            .put("title", title)
            .put("type", type)
            .put("group", group)
            .put("description", description)
            .put("risky", risky)
            .put("requires_restart", requiresRestart)
    }

    fun getConfig(group: String?, keys: JSONArray?): String {
        val result = JSONObject().put("ok", true)
        val values = JSONObject()
        val filter = keys?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        }
        if (group.isNullOrBlank() || group == "settings" || group == "all") {
            appendUserSettings(values, filter)
        }
        if (group.isNullOrBlank() || group == "beautify" || group == "beautify_ui" || group == "all") {
            if (group != "beautify_ui") {
                appendBeautifySettings(values, filter)
            }
            appendBeautifyUiSettings(values, filter)
        }
        result.put("values", values)
        return result.toString(2)
    }

    fun setConfig(key: String, value: String): String {
        val trimmedKey = key.trim()
        val trimmedValue = value.trim()
        booleanBindings().find { it.def.key == trimmedKey }?.let { binding ->
            val bool = trimmedValue.equals("true", true) || trimmedValue == "1"
            val bean = UserSetManage.get().getZTUserBean()
            when (trimmedKey) {
                "isRainShow" -> if (bool) bean.isSnowflakeShow = false
                "isSnowflakeShow" -> if (bool) bean.isRainShow = false
            }
            binding.setter(bean, bool)
            UserSetManage.get().setZTUserBean(bean)
            ZtAiConfigSideEffects.apply(trimmedKey, bean)
            return ok("set ${binding.def.key}=$bool", binding.def.requiresRestart)
        }
        intBindings().find { it.def.key == trimmedKey }?.let { binding ->
            val intVal = trimmedValue.toIntOrNull()
                ?: return err("invalid int for ${binding.def.key}")
            val bean = UserSetManage.get().getZTUserBean()
            binding.setter(bean, intVal)
            UserSetManage.get().setZTUserBean(bean)
            return ok("set ${binding.def.key}=$intVal")
        }
        stringBindings().find { it.def.key == trimmedKey }?.let { binding ->
            val bean = UserSetManage.get().getZTUserBean()
            binding.setter(bean, trimmedValue)
            UserSetManage.get().setZTUserBean(bean)
            return ok("set ${binding.def.key}")
        }
        beautifyKeys().find { it.key == trimmedKey }?.let { def ->
            val stored = when (def.type) {
                "boolean" -> when {
                    trimmedValue.equals("true", true) || trimmedValue == "1" -> "true"
                    trimmedValue.equals("false", true) || trimmedValue == "0" -> "false"
                    trimmedValue == "def" -> "def"
                    else -> return err("invalid boolean for ${def.key}")
                }
                "int" -> {
                    trimmedValue.toIntOrNull()?.toString()
                        ?: return err("invalid int for ${def.key}")
                }
                else -> trimmedValue
            }
            SaveData.saveStringOther(trimmedKey, stored)
            ZtAiConfigSideEffects.applyBeautify(trimmedKey)
            return ok("set beautify ${def.key}=$stored")
        }
        return err("unknown key: $trimmedKey. Call list_zerotermux_capabilities first.")
    }

    private fun appendUserSettings(values: JSONObject, filter: Set<String>?) {
        val bean = UserSetManage.get().getZTUserBean()
        booleanBindings().forEach { b ->
            if (filter == null || b.def.key in filter) {
                values.put(b.def.key, b.getter(bean))
            }
        }
        intBindings().forEach { b ->
            if (filter == null || b.def.key in filter) {
                values.put(b.def.key, b.getter(bean))
            }
        }
        stringBindings().forEach { b ->
            if (filter == null || b.def.key in filter) {
                val raw = b.getter(bean).orEmpty()
                values.put(b.def.key, if (b.secret && raw.isNotBlank()) maskSecret(raw) else raw)
            }
        }
    }

    private fun appendBeautifySettings(values: JSONObject, filter: Set<String>?) {
        beautifyKeys().forEach { def ->
            if (filter != null && def.key !in filter) return@forEach
            val raw = SaveData.getStringOther(def.key)
            values.put(def.key, raw ?: JSONObject.NULL)
        }
    }

    private fun appendBeautifyUiSettings(values: JSONObject, filter: Set<String>?) {
        val bean = UserSetManage.get().getZTUserBean()
        booleanBindings().filter { it.def.group == "beautify_ui" }.forEach { b ->
            if (filter != null && b.def.key !in filter) return@forEach
            values.put(b.def.key, b.getter(bean))
        }
    }

    private fun maskSecret(value: String): String {
        if (value.length <= 8) return "***"
        return value.take(4) + "…" + value.takeLast(4)
    }

    private fun ok(message: String, requiresRestart: Boolean = false): String {
        val map = mutableMapOf("ok" to true, "message" to message)
        if (requiresRestart) map["requires_restart"] = true
        return gson.toJson(map)
    }

    private fun err(message: String): String = gson.toJson(mapOf("ok" to false, "error" to message))
}
