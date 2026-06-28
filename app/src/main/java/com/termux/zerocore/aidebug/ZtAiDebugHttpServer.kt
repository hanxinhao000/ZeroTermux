package com.termux.zerocore.aidebug

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.termux.zerocore.workstation.ZtWorkstationContactsHelper
import com.termux.zerocore.workstation.ZtWorkstationFileHelper
import com.termux.zerocore.workstation.ZtWorkstationSmsHelper
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream

class ZtAiDebugHttpServer(
    private val appContext: Context,
    port: Int
) : NanoHTTPD(port) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            return cors(newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, ""))
        }
        if (!ZtAiDebugManager.isRunning()) {
            return textResponse(jsonError("service disabled"), MIME_JSON)
        }
        val uri = session.uri
        val method = session.method
        val matchCode = extractMatchCode(session)
        val authorized = ZtAiDebugMatchCodeHelper.isValid(matchCode)
        if (!authorized) {
            if ((uri == "/" || uri == "/api") && method == Method.GET) {
                return discoveryResponse(appContext, authorized = false, session)
            }
            return unauthorizedResponse()
        }
        return try {
            when {
                (uri == "/" || uri == "/api") && method == Method.GET ->
                    discoveryResponse(appContext, authorized = true, session)
                uri == "/api/info" && method == Method.GET ->
                    textResponse(ZtAiDebugInfoHelper.deviceInfo(appContext), MIME_JSON)
                uri == "/api/permissions" && method == Method.GET ->
                    textResponse(ZtAiDebugPermissionHelper.statusJson(appContext), MIME_JSON)
                uri == "/api/terminal/snapshot" && method == Method.GET -> {
                    val maxChars = session.parms["maxChars"]?.toIntOrNull()?.coerceIn(1000, 50000) ?: 12000
                    textResponse(terminalSnapshotJson(maxChars), MIME_JSON)
                }
                uri == "/api/terminal/exec" && method == Method.POST ->
                    handleTerminalExec(session)
                uri == "/api/logs/logcat" && method == Method.GET -> {
                    val lines = session.parms["lines"]?.toIntOrNull() ?: 300
                    val filter = session.parms["filter"]
                    textResponse(ZtAiDebugLogHelper.readLogcat(lines, filter), MIME_JSON)
                }
                uri == "/api/screenshot" && method == Method.GET ->
                    handleScreenshot(session)
                uri == "/api/camera/frame" && method == Method.GET ->
                    handleCameraFrame(session.parms["facing"] ?: "back")
                uri == "/api/contacts" && method == Method.GET ->
                    guardFeature("contacts") {
                        textResponse(wrapOkJson(ZtWorkstationContactsHelper.listContacts(appContext)), MIME_JSON)
                    }
                uri == "/api/sms/threads" && method == Method.GET ->
                    guardFeature("sms") {
                        textResponse(wrapOkJson(ZtWorkstationSmsHelper.listThreads(appContext)), MIME_JSON)
                    }
                uri == "/api/sms/messages" && method == Method.GET ->
                    guardFeature("sms") {
                        textResponse(
                            wrapOkJson(
                                ZtWorkstationSmsHelper.listMessages(
                                    appContext,
                                    session.parms["address"] ?: ""
                                )
                            ),
                            MIME_JSON
                        )
                    }
                uri == "/api/phone/info" && method == Method.GET ->
                    textResponse(ZtAiDebugPhoneHelper.phoneInfo(appContext), MIME_JSON)
                uri == "/api/files/read" && method == Method.GET ->
                    guardFeature("storage") {
                        textResponse(ZtWorkstationFileHelper.readText(session.parms["path"] ?: ""), MIME_JSON)
                    }
                uri == "/api/vnc/status" && method == Method.GET ->
                    textResponse(ZtAiDebugVncHelper.statusJson(), MIME_JSON)
                uri == "/api/vnc/start" && method == Method.POST ->
                    textResponse(ZtAiDebugVncHelper.startJson(), MIME_JSON)
                uri == "/api/editor/open" && method == Method.POST ->
                    handleEditorOpen(session)
                uri == "/api/root/status" && method == Method.GET ->
                    textResponse(ZtAiDebugRootHelper.statusJson(appContext), MIME_JSON)
                uri == "/api/root/exec" && method == Method.POST ->
                    guardRoot { handleRootExec(session) }
                uri == "/api/system/status" && method == Method.GET ->
                    textResponse(ZtAiDebugSystemHelper.statusJson(appContext), MIME_JSON)
                uri == "/api/system/dumpsys" && method == Method.GET ->
                    guardRoot {
                        textResponse(
                            ZtAiDebugSystemHelper.dumpsysJson(session.parms["service"] ?: "activity"),
                            MIME_JSON
                        )
                    }
                uri == "/api/system/packages" && method == Method.GET ->
                    guardRoot {
                        textResponse(ZtAiDebugSystemHelper.packagesJson(session.parms["filter"]), MIME_JSON)
                    }
                uri == "/api/system/processes" && method == Method.GET ->
                    guardRoot { textResponse(ZtAiDebugSystemHelper.processesJson(), MIME_JSON) }
                uri == "/api/system/getprop" && method == Method.GET ->
                    textResponse(ZtAiDebugSystemHelper.getpropJson(session.parms["key"]), MIME_JSON)
                uri == "/api/system/setprop" && method == Method.POST ->
                    guardRoot { handleSetprop(session) }
                uri == "/api/logs/dmesg" && method == Method.GET ->
                    guardRoot {
                        val lines = session.parms["lines"]?.toIntOrNull() ?: 200
                        textResponse(ZtAiDebugSystemHelper.dmesgJson(lines), MIME_JSON)
                    }
                uri == "/api/adb/status" && method == Method.GET ->
                    textResponse(ZtAiDebugRootHelper.statusJson(appContext), MIME_JSON)
                uri == "/api/adb/tcp/enable" && method == Method.POST ->
                    guardRoot { handleAdbTcpEnable(session) }
                uri == "/api/adb/tcp/disable" && method == Method.POST ->
                    guardRoot { handleAdbTcpDisable() }
                uri == "/api/input/tap" && method == Method.POST ->
                    guardRoot { handleInputTap(session) }
                uri == "/api/input/swipe" && method == Method.POST ->
                    guardRoot { handleInputSwipe(session) }
                uri == "/api/input/text" && method == Method.POST ->
                    guardRoot { handleInputText(session) }
                uri == "/api/input/keyevent" && method == Method.POST ->
                    guardRoot { handleInputKeyevent(session) }
                uri == "/api/ui/launch" && method == Method.POST ->
                    guardRoot { handleUiLaunch(session) }
                uri == "/api/ui/force-stop" && method == Method.POST ->
                    guardRoot { handleUiForceStop(session) }
                uri == "/api/files/list" && method == Method.GET ->
                    handleFilesList(session)
                uri == "/api/files/write" && method == Method.POST ->
                    handleFilesWrite(session)
                uri == "/api/llm/tools" && method == Method.GET ->
                    textResponse(ZtAiDebugLlmHelper.listToolsJson(), MIME_JSON)
                uri == "/api/llm/tool" && method == Method.POST ->
                    handleLlmTool(session)
                uri == "/api/config/get" && method == Method.POST ->
                    handleConfigGet(session)
                uri == "/api/config/set" && method == Method.POST ->
                    handleConfigSet(session)
                uri == "/api/config/zt" && method == Method.POST ->
                    handleConfigZt(session)
                uri == "/api/beautify/colors" && method == Method.GET ->
                    textResponse(ZtAiDebugLlmHelper.getBeautifyColorsJson(), MIME_JSON)
                uri == "/api/beautify/colors" && method == Method.POST ->
                    handleBeautifyColors(session)
                uri == "/api/beautify/clear" && method == Method.POST ->
                    handleBeautifyClear(session)
                uri == "/api/containers" && method == Method.GET ->
                    textResponse(ZtAiDebugLlmHelper.listContainersJson(), MIME_JSON)
                uri == "/api/containers/create" && method == Method.POST ->
                    handleContainerCreate(session)
                uri == "/api/containers/switch" && method == Method.POST ->
                    handleContainerSwitch(session)
                uri == "/api/containers/delete" && method == Method.POST ->
                    handleContainerDelete(session)
                uri == "/api/commands" && method == Method.GET ->
                    textResponse(ZtAiDebugLlmHelper.listCommandDefsJson(), MIME_JSON)
                uri == "/api/commands/add" && method == Method.POST ->
                    handleCommandDefAdd(session)
                uri == "/api/commands/update" && method == Method.POST ->
                    handleCommandDefUpdate(session)
                uri == "/api/commands/delete" && method == Method.POST ->
                    handleCommandDefDelete(session)
                uri == "/api/commands/run" && method == Method.POST ->
                    handleCommandDefRun(session)
                else -> cors(newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_JSON, jsonError("not found")))
            }
        } catch (e: Exception) {
            cors(newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_JSON, jsonError(e.message ?: "error")))
        }
    }

    private fun guardRoot(block: () -> Response): Response {
        if (!ZtAiDebugRootGate.isAllowed(appContext)) {
            return textResponse(ZtAiDebugRootGate.denyJson(appContext), MIME_JSON)
        }
        return block()
    }

    private fun guardFeature(feature: String, block: () -> Response): Response {
        if (!ZtAiDebugPermissionHelper.isFeatureGranted(appContext, feature)) {
            return textResponse(ZtAiDebugPermissionHelper.denyJson(appContext, feature), MIME_JSON)
        }
        return block()
    }

    private fun wrapOkJson(innerJson: String): String {
        val parsed = JsonParser.parseString(innerJson)
        return if (parsed.isJsonObject) {
            val obj = parsed.asJsonObject.deepCopy()
            obj.addProperty("ok", true)
            obj.toString()
        } else {
            gson.toJson(mapOf("ok" to true, "data" to parsed))
        }
    }

    private fun handleTerminalExec(session: IHTTPSession): Response {
        val body = readBody(session)
        val obj = JsonParser.parseString(body).asJsonObject
        if (!ZtAiDebugMatchCodeHelper.isValid(extractMatchCode(session, obj))) {
            return unauthorizedResponse()
        }
        val command = obj.get("command")?.asString?.trim().orEmpty()
        if (command.isEmpty()) {
            return textResponse(jsonError("command required"), MIME_JSON)
        }
        val waitMs = obj.get("waitMs")?.asLong ?: 2500L
        val snapshot = ZtAiDebugTerminalHelper.execAndSnapshot(command, waitMs)
        return textResponse(
            gson.toJson(
                mapOf(
                    "ok" to true,
                    "command" to command,
                    "waitMs" to waitMs,
                    "snapshot" to snapshot
                )
            ),
            MIME_JSON
        )
    }

    private fun terminalSnapshotJson(maxChars: Int): String {
        val text = ZtAiDebugTerminalHelper.snapshot(maxChars)
        return gson.toJson(
            mapOf(
                "ok" to true,
                "maxChars" to maxChars,
                "snapshot" to text
            )
        )
    }

    private fun handleScreenshot(session: IHTTPSession): Response {
        val source = session.parms["source"]?.trim()?.lowercase() ?: "auto"
        val bytes = ZtAiDebugScreenshotHelper.capturePng(appContext, source)
        if (bytes == null || bytes.isEmpty()) {
            return textResponse(
                gson.toJson(
                    mapOf(
                        "ok" to false,
                        "error" to "screenshot failed — keep ZeroTermux in foreground or use ?source=root with Root debug enabled",
                        "hint_for_ai" to "Try GET /api/screenshot?source=root when Root full debug is on. Otherwise bring ZeroTermux to foreground and retry ?source=app."
                    )
                ),
                MIME_JSON
            )
        }
        return cors(
            newFixedLengthResponse(
                Response.Status.OK,
                "image/png",
                ByteArrayInputStream(bytes),
                bytes.size.toLong()
            )
        )
    }

    private fun handleCameraFrame(facing: String): Response {
        if (!ZtAiDebugPermissionHelper.isFeatureGranted(appContext, "camera")) {
            return textResponse(ZtAiDebugPermissionHelper.denyJson(appContext, "camera"), MIME_JSON)
        }
        val helper = ZtAiDebugManager.cameraHelper
        val bytes = helper?.getFrame(facing)
        if (bytes == null || bytes.isEmpty()) {
            return textResponse(
                gson.toJson(
                    mapOf(
                        "ok" to false,
                        "error" to "camera frame unavailable",
                        "hint_for_ai" to "Ensure CAMERA permission granted (GET /api/permissions). Retry GET /api/camera/frame?facing=back"
                    )
                ),
                MIME_JSON
            )
        }
        return cors(
            newFixedLengthResponse(
                Response.Status.OK,
                "image/jpeg",
                ByteArrayInputStream(bytes),
                bytes.size.toLong()
            )
        )
    }

    private fun discoveryResponse(context: Context, authorized: Boolean, session: IHTTPSession): Response {
        if (wantsHtml(session)) {
            val lang = session.parms["lang"]?.trim()?.lowercase()?.takeIf { it in ZtAiDebugApiDocs.LANGUAGES }
            val html = ZtAiDebugInfoHelper.discoveryHtml(context, authorized, lang)
            return cors(newFixedLengthResponse(Response.Status.OK, MIME_HTML, html))
        }
        val json = if (authorized) {
            ZtAiDebugInfoHelper.discovery(context)
        } else {
            ZtAiDebugInfoHelper.publicLockedDiscovery(context)
        }
        return textResponse(json, MIME_JSON)
    }

    private fun wantsHtml(session: IHTTPSession): Boolean {
        session.parms["format"]?.trim()?.equals("html", ignoreCase = true)?.let { if (it) return true }
        val accept = session.headers["accept"]?.lowercase().orEmpty()
        if (accept.contains("text/html") && !accept.contains("application/json")) return true
        val ua = session.headers["user-agent"]?.lowercase().orEmpty()
        return ua.contains("mozilla") && !accept.contains("application/json")
    }

    private fun parseBodyObject(session: IHTTPSession): com.google.gson.JsonObject? {
        val body = readBody(session)
        if (body.isBlank()) return null
        return try {
            JsonParser.parseString(body).asJsonObject
        } catch (_: Exception) {
            null
        }
    }

    private fun handleRootExec(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        if (!ZtAiDebugMatchCodeHelper.isValid(extractMatchCode(session, obj))) {
            return unauthorizedResponse()
        }
        val command = obj.get("command")?.asString?.trim().orEmpty()
        if (command.isEmpty()) return textResponse(jsonError("command required"), MIME_JSON)
        val timeoutMs = obj.get("timeoutMs")?.asLong ?: 5000L
        val asRoot = obj.get("asRoot")?.asBoolean ?: true
        val result = ZtAiDebugRootHelper.exec(command, timeoutMs, asRoot)
        return textResponse(ZtAiDebugRootHelper.execResultJson(result, command, timeoutMs), MIME_JSON)
    }

    private fun handleSetprop(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val key = obj.get("key")?.asString?.trim().orEmpty()
        val value = obj.get("value")?.asString.orEmpty()
        if (key.isEmpty()) return textResponse(jsonError("key required"), MIME_JSON)
        return textResponse(ZtAiDebugSystemHelper.setpropJson(key, value), MIME_JSON)
    }

    private fun handleAdbTcpEnable(session: IHTTPSession): Response {
        val obj = parseBodyObject(session)
        val port = obj?.get("port")?.asInt ?: session.parms["port"]?.toIntOrNull() ?: 5555
        val result = ZtAiDebugRootHelper.enableAdbTcp(port)
        return textResponse(
            gson.toJson(
                mapOf(
                    "ok" to result.ok,
                    "port" to port,
                    "adb_tcp_port" to ZtAiDebugRootHelper.readAdbTcpPort(),
                    "stdout" to result.stdout,
                    "hint_for_ai" to "PC can now: adb connect <phone-ip>:$port"
                )
            ),
            MIME_JSON
        )
    }

    private fun handleAdbTcpDisable(): Response {
        val result = ZtAiDebugRootHelper.disableAdbTcp()
        return textResponse(
            gson.toJson(
                mapOf(
                    "ok" to result.ok,
                    "adb_tcp_port" to ZtAiDebugRootHelper.readAdbTcpPort()
                )
            ),
            MIME_JSON
        )
    }

    private fun handleInputTap(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val x = obj.get("x")?.asInt ?: return textResponse(jsonError("x required"), MIME_JSON)
        val y = obj.get("y")?.asInt ?: return textResponse(jsonError("y required"), MIME_JSON)
        return textResponse(ZtAiDebugInputHelper.tapJson(x, y), MIME_JSON)
    }

    private fun handleInputSwipe(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val x1 = obj.get("x1")?.asInt ?: return textResponse(jsonError("x1 required"), MIME_JSON)
        val y1 = obj.get("y1")?.asInt ?: return textResponse(jsonError("y1 required"), MIME_JSON)
        val x2 = obj.get("x2")?.asInt ?: return textResponse(jsonError("x2 required"), MIME_JSON)
        val y2 = obj.get("y2")?.asInt ?: return textResponse(jsonError("y2 required"), MIME_JSON)
        val duration = obj.get("durationMs")?.asInt ?: 300
        return textResponse(ZtAiDebugInputHelper.swipeJson(x1, y1, x2, y2, duration), MIME_JSON)
    }

    private fun handleInputText(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val text = obj.get("text")?.asString ?: return textResponse(jsonError("text required"), MIME_JSON)
        return textResponse(ZtAiDebugInputHelper.textJson(text), MIME_JSON)
    }

    private fun handleInputKeyevent(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val code = obj.get("code")?.asInt ?: return textResponse(jsonError("code required"), MIME_JSON)
        return textResponse(ZtAiDebugInputHelper.keyeventJson(code), MIME_JSON)
    }

    private fun handleUiLaunch(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val pkg = obj.get("package")?.asString?.trim().orEmpty()
        if (pkg.isEmpty()) return textResponse(jsonError("package required"), MIME_JSON)
        val activity = obj.get("activity")?.asString
        return textResponse(ZtAiDebugInputHelper.launchJson(pkg, activity), MIME_JSON)
    }

    private fun handleUiForceStop(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val pkg = obj.get("package")?.asString?.trim().orEmpty()
        if (pkg.isEmpty()) return textResponse(jsonError("package required"), MIME_JSON)
        return textResponse(ZtAiDebugInputHelper.forceStopJson(pkg), MIME_JSON)
    }

    private fun handleFilesList(session: IHTTPSession): Response {
        val path = session.parms["path"] ?: ""
        val useRoot = session.parms["root"]?.equals("true", ignoreCase = true) == true
        if (useRoot) {
            return guardRoot {
                textResponse(ZtAiDebugFileWriteHelper.listJson(path, true), MIME_JSON)
            }
        }
        return guardFeature("storage") {
            textResponse(ZtAiDebugFileWriteHelper.listJson(path, false), MIME_JSON)
        }
    }

    private fun handleFilesWrite(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val path = obj.get("path")?.asString?.trim().orEmpty()
        val content = obj.get("content")?.asString ?: return textResponse(jsonError("content required"), MIME_JSON)
        val useRoot = obj.get("root")?.asBoolean == true
        if (useRoot) {
            return guardRoot {
                textResponse(ZtAiDebugFileWriteHelper.writeJson(path, content, "utf-8", true), MIME_JSON)
            }
        }
        return guardFeature("storage") {
            textResponse(ZtAiDebugFileWriteHelper.writeJson(path, content, "utf-8", false), MIME_JSON)
        }
    }

    private fun handleLlmTool(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val tool = obj.get("tool")?.asString?.trim().orEmpty()
        if (tool.isEmpty()) return textResponse(jsonError("tool required"), MIME_JSON)
        val argsEl = obj.get("arguments")
        val args = if (argsEl != null && argsEl.isJsonObject) {
            org.json.JSONObject(argsEl.asJsonObject.toString())
        } else {
            org.json.JSONObject()
        }
        return textResponse(ZtAiDebugLlmHelper.executeTool(tool, args), MIME_JSON)
    }

    private fun handleConfigGet(session: IHTTPSession): Response {
        val obj = parseBodyObject(session)
        val group = obj?.get("group")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val keys = obj?.getAsJsonArray("keys")?.let { arr ->
            org.json.JSONArray().apply {
                for (element in arr) {
                    if (element.isJsonPrimitive) put(element.asString)
                }
            }
        }
        return textResponse(ZtAiDebugLlmHelper.getConfig(group, keys), MIME_JSON)
    }

    private fun handleConfigSet(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val key = obj.get("key")?.asString?.trim().orEmpty()
        if (key.isEmpty()) return textResponse(jsonError("key required"), MIME_JSON)
        val value = obj.get("value")?.asString.orEmpty()
        return textResponse(ZtAiDebugLlmHelper.setConfig(key, value), MIME_JSON)
    }

    private fun handleConfigZt(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        val command = obj.get("command")?.asString?.trim().orEmpty()
        if (command.isEmpty()) return textResponse(jsonError("command required"), MIME_JSON)
        return textResponse(ZtAiDebugLlmHelper.runZt(command), MIME_JSON)
    }

    private fun handleBeautifyColors(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(
            ZtAiDebugLlmHelper.setBeautifyColors(org.json.JSONObject(obj.toString())),
            MIME_JSON
        )
    }

    private fun handleBeautifyClear(session: IHTTPSession): Response {
        parseBodyObject(session)
        return textResponse(ZtAiDebugLlmHelper.clearBeautifyJson(), MIME_JSON)
    }

    private fun handleContainerCreate(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(
            ZtAiDebugLlmHelper.createContainerJson(org.json.JSONObject(obj.toString())),
            MIME_JSON
        )
    }

    private fun handleContainerSwitch(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(
            ZtAiDebugLlmHelper.switchContainerJson(org.json.JSONObject(obj.toString())),
            MIME_JSON
        )
    }

    private fun handleContainerDelete(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(
            ZtAiDebugLlmHelper.deleteContainerJson(org.json.JSONObject(obj.toString())),
            MIME_JSON
        )
    }

    private fun handleCommandDefAdd(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(ZtAiDebugLlmHelper.addCommandDefJson(org.json.JSONObject(obj.toString())), MIME_JSON)
    }

    private fun handleCommandDefUpdate(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(ZtAiDebugLlmHelper.updateCommandDefJson(org.json.JSONObject(obj.toString())), MIME_JSON)
    }

    private fun handleCommandDefDelete(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(ZtAiDebugLlmHelper.deleteCommandDefJson(org.json.JSONObject(obj.toString())), MIME_JSON)
    }

    private fun handleCommandDefRun(session: IHTTPSession): Response {
        val obj = parseBodyObject(session) ?: return textResponse(jsonError("JSON body required"), MIME_JSON)
        return textResponse(ZtAiDebugLlmHelper.runCommandDefJson(org.json.JSONObject(obj.toString())), MIME_JSON)
    }

    private fun handleEditorOpen(session: IHTTPSession): Response {
        val body = readBody(session)
        val path = if (body.isNotBlank()) {
            try {
                JsonParser.parseString(body).asJsonObject.let { obj ->
                    obj.get("path")?.asString
                }
            } catch (_: Exception) {
                null
            }
        } else {
            session.parms["path"]
        }
        val openX11 = parseBool(session, body, "open_x11_tab", "openX11")
        val autoRun = parseBool(session, body, "auto_run", "autoRun")
        return textResponse(
            ZtAiDebugVncHelper.openEditorJson(appContext, path ?: "", openX11, autoRun),
            MIME_JSON
        )
    }

    private fun parseBool(session: IHTTPSession, body: String, vararg keys: String): Boolean {
        keys.forEach { key ->
            session.parms[key]?.trim()?.let {
                if (it.equals("true", ignoreCase = true) || it == "1") return true
            }
        }
        if (body.isNotBlank()) {
            try {
                val obj = JsonParser.parseString(body).asJsonObject
                keys.forEach { key ->
                    if (obj.has(key) && obj.get(key).asBoolean) return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"] ?: ""
    }

    private fun extractMatchCode(
        session: IHTTPSession,
        body: com.google.gson.JsonObject? = null
    ): String? {
        session.parms[ZtAiDebugMatchCodeHelper.QUERY_PARAM]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        body?.get(ZtAiDebugMatchCodeHelper.QUERY_PARAM)?.asString?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val headerKey = ZtAiDebugMatchCodeHelper.HEADER_NAME.lowercase()
        session.headers[headerKey]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        session.headers[ZtAiDebugMatchCodeHelper.HEADER_NAME]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return null
    }

    private fun unauthorizedResponse(): Response {
        return textResponse(
            gson.toJson(
                mapOf(
                    "ok" to false,
                    "error" to "invalid or missing match_code",
                    "auth" to mapOf(
                        "param" to ZtAiDebugMatchCodeHelper.QUERY_PARAM,
                        "header" to ZtAiDebugMatchCodeHelper.HEADER_NAME,
                        "format" to "7-digit number"
                    ),
                    "hint_for_ai" to "Read GET / or GET /?code=... for full multilingual docs (docs.i18n.zh / docs.i18n.en). Ask user for 7-digit match code from ZeroTermux Settings → External AI debug → eye icon.",
                    "hint_for_user_zh" to "请在 ZeroTermux 设置 → 启用外部AI调用ZeroTermux → 点击匹配码右侧眼睛图标查看 7 位匹配码，并提供给 AI。",
                    "hint_for_user_en" to "ZeroTermux Settings → External AI debug → tap eye icon → provide the 7-digit code to your AI.",
                    "docs_url" to "GET /?format=html for human docs; GET / returns JSON with docs.i18n"
                )
            ),
            MIME_JSON
        )
    }

    private fun jsonError(message: String): String {
        return gson.toJson(mapOf("ok" to false, "error" to message))
    }

    private fun textResponse(body: String, mime: String): Response {
        return cors(newFixedLengthResponse(Response.Status.OK, mime, body))
    }

    private fun cors(response: Response): Response {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Zt-Ai-Debug-Code")
        return response
    }

    companion object {
        private const val MIME_JSON = "application/json; charset=utf-8"
        private const val MIME_HTML = "text/html; charset=utf-8"
    }
}
