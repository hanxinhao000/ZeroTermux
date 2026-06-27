package com.termux.zerocore.ai.config

import org.json.JSONArray
import org.json.JSONObject

object ZtAiConfigTools {

    fun addConfigTools(tools: JSONArray) {
        tools.put(tool(
            "list_zerotermux_capabilities",
            ZtAiStrings.toolListCapabilities(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "category",
                    JSONObject()
                        .put("type", "string")
                        .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_category))
                )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "get_zerotermux_config",
            ZtAiStrings.toolGetConfig(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "group",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_group))
                    )
                    .put(
                        "keys",
                        JSONObject()
                            .put("type", "array")
                            .put("items", JSONObject().put("type", "string"))
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_keys))
                    )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "set_zerotermux_config",
            ZtAiStrings.toolSetConfig(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "key",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_config_key))
                    )
                    .put(
                        "value",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_config_value))
                    )
            ).put("required", JSONArray().put("key").put("value"))
        ))
        tools.put(tool(
            "open_zerotermux_page",
            ZtAiStrings.toolOpenPage(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "page_id",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_page_id))
                    )
                    .put(
                        "extras",
                        JSONObject()
                            .put("type", "object")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_page_extras))
                    )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "run_zerotermux_zt",
            ZtAiStrings.toolRunZt(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "command",
                    JSONObject()
                        .put("type", "string")
                        .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_zt_command))
                )
            ).put("required", JSONArray().put("command"))
        ))
        tools.put(tool(
            "get_zerotermux_left_menu",
            ZtAiStrings.toolGetLeftMenu(),
            JSONObject().put("type", "object").put("properties", JSONObject()).put("required", JSONArray())
        ))
        tools.put(tool(
            "update_zerotermux_left_menu",
            ZtAiStrings.toolUpdateLeftMenu(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "menu_package_id",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_menu_package_id))
                    )
                    .put(
                        "xml_content",
                        JSONObject().put("type", "string").put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_xml_content))
                    )
                    .put(
                        "append_group_xml",
                        JSONObject().put("type", "string").put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_append_group))
                    )
                    .put(
                        "create_menu_package",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_create_menu))
                    )
                    .put(
                        "switch_to_menu",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_switch_menu))
                    )
                    .put(
                        "create_tab_name",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_create_tab_alias))
                    )
                    .put(
                        "items",
                        JSONObject()
                            .put("type", "array")
                            .put(
                                "items",
                                JSONObject()
                                    .put("type", "object")
                                    .put(
                                        "properties",
                                        JSONObject()
                                            .put("name", JSONObject().put("type", "string"))
                                            .put("click", JSONObject().put("type", "string"))
                                            .put("icon", JSONObject().put("type", "string"))
                                            .put("tag", JSONObject().put("type", "string"))
                                            .put("autoRunShell", JSONObject().put("type", "string"))
                                    )
                            )
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_menu_items))
                    )
                    .put(
                        "switch_to_tab",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_switch_tab))
                    )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "list_zerotermux_pkg_sources",
            ZtAiStrings.toolListPkgSources(),
            JSONObject().put("type", "object").put("properties", JSONObject()).put("required", JSONArray())
        ))
        tools.put(tool(
            "switch_zerotermux_pkg_source",
            ZtAiStrings.toolSwitchPkgSource(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "source_id",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_source_id))
                    )
                    .put(
                        "user_confirmed",
                        JSONObject().put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_user_confirmed))
                    )
            ).put("required", JSONArray().put("source_id").put("user_confirmed"))
        ))
    }

    private fun tool(name: String, description: String, parameters: JSONObject): JSONObject {
        return JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("parameters", parameters)
            )
    }
}
