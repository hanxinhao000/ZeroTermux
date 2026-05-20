package com.termux.zerocore.editor.lsp

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.QuickQuoteHandler
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
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
        return delegate.interruptionLevel
    }

    @Throws(CompletionCancelledException::class)
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        delegate.requireAutoComplete(content, position, publisher, extraArguments)
        val id = languageId ?: return
        if (!lspManager.isEnabledFor(id)) return
        publisher.checkCancelled()
        val items = ArrayList<CompletionItem>()
        lspManager.completion(file, id, content, position).forEach { candidate ->
            items.add(EditorLspCompletionItem(candidate))
        }
        publisher.addItems(items)
        publisher.updateList(true)
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
}
