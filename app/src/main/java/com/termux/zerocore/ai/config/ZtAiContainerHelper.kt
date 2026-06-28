package com.termux.zerocore.ai.config

import android.content.Intent
import com.example.xh_lib.utils.UUtils
import com.termux.app.TermuxService
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.activity.utils.CreateSystemUtils
import com.termux.zerocore.bean.ReadSystemBean
import com.termux.zerocore.utils.SingletonCommunicationUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 多 Termux 容器（左侧「容器切换」）— AI / 调试 API 与 [CreateSystemUtils] 共用逻辑。
 */
object ZtAiContainerHelper {

    private fun loadMarkedContainers(): Pair<List<ReadSystemBean>, String?> {
        CreateSystemUtils.ensureDefaultActiveConfig()
        val result = CreateSystemUtils.loadContainers()
        if (result.configError) {
            return emptyList<ReadSystemBean>() to "container config corrupted; open 容器切换 in app to repair"
        }
        val list = ArrayList(result.containers)
        CreateSystemUtils.markActiveContainer(list)
        return list to null
    }

    fun listContainersJson(): String {
        val (containers, err) = loadMarkedContainers()
        if (err != null) {
            return errorJson(err)
        }
        val arr = JSONArray()
        containers.forEach { c ->
            arr.put(containerJson(c))
        }
        val active = containers.firstOrNull { it.isCkeck }
        return JSONObject()
            .put("ok", true)
            .put("hint", ZtAiStrings.containerHint())
            .put("menu_equivalent", ZtAiStrings.str(com.termux.R.string.zt_ai_container_menu_equiv))
            .put("active_container_id", active?.let { containerId(it) } ?: JSONObject.NULL)
            .put("active_container_name", active?.name ?: JSONObject.NULL)
            .put("containers", arr)
            .toString(2)
    }

    fun createContainer(args: JSONObject): String {
        val name = args.optString("container_name", "").trim()
            .ifEmpty { args.optString("name", "").trim() }
        if (name.isEmpty()) {
            return errorJson("container_name is required")
        }
        if (!CreateSystemUtils.createContainer(name)) {
            return errorJson("create failed (check storage permissions)")
        }
        val (containers, err) = loadMarkedContainers()
        if (err != null) return errorJson(err)
        val created = containers.filter { it.name == name }
            .maxByOrNull { containerId(it).removePrefix("files").toIntOrNull() ?: 0 }
            ?: return errorJson("created but container not found in list")

        val autoSwitch = parseBool(args.optString("switch_after_create", "false"))
        if (autoSwitch) {
            return switchContainer(
                JSONObject()
                    .put("container_id", containerId(created))
                    .put("restart_app", args.optString("restart_app", "false"))
            )
        }

        return JSONObject()
            .put("ok", true)
            .put("message", "container created")
            .put("container_id", containerId(created))
            .put("container_name", created.name)
            .put("path", created.dir)
            .put("hint", "call switch_zerotermux_container to activate; restart app after switch")
            .toString(2)
    }

    fun switchContainer(args: JSONObject): String {
        val identifier = args.optString("container_id", "").trim()
            .ifEmpty { args.optString("container_name", "").trim() }
            .ifEmpty { args.optString("name", "").trim() }
        if (identifier.isEmpty()) {
            return errorJson("container_id or container_name is required; call list_zerotermux_containers first")
        }
        val (containers, err) = loadMarkedContainers()
        if (err != null) return errorJson(err)

        val target = resolveContainer(containers, identifier)
            ?: return errorJson("container not found: $identifier")

        if (target.isCkeck) {
            return JSONObject()
                .put("ok", true)
                .put("message", "already active")
                .put("container_id", containerId(target))
                .put("container_name", target.name)
                .put("reboot_required", false)
                .toString(2)
        }

        if (!CreateSystemUtils.switchContainer(target)) {
            return errorJson("switch failed (check storage permissions and container integrity)")
        }

        val restart = parseBool(args.optString("restart_app", "false"))
        if (restart) {
            requestAppRestart()
        }

        return JSONObject()
            .put("ok", true)
            .put("message", "switched container; restart app for full effect")
            .put("container_id", containerId(target))
            .put("container_name", target.name)
            .put("path", target.dir)
            .put("reboot_required", true)
            .put("restarted", restart)
            .toString(2)
    }

    fun deleteContainer(args: JSONObject): String {
        val identifier = args.optString("container_id", "").trim()
            .ifEmpty { args.optString("container_name", "").trim() }
            .ifEmpty { args.optString("name", "").trim() }
        if (identifier.isEmpty()) {
            return errorJson("container_id or container_name is required")
        }

        val confirmed = parseBool(args.optString("user_confirmed", "false"))
        val (containers, err) = loadMarkedContainers()
        if (err != null) return errorJson(err)

        val target = resolveContainer(containers, identifier)
            ?: return errorJson("container not found: $identifier")

        if (CreateSystemUtils.isMainContainer(target.dir)) {
            return errorJson("cannot delete main container (主容器)")
        }

        if (target.isCkeck) {
            return errorJson("cannot delete the currently active container; switch to another first")
        }

        if (!confirmed) {
            return JSONObject()
                .put("ok", false)
                .put("error", "user confirmation required")
                .put("hint", ZtAiStrings.str(com.termux.R.string.zt_ai_container_delete_confirm_hint))
                .put("pending_container_id", containerId(target))
                .put("pending_container_name", target.name)
                .put("path", target.dir)
                .put("warning", ZtAiStrings.str(com.termux.R.string.zt_ai_container_delete_warning))
                .toString(2)
        }

        val latch = CountDownLatch(1)
        var deleteResult: CreateSystemUtils.DeleteResult? = null
        Thread {
            deleteResult = CreateSystemUtils.deleteContainer(UUtils.getContext(), target.dir)
            latch.countDown()
        }.start()
        latch.await(120, TimeUnit.SECONDS)
        val result = deleteResult ?: return errorJson("delete timed out")

        if (result.blockedAsMain) {
            return errorJson("cannot delete main container")
        }

        if (result.needsFallbackCleanup) {
            val cmd = "chmod 777 -R ${target.dir}&& rm -rf ${target.dir} \n"
            val sent = try {
                val comm = SingletonCommunicationUtils.getInstance()
                if (!comm.hasTerminalListener()) {
                    false
                } else {
                    comm.getmSingletonCommunicationListener().sendTextToTerminal(cmd)
                    true
                }
            } catch (_: Exception) {
                false
            }
            if (!sent) {
                return errorJson("delete incomplete; open terminal and run: rm -rf ${target.dir}")
            }
            return JSONObject()
                .put("ok", true)
                .put("message", "delete sent via terminal fallback")
                .put("container_id", containerId(target))
                .put("container_name", target.name)
                .put("fallback_terminal", true)
                .toString(2)
        }

        return JSONObject()
            .put("ok", true)
            .put("message", "container deleted")
            .put("container_id", containerId(target))
            .put("container_name", target.name)
            .toString(2)
    }

    private fun containerJson(c: ReadSystemBean): JSONObject {
        return JSONObject()
            .put("container_id", containerId(c))
            .put("name", c.name)
            .put("path", c.dir)
            .put("created_at", c.time ?: JSONObject.NULL)
            .put("active", c.isCkeck)
            .put("is_main", CreateSystemUtils.isMainContainer(c.dir))
    }

    private fun containerId(c: ReadSystemBean): String = File(c.dir).name

    private fun resolveContainer(containers: List<ReadSystemBean>, raw: String): ReadSystemBean? {
        val id = raw.trim()
        containers.firstOrNull { it.dir == id }?.let { return it }
        containers.firstOrNull { it.name == id }?.let { return it }
        containers.firstOrNull { containerId(it).equals(id, ignoreCase = true) }?.let { return it }
        val normalized = when {
            id.startsWith("files", ignoreCase = true) -> id.lowercase()
            id.matches(Regex("\\d+")) -> "files$id"
            else -> null
        }
        if (normalized != null) {
            containers.firstOrNull { containerId(it).equals(normalized, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    private fun parseBool(raw: String): Boolean =
        raw.equals("true", ignoreCase = true) || raw == "1"

    private fun requestAppRestart() {
        try {
            val ctx = UUtils.getContext()
            ctx.startService(
                Intent(ctx, TermuxService::class.java).setAction(
                    TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE
                )
            )
        } catch (_: Exception) {
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun errorJson(message: String): String =
        JSONObject().put("ok", false).put("error", message).toString(2)
}
