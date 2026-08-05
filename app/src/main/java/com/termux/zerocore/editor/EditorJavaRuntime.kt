package com.termux.zerocore.editor

import com.termux.shared.termux.TermuxConstants
import java.io.File

/**
 * 编辑器 Java 编译/运行统一使用 OpenJDK 21（与 jdt-ls 一致）。
 * 若 PATH 上仍是 17，通过 JAVA_HOME 强制切到 21。
 */
object EditorJavaRuntime {
    const val PACKAGE_NAME = "openjdk-21"
    const val DIR_NAME = "java-21-openjdk"

    fun preferredHome(): File {
        return File(TermuxConstants.TERMUX_PREFIX_DIR, "lib/jvm/$DIR_NAME")
    }

    fun resolveHome(): File? {
        val preferred = preferredHome()
        if (isUsableJdkHome(preferred)) return preferred
        val fallbacks = listOf(
            File(TermuxConstants.TERMUX_PREFIX_DIR, "lib/jvm/java-17-openjdk"),
            File(TermuxConstants.TERMUX_PREFIX_DIR, "opt/openjdk")
        )
        fallbacks.forEach { home ->
            if (isUsableJdkHome(home)) return home
        }
        return null
    }

    fun isJdk21Ready(): Boolean = isUsableJdkHome(preferredHome())

    /** 终端安装：优先 openjdk-21。 */
    fun installPackageCommand(): String {
        return "pkg install -y $PACKAGE_NAME || pkg install -y openjdk-17 || pkg install -y openjdk"
    }

    /**
     * bash：优先 JAVA_HOME=java-21-openjdk，并把 bin 放到 PATH 最前。
     * 仅当 21 不存在时才回退 17 / 其它。
     */
    fun selectJavaHomeShell(): String {
        val prefixQ = shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)
        val jdk21Q = shellQuote(preferredHome().absolutePath)
        val jdk17Q = shellQuote(
            File(TermuxConstants.TERMUX_PREFIX_DIR, "lib/jvm/java-17-openjdk").absolutePath
        )
        return """
            _zt_prefix=$prefixQ
            if [ -x $jdk21Q/bin/javac ] && [ -x $jdk21Q/bin/java ]; then
              export JAVA_HOME=$jdk21Q
            elif [ -x $jdk17Q/bin/javac ] && [ -x $jdk17Q/bin/java ]; then
              export JAVA_HOME=$jdk17Q
            else
              JAVA_HOME=
              for _zt_jdk in "${'$'}_zt_prefix/lib/jvm"/java-*-openjdk "${'$'}_zt_prefix/opt/openjdk"; do
                if [ -x "${'$'}_zt_jdk/bin/javac" ] && [ -x "${'$'}_zt_jdk/bin/java" ]; then
                  export JAVA_HOME="${'$'}_zt_jdk"
                  break
                fi
              done
            fi
            if [ -n "${'$'}JAVA_HOME" ]; then
              export PATH="${'$'}JAVA_HOME/bin:${'$'}PATH"
            fi
        """.trimIndent()
    }

    /**
     * ensure_java 函数：缺 JDK21 则 pkg 安装；编译前始终 select 到 21。
     */
    fun ensureJavaFunctionShell(installEchoQuoted: String, failedEchoQuoted: String): String {
        val jdk21Q = shellQuote(preferredHome().absolutePath)
        val select = selectJavaHomeShell()
        return """
            ensure_java() {
              $select
              if [ ! -x $jdk21Q/bin/javac ] || [ ! -x $jdk21Q/bin/java ]; then
                echo $installEchoQuoted
                ${installPackageCommand()}
                $select
              fi
              if [ ! -x "${'$'}{JAVA_HOME:-}/bin/javac" ] || [ ! -x "${'$'}{JAVA_HOME:-}/bin/java" ]; then
                echo $failedEchoQuoted
                exit 1
              fi
            }
        """.trimIndent()
    }

    /** 在子 shell 中带 JDK21 环境执行命令。 */
    fun withJavaEnv(command: String): String {
        val compact = selectJavaHomeShell().lines().joinToString(" ") { it.trim() }.trim()
        return "( $compact ; $command )"
    }

    private fun isUsableJdkHome(home: File): Boolean {
        return File(home, "bin/javac").canExecute() && File(home, "bin/java").canExecute()
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
