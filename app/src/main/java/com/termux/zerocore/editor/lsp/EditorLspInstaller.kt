package com.termux.zerocore.editor.lsp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.xh_lib.utils.UUtils
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment
import com.termux.zerocore.utils.SingletonCommunicationUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class EditorLspInstaller(private val context: Context) {
    data class ServerPackage(
        val id: String,
        val displayName: String,
        val description: String,
        val languageIds: List<String>,
        val npmPackages: List<String>,
        val commands: Map<String, String>,
        val requiredOnFirstOpen: Boolean = false
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    fun availablePackages(): List<ServerPackage> {
        return PACKAGES
    }

    fun isNpmInstalled(): Boolean {
        return commandExists("npm")
    }

    fun ensureBasicShellInstalled(onFinished: ((Boolean) -> Unit)? = null) {
        installPackage(SHELL_BASIC_ID, quietIfInstalled = true) { success, _ ->
            onFinished?.invoke(success)
        }
    }

    fun installPackage(packageId: String, quietIfInstalled: Boolean = false, onFinished: ((Boolean, String) -> Unit)? = null) {
        val serverPackage = packageById(packageId) ?: return
        if (isPackageInstalled(packageId)) {
            if (!quietIfInstalled) postMessage("${serverPackage.displayName} 已安装")
            onFinished?.invoke(true, "installed")
            return
        }
        synchronized(installingPackages) {
            if (!installingPackages.add(packageId)) {
                if (!quietIfInstalled) postMessage("${serverPackage.displayName} 正在下载")
                return
            }
        }
        postMessage("正在安装 ${serverPackage.displayName}")
        Thread {
            val result = runCatching {
                installPackageWorker(serverPackage)
            }
            synchronized(installingPackages) {
                installingPackages.remove(packageId)
            }
            if (result.isSuccess) {
                markerFile(packageId).writeText(serverPackage.npmPackages.joinToString("\n"))
                postMessage("${serverPackage.displayName} 安装完成")
                mainHandler.post { onFinished?.invoke(true, "installed") }
            } else {
                val message = result.exceptionOrNull()?.message ?: "安装失败"
                postMessage("${serverPackage.displayName} 安装失败: ${message.take(120)}")
                mainHandler.post { onFinished?.invoke(false, message) }
            }
        }.apply {
            name = "ZT-LSP-Install-$packageId"
            isDaemon = true
            start()
        }
    }

    fun isInstalling(packageId: String): Boolean {
        synchronized(installingPackages) {
            return installingPackages.contains(packageId)
        }
    }

    fun isPackageInstalled(packageId: String): Boolean {
        val serverPackage = packageById(packageId) ?: return false
        return serverPackage.commands.values.all { command -> commandExists(command.substringBefore(' ')) }
    }

    fun isLanguageInstalled(languageId: String): Boolean {
        return PACKAGES.any { serverPackage ->
            languageId in serverPackage.languageIds && isPackageInstalled(serverPackage.id)
        }
    }

    fun commandForLanguage(languageId: String): String? {
        return PACKAGES.firstOrNull { serverPackage ->
            languageId in serverPackage.languageIds && isPackageInstalled(serverPackage.id)
        }?.commands?.get(languageId)
    }

    private fun installPackageWorker(serverPackage: ServerPackage) {
        ensureNpmReady()
        if (canUseTerminal()) {
            sendLspInstallToTerminal(serverPackage)
            if (!waitForCondition({ isPackageInstalled(serverPackage.id) }, LSP_INSTALL_WAIT_MS)) {
                throw IllegalStateException("终端安装超时，请在 Termux 中确认命令是否执行完成")
            }
        } else {
            installPackageBlocking(serverPackage)
        }
    }

    private fun ensureNpmReady() {
        if (isNpmInstalled()) return
        if (canUseTerminal()) {
            mainHandler.post {
                postMessage("正在通过 Termux 终端安装 Node.js / npm…")
                sendNpmInstallToTerminal()
            }
            if (!waitForCondition({ isNpmInstalled() }, NPM_INSTALL_WAIT_MS)) {
                throw IllegalStateException("npm 安装超时，请在 Termux 终端中手动执行: pkg install -y nodejs-lts")
            }
            return
        }
        installNpmBlocking()
        if (!isNpmInstalled()) {
            throw IllegalStateException("npm 未安装且无法连接 Termux 终端")
        }
    }

    private fun canUseTerminal(): Boolean {
        return SingletonCommunicationUtils.getInstance().hasTerminalListener()
    }

    private fun sendToTerminal(command: String) {
        SingletonCommunicationUtils.getInstance()
            .getmSingletonCommunicationListener()
            .sendTextToTerminal(command)
    }

    private fun sendNpmInstallToTerminal() {
        sendToTerminal("echo '[ZeroTermux Editor] Installing nodejs/npm...'\n")
        sendToTerminal("pkg install -y nodejs-lts || pkg install -y nodejs\n")
    }

    private fun sendLspInstallToTerminal(serverPackage: ServerPackage) {
        val npmPackages = serverPackage.npmPackages.joinToString(" ")
        sendToTerminal("echo '[ZeroTermux Editor] Installing LSP: ${serverPackage.displayName}'\n")
        sendToTerminal("npm install -g --no-audit --no-fund $npmPackages\n")
    }

    private fun waitForCondition(check: () -> Boolean, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (check()) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return check()
    }

    private fun installNpmBlocking() {
        val script = """
            set -e
            export HOME=${shellQuote(TermuxConstants.TERMUX_HOME_DIR_PATH)}
            export PREFIX=${shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)}
            export PATH=${shellQuote(buildPath(null))}
            if command -v pkg >/dev/null 2>&1; then
                pkg install -y nodejs-lts || pkg install -y nodejs
            else
                echo "pkg not found"
                exit 127
            fi
        """.trimIndent()
        runShellScript(script)
    }

    private fun installPackageBlocking(serverPackage: ServerPackage) {
        baseDir().mkdirs()
        if (!isNpmInstalled()) {
            installNpmBlocking()
        }
        val npmPackages = serverPackage.npmPackages.joinToString(" ") { shellQuote(it) }
        val script = """
            set -e
            export HOME=${shellQuote(TermuxConstants.TERMUX_HOME_DIR_PATH)}
            export PREFIX=${shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)}
            export PATH=${shellQuote(buildPath(null))}
            npm install -g --no-audit --no-fund $npmPackages
        """.trimIndent()
        runShellScript(script)
        if (!isPackageInstalled(serverPackage.id)) {
            throw IllegalStateException("LSP 命令未就绪，请检查 npm 全局安装路径")
        }
    }

    private fun runShellScript(script: String) {
        val shell = File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "sh")
        val shellPath = if (shell.canExecute()) shell.absolutePath else "/system/bin/sh"
        val processBuilder = ProcessBuilder(shellPath, "-lc", script)
        processBuilder.directory(TermuxConstants.TERMUX_HOME_DIR)
        processBuilder.redirectErrorStream(true)
        processBuilder.environment().putAll(TermuxShellEnvironment().getEnvironment(context, false))
        processBuilder.environment()["PATH"] = buildPath(processBuilder.environment()["PATH"])
        val process = processBuilder.start()
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
            lines.forEach { line ->
                if (output.length < MAX_OUTPUT_LENGTH) {
                    output.append(line).append('\n')
                }
            }
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException(output.toString().trim().ifEmpty { "exit code $exitCode" })
        }
    }

    private fun commandExists(commandName: String): Boolean {
        return File(TermuxConstants.TERMUX_BIN_PREFIX_DIR, commandName).canExecute()
    }

    private fun markerFile(packageId: String): File {
        return File(baseDir(), "installed-$packageId.marker")
    }

    private fun postMessage(message: String) {
        mainHandler.post {
            UUtils.showMsg(message)
        }
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    companion object {
        const val SHELL_BASIC_ID = "shell-basic"
        private const val MAX_OUTPUT_LENGTH = 4000
        private const val POLL_INTERVAL_MS = 2000L
        private const val NPM_INSTALL_WAIT_MS = 10 * 60 * 1000L
        private const val LSP_INSTALL_WAIT_MS = 15 * 60 * 1000L
        private val installingPackages = LinkedHashSet<String>()

        private val PACKAGES = listOf(
            ServerPackage(
                id = SHELL_BASIC_ID,
                displayName = "Shell 基础 LSP (bash/zsh/fish)",
                description = "首次打开编辑器自动初始化，提供 shell 脚本基础补全",
                languageIds = listOf(EditorLspManager.LANGUAGE_SHELL),
                npmPackages = listOf("bash-language-server"),
                commands = mapOf(EditorLspManager.LANGUAGE_SHELL to "bash-language-server start"),
                requiredOnFirstOpen = true
            ),
            ServerPackage(
                id = "json",
                displayName = "JSON / JSONC LSP",
                description = "提供 JSON、JSONC 补全",
                languageIds = listOf(EditorLspManager.LANGUAGE_JSON, EditorLspManager.LANGUAGE_JSONC),
                npmPackages = listOf("vscode-langservers-extracted"),
                commands = mapOf(
                    EditorLspManager.LANGUAGE_JSON to "vscode-json-language-server --stdio",
                    EditorLspManager.LANGUAGE_JSONC to "vscode-json-language-server --stdio"
                )
            ),
            ServerPackage(
                id = "typescript",
                displayName = "JavaScript / TypeScript LSP",
                description = "提供 JS、TS、JSX、TSX 补全",
                languageIds = listOf(EditorLspManager.LANGUAGE_JAVASCRIPT, EditorLspManager.LANGUAGE_TYPESCRIPT),
                npmPackages = listOf("typescript", "typescript-language-server"),
                commands = mapOf(
                    EditorLspManager.LANGUAGE_JAVASCRIPT to "typescript-language-server --stdio",
                    EditorLspManager.LANGUAGE_TYPESCRIPT to "typescript-language-server --stdio"
                )
            ),
            ServerPackage(
                id = "python",
                displayName = "Python LSP",
                description = "提供 Python 补全",
                languageIds = listOf(EditorLspManager.LANGUAGE_PYTHON),
                npmPackages = listOf("pyright"),
                commands = mapOf(EditorLspManager.LANGUAGE_PYTHON to "pyright-langserver --stdio")
            ),
            ServerPackage(
                id = "yaml",
                displayName = "YAML LSP",
                description = "提供 YAML 补全",
                languageIds = listOf(EditorLspManager.LANGUAGE_YAML),
                npmPackages = listOf("yaml-language-server"),
                commands = mapOf(EditorLspManager.LANGUAGE_YAML to "yaml-language-server --stdio")
            )
        )

        fun baseDir(): File {
            return File(TermuxConstants.TERMUX_HOME_DIR, ".zerotermux/editor-lsp")
        }

        fun buildPath(existingPath: String?): String {
            val parts = arrayListOf(
                TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,
                "/system/bin",
                "/system/xbin"
            )
            existingPath?.split(':')?.filterTo(parts) { it.isNotBlank() }
            return parts.distinct().joinToString(":")
        }

        fun packageById(packageId: String): ServerPackage? {
            return PACKAGES.firstOrNull { it.id == packageId }
        }
    }
}
