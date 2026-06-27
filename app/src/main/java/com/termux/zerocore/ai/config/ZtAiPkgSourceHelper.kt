package com.termux.zerocore.ai.config

import android.content.Context
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.code.CodeString
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.SingletonCommunicationUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Termux APT 换源（与左侧菜单「切换源」一致）。
 * AI 须先列举方案供用户选择，user_confirmed=true 后才执行。
 */
object ZtAiPkgSourceHelper {

    const val DEFAULT_SOURCE_ID = "tsinghua"

    private data class SourceDef(
        val id: String,
        val labelRes: Int,
        val summaryRes: Int,
        val noteRes: Int = 0,
    )

    private val SOURCES = listOf(
        SourceDef("tsinghua", R.string.清华源, R.string.zt_ai_pkg_source_tsinghua_summary, R.string.zt_ai_pkg_source_default_note),
        SourceDef("bfsu", R.string.北京源, R.string.zt_ai_pkg_source_bfsu_summary),
        SourceDef("official", R.string.官方源, R.string.zt_ai_pkg_source_official_summary, R.string.zt_ai_pkg_source_official_note),
        SourceDef("nju", R.string.nju, R.string.zt_ai_pkg_source_nju_summary),
        SourceDef("ustc", R.string.ustc, R.string.zt_ai_pkg_source_ustc_summary),
        SourceDef("hit", R.string.hit, R.string.zt_ai_pkg_source_hit_summary),
    )

    fun listSources(): String {
        val context = UUtils.getContext()
        val root = JSONObject()
            .put("ok", true)
            .put("hint", ZtAiStrings.pkgSourceHint())
            .put("default_source_id", DEFAULT_SOURCE_ID)
            .put("warning", UUtils.getString(R.string.该操作会覆盖您的文件记录))
            .put("menu_equivalent", ZtAiStrings.str(R.string.zt_ai_pkg_source_menu_equiv))
            .put("sources", JSONArray().apply {
                SOURCES.forEach { def ->
                    put(
                        JSONObject()
                            .put("id", def.id)
                            .put("label", context.getString(def.labelRes))
                            .put("summary", ZtAiStrings.str(def.summaryRes))
                            .put("is_default", def.id == DEFAULT_SOURCE_ID)
                            .put("note", if (def.noteRes != 0) ZtAiStrings.str(def.noteRes) else JSONObject.NULL)
                    )
                }
            })
            .put("workflow", JSONArray().apply {
                ZtAiStrings.strArray(R.array.zt_ai_pkg_source_workflow).forEach { put(it) }
            })
        return root.toString(2)
    }

    fun switchSource(args: JSONObject): String {
        val sourceId = args.optString("source_id", "").trim().lowercase()
        val confirmedRaw = args.optString("user_confirmed", "false").trim()
        val userConfirmed = confirmedRaw.equals("true", ignoreCase = true) || confirmedRaw == "1"

        if (sourceId.isEmpty()) {
            return errorJson("source_id is required; call list_zerotermux_pkg_sources first")
        }
        val def = SOURCES.find { it.id == sourceId }
            ?: return errorJson("unknown source_id: $sourceId")

        if (!userConfirmed) {
            return JSONObject()
                .put("ok", false)
                .put("error", "user confirmation required")
                .put("hint", "Present options to user first; set user_confirmed=true only after explicit approval")
                .put("pending_source_id", sourceId)
                .put("pending_source_label", UUtils.getContext().getString(def.labelRes))
                .put("warning", UUtils.getString(R.string.该操作会覆盖您的文件记录))
                .toString(2)
        }

        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return errorJson("terminal not ready; open main terminal first")
        }

        val applied = applySource(UUtils.getContext(), sourceId)
        return if (applied) {
            JSONObject()
                .put("ok", true)
                .put("source_id", sourceId)
                .put("source_label", UUtils.getContext().getString(def.labelRes))
                .put("message", ZtAiStrings.str(R.string.zt_ai_pkg_source_switched_msg))
                .put("warning", UUtils.getString(R.string.该操作会覆盖您的文件记录))
                .toString(2)
        } else {
            errorJson("failed to send switch command (terminal not ready?)")
        }
    }

    private fun applySource(context: Context, sourceId: String): Boolean {
        if (!SingletonCommunicationUtils.getInstance().hasTerminalListener()) {
            return false
        }
        val listener = SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener()
        return try {
            when (sourceId) {
                "tsinghua" -> {
                    listener.sendTextToTerminal(CodeString.QH)
                    true
                }
                "bfsu" -> {
                    listener.sendTextToTerminal(CodeString.BJ)
                    true
                }
                "official" -> {
                    UUtils.writerFile("code/sources.list", File(FileUrl.sourcesUrl))
                    UUtils.writerFile("code/science.list", File(FileUrl.scienceUrl))
                    UUtils.writerFile("code/game.list", File(FileUrl.gameUrl))
                    listener.sendTextToTerminal(CodeString.UpDate)
                    true
                }
                "nju" -> {
                    listener.sendTextToTerminal(CodeString.NJU)
                    true
                }
                "ustc" -> {
                    listener.sendTextToTerminal(CodeString.USTC)
                    true
                }
                "hit" -> {
                    listener.sendTextToTerminal(CodeString.HEB)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e("ZtAiPkgSourceHelper", "applySource failed", e)
            false
        }
    }

    private fun errorJson(message: String): String {
        return JSONObject().put("ok", false).put("error", message).toString(2)
    }
}
