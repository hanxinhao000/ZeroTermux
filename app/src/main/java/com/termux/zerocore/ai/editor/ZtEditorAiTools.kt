package com.termux.zerocore.ai.editor

import org.json.JSONArray
import org.json.JSONObject

object ZtEditorAiTools {

    fun definitions(): JSONArray {
        val tools = JSONArray()
        tools.put(tool(
            "read_editor",
            "Read current editor file content, cursor, and selection. Always call before editing.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "max_chars",
                    JSONObject()
                        .put("type", "integer")
                        .put("description", "Max characters to return, default 12000")
                )
            ).put("required", JSONArray())
        ))
        tools.put(tool(
            "insert_at_cursor",
            "Insert text at current cursor position in the editor.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "text",
                    JSONObject().put("type", "string").put("description", "Text to insert")
                )
            ).put("required", JSONArray().put("text"))
        ))
        tools.put(tool(
            "replace_range",
            "Replace editor text from start (inclusive) to end (exclusive) character offset.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put("start", JSONObject().put("type", "integer"))
                    .put("end", JSONObject().put("type", "integer"))
                    .put("text", JSONObject().put("type", "string"))
            ).put("required", JSONArray().put("start").put("end").put("text"))
        ))
        tools.put(tool(
            "replace_all",
            "Replace entire editor content. Use carefully.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "text",
                    JSONObject().put("type", "string").put("description", "New full file content")
                )
            ).put("required", JSONArray().put("text"))
        ))
        tools.put(tool(
            "create_file",
            "Create a new file (and parent folders if needed). Use open_file before editing each file when creating multiple files.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject()
                    .put(
                        "path",
                        JSONObject()
                            .put("type", "string")
                            .put("description", "Absolute path, or relative to current file directory")
                    )
                    .put(
                        "content",
                        JSONObject()
                            .put("type", "string")
                            .put("description", "Initial file content, default empty")
                    )
                    .put(
                        "open",
                        JSONObject()
                            .put("type", "boolean")
                            .put("description", "Switch editor to the new file after creation, default true")
                    )
            ).put("required", JSONArray().put("path"))
        ))
        tools.put(tool(
            "open_file",
            "Open an existing file in the editor and switch the active tab. Call this before read_editor / replace_all when working on another file.",
            JSONObject().put("type", "object").put(
                "properties",
                JSONObject().put(
                    "path",
                    JSONObject()
                        .put("type", "string")
                        .put("description", "Absolute path, or relative to current file directory")
                )
            ).put("required", JSONArray().put("path"))
        ))
        tools.put(tool(
            "save_current_file",
            "Save the currently active editor file to disk.",
            JSONObject().put("type", "object").put("properties", JSONObject()).put("required", JSONArray())
        ))
        tools.put(tool(
            "list_open_files",
            "List files currently open in editor tabs and which one is active.",
            JSONObject().put("type", "object").put("properties", JSONObject()).put("required", JSONArray())
        ))
        addTerminalTools(tools)
        return tools
    }

    private fun addTerminalTools(tools: JSONArray) {
        tools.put(tool(
            "read_terminal",
            "Read editor page embedded terminal screen. ALWAYS call before describing compile output or command results.",
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
            "Send command to editor terminal (newline appended by default). Returns updated terminal snapshot.",
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
            "Send a special key or key combination to the editor terminal.",
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
