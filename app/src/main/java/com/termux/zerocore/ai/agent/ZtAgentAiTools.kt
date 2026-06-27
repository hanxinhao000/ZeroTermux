package com.termux.zerocore.ai.agent

import com.termux.zerocore.ai.config.ZtAiConfigTools
import com.termux.zerocore.ai.config.ZtAiStrings
import org.json.JSONArray
import org.json.JSONObject

object ZtAgentAiTools {

    fun definitions(terminalEnabled: Boolean, ztControlEnabled: Boolean): JSONArray {
        val tools = JSONArray()
        if (terminalEnabled) {
            addTerminalTools(tools)
        }
        if (ztControlEnabled) {
            ZtAiConfigTools.addConfigTools(tools)
            if (terminalEnabled) {
                addZtControlTools(tools)
            }
        }
        return tools
    }

    private fun addTerminalTools(tools: JSONArray) {
        tools.put(tool(
            "read_terminal",
            ZtAiStrings.toolReadTerminal(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "max_chars",
                    JSONObject()
                        .put("type", "integer")
                        .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_max_chars))
                )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "send_terminal_command",
            ZtAiStrings.toolSendCommand(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "command",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_command))
                    )
                    .put(
                        "append_newline",
                        JSONObject()
                            .put("type", "boolean")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_append_newline))
                    )
                    .put(
                        "max_wait_ms",
                        JSONObject()
                            .put("type", "integer")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_max_wait))
                    )
            ).put("required", JSONArray().put("command"))
        ))
        tools.put(tool(
            "send_terminal_key",
            ZtAiStrings.toolSendKey(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "key",
                    JSONObject()
                        .put("type", "string")
                        .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_terminal_key))
                        .put(
                            "enum",
                            JSONArray()
                                .put("enter")
                                .put("tab")
                                .put("escape")
                                .put("backspace")
                                .put("up")
                                .put("down")
                                .put("left")
                                .put("right")
                                .put("ctrl_c")
                                .put("ctrl_d")
                                .put("ctrl_l")
                                .put("ctrl_z")
                        )
                )
            ).put("required", JSONArray().put("key"))
        ))
    }

    private fun addZtControlTools(tools: JSONArray) {
        tools.put(tool(
            "run_zt_command",
            ZtAiStrings.toolRunZtCommand(),
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "command",
                    JSONObject()
                        .put("type", "string")
                        .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_run_zt_cmd))
                )
            ).put("required", JSONArray().put("command"))
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
