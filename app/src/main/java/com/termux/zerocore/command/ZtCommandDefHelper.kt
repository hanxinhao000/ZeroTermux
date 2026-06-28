package com.termux.zerocore.command

import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.bean.MinLBean
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.SaveData
import org.json.JSONArray
import org.json.JSONObject

/**
 * 左侧菜单「命令定义」(CommandDefinitionCLickConfig / BoomCommandDialog) 与终端「添加命令」共用存储。
 * 数据保存在 SaveData [FileIOUtils.COMMEND_KEY]（MinLBean JSON）。
 */
object ZtCommandDefHelper {

    private val gson = Gson()

    fun loadCommands(): List<MinLBean.DataNum> {
        val bean = loadBean() ?: return emptyList()
        return bean.data?.list?.filterNotNull() ?: emptyList()
    }

    fun listJson(): String {
        val commands = loadCommands()
        val arr = JSONArray()
        commands.forEach { c -> arr.put(commandJson(c)) }
        return JSONObject()
            .put("ok", true)
            .put("storage_key", FileIOUtils.COMMEND_KEY)
            .put("menu_equivalent", UUtils.getString(R.string.zt_ai_command_def_menu_equiv))
            .put("hint", commandHint())
            .put("count", commands.size)
            .put("commands", arr)
            .toString(2)
    }

    fun addCommand(args: JSONObject): String {
        val name = args.optString("name", "").trim()
            .ifEmpty { args.optString("command_name", "").trim() }
        val command = args.optString("command", "").trim()
            .ifEmpty { args.optString("value", "").trim() }
        if (name.isEmpty()) return errorJson("name is required")
        if (command.isEmpty()) return errorJson("command is required")
        val appendNewline = parseAppendNewline(args)

        val item = newDataNum(name, command, appendNewline)
        val list = loadMutableList()
        list.add(0, item)
        saveList(list)

        return JSONObject()
            .put("ok", true)
            .put("message", "command added")
            .put("command", commandJson(item))
            .toString(2)
    }

    fun updateCommand(args: JSONObject): String {
        val idRaw = args.optString("command_id", "").trim()
        val nameKey = args.optString("name", "").trim()
            .ifEmpty { args.optString("command_name", "").trim() }
        if (idRaw.isEmpty() && nameKey.isEmpty()) {
            return errorJson("command_id or name is required; call list_zerotermux_command_defs first")
        }

        val list = loadMutableList()
        val index = resolveIndex(list, idRaw, nameKey)
            ?: return errorJson("command not found")

        val current = list[index]
        args.optString("new_name", "").trim().takeIf { it.isNotEmpty() }?.let { current.name = it }
        args.optString("command", "").trim().takeIf { it.isNotEmpty() }?.let { current.value = it }
        if (args.has("append_newline")) {
            current.isChecked = parseAppendNewline(args)
        }
        list[index] = current
        saveList(list)

        return JSONObject()
            .put("ok", true)
            .put("message", "command updated")
            .put("command", commandJson(current))
            .toString(2)
    }

    fun deleteCommand(args: JSONObject): String {
        val idRaw = args.optString("command_id", "").trim()
        val nameKey = args.optString("name", "").trim()
            .ifEmpty { args.optString("command_name", "").trim() }
        if (idRaw.isEmpty() && nameKey.isEmpty()) {
            return errorJson("command_id or name is required")
        }

        val confirmed = parseBool(args.optString("user_confirmed", "false"))
        val list = loadMutableList()
        val target = resolveCommand(list, idRaw, nameKey)
            ?: return errorJson("command not found: ${idRaw.ifEmpty { nameKey }}")

        if (!confirmed) {
            return JSONObject()
                .put("ok", false)
                .put("error", "user confirmation required")
                .put("hint", deleteConfirmHint())
                .put("pending_command_id", target.id)
                .put("pending_command_name", target.name)
                .put("pending_command", target.value)
                .toString(2)
        }

        list.removeAll { it.id == target.id }
        saveList(list)

        return JSONObject()
            .put("ok", true)
            .put("message", "command deleted")
            .put("command_id", target.id)
            .put("command_name", target.name)
            .toString(2)
    }

    fun runCommand(args: JSONObject): String {
        val idRaw = args.optString("command_id", "").trim()
        val nameKey = args.optString("name", "").trim()
            .ifEmpty { args.optString("command_name", "").trim() }
        if (idRaw.isEmpty() && nameKey.isEmpty()) {
            return errorJson("command_id or name is required")
        }
        val list = loadCommands()
        val target = resolveCommand(list, idRaw, nameKey)
            ?: return errorJson("command not found")

        val sent = sendToTerminal(target.value, target.isChecked)
        if (!sent) {
            return errorJson("terminal unavailable; open ZeroTermux main screen first")
        }

        return JSONObject()
            .put("ok", true)
            .put("message", "command sent to terminal")
            .put("command_id", target.id)
            .put("command_name", target.name)
            .put("append_newline", target.isChecked)
            .toString(2)
    }

    /** 终端选区「添加命令」与 MingLShowDialog 添加。 */
    fun addCommandSimple(name: String, command: String, appendNewline: Boolean) {
        val list = loadMutableList()
        list.add(0, newDataNum(name, command, appendNewline))
        saveList(list)
    }

    private fun loadBean(): MinLBean? {
        val raw = SaveData.getData(FileIOUtils.COMMEND_KEY)
        if (raw.isNullOrEmpty() || raw == FileIOUtils.COMMEND_DEF) return null
        return try {
            gson.fromJson(raw, MinLBean::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadMutableList(): MutableList<MinLBean.DataNum> {
        val bean = loadBean()
        if (bean?.data?.list == null) {
            return ArrayList()
        }
        return ArrayList(bean.data.list)
    }

    private fun saveList(list: List<MinLBean.DataNum>) {
        if (list.isEmpty()) {
            SaveData.saveData(FileIOUtils.COMMEND_KEY, FileIOUtils.COMMEND_DEF)
            return
        }
        val bean = MinLBean()
        val data = MinLBean.Data()
        data.list = list
        bean.data = data
        SaveData.saveData(FileIOUtils.COMMEND_KEY, gson.toJson(bean))
    }

    private fun newDataNum(name: String, command: String, appendNewline: Boolean): MinLBean.DataNum {
        return MinLBean.DataNum().apply {
            id = System.currentTimeMillis()
            this.name = name
            value = command
            isChecked = appendNewline
            isPinTop = false
        }
    }

    private fun resolveIndex(
        list: List<MinLBean.DataNum>,
        idRaw: String,
        nameKey: String
    ): Int? {
        val target = resolveCommand(list, idRaw, nameKey) ?: return null
        return list.indexOfFirst { it.id == target.id }.takeIf { it >= 0 }
    }

    private fun resolveCommand(
        list: List<MinLBean.DataNum>,
        idRaw: String,
        nameKey: String
    ): MinLBean.DataNum? {
        if (idRaw.isNotEmpty()) {
            idRaw.toLongOrNull()?.let { id ->
                list.firstOrNull { it.id == id }?.let { return it }
            }
        }
        if (nameKey.isNotEmpty()) {
            list.firstOrNull { it.name == nameKey }?.let { return it }
        }
        return null
    }

    private fun commandJson(c: MinLBean.DataNum): JSONObject {
        return JSONObject()
            .put("command_id", c.id)
            .put("name", c.name)
            .put("command", c.value)
            .put("append_newline", c.isChecked)
            .put("pin_top", c.isPinTop)
    }

    private fun parseAppendNewline(args: JSONObject): Boolean {
        if (args.has("append_newline")) {
            return parseBool(args.optString("append_newline", "false"))
        }
        return parseBool(args.optString("auto_newline", "false"))
    }

    private fun parseBool(raw: String): Boolean =
        raw.equals("true", ignoreCase = true) || raw == "1"

    private fun sendToTerminal(command: String, appendNewline: Boolean): Boolean {
        return try {
            val comm = com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance()
            if (!comm.hasTerminalListener()) return false
            val text = if (appendNewline) "$command \n" else command
            comm.getmSingletonCommunicationListener().sendTextToTerminal(text)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun commandHint(): String = UUtils.getString(R.string.zt_ai_command_def_hint)

    private fun deleteConfirmHint(): String = UUtils.getString(R.string.zt_ai_command_def_delete_confirm_hint)

    private fun errorJson(message: String): String =
        JSONObject().put("ok", false).put("error", message).toString(2)
}
