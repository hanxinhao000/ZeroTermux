package com.termux.zerocore.editor

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.utils.SingletonCommunicationUtils
import java.io.File

class EditorProgramRunner(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun canUseTerminal(): Boolean {
        return SingletonCommunicationUtils.getInstance().hasTerminalListener()
    }

    fun isRuntimeInstalled(language: EditorRunLanguage): Boolean {
        return when (language) {
            EditorRunLanguage.JAVA -> commandExists("javac") && commandExists("java")
            EditorRunLanguage.C -> commandExists("gcc") || commandExists("clang") || commandExists("cc")
            EditorRunLanguage.PYTHON -> commandExists("python3") || commandExists("python")
        }
    }

    fun installRuntimeViaTerminal(language: EditorRunLanguage, onFinished: () -> Unit) {
        when (language) {
            EditorRunLanguage.JAVA -> {
                sendToTerminal("echo '[ZeroTermux Editor] Installing OpenJDK...'\n")
                sendToTerminal("pkg install -y openjdk-17 || pkg install -y openjdk-21 || pkg install -y openjdk\n")
            }
            EditorRunLanguage.C -> {
                sendToTerminal("echo '[ZeroTermux Editor] Installing C compiler (clang)...'\n")
                sendToTerminal("pkg install -y clang || pkg install -y gcc\n")
            }
            EditorRunLanguage.PYTHON -> {
                sendToTerminal("echo '[ZeroTermux Editor] Installing Python...'\n")
                sendToTerminal("pkg install -y python\n")
            }
        }
        mainHandler.post { onFinished() }
    }

    fun runProgram(language: EditorRunLanguage, file: File, source: String) {
        val directory = shellQuote(file.parentFile?.absolutePath ?: TermuxConstants.TERMUX_HOME_DIR_PATH)
        val sourceName = shellQuote(file.name)
        when (language) {
            EditorRunLanguage.JAVA -> {
                val className = shellQuote(EditorRunDetector.inferJavaClassName(source, file.name))
                sendToTerminal("echo '[ZeroTermux Editor] Build & run ${file.name}'\n")
                sendToTerminal("cd $directory && javac $sourceName && java $className\n")
            }
            EditorRunLanguage.C -> {
                val binary = EditorRunDetector.inferCBinaryName(file.name)
                val binaryQuoted = shellQuote(binary)
                val runPath = shellQuote("./$binary")
                val compiler = if (commandExists("gcc")) "gcc" else if (commandExists("clang")) "clang" else "cc"
                sendToTerminal("echo '[ZeroTermux Editor] Build & run ${file.name}'\n")
                sendToTerminal("cd $directory && $compiler $sourceName -o $binaryQuoted && $runPath\n")
            }
            EditorRunLanguage.PYTHON -> {
                val python = if (commandExists("python3")) "python3" else "python"
                sendToTerminal("echo '[ZeroTermux Editor] Run ${file.name}'\n")
                sendToTerminal("cd $directory && $python $sourceName\n")
            }
        }
    }

    private fun sendToTerminal(command: String) {
        SingletonCommunicationUtils.getInstance()
            .getmSingletonCommunicationListener()
            .sendTextToTerminal(command)
    }

    private fun commandExists(commandName: String): Boolean {
        return File(TermuxConstants.TERMUX_BIN_PREFIX_DIR, commandName).canExecute()
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
