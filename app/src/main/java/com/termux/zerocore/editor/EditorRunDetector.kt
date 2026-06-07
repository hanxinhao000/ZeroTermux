package com.termux.zerocore.editor

import java.util.Locale
import java.util.regex.Pattern

enum class EditorRunLanguage {
    JAVA,
    C,
    PYTHON,
    PHP,
    NODE;

    fun matchesExtension(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.ROOT)
        return when (this) {
            JAVA -> lower.endsWith(".java")
            C -> lower.endsWith(".c")
            PYTHON -> lower.endsWith(".py")
            PHP -> lower.endsWith(".php")
            NODE -> lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".cjs")
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

    private val PHP_MAIN_FUNCTION_PATTERN = Pattern.compile(
        """\bfunction\s+main\s*\(""",
        Pattern.MULTILINE
    )

    private val PHP_MAIN_GUARD_PATTERN = Pattern.compile(
        """\bif\s*\(\s*php_sapi_name\s*\(\s*\)\s*===?\s*['\"]cli['\"]\s*\)""",
        Pattern.MULTILINE
    )

    private val PHP_MAIN_CALL_PATTERN = Pattern.compile(
        """\bmain\s*\(\s*\)\s*;""",
        Pattern.MULTILINE
    )

    private val NODE_MAIN_FUNCTION_PATTERN = Pattern.compile(
        """\b(?:async\s+)?function\s+main\s*\(""",
        Pattern.MULTILINE
    )

    private val NODE_MAIN_GUARD_PATTERN = Pattern.compile(
        """\brequire\.main\s*===\s*module\b""",
        Pattern.MULTILINE
    )

    private val NODE_MAIN_CALL_PATTERN = Pattern.compile(
        """\bmain\s*\(\s*\)\s*;""",
        Pattern.MULTILINE
    )

    fun detect(fileName: String, source: String): EditorRunLanguage? {
        if (source.isBlank()) return null
        return when {
            EditorRunLanguage.JAVA.matchesExtension(fileName) && hasJavaMain(source) -> EditorRunLanguage.JAVA
            EditorRunLanguage.C.matchesExtension(fileName) && hasCMain(source) -> EditorRunLanguage.C
            EditorRunLanguage.PYTHON.matchesExtension(fileName) && hasPythonMain(source) -> EditorRunLanguage.PYTHON
            EditorRunLanguage.PHP.matchesExtension(fileName) && hasPhpMain(source) -> EditorRunLanguage.PHP
            EditorRunLanguage.NODE.matchesExtension(fileName) && hasNodeMain(source) -> EditorRunLanguage.NODE
            else -> null
        }
    }

    fun hasJavaMain(source: String): Boolean = JAVA_MAIN_PATTERN.matcher(source).find()

    fun hasCMain(source: String): Boolean = C_MAIN_PATTERN.matcher(source).find()

    fun hasPythonMain(source: String): Boolean {
        return PYTHON_MAIN_GUARD_PATTERN.matcher(source).find()
            || PYTHON_MAIN_FUNCTION_PATTERN.matcher(source).find()
    }

    fun hasPhpMain(source: String): Boolean {
        if (!source.contains("<?php", ignoreCase = true) && !source.contains("<?", ignoreCase = true)) {
            return false
        }
        return PHP_MAIN_GUARD_PATTERN.matcher(source).find()
            || (PHP_MAIN_FUNCTION_PATTERN.matcher(source).find()
            && PHP_MAIN_CALL_PATTERN.matcher(source).find())
    }

    fun hasNodeMain(source: String): Boolean {
        return NODE_MAIN_GUARD_PATTERN.matcher(source).find()
            || (NODE_MAIN_FUNCTION_PATTERN.matcher(source).find()
            && NODE_MAIN_CALL_PATTERN.matcher(source).find())
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
