package com.termux.zerocore.config.ztcommand.navigation

import android.content.Intent
import com.termux.app.TermuxActivity
import com.termux.app.activities.HelpActivity
import com.termux.app.activities.SettingsActivity
import com.termux.zerocore.activity.BackNewActivity
import com.termux.zerocore.activity.EditTextActivity
import com.termux.zerocore.activity.FontActivity
import com.termux.zerocore.activity.SwitchActivity
import com.termux.zerocore.activity.WebViewActivity
import com.termux.zerocore.ai.activity.MainAiSettings
import com.termux.zerocore.ai.agent.ZtAgentAiSettingsActivity
import com.termux.zerocore.ai.deepseek.activity.ZeroTermuxDeepSeekKeyActivity
import com.termux.zerocore.ai.deepseek.activity.ZeroTermuxDeepSeekSettingsActivity
import com.termux.zerocore.config.ztcommand.activity.SocketBaseActivity
import com.termux.zerocore.developer.DeveloperActivity
import com.termux.zerocore.guide.TermuxGuideActivity
import com.termux.zerocore.llm.activity.ZeroTermuxLLMSettingsActivity
import com.termux.zerocore.scrcpy.MainActivity
import com.termux.zerocore.settings.ContainerSettingsMainActivity
import com.termux.zerocore.settings.LeftMenuSettingsActivity
import com.termux.zerocore.settings.MenuUpdateSourceActivity
import com.termux.zerocore.settings.TimerActivity
import com.termux.zerocore.settings.ZTAboutActivity
import com.termux.zerocore.settings.ZTInstallActivity
import com.termux.zerocore.settings.ZTOnlineServerActivity
import com.termux.zerocore.settings.ZeroTermuxSettingsActivity
import com.termux.zerocore.settings.ZtSettingsActivity
import com.termux.zerocore.utermux_windows.qemu.activity.RunWindowActivity
import com.termux.zerocore.workstation.ZtWorkstationSettingsActivity

object ZtPageNavigationRegistry {

    data class PageEntry(
        val id: String,
        val title: String,
        val activityClass: Class<*>,
        val applyExtras: ((Intent) -> Unit)? = null
    )

    private val aliases: Map<String, String> = mapOf(
        "boot_wizard" to "guide",
        "boot_guide" to "guide",
        "boot guide" to "guide",
        "startup_guide" to "guide",
        "wizard" to "guide",
        "开机向导" to "guide",
        "开机引导" to "guide",
        "引导页" to "guide",
        "打开开机向导" to "guide",
        "打开开机引导" to "guide",
        "settings" to "zt_settings",
        "setting" to "zt_settings",
        "设置" to "zt_settings",
        "打开设置" to "zt_settings",
        "zerotermux_settings" to "zt_settings",
        "zerotermux设置" to "zt_settings",
        "功能设置" to "zero_termux_settings",
        "zero_termux" to "zero_termux_settings",
        "workstation" to "workstation_settings",
        "工作站" to "workstation_settings",
        "电脑工作站" to "workstation_settings",
        "agent_ai" to "agent_ai_settings",
        "agent ai" to "agent_ai_settings",
        "智能体" to "agent_ai_settings",
        "智能体设置" to "agent_ai_settings",
        "ai_settings" to "ai_settings",
        "deepseek" to "deepseek_settings",
        "llm" to "llm_settings",
        "about" to "zt_about",
        "关于" to "zt_about",
        "timer" to "timer",
        "定时任务" to "timer",
        "定时" to "timer",
        "backup" to "backup",
        "备份" to "backup",
        "developer" to "developer",
        "开发者" to "developer",
        "help" to "help",
        "帮助" to "help",
        "termux" to "termux",
        "终端" to "termux",
        "主终端" to "termux",
        "editor" to "editor",
        "编辑器" to "editor",
        "scrcpy" to "scrcpy",
        "投屏" to "scrcpy",
        "install" to "zt_install",
        "安装" to "zt_install",
        "font" to "font",
        "字体" to "font",
        "container" to "container_settings",
        "容器" to "container_settings",
        "left_menu" to "left_menu_settings",
        "左侧菜单" to "left_menu_settings"
    )

    /** 从设置/智能体打开向导时需带 jump_other，否则 isJumpGuide=true 会立刻跳回主界面 */
    private fun applyBootWizardExtras(intent: Intent, step: Int) {
        intent.putExtra(TermuxGuideActivity.GUIDE_EXTRA, step)
        intent.putExtra(TermuxGuideActivity.GUIDE_EXTRA_JUMP_OTHER, true)
    }

    private val pages: List<PageEntry> = listOf(
        PageEntry("termux", "主终端", TermuxActivity::class.java),
        PageEntry("zt_settings", "ZeroTermux 设置", ZtSettingsActivity::class.java),
        PageEntry("zero_termux_settings", "ZeroTermux 功能设置", ZeroTermuxSettingsActivity::class.java),
        PageEntry("left_menu_settings", "左侧菜单设置", LeftMenuSettingsActivity::class.java),
        PageEntry("menu_update_source", "菜单更新源", MenuUpdateSourceActivity::class.java),
        PageEntry("ai_settings", "AI 设置", MainAiSettings::class.java),
        PageEntry("agent_ai_settings", "智能体 AI 配置", ZtAgentAiSettingsActivity::class.java),
        PageEntry("deepseek_settings", "DeepSeek 设置", ZeroTermuxDeepSeekSettingsActivity::class.java),
        PageEntry("deepseek_key", "DeepSeek Key", ZeroTermuxDeepSeekKeyActivity::class.java),
        PageEntry("llm_settings", "自定义 LLM 设置", ZeroTermuxLLMSettingsActivity::class.java),
        PageEntry("termux_settings", "Termux 原生设置", SettingsActivity::class.java),
        PageEntry("zt_about", "关于 ZeroTermux", ZTAboutActivity::class.java),
        PageEntry("zt_install", "安装 Termux 软件", ZTInstallActivity::class.java),
        PageEntry("zt_online_server", "在线服务器", ZTOnlineServerActivity::class.java),
        PageEntry("timer", "定时任务", TimerActivity::class.java),
        PageEntry("container_settings", "容器设置", ContainerSettingsMainActivity::class.java),
        PageEntry("workstation_settings", "ZT 电脑工作站", ZtWorkstationSettingsActivity::class.java),
        PageEntry("developer", "开发者选项", DeveloperActivity::class.java),
        PageEntry(
            "guide",
            "开机向导（协议页）",
            TermuxGuideActivity::class.java
        ) { intent ->
            applyBootWizardExtras(intent, TermuxGuideActivity.GUIDE_AGREEMENT)
        },
        PageEntry(
            "guide_usage",
            "开机向导（使用习惯）",
            TermuxGuideActivity::class.java
        ) { intent ->
            applyBootWizardExtras(intent, TermuxGuideActivity.GUIDE_USAGE_HABITS)
        },
        PageEntry(
            "save_path",
            "开机向导（创建文件夹）",
            TermuxGuideActivity::class.java
        ) { intent ->
            applyBootWizardExtras(intent, TermuxGuideActivity.GUIDE_CREATE_FOLDER)
        },
        PageEntry("backup", "备份恢复", BackNewActivity::class.java),
        PageEntry("font", "字体设置", FontActivity::class.java),
        PageEntry("switch_system", "切换系统", SwitchActivity::class.java),
        PageEntry("scrcpy", "Scrcpy 投屏", MainActivity::class.java),
        PageEntry("qemu_run", "QEMU 运行窗口", RunWindowActivity::class.java),
        PageEntry("webview", "内置浏览器", WebViewActivity::class.java),
        PageEntry("help", "帮助", HelpActivity::class.java),
        PageEntry("zt_command_socket", "ZT 命令调试", SocketBaseActivity::class.java),
        PageEntry("editor", "文本编辑器", EditTextActivity::class.java)
    )

    private val keywordRules: List<Pair<List<String>, String>> = listOf(
        listOf("开机向导", "开机引导", "引导页", "boot guide", "boot wizard", "boot_guide", "wizard") to "guide",
        listOf("工作站", "workstation") to "workstation_settings",
        listOf("智能体", "agent ai", "agent_ai") to "agent_ai_settings",
        listOf("功能设置", "zero termux settings") to "zero_termux_settings",
        listOf("deepseek") to "deepseek_settings",
        listOf("llm", "自定义 llm") to "llm_settings",
        listOf("关于", "about") to "zt_about",
        listOf("定时", "timer") to "timer",
        listOf("备份", "backup") to "backup",
        listOf("开发者", "developer") to "developer",
        listOf("容器", "container") to "container_settings",
        listOf("投屏", "scrcpy") to "scrcpy",
        listOf("编辑器", "editor") to "editor",
        listOf("主终端", "termux main") to "termux",
        listOf("termux 设置", "termux settings", "原生设置") to "termux_settings",
        listOf("zerotermux 设置", "zerotermux设置", "zero termux 设置") to "zt_settings"
    )

    @JvmStatic
    fun allPages(): List<PageEntry> = pages

    @JvmStatic
    fun aliasLines(): List<String> {
        return aliases.entries.map { (alias, target) -> "$alias -> $target" }
    }

    @JvmStatic
    fun find(pageId: String): PageEntry? {
        if (pageId.isBlank()) return null
        val query = normalizeQuery(pageId)
        if (query.isBlank()) return null

        resolveAlias(query)?.let { return pageById(it) }
        pageById(query.lowercase())?.let { return it }
        pageByActivitySimpleName(query)?.let { return it }
        resolveByKeywords(query)?.let { return pageById(it) }
        pageByExactTitle(query)?.let { return it }
        pageByBestTitleMatch(query)?.let { return it }
        if (query.contains(".")) {
            return resolveByClassName(query)
        }
        return null
    }

    private fun normalizeQuery(raw: String): String {
        var query = raw.trim()
        val prefixes = listOf(
            "请帮我打开", "帮我打开", "请打开", "打开", "跳转", "进入", "去",
            "open the ", "open ", "go to ", "goto ", "navigate to "
        ).sortedByDescending { it.length }
        for (prefix in prefixes) {
            if (query.startsWith(prefix, ignoreCase = true)) {
                query = query.substring(prefix.length).trim()
                break
            }
        }
        return query.trim(' ', '，', ',', '。', '.', '：', ':', '"', '\'', '「', '」')
    }

    private fun resolveAlias(query: String): String? {
        val lower = query.lowercase()
        return aliases[lower] ?: aliases[query]
    }

    private fun resolveByKeywords(query: String): String? {
        val lower = query.lowercase()
        for ((keywords, targetId) in keywordRules) {
            if (keywords.any { keyword -> lower.contains(keyword.lowercase()) }) {
                return targetId
            }
        }
        return null
    }

    private fun pageById(id: String): PageEntry? {
        return pages.firstOrNull { it.id == id }
    }

    private fun pageByActivitySimpleName(query: String): PageEntry? {
        return pages.firstOrNull {
            it.activityClass.simpleName.equals(query, ignoreCase = true)
        }
    }

    private fun pageByExactTitle(query: String): PageEntry? {
        return pages.firstOrNull { it.title.equals(query, ignoreCase = true) }
    }

    private fun pageByBestTitleMatch(query: String): PageEntry? {
        val lower = query.lowercase()
        val matches = pages.mapNotNull { page ->
            val titleLower = page.title.lowercase()
            val score = when {
                titleLower == lower -> 100
                titleLower.contains(lower) -> 60 + lower.length
                lower.contains(titleLower) -> 50 + titleLower.length
                else -> null
            }
            score?.let { page to it }
        }
        return matches.maxByOrNull { it.second }?.first
    }

    private fun resolveByClassName(className: String): PageEntry? {
        return try {
            val clazz = Class.forName(className)
            if (TermuxGuideActivity::class.java.isAssignableFrom(clazz)) {
                return PageEntry(
                    id = className.substringAfterLast('.').lowercase(),
                    title = className,
                    activityClass = clazz
                ) { intent ->
                    applyBootWizardExtras(intent, TermuxGuideActivity.GUIDE_AGREEMENT)
                }
            }
            PageEntry(
                id = className.substringAfterLast('.').lowercase(),
                title = className,
                activityClass = clazz
            )
        } catch (_: Exception) {
            null
        }
    }
}
