package com.termux.zerocore.ai.config

import com.example.xh_lib.utils.UUtils
import com.termux.zerocore.config.mainmenu.MainMenuPackageInfo
import com.termux.zerocore.config.mainmenu.MainMenuPackageManager
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * AI 仅可操作 Menu 菜单区「可配置安装包」（独立目录，类似 zip 安装），
 * 不可修改程序菜单、默认菜单。
 */
object ZtAiLeftMenuHelper {

    private const val MAX_XML_CHARS = 120_000
    private const val MENU_XML_FILE = "zt_menu_config.xml"
    private const val EMPTY_MENU = """<?xml version="1.0" encoding="utf-8"?>
<zt-menu>
</zt-menu>"""

    fun aiMenuCreateLabel(): String = ZtAiStrings.aiMenuCreateLabel()

    fun getMenuInfo(): String {
        val context = UUtils.getContext()
        val packages = MainMenuPackageManager.buildListItems(context)
        val aiPackages = packages.filter { MainMenuPackageManager.isAiPackageId(it.id) }
        val latestAiId = MainMenuPackageManager.getLatestAiPackageId(context)
        val activeId = MainMenuPackageManager.getActivePackageId(context)

        val root = JSONObject()
            .put("ok", true)
            .put("hint", ZtAiStrings.leftMenuHint())
            .put("xml_rules", ZtAiLeftMenuXmlGuide.buildRulesJson(context))
            .put("active_menu_package_id", activeId)
            .put("latest_ai_menu_package_id", latestAiId ?: JSONObject.NULL)
            .put("system_menus", JSONArray().apply {
                packages.filter { !it.isConfigurable() && it.type != MainMenuPackageInfo.TYPE_INSTALL }
                    .forEach { info ->
                        put(
                            JSONObject()
                                .put("id", info.id)
                                .put("label", info.label)
                                .put("type", info.type)
                                .put("is_active", info.isActive)
                                .put("readonly", true)
                        )
                    }
            })
            .put("ai_menu_packages", JSONArray().apply {
                aiPackages.forEach { info ->
                    put(
                        JSONObject()
                            .put("id", info.id)
                            .put("label", info.label)
                            .put("is_active", info.isActive)
                            .put("path", info.packageDir?.absolutePath ?: JSONObject.NULL)
                    )
                }
            })
            .put("configurable_menu_packages", JSONArray().apply {
                packages.filter { it.isConfigurable() }.forEach { info ->
                    put(
                        JSONObject()
                            .put("id", info.id)
                            .put("label", info.label)
                            .put("is_ai", MainMenuPackageManager.isAiPackageId(info.id))
                            .put("is_active", info.isActive)
                    )
                }
            })

        val readId = latestAiId
        if (readId != null) {
            val aiXml = File(MainMenuPackageManager.getAiPackageDir(context, readId), MENU_XML_FILE)
            if (aiXml.exists()) {
                val content = aiXml.readText(Charsets.UTF_8)
                root.put("menu_package_id", readId)
                root.put("menu_package_label", MainMenuPackageManager.resolveAiPackageLabel(readId))
                root.put("groups", JSONArray().apply {
                    parseGroups(content).forEach { g ->
                        put(
                            JSONObject()
                                .put("name", g.name)
                                .put("id", g.id ?: JSONObject.NULL)
                                .put("item_count", g.itemCount)
                        )
                    }
                })
                if (content.length <= MAX_XML_CHARS) {
                    root.put("xml_content", content)
                } else {
                    root.put("xml_content_truncated", true)
                    root.put("xml_content", content.take(MAX_XML_CHARS))
                }
            }
        } else {
            root.put("note", ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_no_package_note))
        }
        return root.toString(2)
    }

    fun updateMenu(args: JSONObject): String {
        val context = UUtils.getContext()
        val switchRaw = args.optString("switch_to_menu", args.optString("switch_to_tab", "false")).trim()
        val createPackage = args.optString("create_menu_package", args.optString("create_tab_name", "")).trim()
        val packageIdArg = args.optString("menu_package_id", "").trim()
        val fullXml = args.optString("xml_content", "").trim()
        val appendGroup = args.optString("append_group_xml", "").trim()
        val shouldSwitch = !switchRaw.equals("false", ignoreCase = true) && switchRaw != "0"

        if (targetsSystemMenu(createPackage)) {
            return errorJson(ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_err_system_menu))
        }

        var targetPackageId: String? = null
        var packageCreated = false

        when {
            createPackage.isNotEmpty() -> {
                if (!isAiCreateRequest(createPackage)) {
                    return errorJson(ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_err_create_name))
                }
                var xmlToWrite: String? = null
                if (fullXml.isNotEmpty()) {
                    val normalized = normalizeRootXml(fullXml)
                    validateMenuContent(context, normalized)?.let {
                        return errorJson("invalid xml_content: $it")
                    }
                    xmlToWrite = normalized
                } else if (appendGroup.isNotEmpty()) {
                    validateGroupSnippet(appendGroup)?.let { return errorJson("invalid append_group_xml: $it") }
                    val merged = appendBeforeClosing(EMPTY_MENU, appendGroup)
                    validateMenuContent(context, merged)?.let { return errorJson("invalid merged xml: $it") }
                    xmlToWrite = merged
                }
                targetPackageId = MainMenuPackageManager.createAiMenuPackage(context, xmlToWrite)
                    ?: return errorJson("failed to create AI menu package")
                packageCreated = true
            }
            fullXml.isNotEmpty() -> {
                val normalized = normalizeRootXml(fullXml)
                validateMenuContent(context, normalized)?.let { return errorJson("invalid xml_content: $it") }
                targetPackageId = resolveUpdateTargetPackageId(context, packageIdArg)
                    ?: return errorJson(ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_err_no_update_target))
                if (!MainMenuPackageManager.writeAiMenuPackageXml(
                        context,
                        targetPackageId,
                        normalized,
                        applyIfActive = true
                    )) {
                    return errorJson("failed to update AI menu package xml")
                }
            }
            appendGroup.isNotEmpty() -> {
                validateGroupSnippet(appendGroup)?.let { return errorJson("invalid append_group_xml: $it") }
                targetPackageId = resolveUpdateTargetPackageId(context, packageIdArg)
                    ?: return errorJson(ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_err_no_update_target))
                val merged = appendBeforeClosing(readPackageXml(context, targetPackageId), appendGroup)
                validateMenuContent(context, merged)?.let { return errorJson("invalid merged xml: $it") }
                if (!MainMenuPackageManager.writeAiMenuPackageXml(context, targetPackageId, merged, applyIfActive = true)) {
                    return errorJson("failed to append group to AI menu package")
                }
            }
            else -> return errorJson(ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_err_need_content))
        }

        var switched = false
        if (shouldSwitch && targetPackageId != null) {
            switched = MainMenuPackageManager.applyAiMenuPackage(context, targetPackageId)
        }

        ZtAiConfigSideEffects.refreshMenuPackagesAfterAiUpdate(
            switched = switched,
            targetPackageId = targetPackageId
        )

        return buildSuccessJson(context, targetPackageId, packageCreated, switched)
    }

    private fun isAiCreateRequest(name: String): Boolean {
        val label = aiMenuCreateLabel()
        return name.equals(label, ignoreCase = true)
            || name.equals("AI创建", ignoreCase = true)
            || name.equals("AI Created", ignoreCase = true)
            || name.equals(MainMenuPackageInfo.ID_AI_CREATED, ignoreCase = true)
    }

    private fun resolveUpdateTargetPackageId(context: android.content.Context, packageIdArg: String): String? {
        if (packageIdArg.isNotEmpty()) {
            if (!MainMenuPackageManager.isAiPackageId(packageIdArg)) {
                return null
            }
            val dir = MainMenuPackageManager.getAiPackageDir(context, packageIdArg)
            return if (File(dir, MENU_XML_FILE).exists()) packageIdArg else null
        }
        return MainMenuPackageManager.getLatestAiPackageId(context)
    }

    private fun readPackageXml(context: android.content.Context, packageId: String): String {
        val file = File(MainMenuPackageManager.getAiPackageDir(context, packageId), MENU_XML_FILE)
        return if (file.exists()) file.readText(Charsets.UTF_8) else EMPTY_MENU
    }

    private fun targetsSystemMenu(name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }
        val lower = name.lowercase()
        return lower == MainMenuPackageInfo.ID_PROGRAM.lowercase()
            || lower == MainMenuPackageInfo.ID_DEFAULT_XML.lowercase()
            || lower == MainMenuPackageInfo.ID_NETWORK.lowercase()
            || name == UUtils.getString(com.termux.R.string.menu_package_program_label)
            || name == UUtils.getString(com.termux.R.string.menu_package_default_label)
    }

    private fun buildSuccessJson(
        context: android.content.Context,
        packageId: String?,
        created: Boolean,
        switched: Boolean
    ): String {
        val id = packageId ?: MainMenuPackageManager.getLatestAiPackageId(context).orEmpty()
        val content = if (id.isNotEmpty()) readPackageXml(context, id) else ""
        return JSONObject()
            .put("ok", true)
            .put("menu_package_id", id)
            .put("menu_package_label", if (id.isNotEmpty()) {
                MainMenuPackageManager.resolveAiPackageLabel(id)
            } else {
                aiMenuCreateLabel()
            })
            .put("package_created", created)
            .put("switched_to_menu_package", switched)
            .put("active_menu_package_id", MainMenuPackageManager.getActivePackageId(context))
            .put("package_path", if (id.isNotEmpty()) {
                MainMenuPackageManager.getAiPackageDir(context, id).absolutePath
            } else {
                JSONObject.NULL
            })
            .put("groups", JSONArray().apply {
                parseGroups(content).forEach { g ->
                    put(JSONObject().put("name", g.name).put("id", g.id ?: JSONObject.NULL))
                }
            })
            .put("note", ZtAiStrings.str(com.termux.R.string.zt_ai_left_menu_success_note))
            .toString(2)
    }

    private data class GroupInfo(val name: String, val id: Int?, val itemCount: Int)

    private fun parseGroups(xml: String): List<GroupInfo> {
        return try {
            val doc = parseDocument(xml) ?: return emptyList()
            val nodes = doc.getElementsByTagName("group")
            buildList {
                for (i in 0 until nodes.length) {
                    val el = nodes.item(i) as? Element ?: continue
                    val name = el.getAttribute("name").trim()
                    if (name.isEmpty()) continue
                    add(GroupInfo(name, el.getAttribute("id").trim().toIntOrNull(), el.getElementsByTagName("item").length))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun validateMenuContent(context: android.content.Context, content: String): String? {
        validateXml(content)?.let { return it }
        val errors = ZtAiLeftMenuXmlGuide.validateMenuXml(context, content)
        if (errors.isNotEmpty()) {
            return errors.joinToString("; ")
        }
        return null
    }

    private fun normalizeRootXml(content: String): String {
        var xml = content.trim()
        if (!xml.contains("<zt-menu", ignoreCase = true)) {
            throw IllegalArgumentException("xml_content must contain <zt-menu>")
        }
        if (!xml.contains("</zt-menu>", ignoreCase = true)) {
            xml += "\n</zt-menu>"
        }
        return xml
    }

    private fun appendBeforeClosing(xml: String, snippet: String): String {
        val base = xml.ifBlank { EMPTY_MENU }
        val tag = "</zt-menu>"
        val idx = base.lastIndexOf(tag, ignoreCase = true)
        if (idx < 0) throw IllegalArgumentException("missing </zt-menu>")
        return base.substring(0, idx) + "\n" + snippet.trim() + "\n" + base.substring(idx)
    }

    private fun validateXml(content: String): String? {
        return try {
            parseDocument(content)
            null
        } catch (e: Exception) {
            e.message ?: "parse failed"
        }
    }

    private fun validateGroupSnippet(snippet: String): String? {
        return validateXml("<?xml version=\"1.0\" encoding=\"utf-8\"?><zt-menu>$snippet</zt-menu>")
    }

    private fun parseDocument(content: String): org.w3c.dom.Document? {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
    }

    private fun errorJson(message: String): String {
        return JSONObject().put("ok", false).put("error", message).toString(2)
    }
}
