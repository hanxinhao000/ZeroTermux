package com.termux.zerocore.aidebug

/**
 * Multilingual API documentation embedded in GET / and GET /api discovery responses.
 * Structured for external AI (JSON) and human browsers (HTML via ?format=html).
 */
object ZtAiDebugApiDocs {

    const val DOCS_VERSION = 4
    val LANGUAGES = listOf("zh", "en")

    data class DocLocale(
        val title: String,
        val subtitle: String,
        val overview: String,
        val securityTitle: String,
        val securityBullets: List<String>,
        val userSetupTitle: String,
        val userSetupSteps: List<String>,
        val authTitle: String,
        val authMethods: List<String>,
        val workflowTitle: String,
        val workflowSteps: List<String>,
        val endpointsTitle: String,
        val endpoints: List<Map<String, String>>,
        val curlTitle: String,
        val curlExamples: List<Map<String, String>>,
        val errorsTitle: String,
        val errors: List<Map<String, String>>,
        val workstationNote: String,
        val forAiSummary: String
    )

    fun docLocale(lang: String, port: Int, sampleIp: String, codePlaceholder: String): DocLocale {
        val base = "http://$sampleIp:$port"
        val q = "?code=$codePlaceholder"
        return when (lang) {
            "zh" -> DocLocale(
                title = "ZeroTermux 外部 AI 调试 API",
                subtitle = "端口 $port · 局域网 JSON/HTTP 接口 · 供 Cursor 等外部 AI 远程排查 Termux",
                overview = "本服务运行在 Android 手机上的 ZeroTermux 内。PC 与手机须在同一局域网。" +
                    "外部 AI 通过 HTTP 读取终端、执行 shell、logcat、截屏、UI 自动化、系统状态、摄像头、短信、联系人、电话及文件。" +
                    "Root 机可额外开启「Root 完全调试」：su 命令、网络 ADB、dumpsys、任意路径读写。" +
                    "与 ZT 工作站（端口 19999，浏览器手动操作、无匹配码）不同，本接口（端口 $port）专供 AI 自动化调试，且必须匹配码鉴权。",
                securityTitle = "安全警告（AI 必须遵守 — 无论是否 Root）",
                securityBullets = listOf(
                    "【极高危】匹配码可让 AI 完全控制手机可访问的全部数据（终端、截屏、短信、联系人、文件等）。",
                    "【Root 额外风险】开启 Root 完全调试后，AI 可通过 su 修改系统、导致变砖、黑屏、数据永久丢失，且不可逆。",
                    "无论手机是否 Root，上述风险提示均适用；Root 只是扩大破坏范围。",
                    "仅在用户主动开启调试并自愿提供匹配码时使用；调试结束后必须提醒用户关闭开关。",
                    "勿将匹配码写入 git、日志、截图或公开 issue。",
                    "重新开启调试开关会生成新匹配码，旧码立即失效。"
                ),
                userSetupTitle = "用户侧准备（请用户完成）",
                userSetupSteps = listOf(
                    "手机与 PC 连接同一 Wi‑Fi。",
                    "ZeroTermux → 设置 → 开启「启用外部AI调用ZeroTermux」。",
                    "下方详情区 → 点击匹配码旁眼睛图标 → 确认高危提示 → 查看 7 位数字匹配码。",
                    "将手机局域网 IP 与匹配码提供给 AI（例如 $sampleIp + $codePlaceholder）。"
                ),
                authTitle = "鉴权（每个请求都必须带匹配码）",
                authMethods = listOf(
                    "URL 参数（推荐）：$base/?code=$codePlaceholder",
                    "HTTP Header：${ZtAiDebugMatchCodeHelper.HEADER_NAME}: $codePlaceholder",
                    "POST /api/terminal/exec JSON body：{\"code\":\"$codePlaceholder\",\"command\":\"...\",\"waitMs\":3000}",
                    "格式：7 位纯数字（正则 ^\\d{7}\$）"
                ),
                workflowTitle = "推荐调试流程（AI 按顺序执行）",
                workflowSteps = listOf(
                    "GET $base/$q — 读取本说明（当前页面）",
                    "GET $base/api/permissions$q — 检查各功能权限",
                    "GET $base/api/info$q — 设备与版本信息",
                    "GET $base/api/terminal/snapshot$q — 读取终端当前输出",
                    "POST $base/api/terminal/exec$q — 执行 shell 命令排查",
                    "GET $base/api/logs/logcat$q&lines=300 — 读取 logcat",
                    "GET $base/api/screenshot$q — PNG 截屏 ?source=auto|root|app",
                    "GET $base/api/root/status$q — Root/ADB 状态（无需 Root 模式即可查询）",
                    "POST $base/api/root/exec$q — su 执行命令（需 Root 完全调试）",
                    "POST $base/api/input/tap$q — UI 点击 {x,y}（需 Root）",
                    "GET $base/api/system/status$q — 系统状态快照",
                    "GET $base/api/files/read$q&path=相对路径 — 读 Termux \$HOME 下文件",
                    "GET $base/api/llm/tools$q — 列出智能体全部工具",
                    "POST $base/api/beautify/colors$q — 设终端字体/背景色 {\"font_color\":\"#00FF00\"}",
                    "POST $base/api/beautify/clear$q — 清空美化（等同菜单「清空美化」）",
                    "GET $base/api/containers$q — 列出多容器",
                    "POST $base/api/containers/switch$q — 切换容器 {container_id, restart_app?}",
                    "POST $base/api/containers/delete$q — 删除容器 {container_id, user_confirmed}",
                    "POST $base/api/llm/tool$q — 等价 reset: {\"tool\":\"reset_zerotermux_beautify\",\"arguments\":{}}",
                    "权限被拒时：将响应中 hint_for_user_zh 转述用户，授权后重试 GET /api/permissions"
                ),
                endpointsTitle = "端点速查（均需 ?code=匹配码）",
                endpoints = endpointRows(port, "zh"),
                curlTitle = "curl 示例（替换 IP 与 CODE）",
                curlExamples = curlRows(base, codePlaceholder, "zh"),
                errorsTitle = "错误处理",
                errors = errorRows("zh"),
                workstationNote = "ZT 工作站端口 19999 用于浏览器手动操作，无匹配码；本 AI 调试端口为 $port，两者不可混用。",
                forAiSummary = "你是通过 ZeroTermux External AI Debug API 调试用户手机 Termux 的 AI。" +
                    "每个请求必须带 ?code=7位匹配码 或 Header ${ZtAiDebugMatchCodeHelper.HEADER_NAME}。" +
                    "先读 discovery 与 /api/permissions，再用 terminal/snapshot 与 terminal/exec 复现问题。" +
                    "权限失败时 relay hint_for_user_zh。调试结束提醒用户关闭开关。"
            )
            else -> DocLocale(
                title = "ZeroTermux External AI Debug API",
                subtitle = "Port $port · LAN JSON/HTTP · For Cursor and external AI agents",
                overview = "Runs on the user's ZeroTermux Android app. PC and phone must share the same LAN. " +
                    "External AI can read terminal, run shell, logcat, screenshot, UI automation, system status, camera, SMS, contacts, phone, and files. " +
                    "Rooted devices may enable Root full debug: su exec, network ADB, dumpsys, arbitrary path I/O. " +
                    "Unlike ZT Workstation (port 19999, browser, no match code), this API (port $port) is for AI automation and requires a match code.",
                securityTitle = "Security (AI must follow — with or without Root)",
                securityBullets = listOf(
                    "【CRITICAL】Match code grants AI full control of all data the app can access (terminal, screenshot, SMS, contacts, files, etc.).",
                    "【Root extra risk】With Root full debug enabled, AI can run su, modify system, brick device, black screen, permanent data loss — irreversible.",
                    "These warnings apply whether or not the device is rooted; Root only expands the blast radius.",
                    "Use only when user explicitly enabled debug and provided the code; remind them to disable when done.",
                    "Never commit match code to git, logs, screenshots, or public issues.",
                    "Re-enabling the switch generates a new code; old code invalid immediately."
                ),
                userSetupTitle = "User setup (ask the user)",
                userSetupSteps = listOf(
                    "Phone and PC on the same Wi‑Fi.",
                    "ZeroTermux → Settings → enable External AI debug.",
                    "In the detail section → tap the eye icon next to match code → confirm the risk dialog → read the 7-digit code.",
                    "Provide LAN IP and match code to AI (e.g. $sampleIp + $codePlaceholder)."
                ),
                authTitle = "Authentication (required on every request)",
                authMethods = listOf(
                    "Query param (recommended): $base/?code=$codePlaceholder",
                    "HTTP header: ${ZtAiDebugMatchCodeHelper.HEADER_NAME}: $codePlaceholder",
                    "POST /api/terminal/exec JSON: {\"code\":\"$codePlaceholder\",\"command\":\"...\",\"waitMs\":3000}",
                    "Format: 7-digit number only (regex ^\\d{7}\$)"
                ),
                workflowTitle = "Recommended workflow (execute in order)",
                workflowSteps = listOf(
                    "GET $base/$q — read this document",
                    "GET $base/api/permissions$q — check feature permissions",
                    "GET $base/api/info$q — device and version info",
                    "GET $base/api/terminal/snapshot$q — current terminal transcript",
                    "POST $base/api/terminal/exec$q — run shell commands",
                    "GET $base/api/logs/logcat$q&lines=300 — logcat",
                    "GET $base/api/screenshot$q — PNG screenshot (ZeroTermux should be foreground)",
                    "GET $base/api/files/read$q&path=relative — read file under Termux \$HOME",
                    "GET $base/api/llm/tools$q — list all agent tools",
                    "POST $base/api/beautify/colors$q — set colors {\"font_color\":\"#00FF00\"}",
                    "POST $base/api/beautify/clear$q — clear beautify (same as menu Clear Style)",
                    "GET $base/api/containers$q — list multi-containers",
                    "POST $base/api/containers/switch$q — switch container {container_id, restart_app?}",
                    "POST $base/api/containers/delete$q — delete container {container_id, user_confirmed}",
                    "POST $base/api/llm/tool$q — reset: {\"tool\":\"reset_zerotermux_beautify\",\"arguments\":{}}",
                    "On permission denied: relay hint_for_user_zh / hint_for_user_en, then retry GET /api/permissions"
                ),
                endpointsTitle = "Endpoints (append ?code=match-code to all)",
                endpoints = endpointRows(port, "en"),
                curlTitle = "curl examples (replace IP and CODE)",
                curlExamples = curlRows(base, codePlaceholder, "en"),
                errorsTitle = "Error handling",
                errors = errorRows("en"),
                workstationNote = "ZT Workstation port 19999 is for manual browser use without match code; this AI debug port is $port — do not confuse them.",
                forAiSummary = "You debug the user's Termux via ZeroTermux External AI Debug API. " +
                    "Every request needs ?code=7-digit-match-code or header ${ZtAiDebugMatchCodeHelper.HEADER_NAME}. " +
                    "Start with discovery and GET /api/permissions, then terminal/snapshot and terminal/exec. " +
                    "On permission errors relay hint_for_user_* to the user. Remind user to disable the switch when done."
            )
        }
    }

    private fun endpointRows(port: Int, lang: String): List<Map<String, String>> {
        val codeNote = if (lang == "zh") "需 ?code=" else "needs ?code="
        return listOf(
            mapOf("method" to "GET", "path" to "/", "desc" to if (lang == "zh") "Discovery + 多语言说明 + 权限摘要" else "Discovery + multilingual docs + permissions"),
            mapOf("method" to "GET", "path" to "/api/permissions", "desc" to if (lang == "zh") "运行时权限状态" else "Runtime permission status"),
            mapOf("method" to "GET", "path" to "/api/info", "desc" to if (lang == "zh") "Android/ZT 版本、电量、LAN IP" else "Android/ZT version, battery, LAN IPs"),
            mapOf("method" to "GET", "path" to "/api/terminal/snapshot", "desc" to if (lang == "zh") "终端文本 ?maxChars=12000" else "Terminal text ?maxChars=12000"),
            mapOf("method" to "POST", "path" to "/api/terminal/exec", "desc" to if (lang == "zh") "JSON {command, waitMs?}" else "JSON {command, waitMs?}"),
            mapOf("method" to "GET", "path" to "/api/logs/logcat", "desc" to if (lang == "zh") "?lines=300&filter=TAG" else "?lines=300&filter=TAG"),
            mapOf("method" to "GET", "path" to "/api/screenshot", "desc" to if (lang == "zh") "PNG 截屏 ?source=auto|root|app" else "PNG screenshot ?source=auto|root|app"),
            mapOf("method" to "GET", "path" to "/api/root/status", "desc" to if (lang == "zh") "Root/ADB 状态" else "Root/ADB status"),
            mapOf("method" to "POST", "path" to "/api/root/exec", "desc" to if (lang == "zh") "su 执行 {command,timeoutMs?} (需 Root 模式)" else "su exec {command,timeoutMs?} (needs Root mode)"),
            mapOf("method" to "GET", "path" to "/api/system/status", "desc" to if (lang == "zh") "系统状态（内存/磁盘/负载）" else "System status (mem/disk/load)"),
            mapOf("method" to "GET", "path" to "/api/system/dumpsys", "desc" to if (lang == "zh") "?service=activity (需 Root)" else "?service=activity (needs Root)"),
            mapOf("method" to "GET", "path" to "/api/system/packages", "desc" to if (lang == "zh") "已安装包列表 (需 Root)" else "Installed packages (needs Root)"),
            mapOf("method" to "GET", "path" to "/api/system/processes", "desc" to if (lang == "zh") "进程列表 (需 Root)" else "Process list (needs Root)"),
            mapOf("method" to "GET", "path" to "/api/system/getprop", "desc" to if (lang == "zh") "?key= 系统属性" else "?key= system property"),
            mapOf("method" to "POST", "path" to "/api/system/setprop", "desc" to if (lang == "zh") "{key,value} 设置属性 (需 Root)" else "{key,value} setprop (needs Root)"),
            mapOf("method" to "GET", "path" to "/api/logs/dmesg", "desc" to if (lang == "zh") "内核日志 ?lines= (需 Root)" else "Kernel log ?lines= (needs Root)"),
            mapOf("method" to "GET", "path" to "/api/adb/status", "desc" to if (lang == "zh") "ADB TCP 端口状态" else "ADB TCP port status"),
            mapOf("method" to "POST", "path" to "/api/adb/tcp/enable", "desc" to if (lang == "zh") "{port:5555} 开启网络 ADB (需 Root)" else "Enable network ADB (needs Root)"),
            mapOf("method" to "POST", "path" to "/api/adb/tcp/disable", "desc" to if (lang == "zh") "关闭网络 ADB (需 Root)" else "Disable network ADB (needs Root)"),
            mapOf("method" to "POST", "path" to "/api/input/tap", "desc" to if (lang == "zh") "{x,y} 点击 (需 Root)" else "{x,y} tap (needs Root)"),
            mapOf("method" to "POST", "path" to "/api/input/swipe", "desc" to if (lang == "zh") "{x1,y1,x2,y2,durationMs} 滑动" else "{x1,y1,x2,y2,durationMs} swipe"),
            mapOf("method" to "POST", "path" to "/api/input/text", "desc" to if (lang == "zh") "{text} 输入文字" else "{text} type text"),
            mapOf("method" to "POST", "path" to "/api/input/keyevent", "desc" to if (lang == "zh") "{code} 按键" else "{code} keyevent"),
            mapOf("method" to "POST", "path" to "/api/ui/launch", "desc" to if (lang == "zh") "{package,activity?} 启动应用" else "{package,activity?} launch app"),
            mapOf("method" to "POST", "path" to "/api/ui/force-stop", "desc" to if (lang == "zh") "{package} 强制停止" else "{package} force-stop"),
            mapOf("method" to "GET", "path" to "/api/files/list", "desc" to if (lang == "zh") "?path= &root=true 列目录" else "?path= &root=true list dir"),
            mapOf("method" to "POST", "path" to "/api/files/write", "desc" to if (lang == "zh") "{path,content,root?} 写文件" else "{path,content,root?} write file"),
            mapOf("method" to "GET", "path" to "/api/camera/frame", "desc" to if (lang == "zh") "JPEG ?facing=back|front ($codeNote CAMERA)" else "JPEG ?facing=back|front ($codeNote CAMERA)"),
            mapOf("method" to "GET", "path" to "/api/files/read", "desc" to if (lang == "zh") "?path= Termux 相对路径 ($codeNote storage)" else "?path= relative under Termux home ($codeNote storage)"),
            mapOf("method" to "GET", "path" to "/api/contacts", "desc" to if (lang == "zh") "联系人 ($codeNote READ_CONTACTS)" else "Contacts ($codeNote READ_CONTACTS)"),
            mapOf("method" to "GET", "path" to "/api/sms/threads", "desc" to if (lang == "zh") "短信会话 ($codeNote READ_SMS)" else "SMS threads ($codeNote READ_SMS)"),
            mapOf("method" to "GET", "path" to "/api/sms/messages", "desc" to if (lang == "zh") "?address= ($codeNote READ_SMS)" else "?address= ($codeNote READ_SMS)"),
            mapOf("method" to "GET", "path" to "/api/phone/info", "desc" to if (lang == "zh") "SIM/运营商 ($codeNote READ_PHONE_STATE)" else "SIM/operator ($codeNote READ_PHONE_STATE)"),
            mapOf("method" to "GET", "path" to "/api/vnc/status", "desc" to if (lang == "zh") "编辑器 VNC 诊断（Xvfb/x11vnc/端口/日志）" else "Editor VNC diagnostics (Xvfb/x11vnc/port/log)"),
            mapOf("method" to "POST", "path" to "/api/vnc/start", "desc" to if (lang == "zh") "启动 Xvfb + x11vnc（:99 端口 15901）" else "Start Xvfb + x11vnc (:99 port 15901)"),
            mapOf("method" to "POST", "path" to "/api/editor/open", "desc" to if (lang == "zh") "JSON {path} 打开 EditTextActivity" else "JSON {path} open EditTextActivity"),
            mapOf("method" to "GET", "path" to "/api/llm/tools", "desc" to if (lang == "zh") "列出全部 LLM/智能体工具名" else "List all LLM agent tool names"),
            mapOf("method" to "POST", "path" to "/api/llm/tool", "desc" to if (lang == "zh") "执行 LLM 工具 {tool, arguments}" else "Run LLM tool {tool, arguments}"),
            mapOf("method" to "POST", "path" to "/api/config/get", "desc" to if (lang == "zh") "读 ZeroTermux 配置 {group?, keys?}" else "Get ZT config {group?, keys?}"),
            mapOf("method" to "POST", "path" to "/api/config/set", "desc" to if (lang == "zh") "写配置 {key,value}（含 font_color/back_color #RRGGBB）" else "Set config {key,value} incl. font_color/back_color"),
            mapOf("method" to "POST", "path" to "/api/config/zt", "desc" to if (lang == "zh") "执行 zt 命令 {command}" else "Run zt command {command}"),
            mapOf("method" to "GET", "path" to "/api/beautify/colors", "desc" to if (lang == "zh") "读取字体/背景遮罩颜色" else "Get font & overlay colors"),
            mapOf("method" to "POST", "path" to "/api/beautify/colors", "desc" to if (lang == "zh") "设颜色 {font_color,back_color} #RRGGBB" else "Set colors {font_color,back_color}"),
            mapOf("method" to "POST", "path" to "/api/beautify/clear", "desc" to if (lang == "zh") "清空美化（等同菜单）" else "Clear beautify (menu Clear Style)"),
            mapOf("method" to "GET", "path" to "/api/containers", "desc" to if (lang == "zh") "列出 Termux 多容器" else "List Termux multi-containers"),
            mapOf("method" to "POST", "path" to "/api/containers/switch", "desc" to if (lang == "zh") "切换容器 {container_id,restart_app?}" else "Switch container {container_id,restart_app?}"),
            mapOf("method" to "POST", "path" to "/api/containers/delete", "desc" to if (lang == "zh") "删除容器 {container_id,user_confirmed}" else "Delete container {container_id,user_confirmed}"),
        )
    }

    private fun curlRows(base: String, code: String, lang: String): List<Map<String, String>> {
        val q = "?code=$code"
        return listOf(
            mapOf(
                "name" to if (lang == "zh") "Discovery" else "Discovery",
                "cmd" to "curl -s \"$base/$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "权限" else "Permissions",
                "cmd" to "curl -s \"$base/api/permissions$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "终端快照" else "Terminal snapshot",
                "cmd" to "curl -s \"$base/api/terminal/snapshot$q&maxChars=12000\""
            ),
            mapOf(
                "name" to if (lang == "zh") "执行命令" else "Exec command",
                "cmd" to "curl -s -X POST \"$base/api/terminal/exec$q\" -H \"Content-Type: application/json\" -d '{\"command\":\"pwd && ls -la\",\"waitMs\":3000}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "logcat" else "logcat",
                "cmd" to "curl -s \"$base/api/logs/logcat$q&lines=300&filter=EditorVncPanel\""
            ),
            mapOf(
                "name" to if (lang == "zh") "Root 状态" else "Root status",
                "cmd" to "curl -s \"$base/api/root/status$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "Root 执行" else "Root exec",
                "cmd" to "curl -s -X POST \"$base/api/root/exec$q\" -H \"Content-Type: application/json\" -d '{\"command\":\"id && getprop ro.build.version.release\",\"timeoutMs\":5000}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "Root 截屏" else "Root screenshot",
                "cmd" to "curl -s \"$base/api/screenshot$q&source=root\" -o screen.png"
            ),
            mapOf(
                "name" to if (lang == "zh") "UI 点击" else "UI tap",
                "cmd" to "curl -s -X POST \"$base/api/input/tap$q\" -H \"Content-Type: application/json\" -d '{\"x\":540,\"y\":1200}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "开启网络 ADB" else "Enable network ADB",
                "cmd" to "curl -s -X POST \"$base/api/adb/tcp/enable$q\" -H \"Content-Type: application/json\" -d '{\"port\":5555}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "系统状态" else "System status",
                "cmd" to "curl -s \"$base/api/system/status$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "截屏" else "Screenshot",
                "cmd" to "curl -s \"$base/api/screenshot$q&source=auto\" -o screen.png"
            ),
            mapOf(
                "name" to if (lang == "zh") "读文件" else "Read file",
                "cmd" to "curl -s \"$base/api/files/read$q&path=.zerotermux/x11vnc.log\""
            ),
            mapOf(
                "name" to if (lang == "zh") "VNC 状态" else "VNC status",
                "cmd" to "curl -s \"$base/api/vnc/status$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "启动 VNC" else "Start VNC",
                "cmd" to "curl -s -X POST \"$base/api/vnc/start$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "打开编辑器" else "Open editor",
                "cmd" to "curl -s -X POST \"$base/api/editor/open$q\" -H \"Content-Type: application/json\" -d '{\"path\":\"project/project_java/Hello.java\"}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "VNC 黑屏排查" else "VNC black screen",
                "cmd" to "curl -s -X POST \"$base/api/terminal/exec$q\" -H \"Content-Type: application/json\" -d '{\"command\":\"export DISPLAY=:99; pgrep -af x11vnc\",\"waitMs\":4000}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "美化颜色" else "Beautify colors",
                "cmd" to "curl -s -X POST \"$base/api/beautify/colors$q\" -H \"Content-Type: application/json\" -d '{\"font_color\":\"#00FF00\",\"back_color\":\"#000000\"}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "LLM 设配置" else "LLM set config",
                "cmd" to "curl -s -X POST \"$base/api/llm/tool$q\" -H \"Content-Type: application/json\" -d '{\"tool\":\"set_zerotermux_config\",\"arguments\":{\"key\":\"font_color\",\"value\":\"#FF0000\"}}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "读美化颜色" else "Get beautify colors",
                "cmd" to "curl -s \"$base/api/beautify/colors$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "清空美化" else "Clear beautify",
                "cmd" to "curl -s -X POST \"$base/api/beautify/clear$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "LLM 清空美化" else "LLM reset beautify",
                "cmd" to "curl -s -X POST \"$base/api/llm/tool$q\" -H \"Content-Type: application/json\" -d '{\"tool\":\"reset_zerotermux_beautify\",\"arguments\":{}}'"
            ),
            mapOf(
                "name" to if (lang == "zh") "容器列表" else "List containers",
                "cmd" to "curl -s \"$base/api/containers$q\""
            ),
            mapOf(
                "name" to if (lang == "zh") "切换容器" else "Switch container",
                "cmd" to "curl -s -X POST \"$base/api/containers/switch$q\" -H \"Content-Type: application/json\" -d '{\"container_id\":\"files1\",\"restart_app\":\"true\"}'"
            )
        )
    }

    private fun errorRows(lang: String): List<Map<String, String>> {
        return if (lang == "zh") {
            listOf(
                mapOf("signal" to "locked: true", "meaning" to "未带匹配码", "action" to "向用户索取 7 位匹配码"),
                mapOf("signal" to "invalid or missing match_code", "meaning" to "匹配码错误或缺失", "action" to "请用户重新查看设置中的匹配码"),
                mapOf("signal" to "permission_required / hint_for_user_zh", "meaning" to "权限未授予", "action" to "转述 hint_for_user_zh，引导系统设置授权"),
                mapOf("signal" to "root_mode_disabled", "meaning" to "未开启 Root 完全调试", "action" to "请用户在设置中开启 Root 完全调试并确认变砖风险"),
                mapOf("signal" to "root_unavailable", "meaning" to "su 不可用", "action" to "请用户在 Magisk/SU 中授予 ZeroTermux Root"),
                mapOf("signal" to "service disabled", "meaning" to "用户已关闭开关", "action" to "请用户重新开启调试功能")
            )
        } else {
            listOf(
                mapOf("signal" to "locked: true", "meaning" to "No match code", "action" to "Ask user for 7-digit code"),
                mapOf("signal" to "invalid or missing match_code", "meaning" to "Wrong or missing code", "action" to "Ask user to re-read code in Settings"),
                mapOf("signal" to "permission_required / hint_for_user_*", "meaning" to "Permission denied", "action" to "Relay hint_for_user_* and ask user to grant in Settings"),
                mapOf("signal" to "screenshot failed", "meaning" to "截屏失败", "action" to "试 ?source=root 或请用户将 ZeroTermux 切到前台"),
                mapOf("signal" to "root_mode_disabled", "meaning" to "Root mode off", "action" to "Ask user to enable Root full debug in Settings"),
                mapOf("signal" to "root_unavailable", "meaning" to "su unavailable", "action" to "Ask user to grant ZeroTermux root in Magisk/SU"),
                mapOf("signal" to "service disabled", "meaning" to "Feature disabled", "action" to "Ask user to re-enable External AI debug")
            )
        }
    }

    fun localeToMap(doc: DocLocale): Map<String, Any?> = mapOf(
        "title" to doc.title,
        "subtitle" to doc.subtitle,
        "overview" to doc.overview,
        "security" to mapOf(
            "title" to doc.securityTitle,
            "bullets" to doc.securityBullets
        ),
        "user_setup" to mapOf(
            "title" to doc.userSetupTitle,
            "steps" to doc.userSetupSteps
        ),
        "auth" to mapOf(
            "title" to doc.authTitle,
            "methods" to doc.authMethods,
            "query_param" to ZtAiDebugMatchCodeHelper.QUERY_PARAM,
            "header" to ZtAiDebugMatchCodeHelper.HEADER_NAME,
            "format" to "7-digit number"
        ),
        "workflow" to mapOf(
            "title" to doc.workflowTitle,
            "steps" to doc.workflowSteps
        ),
        "endpoints" to mapOf(
            "title" to doc.endpointsTitle,
            "items" to doc.endpoints
        ),
        "curl_examples" to mapOf(
            "title" to doc.curlTitle,
            "items" to doc.curlExamples
        ),
        "errors" to mapOf(
            "title" to doc.errorsTitle,
            "items" to doc.errors
        ),
        "workstation_note" to doc.workstationNote,
        "for_ai" to doc.forAiSummary
    )

    fun buildI18nDocs(port: Int, sampleIp: String, codePlaceholder: String): Map<String, Any?> {
        return mapOf(
            "docs_version" to DOCS_VERSION,
            "languages" to LANGUAGES,
            "default_language" to "zh",
            "html_url_hint" to "Append ?format=html for human-readable page; ?lang=zh or ?lang=en",
            "i18n" to LANGUAGES.associateWith { lang ->
                localeToMap(docLocale(lang, port, sampleIp, codePlaceholder))
            }
        )
    }

    fun buildForAiBlock(authorized: Boolean, port: Int, sampleIp: String, code: String?): Map<String, Any?> {
        val codePh = code ?: "XXXXXXX"
        val base = "http://$sampleIp:$port"
        return mapOf(
            "role" to "external_debug_agent",
            "read_this_first" to true,
            "authorized" to authorized,
            "instruction" to if (authorized) {
                "Match code accepted. Read docs.i18n (zh/en). Follow workflow.steps. Append ?code=$codePh to every URL."
            } else {
                "Service reachable but locked. Read docs.i18n for full API spec. Ask user for 7-digit match code from ZeroTermux Settings → External AI debug → eye icon. Then GET $base/?code=CODE"
            },
            "recommended_first_requests" to if (authorized && code != null) {
                listOf(
                    "GET $base/?code=$code",
                    "GET $base/api/root/status?code=$code",
                    "GET $base/api/permissions?code=$code",
                    "GET $base/api/system/status?code=$code",
                    "GET $base/api/terminal/snapshot?code=$code"
                )
            } else {
                listOf(
                    "GET $base/  (locked overview, no code)",
                    "GET $base/?code=USER_CODE  (full docs after user provides code)"
                )
            },
            "response_fields_for_ai" to listOf(
                "hint_for_ai — follow this when present",
                "hint_for_user_zh / hint_for_user_en — relay to user verbatim",
                "docs.i18n.<lang>.for_ai — role summary in that language"
            ),
            "root_debug_sequence" to listOf(
                "GET $base/api/root/status?code=$codePh",
                "GET $base/api/screenshot?code=$codePh&source=root",
                "POST $base/api/root/exec?code=$codePh {\"command\":\"dumpsys activity top\",\"timeoutMs\":8000}",
                "POST $base/api/input/tap?code=$codePh {\"x\":540,\"y\":1200}",
                "POST $base/api/adb/tcp/enable?code=$codePh {\"port\":5555}"
            ),
            "vnc_debug_sequence" to listOf(
                "POST $base/api/terminal/exec?code=$codePh {\"command\":\"export DISPLAY=:99; pgrep -af x11vnc\",\"waitMs\":4000}",
                "GET $base/api/files/read?code=$codePh&path=.zerotermux/x11vnc.log",
                "GET $base/api/screenshot?code=$codePh",
                "GET $base/api/logs/logcat?code=$codePh&lines=200&filter=EditorVncPanel"
            )
        )
    }

    fun buildHtml(
        port: Int,
        ips: List<String>,
        authorized: Boolean,
        code: String?,
        preferredLang: String?
    ): String {
        val sampleIp = ips.firstOrNull() ?: "<phone-ip>"
        val codePh = if (authorized && code != null) code else "XXXXXXX"
        val langs = if (preferredLang != null && preferredLang in LANGUAGES) {
            listOf(preferredLang)
        } else {
            LANGUAGES
        }
        val lockedBanner = if (authorized) "" else """
            <div class="banner locked">
              <strong>🔒 ${if (preferredLang == "en") "Locked" else "未解锁"}</strong> —
              ${if (preferredLang == "en")
            "Provide ?code=7-digit-match-code for full access. Below is public documentation."
        else
            "需 ?code=7位匹配码 才能调用 API；以下为公开说明文档。"}
            </div>
        """.trimIndent()

        val langSections = langs.joinToString("\n") { lang ->
            val doc = docLocale(lang, port, sampleIp, codePh)
            renderHtmlSection(doc, lang)
        }

        val ipList = if (ips.isEmpty()) "<li>$sampleIp</li>" else ips.joinToString("") { "<li><code>$it</code></li>" }
        val jsonLink = if (authorized && code != null) {
            "/?code=$code"
        } else {
            "/"
        }

        return """
<!DOCTYPE html>
<html lang="${preferredLang ?: "zh"}">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>ZeroTermux AI Debug API :$port</title>
  <style>
    * { box-sizing: border-box; }
    body { font-family: system-ui, -apple-system, sans-serif; margin: 0; padding: 16px;
           background: #1a1a1a; color: #e8e8e8; line-height: 1.55; max-width: 920px; margin-inline: auto; }
    h1 { font-size: 1.35rem; margin: 0 0 4px; color: #fff; }
    h2 { font-size: 1.05rem; margin: 1.4em 0 0.5em; color: #7ec8ff; border-bottom: 1px solid #333; padding-bottom: 4px; }
    h3 { font-size: 0.95rem; margin: 1em 0 0.4em; color: #ccc; }
    .sub { color: #999; font-size: 0.9rem; margin-bottom: 12px; }
    .banner { padding: 10px 12px; border-radius: 6px; margin: 12px 0; }
    .banner.locked { background: #4a3000; border: 1px solid #886600; color: #ffe0a0; }
    .banner.ok { background: #0a3d2a; border: 1px solid #1a6b4a; color: #a0ffc0; }
    ul, ol { padding-left: 1.3em; }
    li { margin: 0.25em 0; }
    code, pre { font-family: ui-monospace, monospace; font-size: 0.85rem; }
    pre { background: #111; border: 1px solid #333; border-radius: 6px; padding: 10px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }
    table { width: 100%; border-collapse: collapse; font-size: 0.88rem; margin: 8px 0; }
    th, td { border: 1px solid #333; padding: 6px 8px; text-align: left; vertical-align: top; }
    th { background: #252525; color: #aaa; }
    .lang-block { margin-top: 28px; padding-top: 8px; border-top: 2px solid #444; }
    .lang-tag { display: inline-block; background: #333; color: #7ec8ff; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; margin-bottom: 8px; }
    .nav { font-size: 0.85rem; margin-bottom: 16px; }
    .nav a { color: #7ec8ff; margin-right: 12px; }
    .warn li { color: #ffb080; }
  </style>
</head>
<body>
  <div class="nav">
    <a href="$jsonLink">JSON</a>
    <a href="/?format=html&lang=zh">中文 HTML</a>
    <a href="/?format=html&lang=en">English HTML</a>
  </div>
  ${if (authorized) "<div class=\"banner ok\">✓ ${if (preferredLang == "en") "Authorized" else "已鉴权"} · code=<code>$codePh</code></div>" else lockedBanner}
  <p class="sub">LAN IPs: <ul style="display:inline; list-style:none; padding:0;">$ipList</ul> · Port <code>$port</code> · ZT Workstation <code>19999</code> (separate)</p>
  $langSections
  <h2>Machine-readable</h2>
  <p>AI agents: <code>GET $jsonLink</code> returns JSON with <code>docs.i18n</code> (zh/en) and <code>for_ai</code> block.</p>
</body>
</html>
        """.trimIndent()
    }

    private fun renderHtmlSection(doc: DocLocale, lang: String): String {
        val bullets = doc.securityBullets.joinToString("") { "<li>$it</li>" }
        val setup = doc.userSetupSteps.mapIndexed { i, s -> "<li>$s</li>" }.joinToString("")
        val auth = doc.authMethods.joinToString("") { "<li><code>${escapeHtml(it)}</code></li>" }
        val workflow = doc.workflowSteps.joinToString("") { "<li><code>${escapeHtml(it)}</code></li>" }
        val endpointRows = doc.endpoints.joinToString("") { e ->
            "<tr><td>${e["method"]}</td><td><code>${escapeHtml(e["path"] ?: "")}</code></td><td>${escapeHtml(e["desc"] ?: "")}</td></tr>"
        }
        val curls = doc.curlExamples.joinToString("") { ex ->
            "<h3>${escapeHtml(ex["name"] ?: "")}</h3><pre>${escapeHtml(ex["cmd"] ?: "")}</pre>"
        }
        val errors = doc.errors.joinToString("") { e ->
            "<tr><td><code>${escapeHtml(e["signal"] ?: "")}</code></td><td>${escapeHtml(e["meaning"] ?: "")}</td><td>${escapeHtml(e["action"] ?: "")}</td></tr>"
        }
        return """
  <section class="lang-block">
    <span class="lang-tag">${lang.uppercase()}</span>
    <h1>${escapeHtml(doc.title)}</h1>
    <p class="sub">${escapeHtml(doc.subtitle)}</p>
    <p>${escapeHtml(doc.overview)}</p>
    <h2>${escapeHtml(doc.securityTitle)}</h2>
    <ul class="warn">$bullets</ul>
    <h2>${escapeHtml(doc.userSetupTitle)}</h2>
    <ol>$setup</ol>
    <h2>${escapeHtml(doc.authTitle)}</h2>
    <ul>$auth</ul>
    <h2>${escapeHtml(doc.workflowTitle)}</h2>
    <ol>$workflow</ol>
    <h2>${escapeHtml(doc.endpointsTitle)}</h2>
    <table><tr><th>Method</th><th>Path</th><th>Description</th></tr>$endpointRows</table>
    <h2>${escapeHtml(doc.curlTitle)}</h2>
    $curls
    <h2>${escapeHtml(doc.errorsTitle)}</h2>
    <table><tr><th>Signal</th><th>Meaning</th><th>AI action</th></tr>$errors</table>
    <p><em>${escapeHtml(doc.workstationNote)}</em></p>
    <h2>for_ai</h2>
    <pre>${escapeHtml(doc.forAiSummary)}</pre>
  </section>
        """.trimIndent()
    }

    private fun escapeHtml(s: String): String {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
