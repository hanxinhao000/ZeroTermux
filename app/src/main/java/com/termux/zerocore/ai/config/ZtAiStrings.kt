package com.termux.zerocore.ai.config

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.example.xh_lib.utils.UUtils
import com.termux.R

/** AI 能力文案：随系统语言切换（values / values-en）。 */
object ZtAiStrings {

    fun str(@StringRes id: Int): String = UUtils.getString(id)

    fun strArray(@ArrayRes id: Int): Array<String> =
        UUtils.getContext().resources.getStringArray(id)

    fun aiMenuCreateLabel(): String = str(R.string.menu_package_ai_created_label)

    fun menuAssetSubdir(): String {
        val lang = UUtils.getContext().resources.configuration.locales[0].language
        return if (lang.equals("en", ignoreCase = true)) "en" else "cn"
    }

    fun menuAssetPath(): String = "mainmenu/${menuAssetSubdir()}/zt_menu_config.xml"

    fun leftMenuExampleKeyword(): String = str(R.string.zt_ai_left_menu_example_keyword)

    fun beautifyHint(): String = str(R.string.zt_ai_hint_beautify)

    fun beautifyUiHint(): String = str(R.string.zt_ai_hint_beautify_ui)

    fun leftMenuRulesSummary(): String = str(R.string.zt_ai_left_menu_rules_summary)

    fun leftMenuHintExtra(): String = str(R.string.zt_ai_left_menu_hint_extra)

    fun leftMenuHint(): String = leftMenuRulesSummary() + leftMenuHintExtra()

    fun pkgSourceHint(): String = str(R.string.zt_ai_pkg_source_hint)

    // --- Tool descriptions (function calling) ---

    fun toolListCapabilities(): String = str(R.string.zt_ai_tool_list_capabilities)

    fun toolGetConfig(): String = str(R.string.zt_ai_tool_get_config)

    fun toolSetConfig(): String = str(R.string.zt_ai_tool_set_config)

    fun toolOpenPage(): String = str(R.string.zt_ai_tool_open_page)

    fun toolRunZt(): String = str(R.string.zt_ai_tool_run_zt)

    fun toolGetLeftMenu(): String = str(R.string.zt_ai_tool_get_left_menu)

    fun toolUpdateLeftMenu(): String = str(R.string.zt_ai_tool_update_left_menu)

    fun toolListPkgSources(): String = str(R.string.zt_ai_tool_list_pkg_sources)

    fun toolSwitchPkgSource(): String = str(R.string.zt_ai_tool_switch_pkg_source)

    fun toolReadTerminal(): String = str(R.string.zt_ai_tool_read_terminal)

    fun toolSendCommand(): String = str(R.string.zt_ai_tool_send_command)

    fun toolSendKey(): String = str(R.string.zt_ai_tool_send_key)

    fun toolRunZtCommand(): String = str(R.string.zt_ai_tool_run_zt_command)

    // --- Executor status labels ---

    fun statusListCapabilities(): String = str(R.string.zt_ai_status_list_capabilities)

    fun statusGetConfig(): String = str(R.string.zt_ai_status_get_config)

    fun statusSetConfig(): String = str(R.string.zt_ai_status_set_config)

    fun statusOpenPage(): String = str(R.string.zt_ai_status_open_page)

    fun statusRunZt(): String = str(R.string.zt_ai_status_run_zt)

    fun statusGetLeftMenu(): String = str(R.string.zt_ai_status_get_left_menu)

    fun statusUpdateLeftMenu(): String = str(R.string.zt_ai_status_update_left_menu)

    fun statusListPkgSources(): String = str(R.string.zt_ai_status_list_pkg_sources)

    fun statusSwitchPkgSource(): String = str(R.string.zt_ai_status_switch_pkg_source)

    fun statusConfigDefault(): String = str(R.string.zt_ai_status_config_default)

    // --- Terminal snapshot ---

    fun terminalSnapshotHeader(): String = str(R.string.zt_ai_terminal_snapshot_header)

    fun terminalSnapshotLastLine(label: String): String =
        str(R.string.zt_ai_terminal_snapshot_last_line).format(label)

    fun terminalSnapshotVisibleHeader(): String = str(R.string.zt_ai_terminal_snapshot_visible_header)

    fun terminalSnapshotScrollbackHeader(): String = str(R.string.zt_ai_terminal_snapshot_scrollback_header)

    fun terminalStatusIdle(): String = str(R.string.zt_ai_terminal_status_idle)

    fun terminalStatusRunning(): String = str(R.string.zt_ai_terminal_status_running)

    fun terminalStatusAwaitingConfirmation(): String = str(R.string.zt_ai_terminal_status_awaiting_confirmation)

    fun terminalSnapshotPrefix(): String = str(R.string.zt_ai_terminal_snapshot_prefix)

    fun terminalHistoryNotice(): String = str(R.string.zt_agent_ai_terminal_history_notice)

    fun resolveCfgTitle(key: String): String {
        val res = cfgTitleRes[key] ?: return key
        return str(res)
    }

    fun resolveCfgDesc(key: String): String {
        val res = cfgDescRes[key] ?: return ""
        return str(res)
    }

    fun resolveZtCmdDesc(signature: String): String {
        val res = ztCmdDescRes[signature] ?: return signature
        return str(res)
    }

    private val cfgTitleRes: Map<String, Int> = mapOf(
        "font_color" to R.string.zt_ai_cfg_font_color_title,
        "font_color_progress" to R.string.zt_ai_cfg_font_color_progress_title,
        "back_color" to R.string.zt_ai_cfg_back_color_title,
        "back_color_progress" to R.string.zt_ai_cfg_back_color_progress_title,
        "change_text" to R.string.zt_ai_cfg_change_text_title,
        "change_text_show" to R.string.zt_ai_cfg_change_text_show_title,
        "blur_enabled" to R.string.zt_ai_cfg_blur_enabled_title,
        "blur_radius" to R.string.zt_ai_cfg_blur_radius_title,
        "text_shadow_enabled" to R.string.zt_ai_cfg_text_shadow_enabled_title,
        "text_shadow_strength" to R.string.zt_ai_cfg_text_shadow_strength_title,
        "isOpenDownloadFileServices" to R.string.zt_ai_cfg_download_server_title,
        "isZtWorkstationEnabled" to R.string.zt_ai_cfg_workstation_title,
        "isZtWorkstationAutoStart" to R.string.zt_ai_cfg_workstation_autostart_title,
        "isZtWorkstationTerminalEnabled" to R.string.zt_ai_cfg_workstation_terminal_title,
        "isZtWorkstationCameraEnabled" to R.string.zt_ai_cfg_workstation_camera_title,
        "isZtWorkstationFilesEnabled" to R.string.zt_ai_cfg_workstation_files_title,
        "isZtWorkstationPhoneSmsEnabled" to R.string.zt_ai_cfg_workstation_sms_title,
        "inputMethodTriggerClose" to R.string.zt_ai_cfg_input_method_close_title,
        "styleTriggerOff" to R.string.zt_ai_cfg_style_trigger_off_title,
        "isToolShow" to R.string.zt_ai_cfg_tool_show_title,
        "forceUseNumpad" to R.string.zt_ai_cfg_force_numpad_title,
        "isOutputLOG" to R.string.zt_ai_cfg_output_log_title,
        "isSnowflakeShow" to R.string.zt_ai_cfg_snowflake_title,
        "isRainShow" to R.string.zt_ai_cfg_rain_title,
        "isResetVolume" to R.string.zt_ai_cfg_reset_volume_title,
        "isAiAgentPanelEnabled" to R.string.zt_ai_cfg_ai_panel_title,
        "isShowCommand" to R.string.zt_ai_cfg_show_command_title,
        "isInternalPassage" to R.string.zt_ai_cfg_internal_passage_title,
        "isCloseFoldMenu" to R.string.zt_ai_cfg_fold_menu_title,
        "isDisableMainConfigMenu" to R.string.zt_ai_cfg_disable_main_menu_title,
        "isHideGuideLayout" to R.string.zt_ai_cfg_hide_guide_title,
        "isWriterMenuBack" to R.string.zt_ai_cfg_writer_menu_back_title,
        "mIsBackMenuVisible" to R.string.zt_ai_cfg_menu_back_visible_title,
        "isCreateFolderForSdcardAndroid" to R.string.zt_ai_cfg_sdcard_android_title,
        "isJumpGuide" to R.string.zt_ai_cfg_jump_guide_title,
        "isEditorWordWrap" to R.string.zt_ai_cfg_editor_wrap_title,
        "mIsDeepSeekVisibleTerminal" to R.string.zt_ai_cfg_deepseek_terminal_title,
        "mIsCustomVisibleTerminal" to R.string.zt_ai_cfg_custom_terminal_title,
        "mIsCustomAi" to R.string.zt_ai_cfg_custom_ai_title,
        "agentAiTerminalEnabled" to R.string.zt_ai_cfg_agent_terminal_title,
        "agentAiZtControlEnabled" to R.string.zt_ai_cfg_agent_zt_control_title,
        "mDoubleClickFun" to R.string.zt_ai_cfg_double_click_title,
        "agentAiActiveProvider" to R.string.zt_ai_cfg_agent_provider_title,
        "agentAiApiUrl" to R.string.zt_ai_cfg_agent_api_url_title,
        "agentAiApiKey" to R.string.zt_ai_cfg_agent_api_key_title,
        "agentAiModel" to R.string.zt_ai_cfg_agent_model_title,
        "agentAiSystemPrompt" to R.string.zt_ai_cfg_agent_system_prompt_title,
        "mDeepSeekApiUrl" to R.string.zt_ai_cfg_deepseek_url_title,
        "mDeepSeekApiKey" to R.string.zt_ai_cfg_deepseek_key_title,
        "mCustomApiUrl" to R.string.zt_ai_cfg_custom_url_title,
        "mCustomApiKey" to R.string.zt_ai_cfg_custom_key_title,
        "mCustomSystemPrompt" to R.string.zt_ai_cfg_custom_prompt_title,
    )

    private val cfgDescRes: Map<String, Int> = mapOf(
        "font_color" to R.string.zt_ai_cfg_font_color_desc,
        "change_text_show" to R.string.zt_ai_cfg_change_text_show_desc,
        "blur_enabled" to R.string.zt_ai_cfg_blur_enabled_desc,
        "blur_radius" to R.string.zt_ai_cfg_blur_radius_desc,
        "text_shadow_enabled" to R.string.zt_ai_cfg_text_shadow_enabled_desc,
        "text_shadow_strength" to R.string.zt_ai_cfg_text_shadow_strength_desc,
        "isSnowflakeShow" to R.string.zt_ai_cfg_snowflake_desc,
        "isRainShow" to R.string.zt_ai_cfg_rain_desc,
        "isInternalPassage" to R.string.zt_ai_cfg_internal_passage_desc,
        "mDoubleClickFun" to R.string.zt_ai_cfg_double_click_desc,
    )

    private val ztCmdDescRes: Map<String, Int> = mapOf(
        "help" to R.string.zt_ai_zt_cmd_help,
        "version / v" to R.string.zt_ai_zt_cmd_version,
        "toast <msg>" to R.string.zt_ai_zt_cmd_toast,
        "openleft / ol" to R.string.zt_ai_zt_cmd_openleft,
        "openright / or" to R.string.zt_ai_zt_cmd_openright,
        "reboot / rb" to R.string.zt_ai_zt_cmd_reboot,
        "openpage <id> / op <id>" to R.string.zt_ai_zt_cmd_openpage,
        "menu update|reset" to R.string.zt_ai_zt_cmd_menu,
        "backgroundimage <path> / bgi" to R.string.zt_ai_zt_cmd_background,
        "x11commandshow / x11xs" to R.string.zt_ai_zt_cmd_x11show,
        "x11commandhide / x11xh" to R.string.zt_ai_zt_cmd_x11hide,
        "x11status" to R.string.zt_ai_zt_cmd_x11status,
        "x11keyboardshow / x11kbs" to R.string.zt_ai_zt_cmd_x11kbshow,
        "x11keyboardhide / x11kbh" to R.string.zt_ai_zt_cmd_x11kbhide,
        "dialog ..." to R.string.zt_ai_zt_cmd_dialog,
        "vnc <ip_port_user_pass>" to R.string.zt_ai_zt_cmd_vnc,
        "ln <src> <dst>" to R.string.zt_ai_zt_cmd_ln,
        "qr" to R.string.zt_ai_zt_cmd_qr,
    )
}
