package com.termux.zerocore.activity

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import android.net.Uri
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.dialog.LoadingDialog
import com.termux.zerocore.editor.AndroidProjectManager
import com.termux.zerocore.editor.EditorAndroidRunner
import com.termux.zerocore.editor.EditorFileTreeClipboard
import com.termux.zerocore.editor.EditorFileTreeIcon
import com.termux.zerocore.editor.EditorFileTreeListView
import com.termux.zerocore.editor.EditorFileTreeOperations
import com.termux.zerocore.editor.EditorFileTreeScrollView
import com.termux.zerocore.editor.EditorProgramRunner
import com.termux.zerocore.editor.EditorRunDetector
import com.termux.zerocore.editor.EditorRunLanguage
import com.termux.zerocore.editor.EditorTerminalInputView
import com.termux.zerocore.editor.EditorTerminalPanel
import com.termux.zerocore.editor.lsp.EditorLspLanguage
import com.termux.zerocore.editor.lsp.EditorLspManager
import com.termux.zerocore.editor.lsp.EditorLspServerAdapter
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
import com.termux.shared.view.KeyboardUtils
import com.termux.view.TerminalView
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.eclipse.tm4e.core.registry.IThemeSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedHashSet
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.abs
import kotlin.math.max

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
        val CODE_C = "source.c"
        val CODE_GO = "source.go"
        val CODE_RUST = "source.rust"
        val CODE_ZIG = "source.zig"
        val CODE_PHP = "source.php"
        const val MAX_SIDEBAR_SEARCH_RESULTS = 500
        const val MAX_FILE_TREE_ITEMS = 1000
        const val SYMBOL_PREF_NAME = "zero_editor_symbol_input"
        const val SYMBOL_PREF_CUSTOM = "custom_symbols"
        const val EDITOR_PREF_NAME = "zero_editor_settings"
        const val EDITOR_PREF_TAB_SIZE = "tab_size"
        const val EDITOR_PREF_THEME = "theme"
        const val EDITOR_PREF_FONT_PATH = "font_path"
        const val EDITOR_PREF_LSP_ENABLED = "lsp_enabled"
        const val EDITOR_PREF_LSP_TIMEOUT = "lsp_timeout"
        const val EDITOR_STATE_SIDEBAR_VISIBLE = "sidebar_visible"
        const val EDITOR_STATE_SIDEBAR_SEARCH_PANEL = "sidebar_search_panel"
        const val EDITOR_STATE_SIDEBAR_SEARCH_QUERY = "sidebar_search_query"
        const val EDITOR_STATE_SIDEBAR_REPLACE_QUERY = "sidebar_replace_query"
        const val EDITOR_STATE_SIDEBAR_REGEX = "sidebar_regex"
        const val EDITOR_STATE_SIDEBAR_MATCH_CASE = "sidebar_match_case"
        const val EDITOR_STATE_TERMINAL_VISIBLE = "terminal_visible"
        const val DEFAULT_TAB_SIZE = 4
        const val DIRTY_CHECK_INTERVAL = 600L
        const val SIDEBAR_WIDTH_DP = 280
        const val SIDEBAR_EDGE_SWIPE_DP = 28
        const val SIDEBAR_ANIMATION_DURATION = 200L
        const val SIDEBAR_FLING_VELOCITY_DP = 400
        const val REQUEST_EDITOR_FONT_FILE = 1001

        private val JAVA_HELLO_TEMPLATE = """
            public class Hello {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
        """.trimIndent()

        private val C_HELLO_TEMPLATE = """
            #include <stdio.h>

            int main(void) {
                printf("Hello, World!\n");
                return 0;
            }
        """.trimIndent()

        private val PYTHON_HELLO_TEMPLATE = """
            #!/usr/bin/env python3

            def main():
                print("Hello, World!")


            if __name__ == "__main__":
                main()
        """.trimIndent()
    }

    private data class HelloProjectSpec(
        val dirBaseName: String,
        val entryFileName: String,
        val content: String
    )

    private data class SidebarSearchMatch(
        val line: Int,
        val column: Int,
        val startOffset: Int,
        val endOffset: Int,
        val matchText: String,
        val preview: String
    )

    private enum class FileTreeEntryKind {
        NORMAL,
        GO_UP,
        NEW_MENU
    }

    private data class FileTreeNode(
        val kind: FileTreeEntryKind,
        val file: File,
        val depth: Int
    )

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

    private var mEditorMenuButton: ImageView? = null
    private var mEditorFilesButton: ImageView? = null
    private var mEditorUndoButton: ImageView? = null
    private var mEditorRedoButton: ImageView? = null
    private var mEditorMoreButton: ImageView? = null
    private var mEditorTerminalButton: ImageView? = null
    private var mEditorRunButton: ImageView? = null
    private var mEditorRunLoading: ProgressBar? = null
    private var editorTerminalPanel: EditorTerminalPanel? = null
    private var programRunner: EditorProgramRunner? = null
    private var isProgramRunInProgress = false
    private var mEditorAndroidBuildButton: ImageView? = null
    private var mEditorAndroidBuildLoading: ProgressBar? = null
    private var androidRunner: EditorAndroidRunner? = null
    private var activeAndroidProjectRoot: File? = null
    private var isAndroidBuildInProgress = false
    private var mEditorToolbar: View? = null
    private var mEditorTabBar: View? = null
    private var mEditorTabsContainer: LinearLayout? = null
    private var mEditorContentLayout: RelativeLayout? = null
    private var mEditorSymbolBar: LinearLayout? = null
    private var mEditorTerminalPanelView: View? = null
    private var mEditorSidebar: LinearLayout? = null
    private var mSidebarFileTab: TextView? = null
    private var mSidebarSearchTab: TextView? = null
    private var mSidebarFilePanel: LinearLayout? = null
    private var mSidebarSearchPanel: LinearLayout? = null
    private var mSidebarFilePath: TextView? = null
    private var mSidebarFileTreeScroll: EditorFileTreeScrollView? = null
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
    private var mSidebarMainPage: LinearLayout? = null
    private var mSidebarProjectPage: View? = null
    private var mSidebarProjectPath: TextView? = null
    private var mSidebarPageBrowserTab: TextView? = null
    private var mSidebarPageProjectTab: TextView? = null
    private var mEditorSymbolInput: SymbolInputView? = null
    private var mEditorSettings: TextView? = null
    private var mEditorPreviewContainer: RelativeLayout? = null
    private var mEditorImagePreview: ImageView? = null
    private var mEditorSvgPreview: WebView? = null
    private var mEditorSvgModeToggle: TextView? = null

    private var currentFile: File? = null
    private var fileTreeRoot: File? = null
    private var fileTreeCurrentDir: File? = null
    private var isDirty = false
    private var currentThemeName = "vscode_dark"
    private var currentTabSize = DEFAULT_TAB_SIZE
    private var currentFontPath = ""
    private var lspEnabled = false
    private var lspTimeoutMillis = EditorLspManager.DEFAULT_TIMEOUT_MILLIS
    private lateinit var lspManager: EditorLspManager
    private var editorSettingsFontInput: EditText? = null
    private var isSidebarVisible = false
    private var isSidebarSearchPanelVisible = false
    private var sidebarGestureActive = false
    private var sidebarGestureDragging = false
    private var sidebarGestureOpenedFromClosed = false
    private var sidebarGestureDownX = 0f
    private var sidebarGestureDownY = 0f
    private var sidebarGestureStartTranslation = 0f
    private var sidebarVelocityTracker: VelocityTracker? = null
    private var sidebarAnimator: ValueAnimator? = null
    private val sidebarTouchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var lspInstallRefreshRunnable: Runnable? = null
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
        loadingFun(savedInstanceState, file)
    }

    private fun loadingFun(savedInstanceState: Bundle?, file: File) {
        lspManager = EditorLspManager(applicationContext)
        programRunner = EditorProgramRunner(applicationContext)
        androidRunner = EditorAndroidRunner(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            loadEditorSettings()
            applyLspSettings()
            setupTextmate()
            val ztUserBean = UserSetManage.get().getZTUserBean()
            withContext(Dispatchers.Main) {
                ensureTextmateTheme()
                code_editor?.isWordwrap = ztUserBean.isEditorWordWrap
                applyEditorFont(false)
                initEditorTopBar()
                initEditorTerminal(savedInstanceState)
                initSymbolInput()
                initSidebar()
                restoreSidebarState(savedInstanceState)
                loadFile(file)
                initFileTree(file.parentFile ?: file)
                dirtyCheckHandler.postDelayed(dirtyCheckRunnable, DIRTY_CHECK_INTERVAL)
            }
        }
    }

    private fun initViews() {
        mEditText = findViewById(R.id.edit_text)
        mCancelText = findViewById(R.id.cancel)
        code_editor = findViewById(R.id.code_editor)
        mSaveText = findViewById(R.id.ok)
        mEditorToolbar = findViewById(R.id.editor_toolbar)
        mEditorMenuButton = findViewById(R.id.editor_menu)
        mEditorFilesButton = findViewById(R.id.editor_action_files)
        mEditorUndoButton = findViewById(R.id.editor_action_undo)
        mEditorRedoButton = findViewById(R.id.editor_action_redo)
        mEditorMoreButton = findViewById(R.id.editor_action_more)
        mEditorTerminalButton = findViewById(R.id.editor_action_terminal)
        mEditorRunButton = findViewById(R.id.editor_action_run)
        mEditorRunLoading = findViewById(R.id.editor_action_run_loading)
        mEditorAndroidBuildButton = findViewById(R.id.editor_action_android_build)
        mEditorAndroidBuildLoading = findViewById(R.id.editor_action_android_build_loading)
        mEditorTabBar = findViewById(R.id.editor_tab_bar)
        mEditorTabsContainer = findViewById(R.id.editor_tabs_container)
        mEditorContentLayout = findViewById(R.id.editor_content_layout)
        mEditorSymbolBar = findViewById(R.id.editor_symbol_bar)
        mEditorTerminalPanelView = findViewById(R.id.editor_terminal_panel)
        mEditorSidebar = findViewById(R.id.editor_sidebar)
        mSidebarFileTab = findViewById(R.id.sidebar_file_tab)
        mSidebarSearchTab = findViewById(R.id.sidebar_search_tab)
        mSidebarFilePanel = findViewById(R.id.sidebar_file_panel)
        mSidebarSearchPanel = findViewById(R.id.sidebar_search_panel)
        mSidebarFilePath = findViewById(R.id.sidebar_file_path)
        mSidebarFileTreeScroll = findViewById(R.id.sidebar_file_tree_scroll)
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
        mSidebarMainPage = findViewById(R.id.sidebar_main_page)
        mSidebarProjectPage = findViewById(R.id.sidebar_project_page)
        mSidebarProjectPath = findViewById(R.id.sidebar_project_path)
        mSidebarPageBrowserTab = findViewById(R.id.sidebar_page_browser_tab)
        mSidebarPageProjectTab = findViewById(R.id.sidebar_page_project_tab)
        mEditorSymbolInput = findViewById(R.id.editor_symbol_input)
        mEditorSettings = findViewById(R.id.editor_settings)
        mEditorPreviewContainer = findViewById(R.id.editor_preview_container)
        mEditorImagePreview = findViewById(R.id.editor_image_preview)
        mEditorSvgPreview = findViewById(R.id.editor_svg_preview)
        mEditorSvgModeToggle = findViewById(R.id.editor_svg_mode_toggle)
        code_editor?.setBackgroundColor(0xff1e1e1e.toInt())
        code_editor?.setPadding(dp(12), dp(10), dp(12), dp(10))
        configureCodeEditorInput()
        configurePreviewWebView()
    }

    private fun configureCodeEditorInput() {
        code_editor?.apply {
            setSoftKeyboardEnabled(true)
            setDisableSoftKbdIfHardKbdAvailable(false)
            isFocusableInTouchMode = true
            getComponent(EditorAutoCompletion::class.java)?.isEnabled = true
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && editorTerminalPanel?.isVisible() == true) {
                    findViewById<EditorTerminalInputView>(R.id.editor_terminal_input)?.clearFocus()
                    setSoftKeyboardEnabled(true)
                }
            }
        }
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
        mEditorMenuButton?.setOnClickListener {
            toggleSidebar()
        }
        mEditorFilesButton?.setOnClickListener { view ->
            showEditorFileMenu(view)
        }
        mEditorUndoButton?.setOnClickListener {
            invokeEditorAction("undo")
            updateEditorActionButtons()
        }
        mEditorRedoButton?.setOnClickListener {
            invokeEditorAction("redo")
            updateEditorActionButtons()
        }
        mEditorRunButton?.setOnClickListener {
            onRunProgramClicked()
        }
        mEditorAndroidBuildButton?.setOnClickListener {
            onAndroidBuildClicked()
        }
        mEditorTerminalButton?.setOnClickListener {
            editorTerminalPanel?.toggle()
        }
        mEditorMoreButton?.setOnClickListener { view ->
            showEditorMoreMenu(view)
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
        updateEditorActionButtons()
    }

    private fun initEditorTerminal(savedInstanceState: Bundle?) {
        val panel = findViewById<View>(R.id.editor_terminal_panel) ?: return
        val terminalView = findViewById<TerminalView>(R.id.editor_terminal_view) ?: return
        val inputView = findViewById<EditorTerminalInputView>(R.id.editor_terminal_input) ?: return
        val contentLayout = mEditorContentLayout ?: return
        val symbolBar = mEditorSymbolBar ?: return
        // 每次进入编辑器默认隐藏终端，仅在配置变更（如旋转屏幕）时恢复状态
        val restoredVisible = savedInstanceState?.getBoolean(EDITOR_STATE_TERMINAL_VISIBLE, false) == true
        editorTerminalPanel = EditorTerminalPanel(
            activity = this,
            panelView = panel,
            terminalView = terminalView,
            inputView = inputView,
            contentLayout = contentLayout,
            symbolBar = symbolBar,
            onBlurEditor = { blurEditorForTerminal() },
            onRestoreEditorFocus = { restoreEditorFocusAfterTerminal() }
        ) { visible -> updateTerminalToolbarState(visible) }
        val resizeHandle = findViewById<View>(R.id.editor_terminal_resize_handle) ?: return
        editorTerminalPanel?.init(restoredVisible, resizeHandle)
        findViewById<View>(R.id.editor_terminal_hide)?.setOnClickListener {
            editorTerminalPanel?.setVisible(false)
        }
    }

    private fun updateTerminalToolbarState(visible: Boolean) {
        mEditorTerminalButton?.alpha = if (visible) 1f else 0.65f
        mEditorSymbolBar?.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun blurEditorForTerminal() {
        code_editor?.let { editor ->
            editor.setSoftKeyboardEnabled(false)
            KeyboardUtils.hideSoftKeyboard(this, editor)
            editor.clearFocus()
        }
        currentFocus?.clearFocus()
    }

    private fun restoreEditorFocusAfterTerminal() {
        findViewById<EditorTerminalInputView>(R.id.editor_terminal_input)?.clearFocus()
        code_editor?.setSoftKeyboardEnabled(true)
        code_editor?.requestFocus()
    }

    private fun onRunProgramClicked() {
        if (isProgramRunInProgress) return
        val runner = programRunner ?: return
        val file = currentFile ?: return
        val content = code_editor?.text?.toString().orEmpty()
        val language = EditorRunDetector.detect(file.name, content) ?: return
        if (!runner.canUseTerminal()) {
            UUtils.showMsg(getString(R.string.editor_java_terminal_unavailable))
            return
        }
        setRunLoading(true)
        editorTerminalPanel?.setVisible(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val runtimeInstalled = runner.isRuntimeInstalled(language)
            withContext(Dispatchers.Main) {
                if (!runtimeInstalled) {
                    setRunLoading(false)
                    showInstallRuntimeDialog(runner, language)
                } else {
                    runProgramWithRuntime(runner, language, file, content)
                }
            }
        }
    }

    private fun showInstallRuntimeDialog(runner: EditorProgramRunner, language: EditorRunLanguage) {
        val titleRes: Int
        val messageRes: Int
        when (language) {
            EditorRunLanguage.JAVA -> {
                titleRes = R.string.editor_java_install_jdk_title
                messageRes = R.string.editor_java_install_jdk_message
            }
            EditorRunLanguage.C -> {
                titleRes = R.string.editor_run_install_c_title
                messageRes = R.string.editor_run_install_c_message
            }
            EditorRunLanguage.PYTHON -> {
                titleRes = R.string.editor_run_install_python_title
                messageRes = R.string.editor_run_install_python_message
            }
        }
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setPositiveButton(R.string.editor_java_install_jdk_confirm) { _, _ ->
                runner.installRuntimeViaTerminal(language) { }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun runProgramWithRuntime(
        runner: EditorProgramRunner,
        language: EditorRunLanguage,
        file: File,
        content: String
    ) {
        if (!saveCurrentFileForRun()) {
            setRunLoading(false)
            UUtils.showMsg(getString(R.string.editor_java_save_failed))
            return
        }
        runner.runProgram(language, file, content)
        setRunLoading(false)
        updateRunButton()
    }

    private fun saveCurrentFileForRun(): Boolean {
        val file = currentFile ?: return false
        val tab = currentTab() ?: return false
        if (tab.previewOnly || isTextPreviewMode(tab)) return false
        val content = code_editor?.text?.toString().orEmpty()
        if (content.isEmpty()) return false
        return if (UUtils.setFileString(file, content)) {
            tab.content = content
            tab.savedContent = content
            tab.dirty = false
            isDirty = false
            renderEditorTabs()
            true
        } else {
            false
        }
    }

    private fun setRunLoading(loading: Boolean) {
        isProgramRunInProgress = loading
        updateRunButton()
    }

    private fun updateRunButton() {
        val file = currentFile
        val tab = currentTab()
        val content = code_editor?.text?.toString().orEmpty()
        val canRun = file != null
            && tab != null
            && !tab.previewOnly
            && !isTextPreviewMode(tab)
            && EditorRunDetector.detect(file.name, content) != null
        if (!canRun) {
            mEditorRunButton?.visibility = View.GONE
            mEditorRunLoading?.visibility = View.GONE
        } else if (isProgramRunInProgress) {
            mEditorRunButton?.visibility = View.GONE
            mEditorRunLoading?.visibility = View.VISIBLE
        } else {
            mEditorRunButton?.visibility = View.VISIBLE
            mEditorRunLoading?.visibility = View.GONE
        }
        refreshActiveAndroidProjectRoot()
        updateAndroidBuildButton()
    }

    private fun refreshActiveAndroidProjectRoot() {
        val contexts = listOfNotNull(currentFile, fileTreeCurrentDir, fileTreeRoot)
        activeAndroidProjectRoot = contexts
            .mapNotNull { AndroidProjectManager.findProjectRoot(it) }
            .distinctBy { it.absolutePath }
            .firstOrNull { root ->
                contexts.any { context -> AndroidProjectManager.isPathInsideProject(root, context) }
            }
    }

    private fun updateAndroidBuildButton() {
        val show = activeAndroidProjectRoot != null
        if (!show) {
            mEditorAndroidBuildButton?.visibility = View.GONE
            mEditorAndroidBuildLoading?.visibility = View.GONE
            return
        }
        if (isAndroidBuildInProgress) {
            mEditorAndroidBuildButton?.visibility = View.GONE
            mEditorAndroidBuildLoading?.visibility = View.VISIBLE
        } else {
            mEditorAndroidBuildButton?.visibility = View.VISIBLE
            mEditorAndroidBuildLoading?.visibility = View.GONE
        }
    }

    private fun onAndroidBuildClicked() {
        if (isAndroidBuildInProgress) return
        val runner = androidRunner ?: return
        val projectRoot = activeAndroidProjectRoot ?: return
        if (!runner.canUseTerminal()) {
            UUtils.showMsg(getString(R.string.editor_java_terminal_unavailable))
            return
        }
        setAndroidBuildLoading(true)
        editorTerminalPanel?.setVisible(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val envReady = runner.isGradleEnvInstalled()
            withContext(Dispatchers.Main) {
                if (!envReady) {
                    setAndroidBuildLoading(false)
                    showInstallGradleDialog(runner, projectRoot)
                } else {
                    runAndroidBuild(runner, projectRoot)
                }
            }
        }
    }

    private fun showInstallGradleDialog(runner: EditorAndroidRunner, projectRoot: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.editor_android_install_gradle_title)
            .setMessage(R.string.editor_android_install_gradle_message)
            .setPositiveButton(R.string.editor_android_install_gradle_confirm) { _, _ ->
                editorTerminalPanel?.setVisible(true)
                runner.installGradleEnvViaTerminal { }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun runAndroidBuild(runner: EditorAndroidRunner, projectRoot: File) {
        storeCurrentTabState()
        saveCurrentFileForRun()
        runner.prepareAndBuild(projectRoot)
        setAndroidBuildLoading(false)
        updateAndroidBuildButton()
    }

    private fun setAndroidBuildLoading(loading: Boolean) {
        isAndroidBuildInProgress = loading
        updateAndroidBuildButton()
    }

    private fun createAndroidProject() {
        val parentDir = getProjectTargetDir()
        if (parentDir == null || !parentDir.isDirectory) {
            UUtils.showMsg(getString(R.string.editor_sidebar_project_dir_invalid))
            return
        }
        val projectDir = allocateProjectDirectory(parentDir, "Android_project")
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show()
        lifecycleScope.launch(Dispatchers.IO) {
            val success = AndroidProjectManager.createFromAssets(applicationContext, projectDir)
            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                if (!success) {
                    UUtils.showMsg(getString(R.string.editor_sidebar_android_project_failed))
                    return@withContext
                }
                UUtils.showMsg(getString(R.string.editor_sidebar_android_project_created, projectDir.name))
                if (fileTreeRoot?.absolutePath != parentDir.absolutePath) {
                    initFileTree(parentDir)
                }
                initFileTree(projectDir)
                activeAndroidProjectRoot = projectDir
                AndroidProjectManager.defaultEntryFile(projectDir)?.let { entry ->
                    if (canOpenFile(entry)) {
                        loadFile(entry)
                    }
                }
                updateAndroidBuildButton()
                setSidebarVisible(false)
                promptInstallGradleAfterProjectCreated()
            }
        }
    }

    private fun promptInstallGradleAfterProjectCreated() {
        val runner = androidRunner ?: return
        if (!runner.canUseTerminal()) return
        if (runner.isGradleEnvInstalled()) return
        AlertDialog.Builder(this)
            .setTitle(R.string.editor_android_install_gradle_title)
            .setMessage(R.string.editor_android_install_gradle_message)
            .setPositiveButton(R.string.editor_android_install_gradle_confirm) { _, _ ->
                editorTerminalPanel?.setVisible(true)
                runner.installGradleEnvViaTerminal { }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEditorFileMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, getString(R.string.edit_save))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> saveFile()
                }
                updateEditorActionButtons()
                true
            }
            show()
        }
    }

    private fun showEditorMoreMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, getString(R.string.notification_action_exit))
            menu.add(0, 2, 1, getString(R.string.editor_settings))
            menu.add(0, 3, 2, getString(R.string.editor_symbol_customize))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> confirmExitIfDirty()
                    2 -> showEditorSettingsDialog()
                    3 -> showSymbolCustomizeDialog()
                }
                true
            }
            show()
        }
    }

    private fun invokeEditorAction(action: String) {
        val editor = code_editor ?: return
        when (action) {
            "undo" -> if (editor.canUndo()) editor.undo() else return
            "redo" -> if (editor.canRedo()) editor.redo() else return
            else -> return
        }
        updateDirtyState()
        updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
    }

    private fun isEditorActionAvailable(action: String): Boolean {
        val editor = code_editor ?: return false
        return when (action) {
            "undo" -> editor.canUndo()
            "redo" -> editor.canRedo()
            else -> false
        }
    }

    private fun updateEditorActionButtons() {
        val tab = currentTab()
        val editable = tab != null && !tab.previewOnly && !isTextPreviewMode(tab)
        updateToolbarButtonState(mEditorUndoButton, editable && isEditorActionAvailable("undo"))
        updateToolbarButtonState(mEditorRedoButton, editable && isEditorActionAvailable("redo"))
    }

    private fun updateToolbarButtonState(button: ImageView?, enabled: Boolean) {
        button ?: return
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.38f
    }

    private fun loadEditorSettings() {
        val prefs = getSharedPreferences(EDITOR_PREF_NAME, Context.MODE_PRIVATE)
        currentTabSize = prefs.getInt(EDITOR_PREF_TAB_SIZE, DEFAULT_TAB_SIZE).coerceIn(2, 8)
        currentThemeName = prefs.getString(EDITOR_PREF_THEME, "vscode_dark") ?: "vscode_dark"
        currentFontPath = prefs.getString(EDITOR_PREF_FONT_PATH, "")?.trim().orEmpty()
        lspEnabled = prefs.getBoolean(EDITOR_PREF_LSP_ENABLED, false)
        lspTimeoutMillis = prefs.getLong(EDITOR_PREF_LSP_TIMEOUT, EditorLspManager.DEFAULT_TIMEOUT_MILLIS)
            .coerceIn(500L, 30000L)
    }

    private fun applyLspSettings() {
        if (!::lspManager.isInitialized) return
        lspManager.updateSettings(EditorLspManager.Settings(lspEnabled, lspTimeoutMillis))
    }

    private fun lspLanguageId(extension: String): String? {
        return EditorLspManager.languageIdForExtension(extension)
    }

    private fun closeLspDocument(file: File) {
        if (!::lspManager.isInitialized) return
        lifecycleScope.launch(Dispatchers.IO) {
            lspManager.closeDocument(file)
        }
    }

    private fun shutdownLspManager() {
        if (!::lspManager.isInitialized) return
        val manager = lspManager
        Thread({
            manager.closeAll()
        }, "ZT-LSP-Shutdown").apply {
            isDaemon = true
            start()
        }
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
        val lspEnableSwitch = android.widget.CheckBox(this).apply {
            text = getString(R.string.editor_settings_lsp_enable)
            isChecked = lspEnabled
            setTextColor(0xffd4d4d4.toInt())
            setPadding(0, dp(4), 0, dp(4))
        }
        val lspTimeoutInput = EditText(this).apply {
            setText(lspTimeoutMillis.toString())
            hint = getString(R.string.editor_settings_lsp_timeout_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
            setHintTextColor(0xff9d9d9d.toInt())
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val lspDescLabel = TextView(this).apply {
            text = getString(R.string.editor_settings_lsp_download_desc)
            setTextColor(0xff9d9d9d.toInt())
            textSize = 12f
            setPadding(0, 0, 0, dp(6))
        }
        val lspManageButton = TextView(this).apply {
            text = getString(R.string.editor_settings_lsp_download)
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
            setOnClickListener { showLspServersDialog() }
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
            addView(lspEnableSwitch)
            addView(buildSettingsLabel(getString(R.string.editor_settings_lsp_timeout)))
            addView(lspTimeoutInput)
            addView(lspDescLabel)
            addView(lspManageButton)
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
                lspEnabled = lspEnableSwitch.isChecked
                lspTimeoutMillis = lspTimeoutInput.text.toString().toLongOrNull()
                    ?.coerceIn(500L, 30000L) ?: EditorLspManager.DEFAULT_TIMEOUT_MILLIS
                getSharedPreferences(EDITOR_PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putInt(EDITOR_PREF_TAB_SIZE, currentTabSize)
                    .putString(EDITOR_PREF_THEME, currentThemeName)
                    .putString(EDITOR_PREF_FONT_PATH, currentFontPath)
                    .putBoolean(EDITOR_PREF_LSP_ENABLED, lspEnabled)
                    .putLong(EDITOR_PREF_LSP_TIMEOUT, lspTimeoutMillis)
                    .apply()
                applyEditorTabSize()
                applyEditorTheme(currentThemeName)
                applyEditorFont(true)
                applyLspSettings()
                reloadCurrentEditorLanguage()
                if (lspEnabled) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        lspManager.ensureBasicShellInstalled()
                    }
                }
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                currentTabSize = DEFAULT_TAB_SIZE
                currentThemeName = "vscode_dark"
                currentFontPath = ""
                lspEnabled = false
                lspTimeoutMillis = EditorLspManager.DEFAULT_TIMEOUT_MILLIS
                getSharedPreferences(EDITOR_PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                applyEditorTabSize()
                applyEditorTheme(currentThemeName)
                applyEditorFont(false)
                applyLspSettings()
                reloadCurrentEditorLanguage()
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            editorSettingsFontInput = null
        }
        dialog.show()
    }

    private fun showLspServersDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_lsp_servers, null)
        val listView = dialogView.findViewById<RecyclerView>(R.id.lsp_server_list)
        lateinit var lspServerAdapter: EditorLspServerAdapter
        lspServerAdapter = EditorLspServerAdapter(lspManager) { serverPackage ->
            lspManager.installPackage(serverPackage.id) { _, _ ->
                runOnUiThread {
                    lspServerAdapter.refresh()
                    stopLspInstallRefresh()
                }
            }
            lspServerAdapter.refresh()
            startLspInstallRefresh(lspServerAdapter)
        }
        listView.layoutManager = LinearLayoutManager(this)
        listView.adapter = lspServerAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialogView.findViewById<View>(R.id.lsp_dialog_close).setOnClickListener {
            stopLspInstallRefresh()
            dialog.dismiss()
        }
        dialog.setOnDismissListener {
            stopLspInstallRefresh()
        }
        dialog.show()
    }

    private fun startLspInstallRefresh(adapter: EditorLspServerAdapter) {
        stopLspInstallRefresh()
        val refreshRunnable = object : Runnable {
            override fun run() {
                adapter.refresh()
                if (adapter.hasInstallingPackage()) {
                    dirtyCheckHandler.postDelayed(this, 800L)
                }
            }
        }
        lspInstallRefreshRunnable = refreshRunnable
        dirtyCheckHandler.postDelayed(refreshRunnable, 800L)
    }

    private fun stopLspInstallRefresh() {
        lspInstallRefreshRunnable?.let { dirtyCheckHandler.removeCallbacks(it) }
        lspInstallRefreshRunnable = null
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
        mEditorSymbolBar?.background = null
        mEditorSymbolInput?.apply {
            orientation = LinearLayout.HORIZONTAL
            background = null
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
        symbolInput.background = null
        symbolInput.forEachButton { button ->
            button.setAllCaps(false)
            button.minWidth = 0
            button.minimumWidth = 0
            button.minHeight = 0
            button.minimumHeight = 0
            button.textSize = 14f
            button.setTextColor(0xfff5f5f5.toInt())
            button.setPadding(dp(12), 0, dp(12), 0)
            button.includeFontPadding = false
            button.background = null
            button.backgroundTintList = null
            button.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(42)
            ).apply {
                setMargins(dp(6), dp(0), dp(6), dp(0))
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
            fileTreeAdapter?.notifyDataSetChanged()
            updatePositionText()
            updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
            updateDirtyState()
            updateEditorActionButtons()
            updateRunButton()
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
        code_editor?.setEditorLanguage(createEditorLanguage(extension, tab.file))
        code_editor?.setText(tab.content)
        updateRunButton()
    }

    private fun createEditorLanguage(extension: String, file: File): Language {
        val delegate = TextMateLanguage.create(getCodeType(extension), true)
        if (!lspEnabled) return delegate
        val languageId = lspLanguageId(extension) ?: return delegate
        return EditorLspLanguage(delegate, lspManager, file, languageId)
    }

    private fun reloadCurrentEditorLanguage() {
        storeCurrentTabState()
        val tab = currentTab() ?: return
        if (tab.previewOnly || isTextPreviewMode(tab)) return
        val extension = getFileExtension(tab.file)
        code_editor?.setEditorLanguage(createEditorLanguage(extension, tab.file))
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
        updateRunButton()
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
        updateEditorActionButtons()
        updateRunButton()
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
                setPadding(dp(8), 0, 0, 0)
                background = ContextCompat.getDrawable(
                    this@EditTextActivity,
                    if (active) R.drawable.shape_editor_tab_active else R.drawable.shape_editor_tab_inactive
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, dp(1), 0)
                }
                setOnClickListener {
                    if (tab.file.absolutePath != currentFile?.absolutePath) {
                        loadFile(tab.file)
                    }
                }
            }
            tabView.addView(TextView(this).apply {
                text = tab.file.name + if (tab.dirty) " •" else ""
                setTextColor(
                    when {
                        tab.dirty -> 0xfff4c38b.toInt()
                        active -> 0xfff5f5f5.toInt()
                        else -> 0xffd0d0d0.toInt()
                    }
                )
                textSize = 12.5f
                setSingleLine(true)
                setMaxWidth(dp(180))
                gravity = android.view.Gravity.CENTER_VERTICAL
                includeFontPadding = false
                setTypeface(Typeface.DEFAULT, if (active) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            })
            tabView.addView(TextView(this).apply {
                text = "×"
                setTextColor(if (active) 0xffd7d7d7.toInt() else 0xff8b8b8b.toInt())
                textSize = 15f
                gravity = android.view.Gravity.CENTER
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.MATCH_PARENT)
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
        closeLspDocument(tab.file)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EDITOR_STATE_SIDEBAR_VISIBLE, isSidebarVisible)
        outState.putBoolean(EDITOR_STATE_SIDEBAR_SEARCH_PANEL, isSidebarSearchPanelVisible)
        outState.putString(EDITOR_STATE_SIDEBAR_SEARCH_QUERY, mSidebarSearchInput?.text?.toString().orEmpty())
        outState.putString(EDITOR_STATE_SIDEBAR_REPLACE_QUERY, mSidebarReplaceInput?.text?.toString().orEmpty())
        outState.putBoolean(EDITOR_STATE_SIDEBAR_REGEX, isRegexSearch)
        outState.putBoolean(EDITOR_STATE_SIDEBAR_MATCH_CASE, isMatchCase)
        outState.putBoolean(
            EDITOR_STATE_TERMINAL_VISIBLE,
            editorTerminalPanel?.isVisible() == true
        )
    }

    override fun onBackPressed() {
        confirmExitIfDirty()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val inputView = findViewById<EditorTerminalInputView>(R.id.editor_terminal_input)
        val terminalView = findViewById<TerminalView>(R.id.editor_terminal_view)
        if (editorTerminalPanel?.isVisible() == true && inputView != null && inputView.hasFocus()) {
            val session = terminalView?.currentSession
            if (session != null && event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        session.write("\r")
                        terminalView?.onScreenUpdated()
                        return true
                    }
                    KeyEvent.KEYCODE_DEL -> {
                        session.write("\u007f")
                        terminalView?.onScreenUpdated()
                        return true
                    }
                }
            }
            if (terminalView != null && terminalView.dispatchKeyEvent(event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (handleSidebarSwipeGesture(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        editorTerminalPanel?.onResume()
    }

    override fun onDestroy() {
        dirtyCheckHandler.removeCallbacks(dirtyCheckRunnable)
        stopLspInstallRefresh()
        sidebarAnimator?.cancel()
        cancelSidebarGesture()
        shutdownLspManager()
        editorTerminalPanel?.destroy()
        editorTerminalPanel = null
        super.onDestroy()
    }

    private fun toggleSidebar() {
        setSidebarVisible(!isSidebarVisible)
    }

    private fun getSidebarWidthPx(): Float {
        return dp(SIDEBAR_WIDTH_DP).toFloat()
    }

    private fun setSidebarVisible(visible: Boolean, animated: Boolean = true) {
        val sidebar = mEditorSidebar ?: return
        val sidebarWidth = getSidebarWidthPx()
        cancelSidebarGesture()
        sidebar.animate().cancel()
        sidebarAnimator?.cancel()

        if (visible) {
            if (isSidebarVisible && sidebar.visibility == View.VISIBLE && sidebar.translationX == 0f) {
                return
            }
            sidebar.visibility = View.VISIBLE
            val startTx = if (sidebar.translationX != 0f) sidebar.translationX else -sidebarWidth
            applySidebarTranslation(startTx)
            if (animated) {
                animateSidebarTo(0f, true)
            } else {
                finishSidebarState(true)
            }
        } else {
            if (!isSidebarVisible && sidebar.visibility != View.VISIBLE) {
                finishSidebarState(false)
                return
            }
            if (animated) {
                val startTx = if (sidebar.visibility == View.VISIBLE) sidebar.translationX else -sidebarWidth
                applySidebarTranslation(startTx)
                animateSidebarTo(-sidebarWidth, false)
            } else {
                finishSidebarState(false)
            }
        }
    }

    private fun applySidebarTranslation(translationX: Float) {
        val sidebar = mEditorSidebar ?: return
        val sidebarWidth = getSidebarWidthPx()
        val clamped = translationX.coerceIn(-sidebarWidth, 0f)
        val progress = 1f + clamped / sidebarWidth
        sidebar.translationX = clamped
        if (progress > 0.001f) {
            sidebar.visibility = View.VISIBLE
        }
        applySidebarLeftMargin((sidebarWidth * progress).toInt())
    }

    private fun applySidebarLeftMargin(margin: Int) {
        listOf(
            mEditorToolbar,
            mEditorTabBar,
            mEditorContentLayout,
            mEditorTerminalPanelView,
            mEditorSymbolBar
        ).forEach { view ->
            (view?.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                if (leftMargin != margin) {
                    leftMargin = margin
                    view.layoutParams = this
                }
            }
        }
        editorTerminalPanel?.onHostLayoutChanged()
    }

    private fun finishSidebarState(visible: Boolean) {
        isSidebarVisible = visible
        val sidebar = mEditorSidebar ?: return
        if (visible) {
            applySidebarTranslation(0f)
            sidebar.translationX = 0f
            scheduleFileTreeContentWidthUpdate()
        } else {
            sidebar.visibility = View.GONE
            sidebar.translationX = 0f
            applySidebarLeftMargin(0)
        }
    }

    private fun animateSidebarTo(targetTranslation: Float, visible: Boolean) {
        val sidebar = mEditorSidebar ?: return
        val sidebarWidth = getSidebarWidthPx()
        val start = sidebar.translationX
        sidebarAnimator?.cancel()
        if (abs(start - targetTranslation) < 1f) {
            finishSidebarState(visible)
            return
        }
        val duration = (SIDEBAR_ANIMATION_DURATION * abs(start - targetTranslation) / sidebarWidth)
            .toLong()
            .coerceIn(80L, SIDEBAR_ANIMATION_DURATION)
        sidebarAnimator = ValueAnimator.ofFloat(start, targetTranslation).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                applySidebarTranslation(animator.animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishSidebarState(visible)
                    sidebarAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    sidebarAnimator = null
                }
            })
            start()
        }
    }

    private fun isTouchOnFileTree(ev: MotionEvent): Boolean {
        val scroll = mSidebarFileTreeScroll ?: return false
        if (scroll.visibility != View.VISIBLE) return false
        if (mSidebarFilePanel?.visibility != View.VISIBLE) return false
        val location = IntArray(2)
        scroll.getLocationOnScreen(location)
        return ev.rawX >= location[0] && ev.rawX < location[0] + scroll.width &&
            ev.rawY >= location[1] && ev.rawY < location[1] + scroll.height
    }

    private fun shouldStartSidebarGesture(ev: MotionEvent): Boolean {
        if (sidebarGestureActive || sidebarGestureDragging) return false
        val sidebar = mEditorSidebar ?: return false
        if (isSidebarVisible && sidebar.visibility == View.VISIBLE) {
            if (isTouchOnFileTree(ev)) return false
            val location = IntArray(2)
            sidebar.getLocationOnScreen(location)
            val x = ev.rawX
            val y = ev.rawY
            return x >= location[0] && x < location[0] + sidebar.width &&
                y >= location[1] && y < location[1] + sidebar.height
        }
        if (!isSidebarVisible) {
            return ev.rawX <= dp(SIDEBAR_EDGE_SWIPE_DP)
        }
        return false
    }

    private fun beginSidebarGesture(ev: MotionEvent) {
        val sidebar = mEditorSidebar ?: return
        val sidebarWidth = getSidebarWidthPx()
        sidebarGestureActive = true
        sidebarGestureDragging = false
        sidebarGestureOpenedFromClosed = !isSidebarVisible
        sidebarGestureDownX = ev.rawX
        sidebarGestureDownY = ev.rawY
        sidebarGestureStartTranslation = if (isSidebarVisible) {
            sidebar.translationX
        } else {
            sidebar.visibility = View.VISIBLE
            -sidebarWidth
        }
        sidebar.translationX = sidebarGestureStartTranslation
        applySidebarTranslation(sidebarGestureStartTranslation)
        sidebarVelocityTracker?.recycle()
        sidebarVelocityTracker = VelocityTracker.obtain()
        sidebarVelocityTracker?.addMovement(ev)
    }

    private fun beginSidebarGestureDrag(startRawX: Float, startRawY: Float, ev: MotionEvent) {
        beginSidebarGesture(ev)
        sidebarGestureDownX = startRawX
        sidebarGestureDownY = startRawY
        sidebarGestureDragging = true
        mEditorSidebar?.animate()?.cancel()
        sidebarAnimator?.cancel()
    }

    private fun updateSidebarGestureDrag(ev: MotionEvent): Boolean {
        if (!sidebarGestureActive) return false
        sidebarVelocityTracker?.addMovement(ev)
        if (!sidebarGestureDragging) {
            val dx = ev.rawX - sidebarGestureDownX
            val dy = ev.rawY - sidebarGestureDownY
            if (abs(dx) < sidebarTouchSlop && abs(dy) < sidebarTouchSlop) {
                return false
            }
            if (abs(dy) > abs(dx)) {
                cancelSidebarGesture(resetPartialOpen = true)
                return false
            }
            sidebarGestureDragging = true
            mEditorSidebar?.animate()?.cancel()
            sidebarAnimator?.cancel()
        }
        val dx = ev.rawX - sidebarGestureDownX
        val newTranslation = (sidebarGestureStartTranslation + dx)
            .coerceIn(-getSidebarWidthPx(), 0f)
        applySidebarTranslation(newTranslation)
        return true
    }

    private fun finishSidebarGestureDrag(ev: MotionEvent): Boolean {
        if (!sidebarGestureDragging) {
            cancelSidebarGesture(resetPartialOpen = true)
            return false
        }
        sidebarVelocityTracker?.addMovement(ev)
        sidebarVelocityTracker?.computeCurrentVelocity(1000)
        val velocityX = sidebarVelocityTracker?.xVelocity ?: 0f
        snapSidebarFromGesture(velocityX)
        cancelSidebarGesture()
        return true
    }

    private fun cancelSidebarGesture(resetPartialOpen: Boolean = false) {
        sidebarGestureActive = false
        sidebarGestureDragging = false
        mSidebarFileTreeScroll?.resetSidebarDragState()
        sidebarVelocityTracker?.recycle()
        sidebarVelocityTracker = null
        if (resetPartialOpen && sidebarGestureOpenedFromClosed && !isSidebarVisible) {
            finishSidebarState(false)
        }
        sidebarGestureOpenedFromClosed = false
    }

    private fun snapSidebarFromGesture(velocityX: Float) {
        val sidebarWidth = getSidebarWidthPx()
        val current = mEditorSidebar?.translationX ?: return
        val flingVelocity = dp(SIDEBAR_FLING_VELOCITY_DP).toFloat()
        val open = when {
            velocityX > flingVelocity -> true
            velocityX < -flingVelocity -> false
            current > -sidebarWidth * 0.5f -> true
            else -> false
        }
        animateSidebarTo(if (open) 0f else -sidebarWidth, open)
    }

    private fun handleSidebarSwipeGesture(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                sidebarAnimator?.cancel()
                if (shouldStartSidebarGesture(ev)) {
                    beginSidebarGesture(ev)
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!sidebarGestureActive) return false
                return updateSidebarGestureDrag(ev)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!sidebarGestureActive) return false
                return finishSidebarGestureDrag(ev)
            }
        }
        return false
    }

    private fun restoreSidebarState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            showFilePanel(updateSearch = false)
            setSidebarVisible(false, animated = false)
            return
        }

        isRegexSearch = savedInstanceState.getBoolean(EDITOR_STATE_SIDEBAR_REGEX, false)
        isMatchCase = savedInstanceState.getBoolean(EDITOR_STATE_SIDEBAR_MATCH_CASE, false)
        mSidebarSearchInput?.setText(savedInstanceState.getString(EDITOR_STATE_SIDEBAR_SEARCH_QUERY).orEmpty())
        mSidebarReplaceInput?.setText(savedInstanceState.getString(EDITOR_STATE_SIDEBAR_REPLACE_QUERY).orEmpty())
        updateSearchToggles()

        if (savedInstanceState.getBoolean(EDITOR_STATE_SIDEBAR_SEARCH_PANEL, false)) {
            showSearchPanel()
        } else {
            showFilePanel(updateSearch = false)
        }

        setSidebarVisible(
            savedInstanceState.getBoolean(EDITOR_STATE_SIDEBAR_VISIBLE, false),
            animated = false
        )
        if (isSidebarVisible) {
            updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
        }
    }

    private fun initSidebar() {
        mSidebarFileTab?.setOnClickListener { showFilePanel() }
        mSidebarSearchTab?.setOnClickListener { showSearchPanel() }
        mSidebarPageBrowserTab?.setOnClickListener { showSidebarBrowserPage() }
        mSidebarPageProjectTab?.setOnClickListener { showSidebarProjectPage() }
        findViewById<TextView>(R.id.sidebar_create_java)?.setOnClickListener {
            createHelloProject(HelloProjectSpec("project_java", "Hello.java", JAVA_HELLO_TEMPLATE))
        }
        findViewById<TextView>(R.id.sidebar_create_c)?.setOnClickListener {
            createHelloProject(HelloProjectSpec("project_c", "hello.c", C_HELLO_TEMPLATE))
        }
        findViewById<TextView>(R.id.sidebar_create_python)?.setOnClickListener {
            createHelloProject(HelloProjectSpec("project_python", "hello.py", PYTHON_HELLO_TEMPLATE))
        }
        findViewById<TextView>(R.id.sidebar_create_android)?.setOnClickListener {
            createAndroidProject()
        }
        initFileTreeAdapter()
        initFileTreeScrollGesture()
        initSidebarSearch()
        showFilePanel(updateSearch = false)
        showSidebarBrowserPage()
        updateSearchToggles()
    }

    private fun showSidebarBrowserPage() {
        mSidebarMainPage?.visibility = View.VISIBLE
        mSidebarProjectPage?.visibility = View.GONE
        mSidebarPageBrowserTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb)
        mSidebarPageProjectTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb_inactive)
        mSidebarPageBrowserTab?.setTextColor(0xfff5f5f5.toInt())
        mSidebarPageProjectTab?.setTextColor(0xff9d9d9d.toInt())
        mSidebarPageBrowserTab?.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        mSidebarPageProjectTab?.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private fun showSidebarProjectPage() {
        mSidebarMainPage?.visibility = View.GONE
        mSidebarProjectPage?.visibility = View.VISIBLE
        mSidebarPageBrowserTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb_inactive)
        mSidebarPageProjectTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb)
        mSidebarPageBrowserTab?.setTextColor(0xff9d9d9d.toInt())
        mSidebarPageProjectTab?.setTextColor(0xfff5f5f5.toInt())
        mSidebarPageBrowserTab?.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        mSidebarPageProjectTab?.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        updateSidebarProjectPath()
    }

    private fun getProjectTargetDir(): File? {
        val current = fileTreeCurrentDir
        if (current != null && current.isDirectory) return current
        val parent = currentFile?.parentFile
        if (parent != null && parent.isDirectory) return parent
        return null
    }

    private fun updateSidebarProjectPath() {
        mSidebarProjectPath?.text = getProjectTargetDir()?.absolutePath
            ?: getString(R.string.editor_sidebar_project_dir_invalid)
    }

    private fun allocateProjectDirectory(parent: File, baseName: String): File {
        val first = File(parent, baseName)
        if (!first.exists()) return first
        var index = 1
        while (true) {
            val candidate = File(parent, baseName + index)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun createHelloProject(spec: HelloProjectSpec) {
        val parentDir = getProjectTargetDir()
        if (parentDir == null || !parentDir.isDirectory) {
            UUtils.showMsg(getString(R.string.editor_sidebar_project_dir_invalid))
            return
        }
        val projectDir = allocateProjectDirectory(parentDir, spec.dirBaseName)
        if (!projectDir.mkdirs()) {
            UUtils.showMsg(UUtils.getString(R.string.save_error_))
            return
        }
        val targetFile = File(projectDir, spec.entryFileName)
        if (!UUtils.setFileString(targetFile, spec.content)) {
            UUtils.showMsg(UUtils.getString(R.string.save_error_))
            return
        }
        UUtils.showMsg(getString(R.string.editor_sidebar_project_created, projectDir.name))
        if (fileTreeRoot?.absolutePath != parentDir.absolutePath) {
            initFileTree(parentDir)
        }
        initFileTree(projectDir)
        if (canOpenFile(targetFile)) {
            loadFile(targetFile)
            setSidebarVisible(false)
        }
    }

    private fun showFilePanel(updateSearch: Boolean = true) {
        isSidebarSearchPanelVisible = false
        mSidebarFilePanel?.visibility = View.VISIBLE
        mSidebarSearchPanel?.visibility = View.GONE
        mSidebarFileTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb)
        mSidebarSearchTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb_inactive)
        mSidebarFileTab?.setTextColor(0xfff5f5f5.toInt())
        mSidebarSearchTab?.setTextColor(0xff9d9d9d.toInt())
        mSidebarFileTab?.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        mSidebarSearchTab?.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        if (updateSearch) {
            updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
        }
    }

    private fun showSearchPanel() {
        isSidebarSearchPanelVisible = true
        mSidebarFilePanel?.visibility = View.GONE
        mSidebarSearchPanel?.visibility = View.VISIBLE
        mSidebarFileTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb_inactive)
        mSidebarSearchTab?.setBackgroundResource(R.drawable.shape_editor_slider_thumb)
        mSidebarFileTab?.setTextColor(0xff9d9d9d.toInt())
        mSidebarSearchTab?.setTextColor(0xfff5f5f5.toInt())
        mSidebarFileTab?.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        mSidebarSearchTab?.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        mSidebarSearchInput?.requestFocus()
        updateSidebarSearch(mSidebarSearchInput?.text?.toString() ?: "")
    }

    private fun initFileTree(root: File) {
        fileTreeRoot = if (root.isDirectory) root else root.parentFile
        fileTreeRoot?.absolutePath?.let { expandedDirectories.add(it) }
        setFileTreeCurrentDir(fileTreeRoot ?: return)
        refreshFileTree()
    }

    private fun setFileTreeCurrentDir(directory: File?) {
        if (directory == null || !directory.isDirectory) return
        fileTreeCurrentDir = directory
        mSidebarFilePath?.text = directory.absolutePath
        refreshActiveAndroidProjectRoot()
        updateAndroidBuildButton()
        updateSidebarProjectPath()
    }

    private fun initFileTreeAdapter() {
        fileTreeAdapter = object : ArrayAdapter<String>(this, R.layout.item_editor_file_tree, fileTreeItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_editor_file_tree, parent, false)
                val iconView = view.findViewById<ImageView>(R.id.file_tree_icon)
                val textView = view.findViewById<TextView>(R.id.file_tree_name)
                val node = fileTreeNodes.getOrNull(position)
                val depthPadding = node?.let { dp(10 + it.depth * 18) } ?: dp(10)
                view.layoutParams = android.widget.AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                view.setPadding(depthPadding, view.paddingTop, view.paddingRight, view.paddingBottom)
                when (node?.kind) {
                    FileTreeEntryKind.GO_UP -> {
                        view.setBackgroundResource(android.R.color.transparent)
                        textView.text = fileTreeItems.getOrNull(position).orEmpty()
                        textView.setTextColor(0xff9cdcfe.toInt())
                        textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                        iconView.visibility = View.VISIBLE
                        iconView.setImageResource(R.drawable.ic_editor_go_up)
                        iconView.setColorFilter(0xff9cdcfe.toInt(), PorterDuff.Mode.SRC_IN)
                    }
                    FileTreeEntryKind.NEW_MENU -> {
                        view.setBackgroundResource(android.R.color.transparent)
                        textView.text = fileTreeItems.getOrNull(position).orEmpty()
                        textView.setTextColor(0xffb5cea8.toInt())
                        textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                        iconView.visibility = View.VISIBLE
                        iconView.setImageResource(R.drawable.ic_editor_add)
                        iconView.setColorFilter(0xffb5cea8.toInt(), PorterDuff.Mode.SRC_IN)
                    }
                    else -> {
                        val isDirectory = node?.file?.isDirectory == true
                        val isCurrentFile = node?.file?.absolutePath == currentFile?.absolutePath
                        view.setBackgroundResource(
                            if (isCurrentFile) R.drawable.shape_editor_toolbar_chip_active else android.R.color.transparent
                        )
                        textView.text = fileTreeItems.getOrNull(position).orEmpty()
                        textView.setTextColor(
                            when {
                                isCurrentFile -> 0xfff5f5f5.toInt()
                                isDirectory -> 0xffcccccc.toInt()
                                else -> 0xffd4d4d4.toInt()
                            }
                        )
                        textView.setTypeface(Typeface.DEFAULT, if (isCurrentFile || isDirectory) Typeface.BOLD else Typeface.NORMAL)
                        if (node != null) {
                            val expanded = node.file.isDirectory &&
                                expandedDirectories.contains(node.file.absolutePath)
                            val iconStyle = EditorFileTreeIcon.resolve(node.file, expanded = expanded)
                            iconView.visibility = View.VISIBLE
                            iconView.setImageResource(iconStyle.drawableRes)
                            iconView.setColorFilter(iconStyle.tintColor, PorterDuff.Mode.SRC_IN)
                        } else {
                            iconView.visibility = View.GONE
                        }
                    }
                }
                return view
            }
        }
        mSidebarFileTree?.adapter = fileTreeAdapter
        mSidebarFileTree?.setOnItemClickListener { _, view, position, _ ->
            val node = fileTreeNodes.getOrNull(position) ?: return@setOnItemClickListener
            when (node.kind) {
                FileTreeEntryKind.GO_UP -> navigateFileTreeUp()
                FileTreeEntryKind.NEW_MENU -> showFileTreeCreateMenu(view)
                FileTreeEntryKind.NORMAL -> {
                    if (node.file.isDirectory) {
                        val path = node.file.absolutePath
                        if (expandedDirectories.contains(path)) {
                            expandedDirectories.remove(path)
                        } else {
                            expandedDirectories.add(path)
                        }
                        setFileTreeCurrentDir(node.file)
                        refreshFileTree()
                    } else if (canOpenFile(node.file)) {
                        loadFile(node.file)
                        setSidebarVisible(false)
                    }
                }
            }
        }
        mSidebarFileTree?.setOnItemLongClickListener { _, view, position, _ ->
            val node = fileTreeNodes.getOrNull(position) ?: return@setOnItemLongClickListener false
            if (node.kind == FileTreeEntryKind.GO_UP) return@setOnItemLongClickListener false
            if (node.kind == FileTreeEntryKind.NEW_MENU && !EditorFileTreeClipboard.hasContent()) {
                showFileTreeCreateMenu(view)
                return@setOnItemLongClickListener true
            }
            showFileTreeItemMenu(node, view)
            true
        }
    }

    private fun initFileTreeScrollGesture() {
        mSidebarFileTreeScroll?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            scheduleFileTreeContentWidthUpdate()
        }
        mSidebarFileTreeScroll?.sidebarGestureDelegate = object : EditorFileTreeScrollView.SidebarGestureDelegate {
            override fun onSidebarGestureDragStart(startRawX: Float, startRawY: Float, ev: MotionEvent) {
                beginSidebarGestureDrag(startRawX, startRawY, ev)
            }

            override fun onSidebarGestureMove(ev: MotionEvent): Boolean {
                return updateSidebarGestureDrag(ev)
            }

            override fun onSidebarGestureUp(ev: MotionEvent): Boolean {
                return finishSidebarGestureDrag(ev)
            }
        }
    }

    private fun scheduleFileTreeContentWidthUpdate() {
        mSidebarFileTreeScroll?.post { updateFileTreeContentWidth() }
    }

    private fun updateFileTreeContentWidth() {
        val scroll = mSidebarFileTreeScroll ?: return
        val list = mSidebarFileTree as? EditorFileTreeListView ?: return
        val viewportWidth = scroll.width
        if (viewportWidth <= 0) return
        val textPaint = TextPaint().apply {
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                13f,
                resources.displayMetrics
            )
        }
        var maxRowWidth = viewportWidth
        for (i in fileTreeNodes.indices) {
            val node = fileTreeNodes[i]
            val label = fileTreeItems.getOrNull(i).orEmpty()
            val paddingStart = dp(10 + node.depth * 18)
            val iconWidth = dp(18 + 8)
            val paddingEnd = dp(10)
            val bold = node.kind == FileTreeEntryKind.NORMAL &&
                (node.file.isDirectory || node.file.absolutePath == currentFile?.absolutePath)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            val textWidth = textPaint.measureText(label).toInt()
            val rowWidth = paddingStart + iconWidth + textWidth + paddingEnd
            maxRowWidth = max(maxRowWidth, rowWidth)
        }
        if (list.contentWidth == maxRowWidth) return
        list.contentWidth = maxRowWidth
        list.requestLayout()
        scroll.requestLayout()
        scroll.post {
            val maxScrollX = scroll.maxScrollX()
            if (scroll.scrollX > maxScrollX) {
                scroll.scrollTo(maxScrollX, 0)
            }
        }
    }

    private fun navigateFileTreeUp() {
        val parent = fileTreeRoot?.parentFile ?: return
        if (!parent.isDirectory) return
        initFileTree(parent)
    }

    private fun showFileTreeCreateMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, getString(R.string.editor_sidebar_create_folder))
            menu.add(0, 2, 1, getString(R.string.editor_sidebar_create_file))
            if (EditorFileTreeClipboard.hasContent()) {
                menu.add(0, 12, 2, getString(R.string.editor_sidebar_paste))
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> showCreateFileTreeEntryDialog(isFolder = true)
                    2 -> showCreateFileTreeEntryDialog(isFolder = false)
                    12 -> pasteFileTreeEntry(resolvePasteTargetDir(null))
                }
                true
            }
            show()
        }
    }

    private fun showFileTreeItemMenu(node: FileTreeNode, anchor: View) {
        PopupMenu(this, anchor).apply {
            if (node.kind == FileTreeEntryKind.NORMAL) {
                menu.add(0, 10, 0, getString(R.string.editor_sidebar_copy))
                menu.add(0, 11, 1, getString(R.string.editor_sidebar_cut))
                menu.add(0, 13, 2, getString(R.string.editor_sidebar_delete))
            }
            if (EditorFileTreeClipboard.hasContent()) {
                menu.add(0, 12, 3, getString(R.string.editor_sidebar_paste))
            }
            if (menu.size() == 0) return
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    10 -> copyFileTreeEntry(node.file)
                    11 -> cutFileTreeEntry(node.file)
                    12 -> pasteFileTreeEntry(resolvePasteTargetDir(node))
                    13 -> confirmDeleteFileTreeEntry(node.file)
                }
                true
            }
            show()
        }
    }

    private fun resolvePasteTargetDir(node: FileTreeNode?): File? {
        if (node?.kind == FileTreeEntryKind.NORMAL) {
            if (node.file.isDirectory) return node.file
            return node.file.parentFile?.takeIf { it.isDirectory }
        }
        return fileTreeCurrentDir ?: fileTreeRoot
    }

    private fun copyFileTreeEntry(file: File) {
        if (!file.exists()) return
        EditorFileTreeClipboard.setCopy(file)
        UUtils.showMsg(getString(R.string.editor_sidebar_clipboard_copied, file.name))
    }

    private fun cutFileTreeEntry(file: File) {
        if (!file.exists()) return
        EditorFileTreeClipboard.setCut(file)
        UUtils.showMsg(getString(R.string.editor_sidebar_clipboard_cut, file.name))
    }

    private fun pasteFileTreeEntry(targetDir: File?) {
        if (targetDir == null || !targetDir.isDirectory) {
            UUtils.showMsg(getString(R.string.editor_sidebar_project_dir_invalid))
            return
        }
        val source = EditorFileTreeClipboard.source() ?: return
        val cut = EditorFileTreeClipboard.mode() == EditorFileTreeClipboard.Mode.CUT
        if (!EditorFileTreeOperations.canPasteInto(source, targetDir)) {
            UUtils.showMsg(getString(R.string.editor_sidebar_paste_failed))
            return
        }
        val result = EditorFileTreeOperations.paste(source, targetDir, cut)
        if (result == null) {
            UUtils.showMsg(getString(R.string.editor_sidebar_paste_failed))
            return
        }
        if (cut) {
            relocateEditorTabsAfterMove(source, result)
            relocateFileTreeStateAfterMove(source, result)
        }
        UUtils.showMsg(getString(R.string.editor_sidebar_paste_success, result.name))
        refreshFileTreeAfterMutation()
        if (!cut && result.isFile && canOpenFile(result)) {
            loadFile(result)
        }
    }

    private fun confirmDeleteFileTreeEntry(file: File) {
        if (!file.exists()) return
        val messageRes = if (file.isDirectory) {
            R.string.editor_sidebar_delete_confirm_folder
        } else {
            R.string.editor_sidebar_delete_confirm_file
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.editor_sidebar_delete)
            .setMessage(getString(messageRes, file.name))
            .setPositiveButton(R.string.editor_sidebar_delete) { _, _ ->
                deleteFileTreeEntry(file)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteFileTreeEntry(file: File) {
        if (!file.exists()) return
        if (!EditorFileTreeOperations.deleteEntry(file)) {
            UUtils.showMsg(getString(R.string.editor_sidebar_delete_failed))
            return
        }
        EditorFileTreeClipboard.clearIfMatches(file)
        expandedDirectories.remove(file.absolutePath)
        forceRemoveEditorTabsUnder(file)
        if (fileTreeCurrentDir?.let { EditorFileTreeOperations.isSameOrDescendant(it, file) } == true) {
            val fallback = fileTreeRoot?.takeIf { it.isDirectory && it.exists() }
                ?: file.parentFile?.takeIf { it.isDirectory }
            if (fallback != null) {
                setFileTreeCurrentDir(fallback)
            }
        }
        UUtils.showMsg(getString(R.string.editor_sidebar_delete_success, file.name))
        refreshFileTreeAfterMutation()
    }

    private fun refreshFileTreeAfterMutation() {
        refreshFileTree()
        updateSidebarProjectPath()
        refreshActiveAndroidProjectRoot()
        updateAndroidBuildButton()
        fileTreeAdapter?.notifyDataSetChanged()
    }

    private fun forceRemoveEditorTabsUnder(path: File) {
        val affected = editorTabs.filter { tab ->
            EditorFileTreeOperations.isSameOrDescendant(tab.file, path) ||
                tab.file.absolutePath == path.absolutePath
        }
        if (affected.isEmpty()) return
        val activeRemoved = affected.any { it.file.absolutePath == currentFile?.absolutePath }
        affected.forEach { tab ->
            closeLspDocument(tab.file)
            editorTabs.remove(tab)
        }
        if (editorTabs.isEmpty()) {
            currentFile = null
            code_editor?.setText("")
            renderEditorTabs()
            updateRunButton()
            return
        }
        if (activeRemoved) {
            currentFile = null
            loadFile(editorTabs[0].file, storeCurrent = false)
        } else {
            renderEditorTabs()
            updateRunButton()
        }
    }

    private fun canonicalOrAbsolute(file: File): String {
        return try {
            file.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }
    }

    private fun relocateEditorTabsAfterMove(source: File, destination: File) {
        val sourcePath = canonicalOrAbsolute(source)
        val destPath = canonicalOrAbsolute(destination)
        val sourcePrefix = sourcePath + File.separator
        var activeRelocated = false
        for (i in editorTabs.indices) {
            val tab = editorTabs[i]
            val tabPath = canonicalOrAbsolute(tab.file)
            val newFile = when {
                tabPath == sourcePath -> destination
                tabPath.startsWith(sourcePrefix) -> File(destination, tabPath.removePrefix(sourcePrefix))
                else -> null
            } ?: continue
            if (!newFile.exists()) continue
            val wasActive = tab.file.absolutePath == currentFile?.absolutePath
            closeLspDocument(tab.file)
            editorTabs[i] = tab.copy(file = newFile)
            if (wasActive) {
                currentFile = newFile
                activeRelocated = true
            }
        }
        if (activeRelocated) {
            currentTab()?.let { tab ->
                val extension = getFileExtension(tab.file)
                if (tab.previewOnly || isTextPreviewMode(tab)) {
                    showPreviewTab(tab)
                } else {
                    showEditorTab(tab, extension)
                }
            }
        }
        renderEditorTabs()
        updatePositionText()
        updateRunButton()
        fileTreeAdapter?.notifyDataSetChanged()
    }

    private fun relocateFileTreeStateAfterMove(source: File, destination: File) {
        val sourcePath = canonicalOrAbsolute(source)
        val destPath = canonicalOrAbsolute(destination)
        val sourcePrefix = sourcePath + File.separator
        fileTreeRoot?.let { root ->
            val rootPath = canonicalOrAbsolute(root)
            when {
                rootPath == sourcePath -> fileTreeRoot = destination
                rootPath.startsWith(sourcePrefix) -> {
                    fileTreeRoot = File(destination, rootPath.removePrefix(sourcePrefix))
                }
            }
        }
        fileTreeCurrentDir?.let { dir ->
            val dirPath = canonicalOrAbsolute(dir)
            when {
                dirPath == sourcePath -> setFileTreeCurrentDir(destination)
                dirPath.startsWith(sourcePrefix) -> {
                    setFileTreeCurrentDir(File(destination, dirPath.removePrefix(sourcePrefix)))
                }
            }
        }
        val expanded = expandedDirectories.toList()
        expandedDirectories.clear()
        for (path in expanded) {
            when {
                path == sourcePath -> expandedDirectories.add(destPath)
                path.startsWith(sourcePrefix) -> {
                    expandedDirectories.add(File(destination, path.removePrefix(sourcePrefix)).absolutePath)
                }
                else -> expandedDirectories.add(path)
            }
        }
    }

    private fun showCreateFileTreeEntryDialog(isFolder: Boolean) {
        val root = fileTreeCurrentDir ?: fileTreeRoot ?: return
        val input = EditText(this).apply {
            hint = getString(R.string.editor_sidebar_create_name_hint)
            setTextColor(ContextCompat.getColor(this@EditTextActivity, R.color.color_ffffff))
            setHintTextColor(0xff9d9d9d.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val titleRes = if (isFolder) {
            R.string.editor_sidebar_create_folder
        } else {
            R.string.editor_sidebar_create_file
        }
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                createFileTreeEntry(root, input.text?.toString().orEmpty(), isFolder)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createFileTreeEntry(root: File, rawName: String, isFolder: Boolean) {
        val name = rawName.trim()
        if (name.isEmpty() || name.contains('/') || name.contains('\\')) {
            UUtils.showMsg(getString(R.string.editor_sidebar_create_failed))
            return
        }
        val target = File(root, name)
        if (target.exists()) {
            UUtils.showMsg(getString(R.string.editor_sidebar_create_exists, name))
            return
        }
        val success = if (isFolder) {
            target.mkdirs()
        } else {
            runCatching { target.createNewFile() }.getOrDefault(false)
        }
        if (!success) {
            UUtils.showMsg(getString(R.string.editor_sidebar_create_failed))
            return
        }
        refreshFileTree()
        updateSidebarProjectPath()
        if (!isFolder && canOpenFile(target)) {
            loadFile(target)
        }
    }

    private fun refreshFileTree() {
        fileTreeNodes.clear()
        fileTreeItems.clear()
        val root = fileTreeRoot
        if (root == null || !root.isDirectory) {
            fileTreeItems.add(getString(R.string.editor_sidebar_file_empty))
            fileTreeAdapter?.notifyDataSetChanged()
            scheduleFileTreeContentWidthUpdate()
            return
        }
        val parent = root.parentFile
        if (parent != null && parent.isDirectory) {
            fileTreeNodes.add(FileTreeNode(FileTreeEntryKind.GO_UP, parent, 0))
            fileTreeItems.add(getString(R.string.editor_sidebar_go_up))
        }
        fileTreeNodes.add(FileTreeNode(FileTreeEntryKind.NEW_MENU, root, 0))
        fileTreeItems.add(getString(R.string.editor_sidebar_new_menu))
        addFileTreeChildren(root, 0)
        fileTreeAdapter?.notifyDataSetChanged()
        scheduleFileTreeContentWidthUpdate()
    }

    private fun addFileTreeChildren(directory: File, depth: Int) {
        if (fileTreeNodes.size >= MAX_FILE_TREE_ITEMS) return
        val files = directory.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }) ?: return
        for (file in files) {
            if (fileTreeNodes.size >= MAX_FILE_TREE_ITEMS) return
            fileTreeNodes.add(FileTreeNode(FileTreeEntryKind.NORMAL, file, depth))
            fileTreeItems.add(formatFileTreeItem(file))
            if (file.isDirectory && expandedDirectories.contains(file.absolutePath)) {
                addFileTreeChildren(file, depth + 1)
            }
        }
    }

    private fun formatFileTreeItem(file: File): String {
        return if (file.isDirectory) "${file.name}/" else file.name
    }

    private fun initSidebarSearch() {
        sidebarSearchAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sidebarSearchResultItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                view.setBackgroundResource(if (position == currentSidebarMatchIndex) R.drawable.shape_editor_toolbar_chip_active else android.R.color.transparent)
                textView.setTextColor(if (position == currentSidebarMatchIndex) 0xfff5f5f5.toInt() else 0xffd4d4d4.toInt())
                textView.textSize = 13f
                textView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
                textView.setPadding(dp(12), dp(10), dp(12), dp(10))
                textView.minHeight = dp(38)
                textView.includeFontPadding = false
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
        val activeBg = R.drawable.shape_editor_toolbar_chip_active
        val defaultBg = R.drawable.shape_editor_toolbar_chip
        mSidebarRegexToggle?.setBackgroundResource(if (isRegexSearch) activeBg else defaultBg)
        mSidebarCaseToggle?.setBackgroundResource(if (isMatchCase) activeBg else defaultBg)
        mSidebarRegexToggle?.setTextColor(if (isRegexSearch) 0xfff5f5f5.toInt() else 0xffd4d4d4.toInt())
        mSidebarCaseToggle?.setTextColor(if (isMatchCase) 0xfff5f5f5.toInt() else 0xffd4d4d4.toInt())
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
        val editor = code_editor ?: return
        editor.searcher.stopSearch()

        val content = editor.text
        val lineCount = content.lineCount
        if (lineCount <= 0) return

        val text = content.toString()
        val endLineColumn = offsetToLineColumn(buildLineStarts(text), match.endOffset.coerceIn(0, text.length))
        val startLine = match.line.coerceIn(0, lineCount - 1)
        val endLine = endLineColumn.first.coerceIn(0, lineCount - 1)
        val startColumn = match.column.coerceIn(0, content.getColumnCount(startLine))
        val endColumn = endLineColumn.second.coerceIn(0, content.getColumnCount(endLine))

        if (startLine == endLine && startColumn == endColumn) {
            editor.setSelection(startLine, startColumn, true)
        } else {
            editor.setSelectionRegion(startLine, startColumn, endLine, endColumn, true)
        }
        editor.requestFocus()
        sidebarSearchAdapter?.notifyDataSetChanged()
        updatePositionText()
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
            "c", "h", "cc", "cpp", "cxx", "hpp", "hh", "hxx" -> {
                return CODE_C
            }
            "go" -> {
                return CODE_GO
            }
            "rs" -> {
                return CODE_RUST
            }
            "zig" -> {
                return CODE_ZIG
            }
            "php", "phtml", "php3", "php4", "php5", "phpt", "aw", "ctp" -> {
                return CODE_PHP
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
        val editor = code_editor ?: return
        val colorScheme = editor.colorScheme
        editor.colorScheme = colorScheme
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
        val editor = code_editor ?: return
        if (tab == null || tab.previewOnly || isTextPreviewMode(tab)) return
        val cursor = editor.cursor
        var text =
            (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn + ";" + cursor.left + " "

        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            val content = editor.text
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
        val searcher = editor.searcher
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
