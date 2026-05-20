package com.termux.zerocore.activity

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.SymbolInputView
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.eclipse.tm4e.core.registry.IThemeSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class EditTextActivity : AppCompatActivity() {
    companion object {
        val TAG = EditTextActivity::class.java.simpleName
        val CODE_JAVA = "source.java"
        val CODE_KOTLIN = "source.kotlin"
        val CODE_PYTHON = "source.python"
        val CODE_XML = "text.xml"
        val CODE_HTML = "text.html.basic"
        val CODE_JavaScript = "source.js"
        val CODE_JSON = "source.json"
        val CODE_JSONC = "source.json.comments"
        val CODE_CSS = "source.css"
        val CODE_YAML = "source.yaml"
        val CODE_TOML = "source.toml"
        val CODE_PROPERTIES = "source.properties"
        val CODE_SHELL = "source.shell"
        val CODE_DIFF = "source.diff"
        val CODE_MARK_DOWN = "text.html.markdown"
        val CODE_LUA = "source.lua"
        const val MAX_SIDEBAR_SEARCH_RESULTS = 500
        const val MAX_FILE_TREE_ITEMS = 1000
        const val SYMBOL_PREF_NAME = "zero_editor_symbol_input"
        const val SYMBOL_PREF_CUSTOM = "custom_symbols"
        const val EDITOR_PREF_NAME = "zero_editor_settings"
        const val EDITOR_PREF_TAB_SIZE = "tab_size"
        const val EDITOR_PREF_THEME = "theme"
        const val EDITOR_PREF_FONT_PATH = "font_path"
        const val DEFAULT_TAB_SIZE = 4
        const val DIRTY_CHECK_INTERVAL = 600L
        const val REQUEST_EDITOR_FONT_FILE = 1001
    }

    private data class SidebarSearchMatch(
        val line: Int,
        val column: Int,
        val startOffset: Int,
        val endOffset: Int,
        val matchText: String,
        val preview: String
    )

    private data class FileTreeNode(val file: File, val depth: Int)

    private data class EditorTab(
        val file: File,
        var content: String,
        var savedContent: String,
        var dirty: Boolean = false,
        val previewOnly: Boolean = false,
        var svgPreviewMode: Boolean = false,
        var markdownPreviewMode: Boolean = false
    )

    private var mEditText: EditText? = null
    private var mSaveText: TextView? = null
    private var mCancelText: TextView? = null
    private var code_editor: CodeEditor? = null

    private var mEditorMenuText: TextView? = null
    private var mEditorTabsContainer: LinearLayout? = null
    private var mEditorSidebar: LinearLayout? = null
    private var mSidebarFileTab: TextView? = null
    private var mSidebarSearchTab: TextView? = null
    private var mSidebarFilePanel: LinearLayout? = null
    private var mSidebarSearchPanel: LinearLayout? = null
    private var mSidebarFilePath: TextView? = null
    private var mSidebarFileTree: ListView? = null
    private var mSidebarSearchInput: EditText? = null
    private var mSidebarReplaceInput: EditText? = null
    private var mSidebarSearchCount: TextView? = null
    private var mSidebarSearchResults: ListView? = null
    private var mSidebarRegexToggle: TextView? = null
    private var mSidebarCaseToggle: TextView? = null
    private var mSidebarPrev: TextView? = null
    private var mSidebarNext: TextView? = null
    private var mSidebarReplaceOne: TextView? = null
    private var mSidebarReplaceAll: TextView? = null
    private var mEditorSymbolInput: SymbolInputView? = null
    private var mEditorSettings: TextView? = null
    private var mEditorPreviewContainer: RelativeLayout? = null
    private var mEditorImagePreview: ImageView? = null
    private var mEditorSvgPreview: WebView? = null
    private var mEditorSvgModeToggle: TextView? = null

    private var currentFile: File? = null
    private var fileTreeRoot: File? = null
    private var isDirty = false
    private var currentThemeName = "vscode_dark"
    private var currentTabSize = DEFAULT_TAB_SIZE
    private var currentFontPath = ""
    private var editorSettingsFontInput: EditText? = null
    private var currentSidebarMatchIndex = -1
    private var isRegexSearch = false
    private var isMatchCase = false
    private val dirtyCheckHandler = Handler(Looper.getMainLooper())
    private val editorTabs = ArrayList<EditorTab>()
    private val dirtyCheckRunnable = object : Runnable {
        override fun run() {
            updateDirtyState()
            dirtyCheckHandler.postDelayed(this, DIRTY_CHECK_INTERVAL)
        }
    }

    private val sidebarSearchMatches = ArrayList<SidebarSearchMatch>()
    private val sidebarSearchResultItems = ArrayList<String>()
    private var sidebarSearchAdapter: ArrayAdapter<String>? = null

    private val fileTreeNodes = ArrayList<FileTreeNode>()
    private val fileTreeItems = ArrayList<String>()
    private val expandedDirectories = LinkedHashSet<String>()
    private var fileTreeAdapter: ArrayAdapter<String>? = null

    private val defaultSymbolConfig = listOf(
        "Tab" to "\t",
        "{" to "{",
        "}" to "}",
        "(" to "(",
        ")" to ")",
        "[" to "[",
        "]" to "]",
        "<" to "<",
        ">" to ">",
        "=" to "=",
        "\"" to "\"",
        "'" to "'",
        "/" to "/",
        ":" to ":",
        ";" to ";"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text)
        initViews()

        val stringExtra = intent.getStringExtra("edit_path")
        if (stringExtra == null || stringExtra.isEmpty()) {
            finish()
            return
        }
        val file = File(stringExtra)
        if (!file.exists() || !canOpenFile(file)) {
            finish()
            return
        }

        loadEditorSettings()
        setupTextmate()
        ensureTextmateTheme()

        val ztUserBean = UserSetManage.get().getZTUserBean()
        code_editor?.isWordwrap = ztUserBean.isEditorWordWrap
        applyEditorFont(false)

        initEditorTopBar()
        initSymbolInput()
        initSidebar()
        loadFile(file)
        initFileTree(file.parentFile ?: file)
        dirtyCheckHandler.postDelayed(dirtyCheckRunnable, DIRTY_CHECK_INTERVAL)
    }

    private fun initViews() {
        mEditText = findViewById(R.id.edit_text)
        mCancelText = findViewById(R.id.cancel)
        code_editor = findViewById(R.id.code_editor)
        mSaveText = findViewById(R.id.ok)
        mEditorMenuText = findViewById(R.id.editor_menu)
        mEditorTabsContainer = findViewById(R.id.editor_tabs_container)
        mEditorSidebar = findViewById(R.id.editor_sidebar)
        mSidebarFileTab = findViewById(R.id.sidebar_file_tab)
        mSidebarSearchTab = findViewById(R.id.sidebar_search_tab)
        mSidebarFilePanel = findViewById(R.id.sidebar_file_panel)
        mSidebarSearchPanel = findViewById(R.id.sidebar_search_panel)
        mSidebarFilePath = findViewById(R.id.sidebar_file_path)
        mSidebarFileTree = findViewById(R.id.sidebar_file_tree)
        mSidebarSearchInput = findViewById(R.id.sidebar_search_input)
        mSidebarReplaceInput = findViewById(R.id.sidebar_replace_input)
        mSidebarSearchCount = findViewById(R.id.sidebar_search_count)
        mSidebarSearchResults = findViewById(R.id.sidebar_search_results)
        mSidebarRegexToggle = findViewById(R.id.sidebar_regex_toggle)
        mSidebarCaseToggle = findViewById(R.id.sidebar_case_toggle)
        mSidebarPrev = findViewById(R.id.sidebar_prev)
        mSidebarNext = findViewById(R.id.sidebar_next)
        mSidebarReplaceOne = findViewById(R.id.sidebar_replace_one)
        mSidebarReplaceAll = findViewById(R.id.sidebar_replace_all)
        mEditorSymbolInput = findViewById(R.id.editor_symbol_input)
        mEditorSettings = findViewById(R.id.editor_settings)
        mEditorPreviewContainer = findViewById(R.id.editor_preview_container)
        mEditorImagePreview = findViewById(R.id.editor_image_preview)
        mEditorSvgPreview = findViewById(R.id.editor_svg_preview)
        mEditorSvgModeToggle = findViewById(R.id.editor_svg_mode_toggle)
        configurePreviewWebView()
    }

    private fun configurePreviewWebView() {
        mEditorSvgPreview?.settings?.apply {
            javaScriptEnabled = false
            domStorageEnabled = false
            allowFileAccess = true
            allowContentAccess = false
            builtInZoomControls = true
            displayZoomControls = false
            defaultTextEncodingName = "UTF-8"
        }
        mEditorSvgPreview?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return true
            }
        }
    }

    private fun initEditorTopBar() {
        mEditorMenuText?.setOnClickListener {
            toggleSidebar()
        }
        mCancelText?.setOnClickListener {
            confirmExitIfDirty()
        }
        mSaveText?.setOnClickListener {
            saveFile()
        }
        mEditorSettings?.setOnClickListener {
            showEditorSettingsDialog()
        }
        mEditorSvgModeToggle?.setOnClickListener {
            togglePreviewMode()
        }
    }

    private fun loadEditorSettings() {
        val prefs = getSharedPreferences(EDITOR_PREF_NAME, Context.MODE_PRIVATE)
        currentTabSize = prefs.getInt(EDITOR_PREF_TAB_SIZE, DEFAULT_TAB_SIZE).coerceIn(2, 8)
        currentThemeName = prefs.getString(EDITOR_PREF_THEME, "vscode_dark") ?: "vscode_dark"
        currentFontPath = prefs.getString(EDITOR_PREF_FONT_PATH, "")?.trim().orEmpty()
    }

    private fun showEditorSettingsDialog() {
        val tabSizeInput = EditText(this).apply {
            setText(currentTabSize.toString())
            hint = getString(R.string.editor_settings_tab_size)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
            setHintTextColor(0xff9d9d9d.toInt())
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val themeItems = arrayOf("vscode_dark", "darcula", "abyss", "quietlight", "solarized_drak")
        val themeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@EditTextActivity, android.R.layout.simple_spinner_dropdown_item, themeItems)
            setSelection(themeItems.indexOf(currentThemeName).takeIf { it >= 0 } ?: 0)
        }
        val fontPathInput = EditText(this).apply {
            setText(currentFontPath)
            hint = getString(R.string.editor_settings_font_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            setTextColor(ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
            setHintTextColor(0xff9d9d9d.toInt())
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        editorSettingsFontInput = fontPathInput
        val fontSelectButton = TextView(this).apply {
            text = getString(R.string.editor_settings_font_select)
            setTextColor(0xffd4d4d4.toInt())
            textSize = 14f
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundResource(R.drawable.shape_editor_symbol_key)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            ).apply {
                setMargins(0, dp(8), 0, 0)
            }
            setOnClickListener { openEditorFontPicker() }
        }
        val symbolSettingsButton = TextView(this).apply {
            text = getString(R.string.editor_settings_symbols_edit)
            setTextColor(0xffd4d4d4.toInt())
            textSize = 14f
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundResource(R.drawable.shape_editor_symbol_key)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            ).apply {
                setMargins(0, dp(8), 0, 0)
            }
            setOnClickListener { showSymbolCustomizeDialog() }
        }
        val lspTodoLabel = TextView(this).apply {
            text = getString(R.string.editor_settings_lsp_download_desc)
            setTextColor(0xff9d9d9d.toInt())
            textSize = 12f
            setPadding(0, 0, 0, dp(6))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(14), dp(28), 0)
            addView(buildSettingsLabel(getString(R.string.editor_settings_tab_size)))
            addView(tabSizeInput)
            addView(buildSettingsLabel(getString(R.string.editor_settings_theme)))
            addView(themeSpinner)
            addView(buildSettingsLabel(getString(R.string.editor_settings_font)))
            addView(fontPathInput)
            addView(fontSelectButton)
            addView(buildSettingsLabel(getString(R.string.editor_settings_symbols)))
            addView(symbolSettingsButton)
            addView(buildSettingsLabel(getString(R.string.editor_settings_lsp)))
            addView(lspTodoLabel)
        }
        val scrollView = ScrollView(this).apply {
            addView(container)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.editor_settings)
            .setView(scrollView)
            .setPositiveButton(R.string.editor_symbol_customize_save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.editor_symbol_customize_reset, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newFontPath = fontPathInput.text.toString().trim()
                if (newFontPath.isNotEmpty() && createEditorTypeface(newFontPath) == null) {
                    UUtils.showMsg(getString(R.string.editor_settings_font_invalid))
                    return@setOnClickListener
                }
                currentTabSize = tabSizeInput.text.toString().toIntOrNull()?.coerceIn(2, 8) ?: DEFAULT_TAB_SIZE
                currentThemeName = themeSpinner.selectedItem.toString()
                currentFontPath = newFontPath
                val editor = getSharedPreferences(EDITOR_PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putInt(EDITOR_PREF_TAB_SIZE, currentTabSize)
                    .putString(EDITOR_PREF_THEME, currentThemeName)
                    .putString(EDITOR_PREF_FONT_PATH, currentFontPath)
                editor.apply()
                applyEditorTabSize()
                applyEditorTheme(currentThemeName)
                applyEditorFont(true)
                reloadCurrentEditorLanguage()
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                currentTabSize = DEFAULT_TAB_SIZE
                currentThemeName = "vscode_dark"
                currentFontPath = ""
                getSharedPreferences(EDITOR_PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                applyEditorTabSize()
                applyEditorTheme(currentThemeName)
                applyEditorFont(false)
                reloadCurrentEditorLanguage()
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            editorSettingsFontInput = null
        }
        dialog.show()
    }

    private fun openEditorFontPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "font/ttf",
                    "font/otf",
                    "application/font-sfnt",
                    "application/x-font-ttf",
                    "application/x-font-otf",
                    "application/octet-stream"
                )
            )
        }
        try {
            startActivityForResult(intent, REQUEST_EDITOR_FONT_FILE)
        } catch (_: Exception) {
            UUtils.showMsg(getString(R.string.editor_settings_font_picker_error))
        }
    }

    private fun copyEditorFontFile(uri: Uri): String? {
        val extension = editorFontExtension(uri)
        val fontDir = File(filesDir, "editor_font")
        if (!fontDir.exists() && !fontDir.mkdirs()) return null
        val targetFile = File(fontDir, "custom_editor_font.$extension")
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (createEditorTypeface(targetFile.absolutePath) == null) {
                targetFile.delete()
                null
            } else {
                targetFile.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun editorFontExtension(uri: Uri): String {
        val segment = uri.lastPathSegment.orEmpty().lowercase(Locale.ROOT)
        return when {
            segment.endsWith(".otf") -> "otf"
            else -> "ttf"
        }
    }

    private fun applyEditorFont(showError: Boolean) {
        val typeface = if (currentFontPath.isBlank()) {
            Typeface.MONOSPACE
        } else {
            createEditorTypeface(currentFontPath) ?: run {
                if (showError) UUtils.showMsg(getString(R.string.editor_settings_font_invalid))
                Typeface.MONOSPACE
            }
        }
        setCodeEditorTypeface(typeface)
    }

    private fun createEditorTypeface(fontPath: String): Typeface? {
        val file = File(fontPath)
        if (!file.isFile || file.length() <= 0) return null
        return try {
            Typeface.createFromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun setCodeEditorTypeface(typeface: Typeface) {
        val editor = code_editor ?: return
        var applied = false
        val methodNames = arrayOf(
            "setTypefaceText",
            "setTypefaceLineNumber",
            "setTypefaceBold",
            "setTypeface"
        )
        methodNames.forEach { name ->
            try {
                editor.javaClass.getMethod(name, Typeface::class.java).invoke(editor, typeface)
                applied = true
            } catch (_: Exception) {
            }
        }
        val fieldNames = arrayOf("typefaceText", "typefaceLineNumber", "typefaceBold")
        fieldNames.forEach { name ->
            try {
                val field = editor.javaClass.getField(name)
                field.set(editor, typeface)
                applied = true
            } catch (_: Exception) {
            }
        }
        if (applied) {
            editor.invalidate()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun buildSettingsLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(0xffd4d4d4.toInt())
            textSize = 12f
            setPadding(0, 12, 0, 6)
        }
    }

    private fun applyEditorTheme(themeName: String) {
        currentThemeName = themeName
        ThemeRegistry.getInstance().setTheme(themeName)
        ensureTextmateTheme()
    }

    private fun applyEditorTabSize() {
        val editor = code_editor ?: return
        val methodNames = arrayOf("setTabWidth", "setTabSize", "setTabLength")
        for (name in methodNames) {
            try {
                editor.javaClass.getMethod(name, Integer.TYPE).invoke(editor, currentTabSize)
                return
            } catch (_: Exception) {
            }
        }
        try {
            val props = editor.javaClass.getMethod("getProps").invoke(editor)
            val field = props.javaClass.getField("tabWidth")
            field.setInt(props, currentTabSize)
        } catch (_: Exception) {
        }
    }

    private fun initSymbolInput() {
        val editor = code_editor ?: return
        mEditorSymbolInput?.apply {
            orientation = LinearLayout.HORIZONTAL
            bindEditor(editor)
        }
        applyEditorTabSize()
        applySymbolConfig(loadSymbolConfig())
    }

    private fun loadSymbolConfig(): List<Pair<String, String>> {
        val config = getSharedPreferences(SYMBOL_PREF_NAME, Context.MODE_PRIVATE)
            .getString(SYMBOL_PREF_CUSTOM, null)
        if (config.isNullOrBlank()) return defaultSymbolConfig
        return parseSymbolConfig(config).ifEmpty { defaultSymbolConfig }
    }

    private fun defaultSymbolConfigText(): String {
        return symbolConfigToJson(defaultSymbolConfig)
    }

    private fun symbolConfigTextForEditing(): String {
        val savedConfig = getSharedPreferences(SYMBOL_PREF_NAME, Context.MODE_PRIVATE)
            .getString(SYMBOL_PREF_CUSTOM, null)
        if (savedConfig.isNullOrBlank()) return defaultSymbolConfigText()
        return if (savedConfig.trim().startsWith("[")) {
            savedConfig
        } else {
            symbolConfigToJson(parseLegacySymbolConfig(savedConfig))
        }
    }

    private fun symbolConfigToJson(symbols: List<Pair<String, String>>): String {
        val array = JSONArray()
        symbols.forEach { item ->
            if (item.first == item.second) {
                array.put(item.first)
            } else {
                array.put(JSONObject().apply {
                    put("display", item.first)
                    put("insert", encodeSymbolText(item.second))
                })
            }
        }
        return array.toString(2)
    }

    private fun applySymbolConfig(symbols: List<Pair<String, String>>) {
        val symbolInput = mEditorSymbolInput ?: return
        symbolInput.removeSymbols()
        symbolInput.addSymbols(
            symbols.map { it.first }.toTypedArray(),
            symbols.map { it.second }.toTypedArray()
        )
        symbolInput.setTextColor(0xfff2f2f2.toInt())
        symbolInput.forEachButton { button ->
            button.setAllCaps(false)
            button.minWidth = 0
            button.minimumWidth = 0
            button.minHeight = 0
            button.minimumHeight = 0
            button.textSize = 14f
            button.setTextColor(0xfff2f2f2.toInt())
            button.setPadding(dp(14), 0, dp(14), 0)
            button.setBackgroundResource(R.drawable.shape_editor_symbol_input_key)
            button.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(2), dp(6), dp(2), dp(6))
            }
        }
    }

    private fun showSymbolCustomizeDialog() {
        val editText = EditText(this).apply {
            setText(symbolConfigTextForEditing())
            hint = getString(R.string.editor_symbol_customize_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 8
            maxLines = 14
            setSingleLine(false)
            setTextColor(ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
            setHintTextColor(0xff9d9d9d.toInt())
            setPadding(18, 12, 18, 12)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 12, 40, 0)
            addView(TextView(this@EditTextActivity).apply {
                text = getString(R.string.editor_symbol_customize_summary)
                setTextColor(0xff9d9d9d.toInt())
                textSize = 12f
                setPadding(0, 0, 0, 12)
            })
            addView(editText)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.editor_symbol_customize_title)
            .setView(container)
            .setPositiveButton(R.string.editor_symbol_customize_save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.editor_symbol_customize_reset, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val symbols = parseSymbolConfig(editText.text.toString())
                if (symbols.isEmpty()) {
                    UUtils.showMsg(getString(R.string.editor_symbol_customize_invalid))
                    return@setOnClickListener
                }
                val normalizedConfig = symbolConfigToJson(symbols)
                getSharedPreferences(SYMBOL_PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(SYMBOL_PREF_CUSTOM, normalizedConfig)
                    .apply()
                editText.setText(normalizedConfig)
                applySymbolConfig(symbols)
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                getSharedPreferences(SYMBOL_PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(SYMBOL_PREF_CUSTOM)
                    .apply()
                applySymbolConfig(defaultSymbolConfig)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun parseSymbolConfig(config: String): List<Pair<String, String>> {
        val trimConfig = config.trim()
        if (trimConfig.startsWith("[")) {
            return parseSymbolArrayConfig(trimConfig)
        }
        return parseLegacySymbolConfig(config)
    }

    private fun parseSymbolArrayConfig(config: String): List<Pair<String, String>> {
        return try {
            val array = JSONArray(config)
            val symbols = ArrayList<Pair<String, String>>()
            for (index in 0 until array.length()) {
                val item = array.get(index)
                when (item) {
                    is JSONObject -> {
                        val display = item.optString("display", item.optString("key", "")).trim()
                        val insert = item.optString("insert", display)
                        if (display.isNotEmpty()) symbols.add(display to decodeSymbolText(insert))
                    }
                    is JSONArray -> {
                        val display = item.optString(0, "").trim()
                        val insert = item.optString(1, display)
                        if (display.isNotEmpty()) symbols.add(display to decodeSymbolText(insert))
                    }
                    else -> {
                        val display = item.toString().trim()
                        if (display.isNotEmpty()) symbols.add(display to display)
                    }
                }
            }
            symbols
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseLegacySymbolConfig(config: String): List<Pair<String, String>> {
        return config.lines().mapNotNull { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@mapNotNull null
            if (line == "=") return@mapNotNull "=" to "="
            val separator = line.indexOf('=')
            val display = if (separator >= 0) line.substring(0, separator).trim() else line
            val insert = if (separator >= 0) line.substring(separator + 1) else line
            if (display.isEmpty()) null else display to decodeSymbolText(insert)
        }
    }

    private fun decodeSymbolText(text: String): String {
        val builder = StringBuilder()
        var escaping = false
        for (char in text) {
            if (escaping) {
                builder.append(
                    when (char) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        's' -> ' '
                        '\\' -> '\\'
                        else -> char
                    }
                )
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else {
                builder.append(char)
            }
        }
        if (escaping) builder.append('\\')
        return builder.toString()
    }

    private fun encodeSymbolText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun loadFile(file: File, storeCurrent: Boolean = true) {
        if (!canOpenFile(file)) return
        if (storeCurrent) storeCurrentTabState()
        try {
            val tab = findOrCreateTab(file)
            currentFile = tab.file
            isDirty = tab.dirty
            val extension = getFileExtension(file)
            Log.i(TAG, "onCreatexxxxxx extension2: $extension")
            if (tab.previewOnly || isTextPreviewMode(tab)) {
                showPreviewTab(tab)
            } else {
                showEditorTab(tab, extension)
            }
            renderEditorTabs()
            updatePositionText()
            updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
            updateDirtyState()
        } catch (e: Exception) {
            UUtils.showMsg(e.message ?: UUtils.getString(R.string.save_error_))
        }
    }

    private fun findOrCreateTab(file: File): EditorTab {
        val absolutePath = file.absolutePath
        editorTabs.firstOrNull { it.file.absolutePath == absolutePath }?.let { return it }
        val extension = getFileExtension(file)
        return when {
            isBitmapImageFile(extension) -> EditorTab(file, "", "", previewOnly = true, svgPreviewMode = true)
            isSvgFile(extension) -> {
                val content = file.readText()
                EditorTab(file, content, content, svgPreviewMode = true)
            }
            else -> {
                val content = file.readText()
                EditorTab(file, content, content)
            }
        }.also {
            editorTabs.add(it)
        }
    }

    private fun canOpenFile(file: File): Boolean {
        if (!file.isFile) return false
        val extension = getFileExtension(file)
        if (isBitmapImageFile(extension) || isSvgFile(extension) || isLikelyTextFile(file, extension)) return true
        UUtils.showMsg(getString(R.string.editor_file_not_text))
        return false
    }

    private fun getFileExtension(file: File): String {
        return if (file.name.contains('.')) file.name.substringAfterLast('.').lowercase(Locale.ROOT) else ""
    }

    private fun isBitmapImageFile(extension: String): Boolean {
        return extension in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
    }

    private fun isSvgFile(extension: String): Boolean {
        return extension == "svg"
    }

    private fun isMarkdownFile(extension: String): Boolean {
        return extension in setOf("md", "markdown", "mdown")
    }

    private fun isTextPreviewMode(tab: EditorTab): Boolean {
        return tab.svgPreviewMode || tab.markdownPreviewMode
    }

    private fun canTogglePreview(extension: String): Boolean {
        return isSvgFile(extension) || isMarkdownFile(extension)
    }

    private fun isLikelyTextFile(file: File, extension: String): Boolean {
        val textExtensions = setOf(
            "txt", "log", "md", "markdown", "mdown", "json", "json5", "jsonc", "webmanifest", "code-workspace",
            "xml", "html", "htm", "shtml", "xhtml", "xaml", "plist", "vue", "css", "scss", "sass", "less",
            "js", "mjs", "cjs", "ts", "tsx", "jsx", "java", "kt", "kts", "py", "pyw", "rb", "php", "go", "rs",
            "c", "cc", "cpp", "cxx", "h", "hpp", "sh", "bash", "zsh", "fish", "lua", "sql", "toml", "yaml",
            "yml", "ini", "cfg", "conf", "properties", "prop", "gradle", "gitignore", "editorconfig", "env", "diff", "patch"
        )
        if (extension in textExtensions) return true
        return try {
            val buffer = ByteArray(4096)
            val count = file.inputStream().use { it.read(buffer) }
            if (count <= 0) return true
            var controlCount = 0
            for (index in 0 until count) {
                val value = buffer[index].toInt() and 0xff
                if (value == 0) return false
                if (value < 0x09 || (value > 0x0d && value < 0x20)) controlCount++
            }
            controlCount * 100 / count < 5
        } catch (_: Exception) {
            false
        }
    }

    private fun showEditorTab(tab: EditorTab, extension: String) {
        mEditorPreviewContainer?.visibility = View.GONE
        mEditorImagePreview?.visibility = View.GONE
        mEditorSvgPreview?.visibility = View.GONE
        code_editor?.visibility = View.VISIBLE
        updatePreviewModeToggle(extension, false)
        code_editor?.setEditorLanguage(createEditorLanguage(extension))
        code_editor?.setText(tab.content)
    }

    private fun createEditorLanguage(extension: String): Language {
        return TextMateLanguage.create(getCodeType(extension), true)
    }

    private fun reloadCurrentEditorLanguage() {
        storeCurrentTabState()
        val tab = currentTab() ?: return
        if (tab.previewOnly || isTextPreviewMode(tab)) return
        val extension = getFileExtension(tab.file)
        code_editor?.setEditorLanguage(createEditorLanguage(extension))
    }

    private fun showPreviewTab(tab: EditorTab) {
        val extension = getFileExtension(tab.file)
        code_editor?.visibility = View.GONE
        mEditorPreviewContainer?.visibility = View.VISIBLE
        mEditorImagePreview?.visibility = View.GONE
        mEditorSvgPreview?.visibility = View.GONE
        updatePreviewModeToggle(extension, true)
        when {
            isSvgFile(extension) -> {
                mEditorSvgPreview?.visibility = View.VISIBLE
                mEditorSvgPreview?.setBackgroundColor(0xff1e1e1e.toInt())
                mEditorSvgPreview?.loadDataWithBaseURL(null, buildSvgPreviewHtml(tab.content), "text/html", "UTF-8", null)
            }
            isMarkdownFile(extension) -> {
                mEditorSvgPreview?.visibility = View.VISIBLE
                mEditorSvgPreview?.setBackgroundColor(0xff1e1e1e.toInt())
                mEditorSvgPreview?.loadDataWithBaseURL(
                    tab.file.parentFile?.toURI()?.toString(),
                    buildMarkdownPreviewHtml(tab.content),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
            else -> {
                mEditorImagePreview?.visibility = View.VISIBLE
                mEditorImagePreview?.setImageURI(Uri.fromFile(tab.file))
            }
        }
    }

    private fun updatePreviewModeToggle(extension: String, previewMode: Boolean) {
        mEditorSvgModeToggle?.visibility = if (canTogglePreview(extension)) View.VISIBLE else View.GONE
        mEditorSvgModeToggle?.text = when {
            isSvgFile(extension) && previewMode -> getString(R.string.editor_svg_edit_mode)
            isSvgFile(extension) -> getString(R.string.editor_svg_preview_mode)
            isMarkdownFile(extension) && previewMode -> getString(R.string.editor_markdown_edit_mode)
            isMarkdownFile(extension) -> getString(R.string.editor_markdown_preview_mode)
            else -> ""
        }
    }

    private fun buildSvgPreviewHtml(svgContent: String): String {
        val encodedSvg = Base64.encodeToString(svgContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return """
            <!doctype html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    html, body { margin: 0; width: 100%; height: 100%; background: #1E1E1E; }
                    body { display: flex; align-items: center; justify-content: center; overflow: hidden; }
                    img { max-width: calc(100% - 24px); max-height: calc(100% - 24px); }
                </style>
            </head>
            <body><img src="data:image/svg+xml;base64,$encodedSvg" /></body>
            </html>
        """.trimIndent()
    }

    private fun buildMarkdownPreviewHtml(markdownContent: String): String {
        val document = Parser.builder().build().parse(markdownContent)
        val bodyHtml = HtmlRenderer.builder().escapeHtml(true).build().render(document)
        return """
            <!doctype html>
            <html>
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    html, body { margin: 0; min-height: 100%; background: #1E1E1E; color: #D4D4D4; }
                    body { box-sizing: border-box; padding: 18px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; font-size: 15px; line-height: 1.65; }
                    .markdown-body { max-width: 920px; margin: 0 auto; }
                    h1, h2, h3, h4, h5, h6 { color: #FFFFFF; line-height: 1.25; margin: 1.3em 0 0.55em; }
                    h1 { font-size: 1.9em; padding-bottom: 0.3em; border-bottom: 1px solid #3C3C3C; }
                    h2 { font-size: 1.55em; padding-bottom: 0.25em; border-bottom: 1px solid #333333; }
                    p, ul, ol, blockquote, pre, table { margin-top: 0; margin-bottom: 1em; }
                    a { color: #4EA1F3; }
                    img { max-width: 100%; height: auto; border-radius: 4px; }
                    blockquote { margin-left: 0; padding: 0.1em 1em; color: #BDBDBD; border-left: 4px solid #4E5A65; background: #252526; }
                    code { padding: 0.16em 0.38em; border-radius: 4px; background: #2D2D30; color: #CE9178; font-family: "Roboto Mono", "Consolas", monospace; font-size: 0.92em; }
                    pre { padding: 12px; overflow: auto; border-radius: 6px; background: #252526; border: 1px solid #333333; }
                    pre code { padding: 0; color: #D4D4D4; background: transparent; }
                    table { width: 100%; border-collapse: collapse; overflow: auto; display: block; }
                    th, td { border: 1px solid #3C3C3C; padding: 6px 10px; }
                    th { background: #2D2D30; color: #FFFFFF; }
                    hr { border: 0; border-top: 1px solid #3C3C3C; margin: 1.5em 0; }
                </style>
            </head>
            <body><article class="markdown-body">$bodyHtml</article></body>
            </html>
        """.trimIndent()
    }

    private fun togglePreviewMode() {
        val tab = currentTab() ?: return
        val extension = getFileExtension(tab.file)
        when {
            isSvgFile(extension) -> {
                if (tab.svgPreviewMode) {
                    tab.svgPreviewMode = false
                    showEditorTab(tab, extension)
                } else {
                    storeCurrentTabState()
                    tab.svgPreviewMode = true
                    tab.markdownPreviewMode = false
                    showPreviewTab(tab)
                }
            }
            isMarkdownFile(extension) -> {
                if (tab.markdownPreviewMode) {
                    tab.markdownPreviewMode = false
                    showEditorTab(tab, extension)
                } else {
                    storeCurrentTabState()
                    tab.markdownPreviewMode = true
                    tab.svgPreviewMode = false
                    showPreviewTab(tab)
                }
            }
            else -> return
        }
        isDirty = tab.dirty
        renderEditorTabs()
        updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
    }

    private fun storeCurrentTabState() {
        val file = currentFile ?: return
        val tab = editorTabs.firstOrNull { it.file.absolutePath == file.absolutePath } ?: return
        if (tab.previewOnly || isTextPreviewMode(tab)) return
        val content = code_editor?.text?.toString() ?: ""
        tab.content = content
        tab.dirty = content != tab.savedContent
    }

    private fun currentTab(): EditorTab? {
        val file = currentFile ?: return null
        return editorTabs.firstOrNull { it.file.absolutePath == file.absolutePath }
    }

    private fun saveFile(): Boolean {
        val file = currentFile ?: return false
        val tab = currentTab() ?: return false
        if (tab.previewOnly) {
            UUtils.showMsg(getString(R.string.editor_file_not_text))
            return false
        }
        val toString = if (isTextPreviewMode(tab)) tab.content else code_editor?.text?.toString() ?: ""
        if (toString.isEmpty()) {
            UUtils.showMsg(UUtils.getString(R.string.命令不能为空))
            return false
        }
        return if (UUtils.setFileString(file, toString)) {
            currentTab()?.apply {
                content = toString
                savedContent = toString
                dirty = false
            }
            isDirty = false
            renderEditorTabs()
            UUtils.showMsg(UUtils.getString(R.string.保存成功))
            true
        } else {
            UUtils.showMsg(UUtils.getString(R.string.save_error_))
            false
        }
    }

    private fun updateDirtyState() {
        val tab = currentTab() ?: return
        if (tab.previewOnly || isTextPreviewMode(tab)) return
        val content = code_editor?.text?.toString() ?: ""
        val dirty = content != tab.savedContent
        if (tab.content != content || tab.dirty != dirty || isDirty != dirty) {
            tab.content = content
            tab.dirty = dirty
            isDirty = dirty
            renderEditorTabs()
        }
    }

    private fun renderEditorTabs() {
        val container = mEditorTabsContainer ?: return
        container.removeAllViews()
        val currentPath = currentFile?.absolutePath
        for (tab in editorTabs) {
            val active = tab.file.absolutePath == currentPath
            val tabView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, dp(8), 0)
                background = if (active) ContextCompat.getDrawable(this@EditTextActivity, R.drawable.shape_editor_tab_active) else null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(34)
                ).apply {
                    setMargins(0, dp(4), dp(6), dp(4))
                }
                setOnClickListener {
                    if (tab.file.absolutePath != currentFile?.absolutePath) {
                        loadFile(tab.file)
                    }
                }
            }
            tabView.addView(TextView(this).apply {
                text = if (tab.dirty) "●" else ""
                setTextColor(0xff89d185.toInt())
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(14), LinearLayout.LayoutParams.MATCH_PARENT)
            })
            tabView.addView(TextView(this).apply {
                text = tab.file.name
                setTextColor(if (tab.dirty) 0xff89d185.toInt() else 0xffd4d4d4.toInt())
                textSize = 13f
                setSingleLine(true)
                setMaxWidth(dp(150))
                gravity = android.view.Gravity.CENTER_VERTICAL
            })
            tabView.addView(TextView(this).apply {
                text = "×"
                setTextColor(0xff9d9d9d.toInt())
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.MATCH_PARENT)
                setOnClickListener {
                    closeEditorTab(tab)
                }
            })
            container.addView(tabView)
        }
    }

    private fun closeEditorTab(tab: EditorTab) {
        storeCurrentTabState()
        if (tab.dirty) {
            AlertDialog.Builder(this)
                .setTitle(R.string.editor_unsaved_title)
                .setMessage(getString(R.string.editor_unsaved_close_message, tab.file.name))
                .setPositiveButton(R.string.edit_save) { _, _ ->
                    if (tab.file.absolutePath == currentFile?.absolutePath) {
                        if (saveFile()) removeEditorTab(tab)
                    } else if (UUtils.setFileString(tab.file, tab.content)) {
                        tab.savedContent = tab.content
                        tab.dirty = false
                        removeEditorTab(tab)
                    }
                }
                .setNegativeButton(R.string.editor_unsaved_discard) { _, _ ->
                    removeEditorTab(tab)
                }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        } else {
            removeEditorTab(tab)
        }
    }

    private fun removeEditorTab(tab: EditorTab) {
        val wasActive = tab.file.absolutePath == currentFile?.absolutePath
        val index = editorTabs.indexOf(tab)
        editorTabs.remove(tab)
        if (editorTabs.isEmpty()) {
            finish()
            return
        }
        if (wasActive) {
            val nextIndex = index.coerceAtMost(editorTabs.lastIndex)
            currentFile = null
            loadFile(editorTabs[nextIndex].file, storeCurrent = false)
        } else {
            renderEditorTabs()
        }
    }

    private fun confirmExitIfDirty() {
        storeCurrentTabState()
        val dirtyTabs = editorTabs.filter { it.dirty }
        if (dirtyTabs.isEmpty()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.editor_unsaved_title)
            .setMessage(getString(R.string.editor_unsaved_exit_message, dirtyTabs.size))
            .setPositiveButton(R.string.edit_save) { _, _ ->
                saveDirtyTabsAndExit(dirtyTabs)
            }
            .setNegativeButton(R.string.editor_unsaved_discard) { _, _ ->
                finish()
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveDirtyTabsAndExit(dirtyTabs: List<EditorTab>) {
        for (tab in dirtyTabs) {
            if (!UUtils.setFileString(tab.file, tab.content)) {
                UUtils.showMsg(UUtils.getString(R.string.save_error_))
                return
            }
            tab.savedContent = tab.content
            tab.dirty = false
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EDITOR_FONT_FILE || resultCode != android.app.Activity.RESULT_OK) return
        val uri = data?.data ?: return
        val fontPath = copyEditorFontFile(uri)
        if (fontPath.isNullOrEmpty()) {
            UUtils.showMsg(getString(R.string.editor_settings_font_invalid))
            return
        }
        editorSettingsFontInput?.setText(fontPath)
    }

    override fun onBackPressed() {
        confirmExitIfDirty()
    }

    override fun onDestroy() {
        dirtyCheckHandler.removeCallbacks(dirtyCheckRunnable)
        super.onDestroy()
    }

    private fun toggleSidebar() {
        val sidebar = mEditorSidebar ?: return
        if (sidebar.visibility == View.VISIBLE) {
            sidebar.visibility = View.GONE
        } else {
            sidebar.visibility = View.VISIBLE
            showFilePanel()
        }
    }

    private fun initSidebar() {
        mSidebarFileTab?.setOnClickListener { showFilePanel() }
        mSidebarSearchTab?.setOnClickListener { showSearchPanel() }
        initFileTreeAdapter()
        initSidebarSearch()
        updateSearchToggles()
    }

    private fun showFilePanel() {
        mSidebarFilePanel?.visibility = View.VISIBLE
        mSidebarSearchPanel?.visibility = View.GONE
        mSidebarFileTab?.setTextColor(ContextCompat.getColor(this, R.color.color_ffffff))
        mSidebarSearchTab?.setTextColor(0xff9d9d9d.toInt())
    }

    private fun showSearchPanel() {
        mSidebarFilePanel?.visibility = View.GONE
        mSidebarSearchPanel?.visibility = View.VISIBLE
        mSidebarFileTab?.setTextColor(0xff9d9d9d.toInt())
        mSidebarSearchTab?.setTextColor(ContextCompat.getColor(this, R.color.color_ffffff))
        mSidebarSearchInput?.requestFocus()
        updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
    }

    private fun initFileTree(root: File) {
        fileTreeRoot = if (root.isDirectory) root else root.parentFile
        fileTreeRoot?.absolutePath?.let { expandedDirectories.add(it) }
        mSidebarFilePath?.text = fileTreeRoot?.absolutePath ?: ""
        refreshFileTree()
    }

    private fun initFileTreeAdapter() {
        fileTreeAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileTreeItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val node = fileTreeNodes.getOrNull(position)
                textView.setTextColor(if (node?.file?.isDirectory == true) 0xffdcdcaa.toInt() else ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
                textView.textSize = 13f
                val depthPadding = node?.let { dp(14 + it.depth * 18) } ?: dp(10)
                textView.setPadding(depthPadding, dp(10), dp(10), dp(10))
                return view
            }
        }
        mSidebarFileTree?.adapter = fileTreeAdapter
        mSidebarFileTree?.setOnItemClickListener { _, _, position, _ ->
            val node = fileTreeNodes.getOrNull(position) ?: return@setOnItemClickListener
            if (node.file.isDirectory) {
                val path = node.file.absolutePath
                if (expandedDirectories.contains(path)) {
                    expandedDirectories.remove(path)
                } else {
                    expandedDirectories.add(path)
                }
                refreshFileTree()
            } else if (canOpenFile(node.file)) {
                loadFile(node.file)
            }
        }
    }

    private fun refreshFileTree() {
        fileTreeNodes.clear()
        fileTreeItems.clear()
        val root = fileTreeRoot
        if (root == null || !root.isDirectory) {
            fileTreeItems.add(getString(R.string.editor_sidebar_file_empty))
            fileTreeAdapter?.notifyDataSetChanged()
            return
        }
        addFileTreeChildren(root, 0)
        if (fileTreeItems.isEmpty()) {
            fileTreeItems.add(getString(R.string.editor_sidebar_file_empty))
        }
        fileTreeAdapter?.notifyDataSetChanged()
    }

    private fun addFileTreeChildren(directory: File, depth: Int) {
        if (fileTreeNodes.size >= MAX_FILE_TREE_ITEMS) return
        val files = directory.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }) ?: return
        for (file in files) {
            if (fileTreeNodes.size >= MAX_FILE_TREE_ITEMS) return
            fileTreeNodes.add(FileTreeNode(file, depth))
            fileTreeItems.add(formatFileTreeItem(file))
            if (file.isDirectory && expandedDirectories.contains(file.absolutePath)) {
                addFileTreeChildren(file, depth + 1)
            }
        }
    }

    private fun formatFileTreeItem(file: File): String {
        return if (file.isDirectory) {
            val icon = if (expandedDirectories.contains(file.absolutePath)) "▾" else "▸"
            "$icon  ${file.name}/"
        } else {
            "  ${file.name}"
        }
    }

    private fun initSidebarSearch() {
        sidebarSearchAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sidebarSearchResultItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
                textView.textSize = 13f
                textView.setPadding(10, 10, 10, 10)
                return view
            }
        }
        mSidebarSearchResults?.adapter = sidebarSearchAdapter
        mSidebarSearchResults?.setOnItemClickListener { _, _, position, _ ->
            navigateSidebarSearchResult(position)
        }

        mSidebarSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSidebarSearch(s?.toString() ?: "")
            }
        })
        mSidebarSearchInput?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                navigateSearchDelta(1)
                true
            } else {
                false
            }
        }
        mSidebarRegexToggle?.setOnClickListener {
            isRegexSearch = !isRegexSearch
            updateSearchToggles()
            updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
        }
        mSidebarCaseToggle?.setOnClickListener {
            isMatchCase = !isMatchCase
            updateSearchToggles()
            updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
        }
        mSidebarPrev?.setOnClickListener { navigateSearchDelta(-1) }
        mSidebarNext?.setOnClickListener { navigateSearchDelta(1) }
        mSidebarReplaceOne?.setOnClickListener { replaceCurrentMatch() }
        mSidebarReplaceAll?.setOnClickListener { replaceAllMatches() }
    }

    private fun updateSearchToggles() {
        val activeBg = R.drawable.shape_btn_find_highlight
        val defaultBg = R.drawable.shape_r_3dp_553d_all
        mSidebarRegexToggle?.setBackgroundResource(if (isRegexSearch) activeBg else defaultBg)
        mSidebarCaseToggle?.setBackgroundResource(if (isMatchCase) activeBg else defaultBg)
    }

    private fun updateSidebarSearch(query: String) {
        sidebarSearchMatches.clear()
        sidebarSearchResultItems.clear()
        currentSidebarMatchIndex = -1
        val tab = currentTab()
        if (tab == null || tab.previewOnly || isTextPreviewMode(tab)) {
            mSidebarSearchCount?.text = getString(R.string.editor_sidebar_search_empty)
            sidebarSearchAdapter?.notifyDataSetChanged()
            return
        }
        val searchText = query.trim()
        if (searchText.isEmpty()) {
            mSidebarSearchCount?.text = getString(R.string.editor_sidebar_search_empty)
            sidebarSearchAdapter?.notifyDataSetChanged()
            return
        }

        val pattern = createSearchPattern(searchText) ?: return
        val text = code_editor?.text?.toString() ?: ""
        val lineStarts = buildLineStarts(text)
        val matcher = pattern.matcher(text)
        while (matcher.find() && sidebarSearchMatches.size < MAX_SIDEBAR_SEARCH_RESULTS) {
            if (matcher.start() == matcher.end()) continue
            val lineColumn = offsetToLineColumn(lineStarts, matcher.start())
            val preview = buildSidebarSearchPreview(text, lineStarts, lineColumn.first)
            val match = SidebarSearchMatch(
                lineColumn.first,
                lineColumn.second,
                matcher.start(),
                matcher.end(),
                matcher.group(),
                preview
            )
            sidebarSearchMatches.add(match)
            sidebarSearchResultItems.add("${match.line + 1}:${match.column + 1}  ${match.preview}")
        }

        mSidebarSearchCount?.text = when {
            sidebarSearchMatches.isEmpty() -> getString(R.string.editor_sidebar_no_result)
            sidebarSearchMatches.size >= MAX_SIDEBAR_SEARCH_RESULTS ->
                getString(R.string.editor_sidebar_result_count_limited, MAX_SIDEBAR_SEARCH_RESULTS)
            else -> getString(R.string.editor_sidebar_result_count, sidebarSearchMatches.size)
        }
        sidebarSearchAdapter?.notifyDataSetChanged()
    }

    private fun createSearchPattern(searchText: String): Pattern? {
        return try {
            val flags = if (isMatchCase) 0 else Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
            Pattern.compile(if (isRegexSearch) searchText else Pattern.quote(searchText), flags)
        } catch (e: PatternSyntaxException) {
            mSidebarSearchCount?.text = getString(R.string.editor_sidebar_invalid_regex, e.description ?: "")
            sidebarSearchAdapter?.notifyDataSetChanged()
            null
        }
    }

    private fun buildLineStarts(text: String): IntArray {
        val starts = ArrayList<Int>()
        starts.add(0)
        text.forEachIndexed { index, c ->
            if (c == '\n' && index + 1 < text.length) {
                starts.add(index + 1)
            }
        }
        return starts.toIntArray()
    }

    private fun offsetToLineColumn(lineStarts: IntArray, offset: Int): Pair<Int, Int> {
        val searchIndex = lineStarts.binarySearch(offset)
        val line = if (searchIndex >= 0) searchIndex else (-searchIndex - 2).coerceAtLeast(0)
        return Pair(line, offset - lineStarts[line])
    }

    private fun buildSidebarSearchPreview(text: String, lineStarts: IntArray, line: Int): String {
        val lineStart = lineStarts.getOrElse(line) { 0 }
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val rawLine = text.substring(lineStart, lineEnd).replace("\r", "")
        val trimLine = rawLine.trim().replace('\t', ' ')
        val preview = if (trimLine.isEmpty()) rawLine.replace('\t', ' ') else trimLine
        return if (preview.length > 120) preview.substring(0, 120) + "…" else preview
    }

    private fun navigateSearchDelta(delta: Int) {
        if (sidebarSearchMatches.isEmpty()) return
        currentSidebarMatchIndex = if (currentSidebarMatchIndex < 0) {
            if (delta < 0) sidebarSearchMatches.lastIndex else 0
        } else {
            (currentSidebarMatchIndex + delta + sidebarSearchMatches.size) % sidebarSearchMatches.size
        }
        navigateSidebarSearchResult(currentSidebarMatchIndex)
    }

    private fun navigateSidebarSearchResult(position: Int) {
        if (position < 0 || position >= sidebarSearchMatches.size) return
        currentSidebarMatchIndex = position
        val match = sidebarSearchMatches[position]
        val searcher = code_editor?.searcher ?: return
        searcher.stopSearch()
        val highlightText = if (isRegexSearch) match.matchText else mSidebarSearchInput?.text?.toString()?.trim().orEmpty()
        if (highlightText.isNotEmpty()) {
            searcher.search(highlightText, EditorSearcher.SearchOptions(false, false))
            val previousCount = countLiteralOccurrencesBefore(highlightText, match.startOffset)
            repeat(previousCount + 1) {
                searcher.gotoNext()
            }
        }
        updatePositionText()
    }

    private fun countLiteralOccurrencesBefore(query: String, endOffset: Int): Int {
        val text = code_editor?.text?.toString() ?: return 0
        val beforeText = text.substring(0, endOffset.coerceIn(0, text.length))
        val pattern = Pattern.compile(Pattern.quote(query), if (isMatchCase) 0 else Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
        val matcher = pattern.matcher(beforeText)
        var count = 0
        while (matcher.find()) count++
        return count
    }

    private fun replaceCurrentMatch() {
        val tab = currentTab()
        if (tab == null || tab.previewOnly || isTextPreviewMode(tab)) return
        val searchText = mSidebarSearchInput?.text?.toString()?.trim() ?: ""
        if (searchText.isEmpty()) return
        if (sidebarSearchMatches.isEmpty()) updateSidebarSearch(searchText)
        val index = currentSidebarMatchIndex.takeIf { it >= 0 } ?: 0
        if (index !in sidebarSearchMatches.indices) return
        val pattern = createSearchPattern(searchText) ?: return
        val text = code_editor?.text?.toString() ?: return
        val replacement = mSidebarReplaceInput?.text?.toString() ?: ""
        val matcher = pattern.matcher(text)
        val buffer = StringBuffer()
        var matchIndex = 0
        try {
            while (matcher.find()) {
                if (matcher.start() == matcher.end()) continue
                if (matchIndex == index) {
                    matcher.appendReplacement(buffer, if (isRegexSearch) replacement else Matcher.quoteReplacement(replacement))
                    matcher.appendTail(buffer)
                    code_editor?.setText(buffer.toString())
                    updateDirtyState()
                    updateSidebarSearch(searchText)
                    if (sidebarSearchMatches.isNotEmpty()) {
                        navigateSidebarSearchResult(index.coerceAtMost(sidebarSearchMatches.lastIndex))
                    }
                    return
                }
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()))
                matchIndex++
            }
        } catch (e: IllegalArgumentException) {
            UUtils.showMsg(e.message ?: getString(R.string.save_error_))
        }
    }

    private fun replaceAllMatches() {
        val tab = currentTab()
        if (tab == null || tab.previewOnly || isTextPreviewMode(tab)) return
        val searchText = mSidebarSearchInput?.text?.toString()?.trim() ?: ""
        if (searchText.isEmpty()) return
        val pattern = createSearchPattern(searchText) ?: return
        val text = code_editor?.text?.toString() ?: return
        val replacement = mSidebarReplaceInput?.text?.toString() ?: ""
        val matcher = pattern.matcher(text)
        try {
            val replaced = matcher.replaceAll(if (isRegexSearch) replacement else Matcher.quoteReplacement(replacement))
            code_editor?.setText(replaced)
            updateDirtyState()
            updateSidebarSearch(searchText)
        } catch (e: IllegalArgumentException) {
            UUtils.showMsg(e.message ?: getString(R.string.save_error_))
        }
    }

    private fun getCodeType(extension: String) : String {
        if (extension.isEmpty()) {
            return CODE_SHELL
        }
        when (extension.lowercase(Locale.ROOT)) {
            "xml", "svg", "xhtml", "xaml", "plist" -> {
                return CODE_XML
            }
            "html", "htm", "shtml", "vue" -> {
                return CODE_HTML
            }
            "java" -> {
                return CODE_JAVA
            }
            "py", "python", "pyw" -> {
                return CODE_PYTHON
            }
            "kt", "kts", "kotlin" -> {
                return CODE_KOTLIN
            }
            "md", "markdown", "mdown" -> {
                return CODE_MARK_DOWN
            }
            "lua" -> {
                return CODE_LUA
            }
            "json", "webmanifest", "sublime-settings", "sublime-keymap", "sublime-menu", "sublime-theme", "sublime-build" -> {
                return CODE_JSON
            }
            "jsonc", "json5", "code-workspace" -> {
                return CODE_JSONC
            }
            "js", "mjs", "cjs", "jsx", "ts", "tsx" -> {
                return CODE_JavaScript
            }
            "css", "scss", "sass", "less" -> {
                return CODE_CSS
            }
            "yaml", "yml" -> {
                return CODE_YAML
            }
            "toml" -> {
                return CODE_TOML
            }
            "properties", "prop", "ini", "cfg", "conf", "env", "editorconfig" -> {
                return CODE_PROPERTIES
            }
            "sh", "bash", "zsh", "fish", "profile", "bashrc", "zshrc" -> {
                return CODE_SHELL
            }
            "diff", "patch" -> {
                return CODE_DIFF
            }
        }
        return CODE_SHELL
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
        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_drak", "vscode_dark")
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

        themeRegistry.setTheme(currentThemeName)
    }

    private fun updatePositionText() {
        val tab = currentTab()
        if (tab == null || tab.previewOnly || isTextPreviewMode(tab)) return
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
