package com.termux.zerocore.editor.lsp

import com.termux.shared.termux.TermuxConstants
import java.io.File
import java.security.MessageDigest

/**
 * Eclipse JDT Language Server（独立 Java 进程，stdio）。
 * 安装目录：~/.zerotermux/editor-lsp/jdtls/
 */
object EditorJdtLsSupport {
    const val PACKAGE_ID = "java-jdtls"

    /** 较稳妥的 milestone；体积约 46MB。需要 JDK 21+。 */
    const val DEFAULT_DOWNLOAD_URL =
        "https://download.eclipse.org/jdtls/milestones/1.44.0/jdt-language-server-1.44.0-202501221502.tar.gz"

    private const val INSTALL_DIR_NAME = "jdtls"
    private const val CONFIG_RUNTIME_DIR_NAME = "jdtls-config"
    private const val DATA_DIR_NAME = "jdtls-data"
    const val INIT_TIMEOUT_MILLIS = 120_000L

    fun installDir(): File = File(EditorLspInstaller.baseDir(), INSTALL_DIR_NAME)

    fun isInstalled(): Boolean {
        return findLauncherJar() != null && findConfigTemplateDir() != null && resolveJavaHome() != null
    }

    fun resolveJavaHome(): String? {
        val candidates = listOf(
            File(TermuxConstants.TERMUX_PREFIX_DIR, "lib/jvm/java-21-openjdk"),
            File(TermuxConstants.TERMUX_PREFIX_DIR, "lib/jvm/java-17-openjdk"),
            File(TermuxConstants.TERMUX_PREFIX_DIR, "opt/openjdk"),
            File(System.getenv("JAVA_HOME") ?: "")
        )
        candidates.forEach { home ->
            if (!home.isDirectory) return@forEach
            val javaBin = File(home, "bin/java")
            if (javaBin.canExecute() || javaBin.isFile) {
                return home.absolutePath
            }
        }
        val javaExec = EditorLspCommandResolver.resolveExecutablePath("java") ?: return null
        val binDir = File(javaExec).parentFile ?: return null
        val home = binDir.parentFile ?: return null
        return home.absolutePath.takeIf { File(it, "bin/java").exists() }
    }

    fun resolveJavaExecutable(): String? {
        val home = resolveJavaHome()
        if (home != null) {
            val java = File(home, "bin/java")
            if (java.isFile) return java.absolutePath
        }
        return EditorLspCommandResolver.resolveExecutablePath("java")
    }

    fun findLauncherJar(): File? {
        val plugins = File(installDir(), "plugins")
        if (!plugins.isDirectory) return null
        return plugins.listFiles()
            ?.filter { it.isFile && it.name.startsWith("org.eclipse.equinox.launcher_") && it.name.endsWith(".jar") }
            ?.maxByOrNull { it.name }
    }

    fun findConfigTemplateDir(): File? {
        val root = installDir()
        preferredConfigNames().forEach { name ->
            val dir = File(root, name)
            if (dir.isDirectory) return dir
        }
        return root.listFiles()?.firstOrNull { it.isDirectory && it.name.startsWith("config_") }
    }

    /** aarch64 必须用 *_arm，否则 OSGi 原生库架构不匹配。 */
    fun preferredConfigNames(): List<String> {
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val arm = arch.contains("aarch64") || arch.contains("arm64") || arch == "armv8l" || arch == "arm"
        return if (arm) {
            listOf(
                "config_linux_arm",
                "config_ss_linux_arm",
                "config_linux",
                "config_ss_linux",
                "config_mac_arm",
                "config_mac"
            )
        } else {
            listOf(
                "config_linux",
                "config_ss_linux",
                "config_linux_arm",
                "config_ss_linux_arm",
                "config_mac",
                "config_win"
            )
        }
    }

    fun ensureRuntimeConfigDir(): File? {
        val template = findConfigTemplateDir() ?: return null
        val runtime = File(EditorLspInstaller.baseDir(), CONFIG_RUNTIME_DIR_NAME)
        val marker = File(runtime, ".zt_config_source")
        val expected = template.name
        val needRefresh = !runtime.isDirectory ||
            !File(runtime, "config.ini").isFile ||
            marker.takeIf { it.isFile }?.readText()?.trim() != expected
        if (needRefresh) {
            if (runtime.exists()) runtime.deleteRecursively()
            runtime.mkdirs()
            template.copyRecursively(runtime, overwrite = true)
            marker.writeText(expected)
        }
        return runtime.takeIf { it.isDirectory }
    }

    fun dataDir(): File {
        val dir = File(EditorLspInstaller.baseDir(), DATA_DIR_NAME)
        dir.mkdirs()
        return dir
    }

    /** 按工程根划分 -data，避免多项目索引互相污染。 */
    fun dataDirForProject(projectRoot: File?): File {
        val base = dataDir()
        if (projectRoot == null) return File(base, "default").also { it.mkdirs() }
        val hash = sha1Short(projectRoot.absolutePath)
        return File(base, hash).also { it.mkdirs() }
    }

    fun resolveLaunchSpec(projectRoot: File?): EditorLspLaunchSpec? {
        val java = resolveJavaExecutable() ?: return null
        val launcher = findLauncherJar() ?: return null
        val config = ensureRuntimeConfigDir() ?: return null
        val data = dataDirForProject(projectRoot)
        val args = listOf(
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Dlog.level=ERROR",
            "-Xmx1G",
            "--add-modules=ALL-SYSTEM",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "-jar", launcher.absolutePath,
            "-configuration", config.absolutePath,
            "-data", data.absolutePath,
            "--stdio"
        )
        return EditorLspLaunchSpec(java, args)
    }

    fun environmentExtras(): Map<String, String> {
        val home = resolveJavaHome() ?: return emptyMap()
        return mapOf("JAVA_HOME" to home)
    }

    /**
     * 向上查找工程根：pom.xml / build.gradle(.kts) / settings.gradle(.kts) / .git
     * 绝不返回 `/`，避免 jdt-ls 扫描系统根触发 AccessDeniedException 刷屏。
     */
    fun findProjectRoot(file: File): File {
        val home = TermuxConstants.TERMUX_HOME_DIR
        var current: File? = if (file.isDirectory) file else file.parentFile
        val start = current ?: home
        var fallback = start
        while (current != null) {
            val path = current.absolutePath
            if (path == "/" || path == "/data" || path == "/data/data") {
                break
            }
            if (File(current, "pom.xml").isFile ||
                File(current, "build.gradle").isFile ||
                File(current, "build.gradle.kts").isFile ||
                File(current, "settings.gradle").isFile ||
                File(current, "settings.gradle.kts").isFile ||
                File(current, ".git").exists()
            ) {
                return current
            }
            fallback = current
            if (current == home || path == home.absolutePath) {
                break
            }
            current = current.parentFile
        }
        return when {
            fallback.absolutePath == "/" -> home
            fallback.isDirectory -> fallback
            else -> home
        }
    }

    fun installShellScript(downloadUrl: String = DEFAULT_DOWNLOAD_URL): String {
        val baseQ = shellQuote(EditorLspInstaller.baseDir().absolutePath)
        val installQ = shellQuote(installDir().absolutePath)
        val tmpTarQ = shellQuote(File(EditorLspInstaller.baseDir(), "jdtls-download.tar.gz").absolutePath)
        val configRuntimeQ = shellQuote(File(EditorLspInstaller.baseDir(), CONFIG_RUNTIME_DIR_NAME).absolutePath)
        val urlQ = shellQuote(downloadUrl)
        return """
            set -e
            export HOME=${shellQuote(TermuxConstants.TERMUX_HOME_DIR_PATH)}
            export PREFIX=${shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)}
            export PATH=${shellQuote(EditorLspCommandResolver.buildPath(null))}
            mkdir -p $baseQ
            echo '[ZeroTermux Editor] Installing OpenJDK for jdt-ls...'
            if command -v pkg >/dev/null 2>&1; then
              pkg install -y openjdk-21 || pkg install -y openjdk-17 || true
            fi
            if ! command -v java >/dev/null 2>&1; then
              echo 'java not found after pkg install'
              exit 127
            fi
            echo '[ZeroTermux Editor] Downloading Eclipse JDT Language Server...'
            rm -rf $installQ
            mkdir -p $installQ
            if command -v curl >/dev/null 2>&1; then
              curl -L --fail -o $tmpTarQ $urlQ
            else
              wget -O $tmpTarQ $urlQ
            fi
            tar -xzf $tmpTarQ -C $installQ
            rm -f $tmpTarQ
            if [ ! -d $installQ/plugins ]; then
              for d in $installQ/*; do
                if [ -d "${'$'}d/plugins" ]; then
                  mv "${'$'}d"/* $installQ/ 2>/dev/null || true
                  rmdir "${'$'}d" 2>/dev/null || true
                  break
                fi
              done
            fi
            test -d $installQ/plugins
            rm -rf $configRuntimeQ
            echo '[ZeroTermux Editor] jdt-ls install done'
            java -version
        """.trimIndent()
    }

    private fun sha1Short(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(12)
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
