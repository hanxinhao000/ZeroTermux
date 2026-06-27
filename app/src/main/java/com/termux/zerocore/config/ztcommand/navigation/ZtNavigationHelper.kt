package com.termux.zerocore.config.ztcommand.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.xh_lib.utils.UUtils
import com.termux.zerocore.guide.TermuxGuideActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object ZtNavigationHelper {

    private const val TAG = "ZT_NAV"
    const val OPEN_PAGE_MESSAGE_PREFIX = "openpage:"

    @JvmStatic
    fun listPagesJson(): String {
        val arr = JSONArray()
        ZtPageNavigationRegistry.allPages().forEach { page ->
            arr.put(
                JSONObject()
                    .put("id", page.id)
                    .put("title", page.title)
                    .put("activity", page.activityClass.name)
            )
        }
        val aliasArr = JSONArray()
        ZtPageNavigationRegistry.aliasLines().forEach { aliasArr.put(it) }
        return JSONObject()
            .put("code", 0)
            .put("pages", arr)
            .put("aliases", aliasArr)
            .toString()
    }

    @JvmStatic
    fun listPagesText(): String {
        val sb = StringBuilder("Use zt openpage <page_id> to open a page:\n\n")
        ZtPageNavigationRegistry.allPages().forEach { page ->
            sb.append("- ").append(page.id).append(": ").append(page.title).append('\n')
        }
        sb.append("\nCommon aliases (also accepted by openpage):\n")
        ZtPageNavigationRegistry.aliasLines().forEach { sb.append("  ").append(it).append('\n') }
        sb.append("\nExamples: zt openpage zt_settings, zt openpage guide, zt openpage agent_ai_settings")
        return sb.toString().trim()
    }

    @JvmStatic
    fun openPage(context: Context, pageId: String, extrasJson: JSONObject?): String {
        Log.i(TAG, "openPage request pageId=$pageId context=${context.javaClass.simpleName}")
        return runOnMainSync {
            val foreground = ZtForegroundActivityHolder.get()
            if (foreground != null && !foreground.isFinishing) {
                Log.i(TAG, "openPage via foreground: ${foreground.javaClass.simpleName}")
                openPageInternal(foreground, pageId, extrasJson)
            } else {
                Log.e(TAG, "openPage failed: TermuxActivity not in foreground")
                buildResult(
                    code = 1,
                    message = "Cannot open page: Termux main screen is not active. Return to terminal first, then retry.",
                    pageId = pageId,
                    title = "",
                    opened = false
                )
            }
        }
    }

    @JvmStatic
    fun openPageInternal(context: Context, pageId: String, extrasJson: JSONObject?): String {

        val resolved = ZtPageNavigationRegistry.find(pageId)
        if (resolved == null) {
            val hint = ZtPageNavigationRegistry.aliasLines().take(12).joinToString(", ")
            Log.e(TAG, "openPage unknown pageId=$pageId normalized query failed")
            return buildResult(
                code = 1,
                message = "Unknown page_id: $pageId. Run zt openpage list first. Aliases example: $hint",
                pageId = pageId,
                title = "",
                opened = false
            )
        }

        if (context is ZtNavigationHost) {
            context.prepareForPageNavigation()
        }

        val errorRef = AtomicReference<String?>(null)
        try {
            val intent = Intent(context, resolved.activityClass)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            resolved.applyExtras?.invoke(intent)
            applyJsonExtras(intent, extrasJson)
            ensureGuideJumpExtra(intent, resolved)
            Log.i(
                TAG,
                "startActivity ${resolved.activityClass.simpleName} requested=$pageId resolved=${resolved.id} extras=${intent.extras}"
            )
            context.startActivity(intent)
            if (context is Activity) {
                Toast.makeText(context, "已打开: ${resolved.title}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startActivity failed page=${resolved.id}", e)
            errorRef.set(e.message ?: e.toString())
        }

        errorRef.get()?.let { err ->
            return buildResult(
                code = 1,
                message = "Failed to open ${resolved.id}: $err",
                pageId = resolved.id,
                title = resolved.title,
                opened = false
            )
        }
        return buildResult(
            code = 0,
            message = "已打开: ${resolved.title}",
            pageId = resolved.id,
            title = resolved.title,
            opened = true
        )
    }

    private fun buildResult(
        code: Int,
        message: String,
        pageId: String,
        title: String,
        opened: Boolean
    ): String {
        return JSONObject()
            .put("code", code)
            .put("message", message)
            .put("title", title)
            .put("page_id", pageId)
            .put("opened", opened)
            .toString()
    }

    private fun runOnMainSync(block: () -> String): String {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }
        val result = AtomicReference<String>()
        val latch = CountDownLatch(1)
        UUtils.getHandler().post {
            try {
                result.set(block())
            } finally {
                latch.countDown()
            }
        }
        latch.await(8, TimeUnit.SECONDS)
        return result.get() ?: JSONObject()
            .put("code", 1)
            .put("message", "openPage timeout")
            .toString()
    }

    private fun applyJsonExtras(intent: Intent, extrasJson: JSONObject?) {
        if (extrasJson == null) return
        val keys = extrasJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = extrasJson.get(key)) {
                is String -> intent.putExtra(key, value)
                is Int -> intent.putExtra(key, value)
                is Long -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
                is Double -> intent.putExtra(key, value)
                else -> intent.putExtra(key, value.toString())
            }
        }
        if (extrasJson.has("url") && intent.component?.className?.contains("WebViewActivity") == true) {
            intent.putExtra("url", extrasJson.getString("url"))
        }
        if (extrasJson.has("edit_path") && intent.component?.className?.contains("EditTextActivity") == true) {
            intent.putExtra("edit_path", extrasJson.getString("edit_path"))
        }
    }

    private fun ensureGuideJumpExtra(intent: Intent, page: ZtPageNavigationRegistry.PageEntry) {
        if (!TermuxGuideActivity::class.java.isAssignableFrom(page.activityClass)) return
        if (!intent.hasExtra(TermuxGuideActivity.GUIDE_EXTRA_JUMP_OTHER)) {
            intent.putExtra(TermuxGuideActivity.GUIDE_EXTRA_JUMP_OTHER, true)
        }
        if (!intent.hasExtra(TermuxGuideActivity.GUIDE_EXTRA)) {
            intent.putExtra(TermuxGuideActivity.GUIDE_EXTRA, TermuxGuideActivity.GUIDE_AGREEMENT)
        }
    }
}
