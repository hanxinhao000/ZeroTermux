package com.termux.zerocore.editor.lsp

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.QuickQuoteHandler
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.createCompletionItemComparator
import io.github.rosemoe.sora.lang.completion.filterCompletionItems
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch
import java.io.File

class EditorLspLanguage(
    private val delegate: Language,
    private val lspManager: EditorLspManager,
    private val file: File,
    private val languageId: String?
) : Language {

    override fun getAnalyzeManager(): AnalyzeManager {
        return delegate.analyzeManager
    }

    override fun getInterruptionLevel(): Int {
        // Java 默认 STRONG 会在输入过程中 interrupt 补全线程，导致正在进行的
        // textDocument/completion（成员方法）直接失败；LSP 请求用轻微中断即可。
        return if (languageId in LSP_PREFERRED_LANGUAGES) {
            Language.INTERRUPTION_LEVEL_SLIGHT
        } else {
            delegate.interruptionLevel
        }
    }

    @Throws(CompletionCancelledException::class)
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val id = languageId
        val preferLspOnly = id != null &&
            lspManager.isEnabledFor(id) &&
            id in LSP_PREFERRED_LANGUAGES
        if (preferLspOnly) {
            try {
                publisher.checkCancelled()
                val items = ArrayList<CompletionItem>()
                lspManager.completion(file, id, content, position).forEach { candidate ->
                    items.add(EditorLspCompletionItem(candidate, lspManager))
                }
                if (items.isNotEmpty()) {
                    // 避免异常 prefixLength 导致 filter 抛错后整表补全消失
                    clampPrefixLengths(items, position.column)
                    val filtered = runCatching {
                        filterCompletionItems(content, position, items)
                    }.getOrDefault(items)
                    val toShow = filtered.ifEmpty { items }
                    val base = runCatching {
                        createCompletionItemComparator(toShow)
                    }.getOrNull()
                    publisher.setComparator { a, b ->
                        val aCreate = a.sortText?.startsWith(EditorJavaCreateMethodCompletions.SORT_PREFIX) == true
                        val bCreate = b.sortText?.startsWith(EditorJavaCreateMethodCompletions.SORT_PREFIX) == true
                        when {
                            aCreate && !bCreate -> -1
                            !aCreate && bCreate -> 1
                            base != null -> base.compare(a, b)
                            else -> (a.label?.toString() ?: "").compareTo(b.label?.toString() ?: "")
                        }
                    }
                    publisher.addItems(toShow)
                    publisher.updateList(true)
                    return
                }
            } catch (e: CompletionCancelledException) {
                throw e
            } catch (_: Exception) {
            }
            // LSP 无结果时回退到内置补全
            delegate.requireAutoComplete(content, position, publisher, extraArguments)
            return
        }

        delegate.requireAutoComplete(content, position, publisher, extraArguments)
        if (id == null || !lspManager.isEnabledFor(id)) return
        try {
            publisher.checkCancelled()
            val items = ArrayList<CompletionItem>()
            val seen = HashSet<String>()
            lspManager.completion(file, id, content, position).forEach { candidate ->
                val key = candidate.label.lowercase()
                if (!seen.add(key)) return@forEach
                items.add(EditorLspCompletionItem(candidate, lspManager))
            }
            if (items.isNotEmpty()) {
                publisher.addItems(items)
                publisher.updateList(true)
            }
        } catch (e: CompletionCancelledException) {
            throw e
        } catch (_: Exception) {
        }
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return delegate.getIndentAdvance(content, line, column)
    }

    override fun getIndentAdvance(
        content: ContentReference,
        line: Int,
        column: Int,
        spaceCountOnLine: Int,
        tabCountOnLine: Int
    ): Int {
        return delegate.getIndentAdvance(content, line, column, spaceCountOnLine, tabCountOnLine)
    }

    override fun useTab(): Boolean {
        return delegate.useTab()
    }

    override fun getFormatter(): Formatter {
        return delegate.formatter
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return delegate.symbolPairs
    }

    override fun getNewlineHandlers(): Array<NewlineHandler>? {
        return delegate.newlineHandlers
    }

    override fun getQuickQuoteHandler(): QuickQuoteHandler? {
        return delegate.quickQuoteHandler
    }

    override fun destroy() {
        delegate.destroy()
    }

    companion object {
        private val LSP_PREFERRED_LANGUAGES = setOf(
            EditorLspManager.LANGUAGE_JAVA,
            EditorLspManager.LANGUAGE_PYTHON,
            EditorLspManager.LANGUAGE_C,
            EditorLspManager.LANGUAGE_CPP
        )

        private fun clampPrefixLengths(items: List<CompletionItem>, cursorColumn: Int) {
            val maxPrefix = cursorColumn.coerceAtLeast(0)
            for (item in items) {
                if (item.prefixLength < 0) {
                    item.prefixLength = 0
                } else if (item.prefixLength > maxPrefix) {
                    item.prefixLength = maxPrefix
                }
            }
        }
    }
}
