package com.termux.zerocore.config.ztcommand.navigation

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.xh_lib.utils.UUtils
import com.termux.zerocore.config.ztcommand.ZTCommandConfigStore
import com.termux.zerocore.config.ztcommand.ZTSocketService
import com.termux.zerocore.config.ztcommand.config.ZTConfig
import org.json.JSONObject

object ZtCommandExecutor {

    @JvmStatic
    fun execute(commandLine: String): String {
        val command = commandLine.trim()
        if (command.isEmpty()) {
            return error("Command is empty")
        }
        val parts = command.split(" ", limit = 2)
        val commandId = parts[0].lowercase()
        val config: ZTConfig = ZTCommandConfigStore.getConfig(commandId)
        val context = UUtils.getContext()
        return try {
            if (config.isForWard) {
                val intent = Intent(ZTSocketService.ZT_COMMAND_ACTIVITY_ACTION)
                intent.putExtra("message", commandId)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                ok("Forward command sent: $commandId")
            } else {
                val result = config.getCommand(context, command)
                if (result.isNullOrBlank()) {
                    ok("Command executed: $command")
                } else {
                    result
                }
            }
        } catch (e: Exception) {
            error(e.message ?: "Command failed")
        }
    }

    private fun ok(message: String): String {
        return JSONObject().put("code", 0).put("message", message).toString()
    }

    private fun error(message: String): String {
        return JSONObject().put("code", 1).put("message", message).toString()
    }
}
