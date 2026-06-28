package com.termux.zerocore.ai.agent

import android.content.Context
import android.graphics.Color
import com.termux.zerocore.ai.deepseek.markdown.MarkDownAPI
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme

/** 主界面 / 编辑器 AI 智能体面板共用的 Markwon（含 GFM 表格）。 */
object ZtAgentMarkwon {

    @Volatile
    private var instance: Markwon? = null

    @Volatile
    private var appContext: Context? = null

    fun get(context: Context): Markwon {
        val ctx = context.applicationContext
        instance?.let { if (appContext === ctx) return it }
        return synchronized(this) {
            instance?.let { if (appContext === ctx) return it }
            Markwon.builder(ctx)
                .usePlugin(MarkDownAPI.create(ctx))
                .usePlugin(createTablePlugin(ctx))
                .build()
                .also {
                    instance = it
                    appContext = ctx
                }
        }
    }

    private fun createTablePlugin(context: Context): TablePlugin {
        val theme = TableTheme.buildWithDefaults(context)
            .tableBorderColor(Color.argb(0x66, 0xFF, 0xFF, 0xFF))
            .tableHeaderRowBackgroundColor(Color.argb(0x33, 0xFF, 0xFF, 0xFF))
            .tableOddRowBackgroundColor(Color.argb(0x1A, 0xFF, 0xFF, 0xFF))
            .tableEvenRowBackgroundColor(Color.argb(0x0D, 0xFF, 0xFF, 0xFF))
            .build()
        return TablePlugin.create(theme)
    }
}
