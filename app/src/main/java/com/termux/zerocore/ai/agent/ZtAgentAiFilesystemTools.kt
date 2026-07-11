package com.termux.zerocore.ai.agent

import com.termux.zerocore.ai.config.ZtAiStrings
import org.json.JSONArray
import org.json.JSONObject

object ZtAgentAiFilesystemTools {

    fun addFilesystemTools(tools: JSONArray) {
        tools.put(
            tool(
                "list_directory",
                ZtAiStrings.toolListDirectory(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject().put(
                        "path",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                    )
                ).put("required", JSONArray())
            )
        )
        tools.put(
            tool(
                "read_file",
                ZtAiStrings.toolReadFile(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject()
                        .put(
                            "path",
                            JSONObject()
                                .put("type", "string")
                                .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                        )
                        .put(
                            "max_bytes",
                            JSONObject()
                                .put("type", "integer")
                                .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_max_bytes))
                        )
                ).put("required", JSONArray().put("path"))
            )
        )
        tools.put(
            tool(
                "write_file",
                ZtAiStrings.toolWriteFile(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject()
                        .put(
                            "path",
                            JSONObject()
                                .put("type", "string")
                                .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                        )
                        .put(
                            "content",
                            JSONObject()
                                .put("type", "string")
                                .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_content))
                        )
                ).put("required", JSONArray().put("path").put("content"))
            )
        )
        tools.put(
            tool(
                "create_file",
                ZtAiStrings.toolCreateFile(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject().put(
                        "path",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                    )
                ).put("required", JSONArray().put("path"))
            )
        )
        tools.put(
            tool(
                "mkdir",
                ZtAiStrings.toolMkdir(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject().put(
                        "path",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                    )
                ).put("required", JSONArray().put("path"))
            )
        )
        tools.put(
            tool(
                "delete_path",
                ZtAiStrings.toolDeletePath(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject()
                        .put(
                            "path",
                            JSONObject()
                                .put("type", "string")
                                .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                        )
                        .put(
                            "user_confirmed",
                            JSONObject()
                                .put("type", "boolean")
                                .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_user_confirmed))
                        )
                ).put("required", JSONArray().put("path").put("user_confirmed"))
            )
        )
        tools.put(
            tool(
                "stat_path",
                ZtAiStrings.toolStatPath(),
                JSONObject().put("type", "object").put(
                    "properties",
                    JSONObject().put(
                        "path",
                        JSONObject()
                            .put("type", "string")
                            .put("description", ZtAiStrings.str(com.termux.R.string.zt_ai_tool_param_fs_path))
                    )
                ).put("required", JSONArray().put("path"))
            )
        )
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
