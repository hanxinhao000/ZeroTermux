package com.termux.zerocore.editor

import android.content.Context
import com.termux.R
import com.termux.shared.termux.TermuxConstants
import java.io.File

object EditorBuildScriptHelper {

    const val SCRIPT_NAME = "build.sh"
    private const val SCRIPT_MARKER = "# ZeroTermux build script"
    private const val SCRIPT_VNC_MARKER = EditorVncEnvironment.SCRIPT_VNC_MARKER

    fun scriptFile(directory: File): File = File(directory, SCRIPT_NAME)

    fun ensureScript(context: Context, directory: File, contextFile: File?, source: String?): File {
        val script = scriptFile(directory)
        val desired = defaultScript(context, contextFile, source)
        if (!script.exists()) {
            script.parentFile?.mkdirs()
            script.writeText(desired)
        } else if (shouldUpgradeLegacyScript(script.readText(), scriptLocaleTag(context))) {
            script.writeText(desired)
        }
        return script
    }

    /** 旧版 build.sh 或界面语言变化时，升级为当前语言的自动安装版本。 */
    private fun shouldUpgradeLegacyScript(content: String, localeTag: String): Boolean {
        if (!content.contains(SCRIPT_MARKER)) {
            return false
        }
        if (!content.contains(SCRIPT_VNC_MARKER)) {
            return true
        }
        if (!content.contains("lang=$localeTag")) {
            return true
        }
        if (!content.contains("export DISPLAY=${EditorVncEnvironment.DISPLAY}")) {
            return true
        }
        if (!content.contains("ensure_editor_x11vnc")) {
            return true
        }
        if (!content.contains("/dev/tcp/127.0.0.1/${EditorVncEnvironment.VNC_PORT}")) {
            return true
        }
        return !content.contains("ensure_java()")
            && !content.contains("ensure_cc()")
            && !content.contains("ensure_python()")
            && !content.contains("ensure_php()")
            && !content.contains("ensure_node()")
            || content.contains("xorg-fonts-dejavu")
    }

    fun defaultScript(context: Context, contextFile: File?, source: String?): String {
        val strings = ScriptStrings(context)
        val header = scriptHeader(strings)
        if (contextFile == null) {
            return header + "echo ${strings.addCommands}\n"
        }
        val fileName = contextFile.name
        val fileSource = source.orEmpty()
        val body = when {
            EditorRunLanguage.JAVA.matchesExtension(fileName) -> javaScriptBody(strings, fileName, fileSource)
            EditorRunLanguage.C.matchesExtension(fileName) -> cScriptBody(fileName)
            EditorRunLanguage.PYTHON.matchesExtension(fileName) -> pythonScriptBody(fileName)
            EditorRunLanguage.PHP.matchesExtension(fileName) -> phpScriptBody(fileName)
            EditorRunLanguage.NODE.matchesExtension(fileName) -> nodeScriptBody(fileName)
            else -> """
                # ${strings.currentFileComment(fileName)}
                echo ${strings.editForFile(fileName)}
            """.trimIndent()
        }
        return header + body + "\n"
    }

    private fun scriptLocaleTag(context: Context): String {
        return context.resources.configuration.locales[0].toLanguageTag()
    }

    private fun bashSingleQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private class ScriptStrings(private val context: Context) {
        val localeTag = scriptLocaleTag(context)
        val headerNote = context.getString(R.string.editor_build_script_header_note)
        val addCommands = bashSingleQuote(context.getString(R.string.editor_build_script_add_commands))
        private val installJava = bashSingleQuote(context.getString(R.string.editor_build_script_install_java))
        private val installJavaFailed = bashSingleQuote(context.getString(R.string.editor_build_script_install_java_failed))
        private val installC = bashSingleQuote(context.getString(R.string.editor_build_script_install_c))
        private val installCFailed = bashSingleQuote(context.getString(R.string.editor_build_script_install_c_failed))
        private val installPython = bashSingleQuote(context.getString(R.string.editor_build_script_install_python))
        private val installPythonFailed = bashSingleQuote(context.getString(R.string.editor_build_script_install_python_failed))
        private val installPhp = bashSingleQuote(context.getString(R.string.editor_build_script_install_php))
        private val installPhpFailed = bashSingleQuote(context.getString(R.string.editor_build_script_install_php_failed))
        private val installNode = bashSingleQuote(context.getString(R.string.editor_build_script_install_node))
        private val installNodeFailed = bashSingleQuote(context.getString(R.string.editor_build_script_install_node_failed))
        private val installGuiFonts = bashSingleQuote(context.getString(R.string.editor_build_script_install_gui_fonts))
        private val guiStarted = bashSingleQuote(context.getString(R.string.editor_build_script_gui_started))
        private val guiSetVisibleHint = bashSingleQuote(context.getString(R.string.editor_build_script_gui_set_visible_hint))

        fun currentFileComment(fileName: String): String {
            return context.getString(R.string.editor_build_script_current_file, fileName)
        }

        fun editForFile(fileName: String): String {
            return bashSingleQuote(context.getString(R.string.editor_build_script_edit_for_file, fileName))
        }

        fun ensureFunctionsBlock(): String {
            return """
                ensure_java() {
                  if command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
                    return 0
                  fi
                  echo $installJava
                  pkg install -y openjdk-17 || pkg install -y openjdk-21 || pkg install -y openjdk
                  if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
                    echo $installJavaFailed
                    exit 1
                  fi
                }

                ensure_cc() {
                  if command -v gcc >/dev/null 2>&1 || command -v clang >/dev/null 2>&1 || command -v cc >/dev/null 2>&1; then
                    return 0
                  fi
                  echo $installC
                  pkg install -y clang || pkg install -y gcc
                  if ! command -v gcc >/dev/null 2>&1 && ! command -v clang >/dev/null 2>&1 && ! command -v cc >/dev/null 2>&1; then
                    echo $installCFailed
                    exit 1
                  fi
                }

                resolve_cc() {
                  ensure_cc
                  if command -v gcc >/dev/null 2>&1; then
                    CC=gcc
                  elif command -v clang >/dev/null 2>&1; then
                    CC=clang
                  else
                    CC=cc
                  fi
                }

                ensure_python() {
                  if command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1; then
                    return 0
                  fi
                  echo $installPython
                  pkg install -y python
                  if ! command -v python3 >/dev/null 2>&1 && ! command -v python >/dev/null 2>&1; then
                    echo $installPythonFailed
                    exit 1
                  fi
                }

                resolve_python() {
                  ensure_python
                  if command -v python3 >/dev/null 2>&1; then
                    PYTHON=python3
                  else
                    PYTHON=python
                  fi
                }

                ensure_php() {
                  if command -v php >/dev/null 2>&1; then
                    return 0
                  fi
                  echo $installPhp
                  pkg install -y php composer
                  if ! command -v php >/dev/null 2>&1; then
                    echo $installPhpFailed
                    exit 1
                  fi
                }

                ensure_node() {
                  if command -v node >/dev/null 2>&1; then
                    return 0
                  fi
                  echo $installNode
                  pkg install -y nodejs
                  if ! command -v node >/dev/null 2>&1; then
                    echo $installNodeFailed
                    exit 1
                  fi
                }

                ${EditorVncEnvironment.coreVncShellFunctions()}

                ensure_java_gui() {
                  export DISPLAY=${EditorVncEnvironment.DISPLAY}
                  ensure_editor_xvfb || exit 1
                  ensure_editor_x11vnc || exit 1
                  if ! pkg list-installed 2>/dev/null | grep -q ttf-dejavu; then
                    echo $installGuiFonts
                    pkg install -y ttf-dejavu 2>/dev/null || true
                  fi
                }
            """.trimIndent()
        }

        fun guiSetVisibleHintQuoted(): String = guiSetVisibleHint

        fun guiStartedQuoted(): String = guiStarted

        private fun bashSingleQuote(value: String): String {
            return EditorBuildScriptHelper.bashSingleQuote(value)
        }
    }

    private fun scriptHeader(strings: ScriptStrings): String {
        val shebang = "#!${TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH}/bash"
        return buildString {
            appendLine(shebang)
            appendLine("set -e")
            appendLine("export DISPLAY=${EditorVncEnvironment.DISPLAY}")
            appendLine("$SCRIPT_MARKER lang=${strings.localeTag} $SCRIPT_VNC_MARKER — ${strings.headerNote}")
            appendLine()
            appendLine(strings.ensureFunctionsBlock())
            appendLine()
        }
    }

    private fun javaScriptBody(strings: ScriptStrings, fileName: String, fileSource: String): String {
        val className = EditorRunDetector.inferJavaClassName(fileSource, fileName)
        if (!EditorRunDetector.isJavaGuiSource(fileSource)) {
            return """
                ensure_java
                SOURCE="$fileName"
                javac "${'$'}SOURCE"
                java "$className"
            """.trimIndent()
        }
        val setVisibleWarn = if (!EditorRunDetector.hasJavaSetVisible(fileSource)) {
            "echo ${strings.guiSetVisibleHintQuoted()}\n"
        } else {
            ""
        }
        return """
            ensure_java
            ensure_java_gui
            SOURCE="$fileName"
            CLASS="$className"
            javac "${'$'}SOURCE"
            ${setVisibleWarn}export LIBGL_ALWAYS_SOFTWARE=1
            java -Djava.awt.headless=false "${'$'}CLASS" &
            GUI_PID=${'$'}!
            sleep 0.8
            if ! kill -0 "${'$'}GUI_PID" 2>/dev/null; then
              wait "${'$'}GUI_PID" 2>/dev/null || true
              exit 1
            fi
            echo ${strings.guiStartedQuoted()} "(PID ${'$'}GUI_PID, DISPLAY=${EditorVncEnvironment.DISPLAY})"
            refresh_editor_gui_display
        """.trimIndent()
    }

    private fun cScriptBody(fileName: String): String {
        val binary = EditorRunDetector.inferCBinaryName(fileName)
        return """
            resolve_cc
            SOURCE="$fileName"
            BIN="$binary"
            "${'$'}CC" "${'$'}SOURCE" -o "${'$'}BIN"
            ./"${'$'}BIN"
        """.trimIndent()
    }

    private fun pythonScriptBody(fileName: String): String = """
        resolve_python
        SOURCE="$fileName"
        "${'$'}PYTHON" "${'$'}SOURCE"
    """.trimIndent()

    private fun phpScriptBody(fileName: String): String = """
        ensure_php
        SOURCE="$fileName"
        php "${'$'}SOURCE"
    """.trimIndent()

    private fun nodeScriptBody(fileName: String): String = """
        ensure_node
        SOURCE="$fileName"
        node "${'$'}SOURCE"
    """.trimIndent()
}
