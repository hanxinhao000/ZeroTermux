package com.termux.zerocore.editor

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.utils.SingletonCommunicationUtils
import java.io.File

class EditorAndroidRunner(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun canUseTerminal(): Boolean {
        return SingletonCommunicationUtils.getInstance().hasTerminalListener()
    }

    fun isGradleEnvInstalled(): Boolean {
        return commandExists("java") && File(TermuxConstants.TERMUX_HOME_DIR_PATH, "android-sdk/platforms").isDirectory
    }

    fun installGradleEnvViaTerminal(onFinished: () -> Unit) {
        sendToTerminal("echo '[ZeroTermux Editor] Installing Android build environment...'\n")
        sendToTerminal("pkg install -y openjdk-17 || pkg install -y openjdk-21 || pkg install -y openjdk\n")
        sendToTerminal("pkg install -y gradle aapt aapt2 wget git unzip\n")
        sendToTerminal(
            "if [ ! -d \"\$HOME/android-sdk/platforms\" ]; then\n" +
                "  echo '[ZeroTermux Editor] Downloading Android SDK (first run may take a while)...'\n" +
                "  if [ -f \"\$PREFIX/share/termux-packages/scripts/setup-android-sdk.sh\" ]; then\n" +
                "    bash \"\$PREFIX/share/termux-packages/scripts/setup-android-sdk.sh\"\n" +
                "  else\n" +
                "    rm -rf \"\$HOME/.cache/zt-termux-packages\"\n" +
                "    git clone --depth=1 https://github.com/termux/termux-packages.git " +
                "\"\$HOME/.cache/zt-termux-packages\" && " +
                "bash \"\$HOME/.cache/zt-termux-packages/scripts/setup-android-sdk.sh\"\n" +
                "  fi\n" +
                "fi\n"
        )
        sendToTerminal(
            "mkdir -p \"\$HOME/.gradle\" && " +
                "if ! grep -q 'android.aapt2FromMavenOverride' \"\$HOME/.gradle/gradle.properties\" 2>/dev/null; then\n" +
                "  echo 'android.aapt2FromMavenOverride=\$PREFIX/bin/aapt2' >> \"\$HOME/.gradle/gradle.properties\"\n" +
                "fi\n"
        )
        mainHandler.post { onFinished() }
    }

    fun prepareAndBuild(projectRoot: File) {
        AndroidProjectManager.repairProjectGradleFiles(context, projectRoot)
        val directory = shellQuote(projectRoot.absolutePath)
        val home = shellQuote(TermuxConstants.TERMUX_HOME_DIR_PATH)
        val prefix = shellQuote(TermuxConstants.TERMUX_PREFIX_DIR_PATH)
        sendToTerminal("echo '[ZeroTermux Editor] Gradle build ${projectRoot.name}'\n")
        sendToTerminal(
            "mkdir -p \"\$HOME/.gradle\" && " +
                "if ! grep -q 'android.aapt2FromMavenOverride' \"\$HOME/.gradle/gradle.properties\" 2>/dev/null; then\n" +
                "  echo 'android.aapt2FromMavenOverride=\$PREFIX/bin/aapt2' >> \"\$HOME/.gradle/gradle.properties\"\n" +
                "fi && " +
                "ANDROID_HOME=\"\${ANDROID_HOME:-$home/android-sdk}\" && " +
                "[ -d \"\$ANDROID_HOME/platforms\" ] || ANDROID_HOME=\"$prefix/share/android-sdk\" && " +
                "echo \"sdk.dir=\$ANDROID_HOME\" > $directory/local.properties && " +
                "cd $directory && chmod +x ./gradlew && ./gradlew assembleDebug\n"
        )
    }

    private fun sendToTerminal(command: String) {
        SingletonCommunicationUtils.getInstance()
            .getmSingletonCommunicationListener()
            .sendTextToTerminal(command)
    }

    private fun commandExists(commandName: String): Boolean {
        return File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, commandName).canExecute()
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
