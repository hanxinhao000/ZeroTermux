package com.termux.zerocore.editor

import java.util.Locale
import java.util.regex.Pattern

enum class EditorRunLanguage {
    JAVA,
    C,
    PYTHON;

    fun matchesExtension(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.ROOT)
        return when (this) {
            JAVA -> lower.endsWith(".java")
            C -> lower.endsWith(".c")
            PYTHON -> lower.endsWith(".py")
        }
    }
}

object EditorRunDetector {

    private val JAVA_MAIN_PATTERN = Pattern.compile(
        """\bpublic\s+static\s+void\s+main\s*\(\s*String\s*(?:\[\s*\]|\.\.\.)\s*\w*\s*\)""",
        Pattern.MULTILINE
    )

    private val JAVA_PUBLIC_CLASS_PATTERN = Pattern.compile(
        """\bpublic\s+(?:final\s+)?class\s+(\w+)"""
    )

    private val C_MAIN_PATTERN = Pattern.compile(
        """\bint\s+main\s*\(""",
        Pattern.MULTILINE
    )

    private val PYTHON_MAIN_GUARD_PATTERN = Pattern.compile(
        """\bif\s+__name__\s*==\s*['\"]__main__['\"]\s*:""",
        Pattern.MULTILINE
    )

    private val PYTHON_MAIN_FUNCTION_PATTERN = Pattern.compile(
        """\bdef\s+main\s*\(""",
        Pattern.MULTILINE
    )

    fun detect(fileName: String, source: String): EditorRunLanguage? {
        if (source.isBlank()) return null
        return when {
            EditorRunLanguage.JAVA.matchesExtension(fileName) && hasJavaMain(source) -> EditorRunLanguage.JAVA
            EditorRunLanguage.C.matchesExtension(fileName) && hasCMain(source) -> EditorRunLanguage.C
            EditorRunLanguage.PYTHON.matchesExtension(fileName) && hasPythonMain(source) -> EditorRunLanguage.PYTHON
            else -> null
        }
    }

    fun hasJavaMain(source: String): Boolean = JAVA_MAIN_PATTERN.matcher(source).find()

    fun hasCMain(source: String): Boolean = C_MAIN_PATTERN.matcher(source).find()

    fun hasPythonMain(source: String): Boolean {
        return PYTHON_MAIN_GUARD_PATTERN.matcher(source).find()
            || PYTHON_MAIN_FUNCTION_PATTERN.matcher(source).find()
    }

    fun inferJavaClassName(source: String, fileName: String): String {
        val matcher = JAVA_PUBLIC_CLASS_PATTERN.matcher(source)
        if (matcher.find()) {
            return matcher.group(1) ?: fallbackBaseName(fileName)
        }
        return fallbackBaseName(fileName)
    }

    fun inferCBinaryName(fileName: String): String = fallbackBaseName(fileName)

    private fun fallbackBaseName(fileName: String): String {
        val base = fileName.substringBeforeLast('.')
        return base.ifEmpty { "main" }
    }
}
