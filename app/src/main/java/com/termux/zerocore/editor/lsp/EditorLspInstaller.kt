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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class EditorLspInstaller(private val context: Context) {
    data class ServerPackage(
        val id: String,
        val displayName: String,
        val description: String,
        val languageIds: List<String>,
        val npmPackages: List<String>,
        val commands: Map<String, String>,
        val requiredOnFirstOpen: Boolean = false,
        /** npm | jdtls 等自定义安装 */
        val installKind: String = INSTALL_KIND_NPM
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

    /**
     * 安装 LSP。若上一次安装被 ^C 中断，包仍可能停在「正在安装」；
     * 再次调用会取消旧等待并重新往终端发送安装命令。
     */
    fun installPackage(packageId: String, quietIfInstalled: Boolean = false, onFinished: ((Boolean, String) -> Unit)? = null) {
        val serverPackage = packageById(packageId) ?: return
        if (isPackageInstalled(packageId)) {
            if (!quietIfInstalled) postMessage("${serverPackage.displayName} 已安装")
            onFinished?.invoke(true, "installed")
            return
        }
        val cancelFlag = AtomicBoolean(false)
        var restarted = false
        synchronized(installingPackages) {
            val previous = installCancelFlags[packageId]
            if (previous != null || packageId in installingPackages) {
                // 取消上一次卡在 waitForCondition 的线程，允许重新发终端命令
                previous?.set(true)
                restarted = true
            }
            installingPackages.add(packageId)
            installCancelFlags[packageId] = cancelFlag
        }
        if (restarted) {
            if (!quietIfInstalled) postMessage("重新开始安装 ${serverPackage.displayName}")
        } else if (!quietIfInstalled) {
            postMessage("正在安装 ${serverPackage.displayName}")
        }
        Thread {
            val result = runCatching {
                installPackageWorker(serverPackage, cancelFlag, interruptFirst = restarted)
            }
            val stillOwner = synchronized(installingPackages) {
                if (installCancelFlags[packageId] === cancelFlag) {
                    installingPackages.remove(packageId)
                    installCancelFlags.remove(packageId)
                    true
                } else {
                    false
                }
            }
            if (!stillOwner || cancelFlag.get()) {
                // 已被新一轮安装取代，或主动取消：不写 marker、不弹完成
                if (stillOwner && cancelFlag.get()) {
                    mainHandler.post { onFinished?.invoke(false, "cancelled") }
                }
                return@Thread
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

    /** 取消指定包的安装等待（例如用户 ^C 后想重新点安装）。 */
    fun cancelInstall(packageId: String) {
        synchronized(installingPackages) {
            installCancelFlags[packageId]?.set(true)
            installingPackages.remove(packageId)
            installCancelFlags.remove(packageId)
        }
    }

    fun isInstalling(packageId: String): Boolean {
        synchronized(installingPackages) {
            return installingPackages.contains(packageId)
        }
    }

    fun isPackageInstalled(packageId: String): Boolean {
        val serverPackage = packageById(packageId) ?: return false
        when (serverPackage.installKind) {
            INSTALL_KIND_JDTLS -> return EditorJdtLsSupport.isInstalled()
            INSTALL_KIND_CLANGD -> return EditorClangdSupport.isInstalled()
        }
        return serverPackage.commands.values.all { command ->
            EditorLspCommandResolver.isCommandAvailable(command)
        }
    }

    fun isLanguageInstalled(languageId: String): Boolean {
        return PACKAGES.any { serverPackage ->
            languageId in serverPackage.languageIds && isPackageInstalled(serverPackage.id)
        }
    }

    fun commandForLanguage(languageId: String): String? {
        return launchSpecForLanguage(languageId)?.let { spec ->
            (listOf(spec.executable) + spec.arguments).joinToString(" ")
        }
    }

    fun launchSpecForLanguage(languageId: String, projectRoot: File? = null): EditorLspLaunchSpec? {
        val serverPackage = PACKAGES.firstOrNull { pkg ->
            languageId in pkg.languageIds && isPackageInstalled(pkg.id)
        } ?: return null
        when (serverPackage.installKind) {
            INSTALL_KIND_JDTLS -> return EditorJdtLsSupport.resolveLaunchSpec(projectRoot)
            INSTALL_KIND_CLANGD -> return EditorClangdSupport.resolveLaunchSpec(projectRoot)
        }
        val raw = serverPackage.commands[languageId] ?: return null
        return EditorLspCommandResolver.resolveLaunchSpec(raw)
    }

    private fun installPackageWorker(
        serverPackage: ServerPackage,
        cancelFlag: AtomicBoolean,
        interruptFirst: Boolean
    ) {
        when (serverPackage.installKind) {
            INSTALL_KIND_JDTLS -> {
                installJdtLs(serverPackage, cancelFlag, interruptFirst)
                return
            }
            INSTALL_KIND_CLANGD -> {
                installClangd(serverPackage, cancelFlag, interruptFirst)
                return
            }
        }
        ensureNpmReady(cancelFlag, interruptFirst)
        throwIfCancelled(cancelFlag)
        if (canUseTerminal()) {
            sendLspInstallToTerminal(serverPackage, interruptFirst)
            if (!waitForCondition({ isPackageInstalled(serverPackage.id) }, LSP_INSTALL_WAIT_MS, cancelFlag)) {
                throwIfCancelled(cancelFlag)
                throw IllegalStateException("终端安装超时，请在 Termux 中确认命令是否执行完成")
            }
        } else {
            installPackageBlocking(serverPackage)
        }
    }

    private fun installJdtLs(
        serverPackage: ServerPackage,
        cancelFlag: AtomicBoolean,
        interruptFirst: Boolean
    ) {
        val script = EditorJdtLsSupport.installShellScript()
        if (canUseTerminal()) {
            prepareTerminalForInstall(interruptFirst)
            sendToTerminal("echo '[ZeroTermux Editor] Installing LSP: ${serverPackage.displayName}'\n")
            val scriptFile = File(baseDir(), "install-jdtls.sh")
            baseDir().mkdirs()
            scriptFile.writeText(script + "\n")
            sendToTerminal("sh ${shellQuote(scriptFile.absolutePath)}\n")
            if (!waitForCondition({ isPackageInstalled(serverPackage.id) }, JDTLS_INSTALL_WAIT_MS, cancelFlag)) {
                throwIfCancelled(cancelFlag)
                throw IllegalStateException("jdt-ls 安装超时，请在 Termux 中确认下载/解压是否完成，并已安装 openjdk-21")
            }
        } else {
            runShellScript(script)
            if (!isPackageInstalled(serverPackage.id)) {
                throw IllegalStateException("jdt-ls 未就绪：请确认 openjdk-21 与 ~/.zerotermux/editor-lsp/jdtls 已安装")
            }
        }
    }

    private fun installClangd(
        serverPackage: ServerPackage,
        cancelFlag: AtomicBoolean,
        interruptFirst: Boolean
    ) {
        val script = EditorClangdSupport.installShellScript()
        if (canUseTerminal()) {
            prepareTerminalForInstall(interruptFirst)
            sendToTerminal("echo '[ZeroTermux Editor] Installing LSP: ${serverPackage.displayName}'\n")
            val scriptFile = File(baseDir(), "install-clangd.sh")
            baseDir().mkdirs()
            scriptFile.writeText(script + "\n")
            sendToTerminal("sh ${shellQuote(scriptFile.absolutePath)}\n")
            if (!waitForCondition({ isPackageInstalled(serverPackage.id) }, CLANGD_INSTALL_WAIT_MS, cancelFlag)) {
                throwIfCancelled(cancelFlag)
                throw IllegalStateException("clangd 安装超时，请在 Termux 中确认: pkg install -y clang")
            }
        } else {
            runShellScript(script)
            if (!isPackageInstalled(serverPackage.id)) {
                throw IllegalStateException("clangd 未就绪：请执行 pkg install -y clang")
            }
        }
    }

    private fun ensureNpmReady(cancelFlag: AtomicBoolean, interruptFirst: Boolean) {
        if (isNpmInstalled()) return
        if (canUseTerminal()) {
            mainHandler.post {
                postMessage("正在通过 Termux 终端安装 Node.js / npm…")
            }
            prepareTerminalForInstall(interruptFirst)
            sendNpmInstallToTerminal()
            if (!waitForCondition({ isNpmInstalled() }, NPM_INSTALL_WAIT_MS, cancelFlag)) {
                throwIfCancelled(cancelFlag)
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

    /**
     * 重新安装前尽量打断卡在 pkg/npm 的进程，再回车回到提示符。
     * （对应用户在终端按 ^C 后再次点安装的场景。）
     * 须在后台线程调用（含短暂 sleep）。
     */
    private fun prepareTerminalForInstall(interruptFirst: Boolean = false) {
        val listener = SingletonCommunicationUtils.getInstance()
            .getmSingletonCommunicationListener()
        if (interruptFirst) {
            listener.sendTextToTerminalCtrl("c", true)
            try {
                Thread.sleep(400)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        listener.sendTextToTerminal("\n")
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

    private fun sendLspInstallToTerminal(serverPackage: ServerPackage, interruptFirst: Boolean) {
        prepareTerminalForInstall(interruptFirst)
        val npmPackages = serverPackage.npmPackages.joinToString(" ")
        sendToTerminal("echo '[ZeroTermux Editor] Installing LSP: ${serverPackage.displayName}'\n")
        sendToTerminal(
            "mkdir -p ~/.zerotermux/editor-lsp && cd ~/.zerotermux/editor-lsp && " +
                "(test -f package.json || npm init -y >/dev/null 2>&1 || true) && " +
                "npm install --no-audit --no-fund --save $npmPackages\n"
        )
    }

    private fun waitForCondition(
        check: () -> Boolean,
        timeoutMs: Long,
        cancelFlag: AtomicBoolean
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (cancelFlag.get()) return false
            if (check()) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return !cancelFlag.get() && check()
    }

    private fun throwIfCancelled(cancelFlag: AtomicBoolean) {
        if (cancelFlag.get()) {
            throw InterruptedException("安装已取消，可重新点击安装")
        }
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
        val base = baseDir()
        base.mkdirs()
        if (!isNpmInstalled()) {
            installNpmBlocking()
        }
        val npmPackages = serverPackage.npmPackages.joinToString(" ") { shellQuote(it) }
        val script = """
            set -e
            export HOME=${shellQuote(TermuxConstants.TERMUX_HOME_DIR_PATH)}
            export PREFIX=${shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)}
            export PATH=${shellQuote(buildPath(null))}
            cd ${shellQuote(base.absolutePath)}
            if [ ! -f package.json ]; then
                npm init -y >/dev/null 2>&1 || echo '{"name":"zerotermux-editor-lsp","private":true}' > package.json
            fi
            npm install --no-audit --no-fund --save $npmPackages
        """.trimIndent()
        runShellScript(script)
        if (!isPackageInstalled(serverPackage.id)) {
            throw IllegalStateException("LSP 命令未就绪，请检查 ~/.zerotermux/editor-lsp 安装结果")
        }
    }

    private fun runShellScript(script: String) {
        val shell = File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "sh")
        val shellPath = if (shell.canExecute()) shell.absolutePath else "/system/bin/sh"
        val processBuilder = ProcessBuilder(shellPath, "-lc", script)
        processBuilder.directory(TermuxConstants.TERMUX_HOME_DIR)
        processBuilder.redirectErrorStream(true)
        processBuilder.environment().putAll(TermuxShellEnvironment().getEnvironment(context, false))
        processBuilder.environment()["PATH"] = EditorLspCommandResolver.buildPath(processBuilder.environment()["PATH"])
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
        return EditorLspCommandResolver.resolveExecutablePath(commandName) != null
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
        const val PYTHON_PACKAGE_ID = "python"
        const val INSTALL_KIND_NPM = "npm"
        const val INSTALL_KIND_JDTLS = "jdtls"
        const val INSTALL_KIND_CLANGD = "clangd"
        private const val MAX_OUTPUT_LENGTH = 4000
        private const val POLL_INTERVAL_MS = 2000L
        private const val NPM_INSTALL_WAIT_MS = 10 * 60 * 1000L
        private const val LSP_INSTALL_WAIT_MS = 15 * 60 * 1000L
        private const val JDTLS_INSTALL_WAIT_MS = 30 * 60 * 1000L
        private const val CLANGD_INSTALL_WAIT_MS = 30 * 60 * 1000L
        private val installingPackages = LinkedHashSet<String>()
        private val installCancelFlags = ConcurrentHashMap<String, AtomicBoolean>()

        private val PACKAGES = listOf(
            ServerPackage(
                id = SHELL_BASIC_ID,
                displayName = "Shell 基础 LSP (bash/zsh/fish)",
                description = "在 LSP 列表中安装后提供 shell 脚本基础补全",
                languageIds = listOf(EditorLspManager.LANGUAGE_SHELL),
                npmPackages = listOf("bash-language-server"),
                commands = mapOf(EditorLspManager.LANGUAGE_SHELL to "bash-language-server start"),
                requiredOnFirstOpen = false
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
                id = PYTHON_PACKAGE_ID,
                displayName = "Python LSP (Pyright)",
                description = "打开 .py 可提示安装；提供补全与类型/语法诊断（需 npm）",
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
            ),
            ServerPackage(
                id = EditorJdtLsSupport.PACKAGE_ID,
                displayName = "Java LSP (Eclipse JDT.LS)",
                description = "打开 .java 可提示安装；提供类/方法/import 补全与诊断（需 OpenJDK 21+，包体较大）",
                languageIds = listOf(EditorLspManager.LANGUAGE_JAVA),
                npmPackages = emptyList(),
                commands = mapOf(EditorLspManager.LANGUAGE_JAVA to "jdtls --stdio"),
                installKind = INSTALL_KIND_JDTLS
            ),
            ServerPackage(
                id = EditorClangdSupport.PACKAGE_ID,
                displayName = "C/C++ LSP (clangd)",
                description = "打开 .c/.h/.cpp 可提示安装；补全与语法诊断（pkg install clang，体积较大）",
                languageIds = listOf(EditorLspManager.LANGUAGE_C, EditorLspManager.LANGUAGE_CPP),
                npmPackages = emptyList(),
                commands = mapOf(
                    EditorLspManager.LANGUAGE_C to "clangd",
                    EditorLspManager.LANGUAGE_CPP to "clangd"
                ),
                installKind = INSTALL_KIND_CLANGD
            )
        )

        fun baseDir(): File {
            return File(TermuxConstants.TERMUX_HOME_DIR, ".zerotermux/editor-lsp")
        }

        fun buildPath(existingPath: String?): String {
            return EditorLspCommandResolver.buildPath(existingPath)
        }

        fun packageById(packageId: String): ServerPackage? {
            return PACKAGES.firstOrNull { it.id == packageId }
        }
    }
}
