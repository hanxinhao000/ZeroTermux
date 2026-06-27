package com.termux.zerocore.ai.config

import android.content.Context
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.config.mainmenu.XMLMainMenuConfig
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * ZeroTermux 左侧菜单 XML 规范：供 AI 读取与学习，写入前校验。
 * 规则与 assets/mainmenu/{cn|en}/zt_menu_config.xml 一致。
 */
object ZtAiLeftMenuXmlGuide {

    private val CLICK_PREFIXES = listOf(
        "java:",
        "ztShell:",
        "jumpUrl:",
        "ztEditText:",
        "startActivity:",
        "actionActivity:",
        "commands:",
        "shellUrl:",
        "downloadUrl:",
        "appWebUrl:",
    )

    private val INVALID_BARE_CLICKS = setOf(
        "pkg", "mc", "exit", "help", "clear",
    )

    fun buildRulesJson(context: Context): JSONObject {
        return JSONObject()
            .put("summary", ZtAiStrings.leftMenuRulesSummary())
            .put("root", "<zt-menu>…</zt-menu>")
            .put("group", JSONObject()
                .put("required_attrs", jsonArrayOf("name"))
                .put("optional_attrs", jsonArrayOf("id")))
            .put("item", JSONObject()
                .put("required_attrs", jsonArrayOf("click"))
                .put("optional_attrs", jsonArrayOf(
                    "tag", "name", "icon", "autoRunShell", "dialogConfirm", "dialogTitle",
                    "dialogMessage", "listTitle", "activityTitle", "packageName", "intentData"
                ))
                .put("notes", jsonArrayFromRes(R.array.zt_ai_left_menu_item_notes))
            )
            .put("click_types", buildClickTypes())
            .put("forbidden", jsonArrayFromRes(R.array.zt_ai_left_menu_forbidden))
            .put("workflow", jsonArrayFromRes(R.array.zt_ai_left_menu_workflow))
            .put("minimal_example", ZtAiStrings.str(R.string.zt_ai_left_menu_minimal_example).trim())
            .put("builtin_menu_catalog", loadBuiltinCatalog(context))
    }

    fun validateMenuXml(context: Context, content: String): List<String> {
        val errors = ArrayList<String>()
        val doc = try {
            parseDocument(content)
        } catch (e: Exception) {
            errors.add("XML parse failed: ${e.message}")
            return errors
        } ?: run {
            errors.add("XML parse failed")
            return errors
        }

        val groups = doc.getElementsByTagName("group")
        if (groups.length == 0) {
            errors.add(ZtAiStrings.str(R.string.zt_ai_left_menu_err_need_group))
        }

        val knownJava = loadKnownJavaClasses(context)
        var itemCount = 0
        val prefixHint = CLICK_PREFIXES.joinToString("/")

        for (i in 0 until groups.length) {
            val group = groups.item(i) as? Element ?: continue
            val groupName = group.getAttribute("name").trim()
            if (groupName.isEmpty()) {
                errors.add(ZtAiStrings.str(R.string.zt_ai_left_menu_err_group_name).format(i))
            }
            val items = group.getElementsByTagName("item")
            for (j in 0 until items.length) {
                itemCount++
                val item = items.item(j) as? Element ?: continue
                val click = item.getAttribute("click").trim()
                val name = item.getAttribute("name").trim()
                val icon = item.getAttribute("icon").trim()
                val dialogConfirm = item.getAttribute("dialogConfirm").trim()
                val label = name.ifEmpty { item.getAttribute("tag") }

                if (!isValidClick(click)) {
                    errors.add(
                        ZtAiStrings.str(R.string.zt_ai_left_menu_err_invalid_click)
                            .format(label, click, prefixHint)
                    )
                }
                if (click.startsWith("java:")) {
                    val clazz = click.removePrefix("java:").trim()
                    if (clazz.isEmpty()) {
                        errors.add(ZtAiStrings.str(R.string.zt_ai_left_menu_err_empty_java))
                    } else if (knownJava.isNotEmpty() && !knownJava.contains(clazz)) {
                        errors.add(
                            ZtAiStrings.str(R.string.zt_ai_left_menu_err_unknown_java)
                                .format(name.ifEmpty { clazz }, clazz)
                        )
                    }
                    if (icon.isNotEmpty() && !icon.startsWith("imgPath:")) {
                        errors.add(
                            ZtAiStrings.str(R.string.zt_ai_left_menu_err_java_icon)
                                .format(name.ifEmpty { clazz })
                        )
                    }
                }
                if (click.startsWith("ztShell:") && click.contains("&&") && !click.contains("&amp;&amp;")) {
                    errors.add(
                        ZtAiStrings.str(R.string.zt_ai_left_menu_err_shell_amp)
                            .format(name.ifEmpty { "shell" })
                    )
                }
                if (dialogConfirm.isNotEmpty() && !click.startsWith("ztShell:")) {
                    errors.add(
                        ZtAiStrings.str(R.string.zt_ai_left_menu_err_dialog_confirm)
                            .format(name.ifEmpty { click })
                    )
                }
            }
        }

        if (itemCount == 0) {
            errors.add(ZtAiStrings.str(R.string.zt_ai_left_menu_err_need_item))
        }
        if (content.contains("<item") && content.contains(" click=\"pkg\"")) {
            errors.add(ZtAiStrings.str(R.string.zt_ai_left_menu_err_bare_pkg))
        }
        return errors
    }

    private fun isValidClick(click: String): Boolean {
        if (click.isEmpty()) {
            return true
        }
        if (INVALID_BARE_CLICKS.contains(click.lowercase())) {
            return false
        }
        if (click.startsWith("zt ") || click.startsWith("zt\t")) {
            return false
        }
        return CLICK_PREFIXES.any { click.startsWith(it) }
    }

    private fun loadKnownJavaClasses(context: Context): Set<String> {
        val classes = HashSet<String>()
        val catalog = loadBuiltinCatalog(context)
        for (i in 0 until catalog.length()) {
            val group = catalog.optJSONObject(i) ?: continue
            val items = group.optJSONArray("items") ?: continue
            for (j in 0 until items.length()) {
                val click = items.optJSONObject(j)?.optString("click", "") ?: ""
                if (click.startsWith("java:")) {
                    classes.add(click.removePrefix("java:").trim())
                }
            }
        }
        return classes
    }

    private fun loadBuiltinCatalog(context: Context): JSONArray {
        return try {
            val temp = File(context.cacheDir, "zt_ai_menu_catalog.xml")
            UUtils.writerFile(ZtAiStrings.menuAssetPath(), temp)
            val groups = XMLMainMenuConfig.parseXMLFile(temp.absolutePath)
            val exampleKeyword = ZtAiStrings.leftMenuExampleKeyword()
            JSONArray().apply {
                groups.forEach { group ->
                    if (group.groupName.contains(exampleKeyword)) {
                        return@forEach
                    }
                    put(
                        JSONObject()
                            .put("name", group.groupName)
                            .put("id", group.id)
                            .put("items", JSONArray().apply {
                                group.items.forEach { item ->
                                    put(
                                        JSONObject()
                                            .put("name", item.name ?: "")
                                            .put("click", item.clickAction ?: "")
                                    )
                                }
                            })
                    )
                }
            }
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun buildClickTypes(): JSONArray {
        return JSONArray()
            .put(clickType(
                "java:",
                ZtAiStrings.str(R.string.zt_ai_click_java_desc),
                """click="java:com.termux.zerocore.config.mainmenu.config.ZTSettingsClickConfig"""",
                ZtAiStrings.str(R.string.zt_ai_click_java_note)
            ))
            .put(clickType(
                "ztShell:",
                ZtAiStrings.str(R.string.zt_ai_click_ztshell_desc),
                """click="ztShell:cd ~ &amp;&amp; pkg update" autoRunShell="false" dialogConfirm="true" dialogTitle="Tips" dialogMessage="Run this shell?"""",
                ZtAiStrings.str(R.string.zt_ai_click_ztshell_note)
            ))
            .put(clickType(
                "jumpUrl:",
                ZtAiStrings.str(R.string.zt_ai_click_jumpurl_desc),
                """click="jumpUrl:https://www.example.com"""",
                ZtAiStrings.str(R.string.zt_ai_click_jumpurl_note)
            ))
            .put(clickType(
                "ztEditText:",
                ZtAiStrings.str(R.string.zt_ai_click_ztedittext_desc),
                """click="ztEditText:/data/data/com.termux/files/home/ZtInfo/main_menu_path.xml"""",
                ZtAiStrings.str(R.string.zt_ai_click_ztedittext_note)
            ))
            .put(clickType(
                "startActivity:",
                ZtAiStrings.str(R.string.zt_ai_click_startactivity_desc),
                """click="startActivity:com.termux.zerocore.settings.ZtSettingsActivity" packageName="com.termux"""",
                ZtAiStrings.str(R.string.zt_ai_click_startactivity_note)
            ))
            .put(clickType(
                "actionActivity:",
                ZtAiStrings.str(R.string.zt_ai_click_actionactivity_desc),
                """click="actionActivity:android.settings.WIRELESS_SETTINGS" packageName="com.android.settings" intentData="data@@com.termux"""",
                ZtAiStrings.str(R.string.zt_ai_click_actionactivity_note)
            ))
            .put(clickType(
                "commands:",
                ZtAiStrings.str(R.string.zt_ai_click_commands_desc),
                """click="commands:One@@pkg update,Multi@@pkg update &amp;&amp; pkg install vim" listTitle="Pick" autoRunShell="true"""",
                ZtAiStrings.str(R.string.zt_ai_click_commands_note)
            ))
            .put(clickType("shellUrl:", ZtAiStrings.str(R.string.zt_ai_click_shellurl_desc), """click="shellUrl:https://example.com/main.json"""", null))
            .put(clickType("downloadUrl:", ZtAiStrings.str(R.string.zt_ai_click_downloadurl_desc), """click="downloadUrl:https://example.com/main.json"""", null))
            .put(clickType("appWebUrl:", ZtAiStrings.str(R.string.zt_ai_click_appweburl_desc), """click="appWebUrl:https://www.example.com" activityTitle="Title"""", null))
            .put(clickType("(empty)", ZtAiStrings.str(R.string.zt_ai_click_empty_desc), """click=""""", ZtAiStrings.str(R.string.zt_ai_click_empty_note)))
    }

    private fun clickType(prefix: String, desc: String, example: String, note: String?): JSONObject {
        val obj = JSONObject()
            .put("prefix", prefix)
            .put("description", desc)
            .put("example", example)
        if (!note.isNullOrBlank()) {
            obj.put("note", note)
        }
        return obj
    }

    private fun jsonArrayOf(vararg items: String): JSONArray {
        return JSONArray().apply { items.forEach { put(it) } }
    }

    private fun jsonArrayFromRes(arrayRes: Int): JSONArray {
        return JSONArray().apply { ZtAiStrings.strArray(arrayRes).forEach { put(it) } }
    }

    private fun parseDocument(content: String): org.w3c.dom.Document? {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
    }
}
