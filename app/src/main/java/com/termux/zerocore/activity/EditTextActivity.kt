package com.termux.zerocore.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
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
import io.github.rosemoe.sora.widget.EditorSearcher
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

    // Find/Replace toolbar views
    private var toolbarLayout: LinearLayout? = null
    private var btnFind: TextView? = null
    private var btnReplace: TextView? = null
    private var searchInput: EditText? = null
    private var replaceInput: EditText? = null
    private var btnPrev: TextView? = null
    private var btnNext: TextView? = null
    private var btnReplaceOne: TextView? = null
    private var btnReplaceAll: TextView? = null

    // Current mode: true = Find, false = Replace
    private var isFindMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text)
        mEditText = findViewById(R.id.edit_text)
        mCancelText = findViewById(R.id.cancel)
        code_editor = findViewById(R.id.code_editor)
        mSaveText = findViewById(R.id.ok)

        // Init find/replace toolbar views
        toolbarLayout = findViewById(R.id.find_replace_toolbar)
        btnFind = findViewById(R.id.btn_find)
        btnReplace = findViewById(R.id.btn_replace)
        searchInput = findViewById(R.id.search_input)
        replaceInput = findViewById(R.id.replace_input)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnReplaceOne = findViewById(R.id.btn_replace_one)
        btnReplaceAll = findViewById(R.id.btn_replace_all)

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

        val ztUserBean = UserSetManage.get().getZTUserBean()
        code_editor?.isWordwrap = ztUserBean.isEditorWordWrap

        // Init find/replace functionality
        initFindReplace()

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

    /**
     * Initialize Find/Replace toolbar functionality.
     * Default mode: Find (查找) highlighted in light blue (#2e84e6).
     * Click to toggle between Find and Replace modes.
     * Replace mode shows additional replace input and replace buttons.
     */
    private fun initFindReplace() {
        // Set default Find mode - highlight Find button with light blue
        setFindMode(true)

        // Find button click: switch to Find mode
        btnFind?.setOnClickListener {
            if (!isFindMode) {
                setFindMode(true)
                performSearch()
            }
        }

        // Replace button click: switch to Replace mode
        btnReplace?.setOnClickListener {
            toolbarLayout?.visibility = View.VISIBLE
            setFindMode(false)
            performSearch()
        }

        // Search input: perform search on text change
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })

        // Search input: perform search on Enter/action key
        searchInput?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Previous match button
        btnPrev?.setOnClickListener {
            code_editor?.searcher?.gotoPrevious()
            updatePositionText()
        }

        // Next match button
        btnNext?.setOnClickListener {
            code_editor?.searcher?.gotoNext()
            updatePositionText()
        }

        // Replace current match
        btnReplaceOne?.setOnClickListener {
            val replaceText = replaceInput?.text?.toString() ?: ""
            code_editor?.searcher?.replaceThis(replaceText)
            updatePositionText()
        }

        // Replace all matches
        btnReplaceAll?.setOnClickListener {
            val replaceText = replaceInput?.text?.toString() ?: ""
            code_editor?.searcher?.replaceAll(replaceText)
            updatePositionText()
        }
    }

    /**
     * Set the current mode (Find or Replace) and update UI accordingly.
     * In Find mode: only search input visible, Find button highlighted.
     * In Replace mode: both inputs visible, Replace button highlighted, replace action buttons visible.
     */
    private fun setFindMode(findMode: Boolean) {
        isFindMode = findMode
        val highlightColor = ContextCompat.getColor(this, android.R.color.holo_blue_light)
        val defaultBg = R.drawable.shape_r_3dp_553d_all
        val highlightBg = R.drawable.shape_btn_find_highlight

        if (findMode) {
            // Find mode: highlight Find button, normal Replace button
            btnFind?.setBackgroundResource(highlightBg)
            btnReplace?.setBackgroundResource(defaultBg)
            // Hide replace input and replace action buttons
            replaceInput?.visibility = View.GONE
            btnReplaceOne?.visibility = View.GONE
            btnReplaceAll?.visibility = View.GONE
        } else {
            // Replace mode: normal Find button, highlight Replace button
            btnFind?.setBackgroundResource(defaultBg)
            btnReplace?.setBackgroundResource(highlightBg)
            // Show replace input and replace action buttons
            replaceInput?.visibility = View.VISIBLE
            btnReplaceOne?.visibility = View.VISIBLE
            btnReplaceAll?.visibility = View.VISIBLE
        }
    }

    /**
     * Perform text search using CodeEditor's built-in searcher.
     */
    private fun performSearch() {
        val query = searchInput?.text?.toString() ?: ""
        if (query.isNotEmpty()) {
            code_editor?.searcher?.search(query, EditorSearcher.SearchOptions(false, false))
        } else {
            code_editor?.searcher?.stopSearch()
        }
        updatePositionText()
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
