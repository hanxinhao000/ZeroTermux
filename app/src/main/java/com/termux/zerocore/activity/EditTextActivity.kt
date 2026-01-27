package com.termux.zerocore.activity

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.UUtils
import com.termux.R
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.text.TextUtils
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import kotlinx.coroutines.*
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File


class EditTextActivity : AppCompatActivity() {
    companion object {
        val TAG = EditTextActivity::class.java.simpleName
        val CODE_JAVA = "source.java"
        val CODE_KOTLIN = "source.kotlin"
        val CODE_PYTHON = CODE_JAVA
        val CODE_HTML = "text.html.basic"
        val CODE_JavaScript = "source.js"
        val CODE_MARK_DOWN = "text.html.markdown"
        val CODE_LUA = "source.lua"
    }
    private var mEditText: EditText? = null
    private var mSaveText: TextView? = null
    private var mCancelText: TextView? = null
    private var code_editor: CodeEditor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text)
        mEditText = findViewById(R.id.edit_text)
        mCancelText = findViewById(R.id.cancel)
        code_editor = findViewById(R.id.code_editor)
        mSaveText = findViewById(R.id.ok)

        val stringExtra = intent.getStringExtra("edit_path")

        if (stringExtra == null || stringExtra.isEmpty()) {
            finish()
        }
        val file = File(stringExtra)
        if (!file.exists()) {
            finish()
        }
        val extension2 = if (file.name.contains('.')) {
            file.name.substringAfterLast('.')
        } else {
            ""
        }
        Log.i(TAG, "onCreatexxxxxx extension2: $extension2")
        val readLines = file.readLines()
        val stringBuilder = StringBuilder()
        readLines.forEach {
            stringBuilder.append(it + "\n")
        }

        setupTextmate()
        ensureTextmateTheme()

        val language = TextMateLanguage.create(
            getCodeType(extension2), true
        )
        code_editor?.setEditorLanguage(language)

        MainScope().launch(Dispatchers.IO) {
            delay(100)
            withContext(Dispatchers.Main) {
                code_editor?.setText(stringBuilder.toString())
                updatePositionText()
            }
        }

        mCancelText?.setOnClickListener {
            finish()
        }
        mSaveText?.setOnClickListener {
            val toString = code_editor!!.text.toString()
            if (toString == null || toString.isEmpty()) {
               UUtils.showMsg( UUtils.getString(R.string.命令不能为空))
                return@setOnClickListener
            }
            if (UUtils.setFileString(file, toString)) {
                UUtils.showMsg( UUtils.getString(R.string.保存成功))
                finish()
            } else {
                UUtils.showMsg( UUtils.getString(R.string.save_error_))
            }
        }
    }

    private fun getCodeType(extension: String) : String {
        if (extension.isEmpty()) {
            return CODE_JavaScript
        }
        when (extension) {
            "xml", "html" -> {
                return CODE_HTML
            }
            "java", "py", "python" -> {
                return CODE_JAVA
            }
            "kt", "kotlin" -> {
                return CODE_KOTLIN
            }
            "md" -> {
                return CODE_MARK_DOWN
            }
        }
        return CODE_JavaScript
    }
    private fun setupTextmate() {
        // Add assets file provider so that files in assets can be loaded
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                applicationContext.assets // use application context
            )
        )
        loadDefaultThemes()
        loadDefaultLanguages()
    }

    private /*suspend*/ fun loadDefaultLanguages() /*= withContext(Dispatchers.Main)*/ {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    private fun ensureTextmateTheme() {
        resetColorScheme()
        var editorColorScheme = code_editor?.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            code_editor?.colorScheme = editorColorScheme
        }
    }

    private fun resetColorScheme() {
        code_editor!!.apply {
            val colorScheme = this.colorScheme
            // reset
            this.colorScheme = colorScheme
        }
    }

    private /*suspend*/ fun loadDefaultThemes() /*= withContext(Dispatchers.IO)*/ {
        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_drak")
        val themeRegistry = ThemeRegistry.getInstance()
        themes.forEach { name ->
            val path = "textmate/$name.json"
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ), name
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }
            )
        }

        themeRegistry.setTheme("quietlight")
    }

    private fun updatePositionText() {
        val cursor = code_editor!!.cursor
        var text =
            (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn + ";" + cursor.left + " "

        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            val content = code_editor!!.text
            if (content.getColumnCount(cursor.leftLine) == cursor.leftColumn) {
                "(<" + content.getLine(cursor.leftLine).lineSeparator.let {
                    if (it == LineSeparator.NONE) {
                        "EOF"
                    } else {
                        it.name
                    }
                } + ">)"
            } else {
               /* "(" + content.getLine(cursor.leftLine)
                    .codePointStringAt(cursor.leftColumn)
                    .escapeCodePointIfNecessary() + ")"*/
            }
        }

        // Indicator for text matching
        val searcher = code_editor!!.searcher
        if (searcher.hasQuery()) {
            val idx = searcher.currentMatchedPositionIndex
            val count = searcher.matchedPositionCount
            val matchText = if (count == 0) {
                "no match"
            } else if (count == 1) {
                "1 match"
            } else {
                "$count matches"
            }
            text += if (idx == -1) {
                "($matchText)"
            } else {
                "(${idx + 1} of $matchText)"
            }
        }

       /* binding.positionDisplay.text = text*/
    }
}
