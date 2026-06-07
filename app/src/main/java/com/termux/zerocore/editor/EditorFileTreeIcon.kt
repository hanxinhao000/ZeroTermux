package com.termux.zerocore.editor

import com.termux.R
import java.io.File
import java.util.Locale

object EditorFileTreeIcon {

    data class IconStyle(
        val drawableRes: Int,
        val tintColor: Int
    )

    fun resolve(file: File, expanded: Boolean): IconStyle {
        if (file.isDirectory) {
            return IconStyle(
                if (expanded) R.drawable.ic_editor_folder_open else R.drawable.ic_editor_folder,
                COLOR_FOLDER
            )
        }
        val extension = extension(file)
        return IconStyle(R.drawable.ic_editor_file, fileTint(extension))
    }

    private fun extension(file: File): String {
        val name = file.name
        if (!name.contains('.')) return ""
        return name.substringAfterLast('.').lowercase(Locale.ROOT)
    }

    private fun fileTint(extension: String): Int {
        return when (extension) {
            "java" -> COLOR_JAVA
            "kt", "kts" -> COLOR_KOTLIN
            "c", "h" -> COLOR_C
            "cc", "cpp", "cxx", "hpp", "hh", "hxx" -> COLOR_CPP
            "py", "pyw" -> COLOR_PYTHON
            "js", "mjs", "cjs", "jsx" -> COLOR_JS
            "ts", "tsx" -> COLOR_TS
            "json", "jsonc", "json5", "webmanifest" -> COLOR_JSON
            "xml", "xhtml", "xaml", "plist", "svg" -> COLOR_XML
            "html", "htm", "shtml", "vue" -> COLOR_HTML
            "css", "scss", "sass", "less" -> COLOR_CSS
            "md", "markdown", "mdown" -> COLOR_MARKDOWN
            "sh", "bash", "zsh", "fish" -> COLOR_SHELL
            "gradle", "groovy" -> COLOR_GRADLE
            "go" -> COLOR_GO
            "rs" -> COLOR_RUST
            "php", "phtml" -> COLOR_PHP
            "lua" -> COLOR_LUA
            "yaml", "yml" -> COLOR_YAML
            "toml" -> COLOR_TOML
            "sql" -> COLOR_SQL
            "rb" -> COLOR_RUBY
            "swift" -> COLOR_SWIFT
            "zig" -> COLOR_ZIG
            "png", "jpg", "jpeg", "gif", "webp", "bmp" -> COLOR_IMAGE
            "properties", "prop", "ini", "cfg", "conf", "env" -> COLOR_CONFIG
            else -> COLOR_DEFAULT
        }
    }

    private const val COLOR_FOLDER = 0xFFE8C468.toInt()
    private const val COLOR_DEFAULT = 0xFF9D9D9D.toInt()
    private const val COLOR_JAVA = 0xFFE76F00.toInt()
    private const val COLOR_KOTLIN = 0xFF9B7BFF.toInt()
    private const val COLOR_C = 0xFF5C9FD6.toInt()
    private const val COLOR_CPP = 0xFF007ACC.toInt()
    private const val COLOR_PYTHON = 0xFF4584B6.toInt()
    private const val COLOR_JS = 0xFFF0DB4F.toInt()
    private const val COLOR_TS = 0xFF3178C6.toInt()
    private const val COLOR_JSON = 0xFFCBCB41.toInt()
    private const val COLOR_XML = 0xFFE37933.toInt()
    private const val COLOR_HTML = 0xFFE44D26.toInt()
    private const val COLOR_CSS = 0xFF42A5F5.toInt()
    private const val COLOR_MARKDOWN = 0xFF519ABA.toInt()
    private const val COLOR_SHELL = 0xFF89E051.toInt()
    private const val COLOR_GRADLE = 0xFF23A959.toInt()
    private const val COLOR_GO = 0xFF00ADD8.toInt()
    private const val COLOR_RUST = 0xFFDEA584.toInt()
    private const val COLOR_PHP = 0xFF8892BF.toInt()
    private const val COLOR_LUA = 0xFF51A0DA.toInt()
    private const val COLOR_YAML = 0xFFCB171E.toInt()
    private const val COLOR_TOML = 0xFF9C4221.toInt()
    private const val COLOR_SQL = 0xFFFF9900.toInt()
    private const val COLOR_RUBY = 0xFFE0115F.toInt()
    private const val COLOR_SWIFT = 0xFFFF672D.toInt()
    private const val COLOR_ZIG = 0xFFF7A41D.toInt()
    private const val COLOR_IMAGE = 0xFFBC86C8.toInt()
    private const val COLOR_CONFIG = 0xFF8B8B8B.toInt()
}
