package com.termux.zerocore.ai.agent

import org.json.JSONArray
import org.json.JSONObject

object ZtAgentAiTools {

    fun definitions(terminalEnabled: Boolean, ztControlEnabled: Boolean): JSONArray {
        val tools = JSONArray()
        if (terminalEnabled) {
            addTerminalTools(tools)
        }
        if (ztControlEnabled && terminalEnabled) {
            addZtControlTools(tools)
        }
        return tools
    }

    private fun addTerminalTools(tools: JSONArray) {
        tools.put(tool(
            "read_terminal",
            "Read current terminal screen. ALWAYS call before describing terminal state, install progress, or command output. Do not guess.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "max_chars",
                    JSONObject()
                        .put("type", "integer")
                        .put("description", "Maximum characters to return, default 8000")
                )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "send_terminal_command",
            "Send command to terminal (newline appended by default). Returns updated terminal snapshot — describe results ONLY from that snapshot.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "command",
                        JSONObject()
                            .put("type", "string")
                            .put("description", "Text or shell command to send")
                    )
                    .put(
                        "append_newline",
                        JSONObject()
                            .put("type", "boolean")
                            .put("description", "Whether to append newline after command, default true")
                    )
            ).put("required", JSONArray().put("command"))
        ))
        tools.put(tool(
            "send_terminal_key",
            "Send a special key or key combination to the terminal.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "key",
                    JSONObject()
                        .put("type", "string")
                        .put(
                            "description",
                            "One of: enter, tab, escape, backspace, up, down, left, right, ctrl_c, ctrl_d, ctrl_l, ctrl_z"
                        )
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
            "Send a ZeroTermux zt command through the terminal (same as typing `zt ...` in shell). " +
                "Always read the returned terminal snapshot — do not assume success without terminal output. " +
                "Call command=help first to discover commands. " +
                "Examples: help, openleft, openright, toast hello, openpage list, openpage zt_settings, openpage guide, version. " +
                "To open settings use openpage zt_settings. Avoid reboot unless user confirms.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "command",
                    JSONObject()
                        .put("type", "string")
                        .put("description", "Full zt command line without the `zt` prefix, e.g. help or openpage zt_settings")
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
