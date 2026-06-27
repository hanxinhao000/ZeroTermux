package com.termux.zerocore.editor

import com.termux.shared.termux.TermuxConstants
import java.io.File

object EditorBuildScriptHelper {

    const val SCRIPT_NAME = "build.sh"

    fun scriptFile(directory: File): File = File(directory, SCRIPT_NAME)

    fun ensureScript(directory: File, contextFile: File?, source: String?): File {
        val script = scriptFile(directory)
        if (!script.exists()) {
            script.parentFile?.mkdirs()
            script.writeText(defaultScript(contextFile, source))
        }
        return script
    }

    fun defaultScript(contextFile: File?, source: String?): String {
        val shebang = "#!${TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH}/bash"
        val header = buildString {
            appendLine(shebang)
            appendLine("set -e")
            appendLine("# ZeroTermux build script — 可手动编辑或由 AI 修改")
            appendLine()
        }
        if (contextFile == null) {
            return header + "echo \"请在 build.sh 中添加编译/运行命令\"\n"
        }
        val fileName = contextFile.name
        val fileSource = source.orEmpty()
        val body = when {
            EditorRunLanguage.JAVA.matchesExtension(fileName) -> {
                val className = EditorRunDetector.inferJavaClassName(fileSource, fileName)
                """
                SOURCE="$fileName"
                javac "${'$'}SOURCE"
                java "$className"
                """.trimIndent()
            }
            EditorRunLanguage.C.matchesExtension(fileName) -> {
                val binary = EditorRunDetector.inferCBinaryName(fileName)
                """
                SOURCE="$fileName"
                BIN="$binary"
                gcc "${'$'}SOURCE" -o "${'$'}BIN"
                ./"${'$'}BIN"
                """.trimIndent()
            }
            EditorRunLanguage.PYTHON.matchesExtension(fileName) -> """
                SOURCE="$fileName"
                python3 "${'$'}SOURCE"
            """.trimIndent()
            EditorRunLanguage.PHP.matchesExtension(fileName) -> """
                SOURCE="$fileName"
                php "${'$'}SOURCE"
            """.trimIndent()
            EditorRunLanguage.NODE.matchesExtension(fileName) -> """
                SOURCE="$fileName"
                node "${'$'}SOURCE"
            """.trimIndent()
            else -> """
                # 当前文件: $fileName
                echo "请编辑 build.sh 以构建/运行 $fileName"
            """.trimIndent()
        }
        return header + body + "\n"
    }
}
